package br.com.govflow.core.application.service;

import br.com.govflow.core.application.port.in.TriagemUseCase;
import br.com.govflow.core.application.port.out.DocumentoRepositoryPort;
import br.com.govflow.core.domain.exception.AcessoNegadoException;
import br.com.govflow.core.domain.exception.ConvenioNaoEncontradoException;
import br.com.govflow.core.domain.exception.TenantInvalidoException;
import br.com.govflow.core.domain.exception.TriagemItemNaoEncontradoException;
import br.com.govflow.core.domain.model.Documento;
import br.com.govflow.core.domain.model.documento.DocumentoAuditoria;
import br.com.govflow.core.domain.model.documento.FaseCicloVida;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.CadastrarContatoETriarRequest;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.CadastrarContatoRequest;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.response.ContatoConvenioResponse;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.response.ContatoResponse;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.response.TriagemItemResponse;
import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.ContatoConvenioCoreJpaEntity;
import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.ContatoCoreJpaEntity;
import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.ConvenioJpaEntity;
import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.TriagemInboxJpaEntity;
import br.com.govflow.core.infrastructure.adapter.out.persistence.repository.SpringDataContatoConvenioCoreRepository;
import br.com.govflow.core.infrastructure.adapter.out.persistence.repository.SpringDataContatoCoreRepository;
import br.com.govflow.core.infrastructure.adapter.out.persistence.repository.SpringDataConvenioRepository;
import br.com.govflow.core.infrastructure.adapter.out.persistence.repository.SpringDataTriagemInboxRepository;
import br.com.govflow.core.infrastructure.interceptor.UserContext;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TriagemService implements TriagemUseCase {

    private static final Logger log = LoggerFactory.getLogger(TriagemService.class);

    private final SpringDataTriagemInboxRepository triagemInboxRepository;
    private final SpringDataContatoCoreRepository contatoRepository;
    private final SpringDataContatoConvenioCoreRepository contatoConvenioRepository;
    private final SpringDataConvenioRepository convenioRepository;
    private final DocumentoRepositoryPort documentoRepository;
    private final EntityManager entityManager;

    public TriagemService(
            SpringDataTriagemInboxRepository triagemInboxRepository,
            SpringDataContatoCoreRepository contatoRepository,
            SpringDataContatoConvenioCoreRepository contatoConvenioRepository,
            SpringDataConvenioRepository convenioRepository,
            DocumentoRepositoryPort documentoRepository,
            EntityManager entityManager
    ) {
        this.triagemInboxRepository = triagemInboxRepository;
        this.contatoRepository = contatoRepository;
        this.contatoConvenioRepository = contatoConvenioRepository;
        this.convenioRepository = convenioRepository;
        this.documentoRepository = documentoRepository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TriagemItemResponse> listarPendentes() {
        UUID tenantId = obterTenantIdObrigatorio();
        List<TriagemInboxJpaEntity> itens = triagemInboxRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, "PENDENTE");

        boolean isAgente = UserContext.isAgente() && !UserContext.isAdmin();
        Set<UUID> prefeiturasAgente = isAgente ? UserContext.getPrefeiturasAtribuidasIds() : Collections.emptySet();

        List<TriagemItemResponse> resultado = new ArrayList<>();
        for (TriagemInboxJpaEntity item : itens) {
            ConvenioJpaEntity convenioSugerido = null;
            if (item.getConvenioSugeridoId() != null) {
                convenioSugerido = convenioRepository.findById(item.getConvenioSugeridoId()).orElse(null);
            }

            if (isAgente && convenioSugerido != null && !prefeiturasAgente.contains(convenioSugerido.getPrefeituraId())) {
                // Item pertence a convênio de prefeitura que o agente não tem permissão
                continue;
            }

            resultado.add(converterParaTriagemItemResponse(item, convenioSugerido));
        }

        return resultado;
    }

    @Override
    @Transactional
    public TriagemItemResponse cadastrarContatoETriar(UUID inboxId, CadastrarContatoETriarRequest request) {
        Objects.requireNonNull(inboxId, "InboxId não pode ser nulo");
        Objects.requireNonNull(request, "Request não pode ser nula");
        UUID tenantId = obterTenantIdObrigatorio();

        TriagemInboxJpaEntity inbox = triagemInboxRepository.findByIdAndTenantId(inboxId, tenantId)
                .orElseThrow(() -> new TriagemItemNaoEncontradoException(inboxId));

        // 1. Cria ou atualiza o Contato no tenant
        ContatoCoreJpaEntity contato = contatoRepository.findByTenantIdAndPhoneNumber(tenantId, request.phoneNumber())
                .orElseGet(() -> new ContatoCoreJpaEntity(
                        UUID.randomUUID(),
                        tenantId,
                        request.phoneNumber(),
                        request.nome(),
                        request.papel(),
                        request.empresaOuOrgao()
                ));

        contato.setNome(request.nome());
        contato.setPapel(request.papel());
        contato.setEmpresaOuOrgao(request.empresaOuOrgao());
        contato.setAtivo(true);
        contato = contatoRepository.save(contato);

        // 2. Associa N:N com os convênios informados
        List<UUID> conveniosIds = request.conveniosIds() != null ? request.conveniosIds() : Collections.emptyList();
        UUID convenioPrincipalId = request.convenioPrincipalId() != null
                ? request.convenioPrincipalId()
                : (!conveniosIds.isEmpty() ? conveniosIds.get(0) : null);

        for (UUID convId : conveniosIds) {
            ConvenioJpaEntity conv = convenioRepository.findById(convId)
                    .orElseThrow(() -> new ConvenioNaoEncontradoException(convId));

            if (!tenantId.equals(conv.getTenantId())) {
                throw new AcessoNegadoException("Convênio não pertence ao tenant autenticado.");
            }

            if (UserContext.isAgente() && !UserContext.isAdmin() && !UserContext.temAcessoPrefeitura(conv.getPrefeituraId())) {
                throw new AcessoNegadoException("Agente não possui acesso à prefeitura do convênio: " + conv.getPrefeituraId());
            }

            boolean isPrincipal = convId.equals(convenioPrincipalId);
            ContatoConvenioCoreJpaEntity vinculo = new ContatoConvenioCoreJpaEntity(
                    contato.getId(),
                    convId,
                    conv.getPrefeituraId(),
                    request.papel(),
                    isPrincipal
            );
            contatoConvenioRepository.save(vinculo);
        }

        // 3. Atualiza rastreabilidade na tb_mensagens_inbound se houver ID
        if (inbox.getMensagemInboundId() != null) {
            try {
                entityManager.createNativeQuery(
                        "UPDATE whatsapp_schema.tb_mensagens_inbound SET contato_id = :contatoId WHERE id = :msgId"
                ).setParameter("contatoId", contato.getId())
                 .setParameter("msgId", inbox.getMensagemInboundId())
                 .executeUpdate();
            } catch (Exception e) {
                log.warn("Não foi possível atualizar contato_id na tb_mensagens_inbound (pode ser mock ou tabela ausente em teste): {}", e.getMessage());
            }
        }

        // 4. Arquiva documento no GED se solicitado
        ConvenioJpaEntity convEleito = null;
        if (convenioPrincipalId != null) {
            convEleito = convenioRepository.findById(convenioPrincipalId).orElse(null);
        } else if (inbox.getConvenioSugeridoId() != null) {
            convEleito = convenioRepository.findById(inbox.getConvenioSugeridoId()).orElse(null);
        }

        if (request.arquivarDocumento() && inbox.getDocumentoId() != null) {
            if (convEleito == null) {
                throw new IllegalArgumentException("Selecione ao menos um convênio válido para arquivar o documento.");
            }

            FaseCicloVida fase = request.faseCicloVida() != null
                    ? FaseCicloVida.fromCodigoOuNome(request.faseCicloVida())
                    : (inbox.getFaseSugerida() != null
                    ? FaseCicloVida.fromCodigoOuNome(inbox.getFaseSugerida())
                    : FaseCicloVida.FASE_04_EXECUCAO_FISICA);

            Optional<Documento> docOpt = documentoRepository.buscarPorId(inbox.getDocumentoId());
            if (docOpt.isPresent()) {
                Documento doc = docOpt.get();
                doc.setConvenioId(convEleito.getId());
                doc.setPrefeituraId(convEleito.getPrefeituraId());
                doc.setFaseCicloVida(fase);
                doc.setPastaVirtual("/" + fase.getNomePasta());
                doc.enviarParaConferencia();
                documentoRepository.salvar(doc);

                DocumentoAuditoria auditoria = DocumentoAuditoria.registrar(
                        tenantId,
                        doc.getId(),
                        UserContext.getUserId(),
                        "ARQUIVAMENTO_TRIAGEM",
                        "Contato cadastrado e documento arquivado via Caixa de Triagem",
                        null,
                        "{\"fase\": \"" + fase.name() + "\", \"convenioId\": \"" + convEleito.getId() + "\", \"contatoId\": \"" + contato.getId() + "\"}"
                );
                documentoRepository.salvarAuditoria(auditoria);
            }
        }

        // 5. Marca o item de triagem como RESOLVIDO
        inbox.setStatus("RESOLVIDO");
        inbox.setResolvidoEm(Instant.now());
        inbox.setAgenteResponsavelId(UserContext.getUserId());
        if (convEleito != null) {
            inbox.setConvenioSugeridoId(convEleito.getId());
        }
        if (request.faseCicloVida() != null) {
            inbox.setFaseSugerida(request.faseCicloVida());
        }
        TriagemInboxJpaEntity salvo = triagemInboxRepository.save(inbox);

        return converterParaTriagemItemResponse(salvo, convEleito);
    }

    @Override
    @Transactional
    public TriagemItemResponse confirmarArquivamento(UUID inboxId, UUID convenioId, String faseCicloVida) {
        Objects.requireNonNull(inboxId, "InboxId não pode ser nulo");
        UUID tenantId = obterTenantIdObrigatorio();

        TriagemInboxJpaEntity inbox = triagemInboxRepository.findByIdAndTenantId(inboxId, tenantId)
                .orElseThrow(() -> new TriagemItemNaoEncontradoException(inboxId));

        UUID convAlvoId = convenioId != null ? convenioId : inbox.getConvenioSugeridoId();
        if (convAlvoId == null) {
            throw new IllegalArgumentException("Convênio não informado e nenhum sugerido disponível.");
        }

        ConvenioJpaEntity convenio = convenioRepository.findById(convAlvoId)
                .orElseThrow(() -> new ConvenioNaoEncontradoException(convAlvoId));

        if (!tenantId.equals(convenio.getTenantId())) {
            throw new AcessoNegadoException("Convênio não pertence ao tenant autenticado.");
        }

        if (UserContext.isAgente() && !UserContext.isAdmin() && !UserContext.temAcessoPrefeitura(convenio.getPrefeituraId())) {
            throw new AcessoNegadoException("Agente não possui acesso à prefeitura deste convênio.");
        }

        FaseCicloVida fase = faseCicloVida != null
                ? FaseCicloVida.fromCodigoOuNome(faseCicloVida)
                : (inbox.getFaseSugerida() != null
                ? FaseCicloVida.fromCodigoOuNome(inbox.getFaseSugerida())
                : FaseCicloVida.FASE_04_EXECUCAO_FISICA);

        if (inbox.getDocumentoId() != null) {
            Optional<Documento> docOpt = documentoRepository.buscarPorId(inbox.getDocumentoId());
            if (docOpt.isPresent()) {
                Documento doc = docOpt.get();
                doc.setConvenioId(convenio.getId());
                doc.setPrefeituraId(convenio.getPrefeituraId());
                doc.setFaseCicloVida(fase);
                doc.setPastaVirtual("/" + fase.getNomePasta());
                doc.enviarParaConferencia();
                documentoRepository.salvar(doc);

                DocumentoAuditoria auditoria = DocumentoAuditoria.registrar(
                        tenantId,
                        doc.getId(),
                        UserContext.getUserId(),
                        "CONFIRMACAO_TRIAGEM_1_CLIQUE",
                        "Arquivamento em 1 clique confirmado pelo agente na Caixa de Triagem",
                        null,
                        "{\"fase\": \"" + fase.name() + "\", \"convenioId\": \"" + convenio.getId() + "\"}"
                );
                documentoRepository.salvarAuditoria(auditoria);
            }
        }

        inbox.setStatus("RESOLVIDO");
        inbox.setResolvidoEm(Instant.now());
        inbox.setAgenteResponsavelId(UserContext.getUserId());
        inbox.setConvenioSugeridoId(convenio.getId());
        inbox.setFaseSugerida(fase.name());
        TriagemInboxJpaEntity salvo = triagemInboxRepository.save(inbox);

        return converterParaTriagemItemResponse(salvo, convenio);
    }

    @Override
    @Transactional
    public TriagemItemResponse ignorarItem(UUID inboxId) {
        Objects.requireNonNull(inboxId, "InboxId não pode ser nulo");
        UUID tenantId = obterTenantIdObrigatorio();

        TriagemInboxJpaEntity inbox = triagemInboxRepository.findByIdAndTenantId(inboxId, tenantId)
                .orElseThrow(() -> new TriagemItemNaoEncontradoException(inboxId));

        inbox.setStatus("IGNORADO");
        inbox.setResolvidoEm(Instant.now());
        inbox.setAgenteResponsavelId(UserContext.getUserId());
        TriagemInboxJpaEntity salvo = triagemInboxRepository.save(inbox);

        ConvenioJpaEntity conv = salvo.getConvenioSugeridoId() != null
                ? convenioRepository.findById(salvo.getConvenioSugeridoId()).orElse(null)
                : null;

        return converterParaTriagemItemResponse(salvo, conv);
    }

    @Override
    @Transactional
    public ContatoResponse cadastrarContato(CadastrarContatoRequest request) {
        Objects.requireNonNull(request, "Request não pode ser nula");
        UUID tenantId = obterTenantIdObrigatorio();

        ContatoCoreJpaEntity contato = contatoRepository.findByTenantIdAndPhoneNumber(tenantId, request.phoneNumber())
                .orElseGet(() -> new ContatoCoreJpaEntity(
                        UUID.randomUUID(),
                        tenantId,
                        request.phoneNumber(),
                        request.nome(),
                        request.papel(),
                        request.empresaOuOrgao()
                ));

        contato.setNome(request.nome());
        contato.setPapel(request.papel());
        contato.setEmpresaOuOrgao(request.empresaOuOrgao());
        contato.setAtivo(true);
        contato = contatoRepository.save(contato);

        List<UUID> conveniosIds = request.conveniosIds() != null ? request.conveniosIds() : Collections.emptyList();
        UUID convenioPrincipalId = request.convenioPrincipalId() != null
                ? request.convenioPrincipalId()
                : (!conveniosIds.isEmpty() ? conveniosIds.get(0) : null);

        contatoConvenioRepository.deleteByIdContatoId(contato.getId());

        List<ContatoConvenioResponse> vinculosResp = new ArrayList<>();
        for (UUID convId : conveniosIds) {
            ConvenioJpaEntity conv = convenioRepository.findById(convId)
                    .orElseThrow(() -> new ConvenioNaoEncontradoException(convId));

            if (!tenantId.equals(conv.getTenantId())) {
                throw new AcessoNegadoException("Convênio não pertence ao tenant autenticado.");
            }

            boolean isPrincipal = convId.equals(convenioPrincipalId);
            ContatoConvenioCoreJpaEntity vinculo = new ContatoConvenioCoreJpaEntity(
                    contato.getId(),
                    convId,
                    conv.getPrefeituraId(),
                    request.papel(),
                    isPrincipal
            );
            contatoConvenioRepository.save(vinculo);

            vinculosResp.add(new ContatoConvenioResponse(
                    convId,
                    conv.getPrefeituraId(),
                    conv.getNumeroSiconv(),
                    conv.getObjeto(),
                    request.papel(),
                    isPrincipal
            ));
        }

        return new ContatoResponse(
                contato.getId(),
                contato.getTenantId(),
                contato.getPhoneNumber(),
                contato.getNome(),
                contato.getPapel(),
                contato.getEmpresaOuOrgao(),
                contato.isAtivo(),
                contato.getCreatedAt(),
                vinculosResp
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContatoResponse> listarContatos() {
        UUID tenantId = obterTenantIdObrigatorio();
        List<ContatoCoreJpaEntity> contatos = contatoRepository.findByTenantIdOrderByNomeAsc(tenantId);

        return contatos.stream().map(c -> {
            List<ContatoConvenioCoreJpaEntity> vinculos = contatoConvenioRepository.findByIdContatoId(c.getId());
            List<ContatoConvenioResponse> vinculosResp = vinculos.stream().map(v -> {
                ConvenioJpaEntity conv = convenioRepository.findById(v.getConvenioId()).orElse(null);
                return new ContatoConvenioResponse(
                        v.getConvenioId(),
                        v.getPrefeituraId(),
                        conv != null ? conv.getNumeroSiconv() : null,
                        conv != null ? conv.getObjeto() : null,
                        v.getPapelEspecifico(),
                        v.isPrincipal()
                );
            }).collect(Collectors.toList());

            return new ContatoResponse(
                    c.getId(),
                    c.getTenantId(),
                    c.getPhoneNumber(),
                    c.getNome(),
                    c.getPapel(),
                    c.getEmpresaOuOrgao(),
                    c.isAtivo(),
                    c.getCreatedAt(),
                    vinculosResp
            );
        }).collect(Collectors.toList());
    }

    private TriagemItemResponse converterParaTriagemItemResponse(TriagemInboxJpaEntity item, ConvenioJpaEntity convenio) {
        String docNome = null;
        String docContentType = null;
        Long docTamanho = null;

        if (item.getDocumentoId() != null) {
            Optional<Documento> docOpt = documentoRepository.buscarPorId(item.getDocumentoId());
            if (docOpt.isPresent()) {
                Documento doc = docOpt.get();
                if (doc.getArmazenamento() != null) {
                    docNome = doc.getArmazenamento().nomeArquivoOriginal();
                    docContentType = doc.getArmazenamento().contentType();
                    docTamanho = doc.getArmazenamento().tamanhoBytes();
                }
            }
        }

        return new TriagemItemResponse(
                item.getId(),
                item.getTenantId(),
                item.getAgenteResponsavelId(),
                item.getMensagemInboundId(),
                item.getDocumentoId(),
                item.getConvenioSugeridoId(),
                convenio != null ? convenio.getNumeroSiconv() : null,
                convenio != null ? convenio.getObjeto() : null,
                convenio != null ? convenio.getPrefeituraId() : null,
                item.getFaseSugerida(),
                item.getConfidenceScore(),
                item.getMotivoAmbiguidade(),
                item.getPhoneNumber(),
                item.getSenderName(),
                item.getPushName(),
                item.isRemetenteNovo(),
                item.getConteudoResumo(),
                item.getStatus(),
                item.getResolvidoEm(),
                item.getCreatedAt(),
                docNome,
                docContentType,
                docTamanho
        );
    }

    private UUID obterTenantIdObrigatorio() {
        UUID tenantId = UserContext.getTenantId();
        if (tenantId == null) {
            throw new TenantInvalidoException("TenantId é obrigatório para operações de Triagem.");
        }
        return tenantId;
    }
}
