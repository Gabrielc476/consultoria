package br.com.govflow.whatsapp.routing;

import br.com.govflow.whatsapp.domain.entity.ContatoPrefeituraEntity;
import br.com.govflow.whatsapp.domain.repository.ContatoPrefeituraRepository;
import br.com.govflow.whatsapp.routing.service.ContactResolutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactResolutionServiceTest {

    @Mock
    private ContatoPrefeituraRepository contatoRepository;

    private ContactResolutionService contactResolutionService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID prefeituraId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        contactResolutionService = new ContactResolutionService(contatoRepository);
    }

    @Test
    @DisplayName("Deve resolver contato com correspondência exata de telefone E.164")
    void deveResolverContatoComSucessoExato() {
        String phone = "5583999999999";
        ContatoPrefeituraEntity contato = new ContatoPrefeituraEntity(
                tenantId, prefeituraId, phone, "Cícero Lucena", "PREFEITO", "Gabinete"
        );

        when(contatoRepository.findByPhoneNumberAndAtivoTrue(phone)).thenReturn(Optional.of(contato));

        ContactResolutionService.ResolvedContact result = contactResolutionService.resolve(phone);

        assertTrue(result.resolved());
        assertEquals(tenantId, result.tenantId());
        assertEquals(prefeituraId, result.prefeituraId());
        assertEquals("Cícero Lucena", result.nomeContato());
    }

    @Test
    @DisplayName("Deve resolver contato tratando a variação do nono dígito móvel brasileiro")
    void deveResolverContatoComVariacaoNonoDigito() {
        // Telefone recebido com 9 dígitos: 5583988884444
        // Cadastrado no banco sem o 9: 558388884444
        String phoneReceived = "5583988884444";
        String phoneInDb = "558388884444";

        ContatoPrefeituraEntity contato = new ContatoPrefeituraEntity(
                tenantId, prefeituraId, phoneInDb, "Secretário de Obras", "SECRETARIO", "Obras"
        );

        when(contatoRepository.findByPhoneNumberAndAtivoTrue(phoneReceived)).thenReturn(Optional.empty());
        when(contatoRepository.findByPhoneNumberAndAtivoTrue(phoneInDb)).thenReturn(Optional.of(contato));

        ContactResolutionService.ResolvedContact result = contactResolutionService.resolve(phoneReceived);

        assertTrue(result.resolved());
        assertEquals(prefeituraId, result.prefeituraId());
        assertEquals("Secretário de Obras", result.nomeContato());
    }

    @Test
    @DisplayName("Deve retornar não resolvido quando número não estiver cadastrado no banco")
    void deveRetornarNaoResolvidoParaNumeroDesconhecido() {
        String unknownPhone = "5583977771111";

        when(contatoRepository.findByPhoneNumberAndAtivoTrue(anyString())).thenReturn(Optional.empty());

        ContactResolutionService.ResolvedContact result = contactResolutionService.resolve(unknownPhone);

        assertFalse(result.resolved());
        assertNull(result.tenantId());
        assertNull(result.prefeituraId());
    }
}
