package br.com.govflow.core.infrastructure.adapter.in.amqp;

import br.com.govflow.core.application.port.out.DocumentoRepositoryPort;
import br.com.govflow.core.domain.model.ArmazenamentoArquivo;
import br.com.govflow.core.domain.model.Documento;
import br.com.govflow.core.domain.model.documento.CategoriaDocumento;
import br.com.govflow.core.domain.model.documento.DocumentoAuditoria;
import br.com.govflow.core.domain.model.documento.FaseCicloVida;
import br.com.govflow.core.domain.model.documento.OrigemCanal;
import br.com.govflow.core.infrastructure.adapter.in.amqp.dto.DocumentoClassificadoEventDto;
import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.TriagemInboxJpaEntity;
import br.com.govflow.core.infrastructure.adapter.out.persistence.repository.SpringDataTriagemInboxRepository;
import br.com.govflow.core.infrastructure.interceptor.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.ConsultoriaJpaEntity;
import br.com.govflow.core.infrastructure.adapter.out.persistence.repository.SpringDataConsultoriaRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Component
public class DocumentoClassificadoListener {

    private static final Logger log = LoggerFactory.getLogger(DocumentoClassificadoListener.class);
    private static final BigDecimal LIMIAR_CONFIANCA_DIRETA = new BigDecimal("0.90");

    private final DocumentoRepositoryPort documentoRepository;
    private final SpringDataTriagemInboxRepository triagemInboxRepository;
    private final SpringDataConsultoriaRepository consultoriaRepository;

    public DocumentoClassificadoListener(
            DocumentoRepositoryPort documentoRepository,
            SpringDataTriagemInboxRepository triagemInboxRepository,
            SpringDataConsultoriaRepository consultoriaRepository
    ) {
        this.documentoRepository = documentoRepository;
        this.triagemInboxRepository = triagemInboxRepository;
        this.consultoriaRepository = consultoriaRepository;
    }

    @RabbitListener(queues = "${govflow.rabbitmq.queue-documentos-classificados:fila.documentos.classificados}")
    public void onDocumentoClassificado(DocumentoClassificadoEventDto event) {
        log.info("Recebido DocumentoClassificadoEvent: eventId={}, correlationId={}",
                event != null ? event.eventId() : null,
                event != null ? event.correlationId() : null);

        if (event == null) {
            log.error("DocumentoClassificadoEvent nulo. Rejeitando para DLQ.");
            throw new AmqpRejectAndDontRequeueException("Payload nulo.");
        }

        UUID tenantId = event.getEfetivoTenantId();
        if (tenantId == null || tenantId.equals(new UUID(0L, 0L))) {
            tenantId = consultoriaRepository.findAll().stream()
                    .filter(c -> "ATIVO".equalsIgnoreCase(c.getStatus()))
                    .sorted((c1, c2) -> {
                        if ("Consultoria Teste Real".equalsIgnoreCase(c1.getNomeFantasia()) || "Consultoria Teste Real".equalsIgnoreCase(c1.getRazaoSocial())) return -1;
                        if ("Consultoria Teste Real".equalsIgnoreCase(c2.getNomeFantasia()) || "Consultoria Teste Real".equalsIgnoreCase(c2.getRazaoSocial())) return 1;
                        return 0;
                    })
                    .map(ConsultoriaJpaEntity::getId)
                    .findFirst()
                    .orElse(null);
            log.info("TenantId não informado ou nulo; associado ao tenant da consultoria ativa: {}", tenantId);
        }

        if (tenantId == null) {
            log.error("DocumentoClassificadoEvent sem tenantId válido e nenhuma consultoria encontrada no banco. Rejeitando para DLQ.");
            throw new AmqpRejectAndDontRequeueException("TenantId inválido e sem consultoria ativa.");
        }

        TenantContext.setCurrentTenant(tenantId);
        try {
            UUID docId = event.getEfetivoDocumentoId();
            FaseCicloVida fase = FaseCicloVida.fromCodigoOuNome(event.getEfetivoFaseSugerida());
            CategoriaDocumento categoria = CategoriaDocumento.fromString(event.getEfetivoCategoriaSugerida());

            BigDecimal score = event.getEfetivoConfidenceScore();
            UUID convenioId = event.getEfetivoConvenioId();
            boolean direcionarTriagem = event.isEfetivoDirecionarTriagem();
            boolean remetenteNovo = event.isEfetivoRemetenteNovo();

            // REGRA MANDATÓRIA: Apenas documentos de contatos vinculados a convênios são aceitos no sistema.
            // Se for remetente novo não vinculado ou sem convênio elegível, descarta sumariamente. O resto não passa pelo sistema.
            if (remetenteNovo || (convenioId == null && !direcionarTriagem) || score.compareTo(BigDecimal.ZERO) <= 0) {
                log.info("Documento {} descartado: remetente {} não é contato vinculado a convênio. O resto não passa pelo sistema.",
                        docId, event.getEfetivoRemetentePhone());
                return;
            }

            boolean altaConfianca = score.compareTo(LIMIAR_CONFIANCA_DIRETA) > 0
                    && convenioId != null
                    && !direcionarTriagem
                    && !remetenteNovo;

            String pastaVirtual = altaConfianca
                    ? (event.pastaVirtualSugerida() != null && !event.pastaVirtualSugerida().isBlank()
                        ? event.pastaVirtualSugerida()
                        : "/" + fase.getNomePasta())
                    : "/triagem";

            ArmazenamentoArquivo armazenamento = new ArmazenamentoArquivo(
                    event.getEfetivoS3Bucket(),
                    event.getEfetivoS3Key(),
                    event.getEfetivoNomeArquivoOriginal(),
                    event.getEfetivoContentType(),
                    event.getEfetivoTamanhoBytes()
            );

            Optional<Documento> docExistente = documentoRepository.buscarPorId(docId);
            Documento documento;
            if (docExistente.isPresent()) {
                documento = docExistente.get();
                documento.atualizarClassificacao(fase, categoria, pastaVirtual);
                if (armazenamento != null && (documento.getNomeArquivoOriginal() == null || "documento.pdf".equalsIgnoreCase(documento.getNomeArquivoOriginal()))) {
                    documento.atualizarArmazenamento(armazenamento);
                }
            } else {
                documento = Documento.criarNovo(
                        docId,
                        tenantId,
                        event.getEfetivoPrefeituraId(),
                        convenioId,
                        fase,
                        categoria,
                        pastaVirtual,
                        armazenamento,
                        event.getEfetivoHashSha256(),
                        OrigemCanal.WHATSAPP,
                        event.tags(),
                        event.metadados(),
                        null
                );
            }

            Documento salvo = documentoRepository.salvar(documento);

            String acaoAuditoria = altaConfianca ? "CLASSIFICACAO_IA" : "ENCAMINHADO_TRIAGEM";
            String justificativa = String.format("Classificação multimodal IA com score de confiança %s. Canal: %s. Remetente: %s",
                    score, OrigemCanal.WHATSAPP, event.getEfetivoRemetentePhone() != null ? event.getEfetivoRemetentePhone() : "Não informado");

            DocumentoAuditoria auditoria = DocumentoAuditoria.registrar(
                    tenantId,
                    salvo.getId(),
                    null,
                    acaoAuditoria,
                    justificativa,
                    null,
                    "{\"fase\": \"" + fase.name() + "\", \"categoria\": \"" + categoria.name() + "\", \"pastaVirtual\": \"" + pastaVirtual + "\"}"
            );
            documentoRepository.salvarAuditoria(auditoria);

            if (!altaConfianca && triagemInboxRepository != null) {
                TriagemInboxJpaEntity inbox = new TriagemInboxJpaEntity();
                inbox.setId(UUID.randomUUID());
                inbox.setTenantId(tenantId);
                inbox.setDocumentoId(salvo.getId());
                inbox.setMensagemInboundId(event.getEfetivoMensagemInboundId());
                inbox.setConvenioSugeridoId(convenioId);
                inbox.setFaseSugerida(fase.name());
                inbox.setConfidenceScore(score);
                inbox.setMotivoAmbiguidade(event.getEfetivoMotivoAmbiguidade());
                inbox.setPhoneNumber(event.getEfetivoRemetentePhone());
                inbox.setSenderName(event.getEfetivoSenderName());
                inbox.setRemetenteNovo(remetenteNovo);
                inbox.setConteudoResumo(event.getEfetivoConteudoResumo());
                inbox.setStatus("PENDENTE");
                triagemInboxRepository.save(inbox);
                log.info("Item criado na Caixa de Triagem (inboxId={}, docId={}, score={})", inbox.getId(), docId, score);
            }

            log.info("Documento {} registrado com sucesso no Ficheiro Digital (altaConfianca={}, pasta={})",
                    docId, altaConfianca, pastaVirtual);
        } catch (Exception e) {
            log.error("Erro ao processar DocumentoClassificadoEvent: eventId={}", event.eventId(), e);
            throw e;
        } finally {
            TenantContext.clear();
        }
    }
}
