package br.com.govflow.core.application.service;

import br.com.govflow.core.application.port.in.ConsultarDocumentoUseCase;
import br.com.govflow.core.application.port.out.AuditoriaRevisaoRepositoryPort;
import br.com.govflow.core.application.port.out.DocumentoRepositoryPort;
import br.com.govflow.core.domain.model.AuditoriaRevisao;
import br.com.govflow.core.domain.model.Documento;
import br.com.govflow.core.domain.model.StatusDocumento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ConsultarDocumentoService implements ConsultarDocumentoUseCase {

    private final DocumentoRepositoryPort documentoRepository;
    private final AuditoriaRevisaoRepositoryPort auditoriaRepository;

    public ConsultarDocumentoService(DocumentoRepositoryPort documentoRepository,
                                    AuditoriaRevisaoRepositoryPort auditoriaRepository) {
        this.documentoRepository = documentoRepository;
        this.auditoriaRepository = auditoriaRepository;
    }

    @Override
    public Optional<Documento> buscarPorId(UUID id) {
        return documentoRepository.buscarPorId(id);
    }

    @Override
    public List<Documento> listar(int page, int size, StatusDocumento status) {
        return documentoRepository.listar(page, size, status);
    }

    @Override
    public long contar(StatusDocumento status) {
        return documentoRepository.contarPorStatus(status);
    }

    @Override
    public List<AuditoriaRevisao> listarAuditorias(UUID documentoId) {
        return auditoriaRepository.listarPorDocumentoId(documentoId);
    }
}
