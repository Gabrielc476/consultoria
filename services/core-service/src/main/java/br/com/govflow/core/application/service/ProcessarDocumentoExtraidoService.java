package br.com.govflow.core.application.service;

import br.com.govflow.core.application.port.in.ProcessarDocumentoExtraidoUseCase;
import br.com.govflow.core.application.port.out.DocumentoRepositoryPort;
import br.com.govflow.core.domain.model.Documento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Service
public class ProcessarDocumentoExtraidoService implements ProcessarDocumentoExtraidoUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessarDocumentoExtraidoService.class);

    private final DocumentoRepositoryPort documentoRepository;

    public ProcessarDocumentoExtraidoService(DocumentoRepositoryPort documentoRepository) {
        this.documentoRepository = documentoRepository;
    }

    @Override
    @Transactional
    public Documento processar(ProcessarDocumentoExtraidoCommand command) {
        Objects.requireNonNull(command, "Command de processamento não pode ser nulo.");
        Objects.requireNonNull(command.tenantId(), "TenantId é obrigatório.");
        Objects.requireNonNull(command.documentoId(), "DocumentoId é obrigatório.");

        Optional<Documento> docExistente = documentoRepository.buscarPorId(command.documentoId());

        if (docExistente.isPresent() && docExistente.get().isFinalizado()) {
            Documento doc = docExistente.get();
            log.warn("Documento {} já se encontra no estado terminal {}. Ignorando reprocessamento de extração (idempotência garantida).",
                    doc.getId(), doc.getStatus());
            return doc;
        }

        Documento documento = docExistente.orElseGet(() ->
                Documento.criarRecebido(
                        command.documentoId(),
                        command.tenantId(),
                        command.prefeituraId(),
                        command.convenioId(),
                        command.s3Bucket(),
                        command.s3Key(),
                        command.nomeArquivoOriginal(),
                        command.contentType(),
                        command.tamanhoBytes()
                )
        );

        documento.vincularContextoOperacional(command.prefeituraId(), command.convenioId());
        documento.registrarExtracaoIA(command.extracao(), command.boundingBoxes());

        return documentoRepository.salvar(documento);
    }
}
