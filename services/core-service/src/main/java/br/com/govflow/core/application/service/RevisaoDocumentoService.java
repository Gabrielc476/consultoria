package br.com.govflow.core.application.service;

import br.com.govflow.core.application.port.in.AprovarDocumentoUseCase;
import br.com.govflow.core.application.port.in.RejeitarDocumentoUseCase;
import br.com.govflow.core.application.port.out.AuditoriaRevisaoRepositoryPort;
import br.com.govflow.core.application.port.out.DocumentoEventPublisherPort;
import br.com.govflow.core.application.port.out.DocumentoRepositoryPort;
import br.com.govflow.core.domain.event.DocumentoProntoParaTransferegovEvent;
import br.com.govflow.core.domain.event.DocumentoRejeitadoEvent;
import br.com.govflow.core.domain.exception.DocumentoNaoEncontradoException;
import br.com.govflow.core.domain.model.AuditoriaRevisao;
import br.com.govflow.core.domain.model.Documento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
public class RevisaoDocumentoService implements AprovarDocumentoUseCase, RejeitarDocumentoUseCase {

    private final DocumentoRepositoryPort documentoRepository;
    private final AuditoriaRevisaoRepositoryPort auditoriaRepository;
    private final DocumentoEventPublisherPort eventPublisher;

    public RevisaoDocumentoService(DocumentoRepositoryPort documentoRepository,
                                  AuditoriaRevisaoRepositoryPort auditoriaRepository,
                                  DocumentoEventPublisherPort eventPublisher) {
        this.documentoRepository = documentoRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public ResultadoAprovacao aprovar(AprovarDocumentoCommand command) {
        Objects.requireNonNull(command, "Command de aprovação não pode ser nulo.");
        Objects.requireNonNull(command.documentoId(), "DocumentoId é obrigatório.");

        ExecucaoRevisao execucao = executarCicloRevisao(
                command.documentoId(),
                doc -> doc.aprovar(command.analistaId(), command.revisao(), command.observacao()),
                docSalvo -> eventPublisher.publicarDocumentoPronto(
                        DocumentoProntoParaTransferegovEvent.of(
                                docSalvo.getTenantId(),
                                docSalvo.getId(),
                                command.analistaId()
                        )
                )
        );

        return new ResultadoAprovacao(execucao.documento(), execucao.auditoria());
    }

    @Override
    @Transactional
    public ResultadoRejeicao rejeitar(RejeitarDocumentoCommand command) {
        Objects.requireNonNull(command, "Command de rejeição não pode ser nulo.");
        Objects.requireNonNull(command.documentoId(), "DocumentoId é obrigatório.");

        ExecucaoRevisao execucao = executarCicloRevisao(
                command.documentoId(),
                doc -> doc.rejeitar(command.analistaId(), command.motivo()),
                docSalvo -> eventPublisher.publicarDocumentoRejeitado(
                        DocumentoRejeitadoEvent.of(
                                docSalvo.getTenantId(),
                                docSalvo.getId(),
                                command.analistaId(),
                                command.motivo()
                        )
                )
        );

        return new ResultadoRejeicao(execucao.documento(), execucao.auditoria());
    }

    private ExecucaoRevisao executarCicloRevisao(
            UUID documentoId,
            java.util.function.Function<Documento, AuditoriaRevisao> operacaoDominio,
            java.util.function.Consumer<Documento> publicadorEvento
    ) {
        Documento documento = documentoRepository.buscarPorId(documentoId)
                .orElseThrow(() -> new DocumentoNaoEncontradoException(documentoId));

        AuditoriaRevisao auditoria = operacaoDominio.apply(documento);

        Documento docSalvo = documentoRepository.salvar(documento);
        AuditoriaRevisao auditoriaSalva = auditoriaRepository.salvar(auditoria);

        publicadorEvento.accept(docSalvo);

        return new ExecucaoRevisao(docSalvo, auditoriaSalva);
    }

    private record ExecucaoRevisao(Documento documento, AuditoriaRevisao auditoria) {}
}
