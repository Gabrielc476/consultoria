package br.com.govflow.whatsapp.routing;

import br.com.govflow.whatsapp.domain.entity.ContatoConvenioEntity;
import br.com.govflow.whatsapp.domain.entity.ContatoEntity;
import br.com.govflow.whatsapp.domain.entity.ContatoPrefeituraEntity;
import br.com.govflow.whatsapp.domain.repository.ContatoConvenioRepository;
import br.com.govflow.whatsapp.domain.repository.ContatoPrefeituraRepository;
import br.com.govflow.whatsapp.domain.repository.ContatoRepository;
import br.com.govflow.whatsapp.routing.service.ContactResolutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactResolutionServiceTest {

    @Mock
    private ContatoRepository contatoRepository;

    @Mock
    private ContatoConvenioRepository contatoConvenioRepository;

    @Mock
    private ContatoPrefeituraRepository legacyContatoRepository;

    private ContactResolutionService contactResolutionService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID prefeituraId1 = UUID.randomUUID();
    private final UUID prefeituraId2 = UUID.randomUUID();
    private final UUID convenioId1 = UUID.randomUUID();
    private final UUID convenioId2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        contactResolutionService = new ContactResolutionService(
                contatoRepository,
                contatoConvenioRepository,
                legacyContatoRepository
        );
    }

    @Test
    @DisplayName("Deve resolver contato 1:N com múltiplos convênios e definir prefeitura do convênio principal")
    void deveResolverContato1ParaNComSucesso() {
        String phone = "5583999998888";
        ContatoEntity contato = new ContatoEntity(
                tenantId, phone, "Eng. Roberto Farias", "FISCAL_ENGENHEIRO", "Construtora Alvorada"
        );
        UUID contatoId = UUID.randomUUID();
        contato.setId(contatoId);

        ContatoConvenioEntity vinculo1 = new ContatoConvenioEntity(contatoId, convenioId1, prefeituraId1, "FISCAL_TITULAR", true);
        ContatoConvenioEntity vinculo2 = new ContatoConvenioEntity(contatoId, convenioId2, prefeituraId2, "RESPONSAVEL_TECNICO", false);

        when(contatoRepository.findByPhoneNumberAndAtivoTrue(phone)).thenReturn(Optional.of(contato));
        when(contatoConvenioRepository.findByIdContatoId(contatoId)).thenReturn(List.of(vinculo1, vinculo2));

        ContactResolutionService.ResolvedContact result = contactResolutionService.resolve(phone);

        assertTrue(result.resolved());
        assertFalse(result.remetenteNovo());
        assertEquals(tenantId, result.tenantId());
        assertEquals(prefeituraId1, result.prefeituraId(), "Deve adotar a prefeitura do convênio principal");
        assertEquals("Eng. Roberto Farias", result.nomeContato());
        assertEquals(2, result.conveniosCandidatos().size());
        assertEquals(convenioId1, result.conveniosCandidatos().get(0).convenioId());
        assertEquals(convenioId2, result.conveniosCandidatos().get(1).convenioId());
    }

    @Test
    @DisplayName("Deve resolver contato tratando a variação do nono dígito móvel brasileiro no modelo 1:N")
    void deveResolverContatoComVariacaoNonoDigito() {
        String phoneReceived = "5583988884444";
        String phoneInDb = "558388884444";

        ContatoEntity contato = new ContatoEntity(
                tenantId, phoneInDb, "Secretário de Obras", "SECRETARIO_MUNICIPAL", "Prefeitura de Monteiro"
        );
        UUID contatoId = UUID.randomUUID();
        contato.setId(contatoId);

        when(contatoRepository.findByPhoneNumberAndAtivoTrue(phoneReceived)).thenReturn(Optional.empty());
        when(contatoRepository.findByPhoneNumberAndAtivoTrue(phoneInDb)).thenReturn(Optional.of(contato));
        when(contatoConvenioRepository.findByIdContatoId(contatoId)).thenReturn(List.of());

        ContactResolutionService.ResolvedContact result = contactResolutionService.resolve(phoneReceived);

        assertTrue(result.resolved());
        assertFalse(result.remetenteNovo());
        assertEquals("Secretário de Obras", result.nomeContato());
    }

    @Test
    @DisplayName("Deve resolver via fallback legado se não existir em tb_contatos")
    void deveResolverViaFallbackLegado() {
        String phone = "5583911112222";
        ContatoPrefeituraEntity legacy = new ContatoPrefeituraEntity(
                tenantId, prefeituraId1, phone, "Contato Legado", "CARGO_LEGADO", "DEPTO_LEGADO"
        );

        when(contatoRepository.findByPhoneNumberAndAtivoTrue(phone)).thenReturn(Optional.empty());
        when(legacyContatoRepository.findByPhoneNumberAndAtivoTrue(phone)).thenReturn(Optional.of(legacy));

        ContactResolutionService.ResolvedContact result = contactResolutionService.resolve(phone);

        assertTrue(result.resolved());
        assertFalse(result.remetenteNovo());
        assertEquals(prefeituraId1, result.prefeituraId());
        assertEquals("Contato Legado", result.nomeContato());
    }

    @Test
    @DisplayName("Deve retornar remetenteNovo=true e resolved=false para número desconhecido")
    void deveRetornarRemetenteNovoParaNumeroDesconhecido() {
        String unknownPhone = "5583977771111";

        when(contatoRepository.findByPhoneNumberAndAtivoTrue(anyString())).thenReturn(Optional.empty());
        when(legacyContatoRepository.findByPhoneNumberAndAtivoTrue(anyString())).thenReturn(Optional.empty());

        ContactResolutionService.ResolvedContact result = contactResolutionService.resolve(unknownPhone);

        assertFalse(result.resolved());
        assertTrue(result.remetenteNovo());
        assertNull(result.tenantId());
        assertNull(result.prefeituraId());
        assertTrue(result.conveniosCandidatos().isEmpty());
    }
}
