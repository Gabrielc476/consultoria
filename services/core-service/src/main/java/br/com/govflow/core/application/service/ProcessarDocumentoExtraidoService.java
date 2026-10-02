package br.com.govflow.core.application.service;

import br.com.govflow.core.application.port.in.ProcessarDocumentoExtraidoUseCase;
import br.com.govflow.core.application.port.out.DocumentoRepositoryPort;
import br.com.govflow.core.domain.model.Documento;
import br.com.govflow.core.domain.model.StatusDocumento;
import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.TriagemInboxJpaEntity;
import br.com.govflow.core.infrastructure.adapter.out.persistence.repository.SpringDataTriagemInboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProcessarDocumentoExtraidoService implements ProcessarDocumentoExtraidoUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessarDocumentoExtraidoService.class);

    private final DocumentoRepositoryPort documentoRepository;
    private final SpringDataTriagemInboxRepository triagemInboxRepository;

    public ProcessarDocumentoExtraidoService(DocumentoRepositoryPort documentoRepository,
                                             SpringDataTriagemInboxRepository triagemInboxRepository) {
        this.documentoRepository = documentoRepository;
        this.triagemInboxRepository = triagemInboxRepository;
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

        Documento salvo = documentoRepository.salvar(documento);

        if (salvo.getStatus() == StatusDocumento.EM_TRIAGEM && triagemInboxRepository != null) {
            boolean jaExiste = triagemInboxRepository.existsByDocumentoId(salvo.getId());
            if (!jaExiste) {
                TriagemInboxJpaEntity inbox = new TriagemInboxJpaEntity();
                inbox.setId(UUID.randomUUID());
                inbox.setTenantId(command.tenantId());
                inbox.setDocumentoId(salvo.getId());
                inbox.setConvenioSugeridoId(command.convenioId());
                inbox.setConfidenceScore(command.extracao() != null ? BigDecimal.valueOf(command.extracao().confidenceScoreGeral()) : BigDecimal.ZERO);
                inbox.setMotivoAmbiguidade("Documento com baixa certeza na extração da IA ou dados não identificados. Encaminhado para a Caixa de Triagem.");
                inbox.setStatus("PENDENTE");
                triagemInboxRepository.save(inbox);
                log.info("Item de triagem criado na Caixa de Triagem para o documento {} (score={})",
                        salvo.getId(), inbox.getConfidenceScore());
            }
        }

        return salvo;
    }
}
