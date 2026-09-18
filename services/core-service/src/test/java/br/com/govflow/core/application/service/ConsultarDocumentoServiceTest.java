package br.com.govflow.core.application.service;

import br.com.govflow.core.application.port.out.AuditoriaRevisaoRepositoryPort;
import br.com.govflow.core.application.port.out.DocumentoRepositoryPort;
import br.com.govflow.core.domain.model.AuditoriaRevisao;
import br.com.govflow.core.domain.model.Documento;
import br.com.govflow.core.domain.model.StatusDocumento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsultarDocumentoServiceTest {

    @Mock
    private DocumentoRepositoryPort documentoRepository;

    @Mock
    private AuditoriaRevisaoRepositoryPort auditoriaRepository;

    @InjectMocks
    private ConsultarDocumentoService service;

    @Test
    @DisplayName("Deve delegar busca por ID e listagem com filtros ao repository")
    void deveDelegarConsultas() {
        UUID docId = UUID.randomUUID();
        Documento doc = Documento.criarRecebido(docId, UUID.randomUUID(), null, null, "b", "k", "n", "m", 1L);

        when(documentoRepository.buscarPorId(docId)).thenReturn(Optional.of(doc));
        when(documentoRepository.listar(0, 10, StatusDocumento.EM_CONFERENCIA)).thenReturn(List.of(doc));
        when(documentoRepository.contarPorStatus(StatusDocumento.EM_CONFERENCIA)).thenReturn(1L);
        when(auditoriaRepository.listarPorDocumentoId(docId)).thenReturn(List.of());

        assertTrue(service.buscarPorId(docId).isPresent());
        assertEquals(1, service.listar(0, 10, StatusDocumento.EM_CONFERENCIA).size());
        assertEquals(1L, service.contar(StatusDocumento.EM_CONFERENCIA));
        assertTrue(service.listarAuditorias(docId).isEmpty());
    }
}
