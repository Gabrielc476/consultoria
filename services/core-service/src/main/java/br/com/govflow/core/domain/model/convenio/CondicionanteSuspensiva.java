package br.com.govflow.core.domain.model.convenio;

import br.com.govflow.core.domain.exception.RegraNegocioClausulaSuspensivaException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade de domínio para cada pilar condicionante da Cláusula Suspensiva (Fase 2).
 */
public class CondicionanteSuspensiva {

    private final UUID id;
    private final UUID tenantId;
    private final UUID convenioId;
    private final TipoCondicionanteSuspensiva tipoCondicionante;
    private StatusCondicionanteSuspensiva status;
    private String numeroDocumentoComprobatorio;
    private LocalDate dataAprovacao;
    private LocalDate dataValidade;
    private String observacoesAnaliseCaixa;
    private String s3KeyDocumento;
    private LocalDate dataLimiteSaneamento;
    private String s3KeyLaudoPendencias;
    private BigDecimal valorOrcamentoAprovadoCaixa;
    private BigDecimal percentualBdiAprovado;
    private String numeroArtRrt;
    private String orgaoEmissor;
    private final Instant createdAt;
    private Instant updatedAt;

    public CondicionanteSuspensiva(UUID id,
                                  UUID tenantId,
                                  UUID convenioId,
                                  TipoCondicionanteSuspensiva tipoCondicionante,
                                  StatusCondicionanteSuspensiva status,
                                  String numeroDocumentoComprobatorio,
                                  LocalDate dataAprovacao,
                                  LocalDate dataValidade,
                                  String observacoesAnaliseCaixa,
                                  String s3KeyDocumento,
                                  LocalDate dataLimiteSaneamento,
                                  String s3KeyLaudoPendencias,
                                  BigDecimal valorOrcamentoAprovadoCaixa,
                                  BigDecimal percentualBdiAprovado,
                                  String numeroArtRrt,
                                  String orgaoEmissor,
                                  Instant createdAt,
                                  Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "O id da condicionante não pode ser nulo");
        this.tenantId = Objects.requireNonNull(tenantId, "O tenantId não pode ser nulo");
        this.convenioId = Objects.requireNonNull(convenioId, "O convenioId não pode ser nulo");
        this.tipoCondicionante = Objects.requireNonNull(tipoCondicionante, "O tipo da condicionante não pode ser nulo");
        this.status = status != null ? status : StatusCondicionanteSuspensiva.PENDENTE;
        this.numeroDocumentoComprobatorio = numeroDocumentoComprobatorio;
        this.dataAprovacao = dataAprovacao;
        this.dataValidade = dataValidade;
        this.observacoesAnaliseCaixa = observacoesAnaliseCaixa;
        this.s3KeyDocumento = s3KeyDocumento;
        this.dataLimiteSaneamento = dataLimiteSaneamento;
        this.s3KeyLaudoPendencias = s3KeyLaudoPendencias;
        this.valorOrcamentoAprovadoCaixa = valorOrcamentoAprovadoCaixa;
        this.percentualBdiAprovado = percentualBdiAprovado;
        this.numeroArtRrt = numeroArtRrt;
        this.orgaoEmissor = orgaoEmissor;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static CondicionanteSuspensiva nova(UUID tenantId, UUID convenioId, TipoCondicionanteSuspensiva tipo) {
        return new CondicionanteSuspensiva(
                UUID.randomUUID(),
                tenantId,
                convenioId,
                tipo,
                StatusCondicionanteSuspensiva.PENDENTE,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Instant.now(),
                Instant.now()
        );
    }

    /**
     * Submete a documentação técnica reunida pelo município para auditoria da Caixa GIGOV.
     */
    public void submeterParaAnaliseCaixa() {
        if (this.status == StatusCondicionanteSuspensiva.APROVADO) {
            throw new RegraNegocioClausulaSuspensivaException("A condicionante já se encontra aprovada pela Caixa.");
        }
        this.status = StatusCondicionanteSuspensiva.EM_ANALISE_CAIXA;
        this.updatedAt = Instant.now();
    }

    /**
     * Registra notificação formal de diligência emitida pela Caixa (Laudo de Pendências).
     */
    public void registrarDiligencia(String observacoes, String s3KeyLaudoPendencias, LocalDate dataLimiteSaneamento, LocalDate dataReferencia) {
        if (dataLimiteSaneamento != null && dataReferencia != null && dataLimiteSaneamento.isBefore(dataReferencia)) {
            throw new RegraNegocioClausulaSuspensivaException("A data limite de saneamento não pode ser anterior à data atual.");
        }
        this.status = StatusCondicionanteSuspensiva.DILIGENCIA_EMITIDA;
        this.observacoesAnaliseCaixa = observacoes;
        this.s3KeyLaudoPendencias = s3KeyLaudoPendencias;
        this.dataLimiteSaneamento = dataLimiteSaneamento;
        this.updatedAt = Instant.now();
    }

    /**
     * Registra o aceite técnico e aprovação formal pela Mandatária (LAE/SPA, Licença ou CRI).
     */
    public void aprovar(String numeroDocumento,
                        LocalDate dataAprovacao,
                        LocalDate dataValidade,
                        BigDecimal valorOrcamento,
                        BigDecimal percentualBdi,
                        String numeroArtRrt,
                        String orgaoEmissor,
                        String s3KeyDocumento) {
        if (numeroDocumento == null || numeroDocumento.trim().isEmpty()) {
            throw new RegraNegocioClausulaSuspensivaException("O número do documento comprobatório oficial é obrigatório para aprovação da condicionante.");
        }
        if (percentualBdi != null && percentualBdi.compareTo(BigDecimal.ZERO) < 0) {
            throw new RegraNegocioClausulaSuspensivaException("O percentual de BDI não pode ser negativo.");
        }
        this.status = StatusCondicionanteSuspensiva.APROVADO;
        this.numeroDocumentoComprobatorio = numeroDocumento.trim();
        this.dataAprovacao = dataAprovacao != null ? dataAprovacao : LocalDate.now();
        this.dataValidade = dataValidade;
        if (valorOrcamento != null) {
            this.valorOrcamentoAprovadoCaixa = valorOrcamento;
        }
        if (percentualBdi != null) {
            this.percentualBdiAprovado = percentualBdi;
        }
        if (numeroArtRrt != null && !numeroArtRrt.trim().isEmpty()) {
            this.numeroArtRrt = numeroArtRrt.trim();
        }
        if (orgaoEmissor != null && !orgaoEmissor.trim().isEmpty()) {
            this.orgaoEmissor = orgaoEmissor.trim();
        }
        if (s3KeyDocumento != null && !s3KeyDocumento.trim().isEmpty()) {
            this.s3KeyDocumento = s3KeyDocumento.trim();
        }
        this.updatedAt = Instant.now();
    }

    /**
     * Atualiza dados e parâmetros técnicos do pilar.
     */
    public void atualizarParametrosTecnicos(String numeroDocumento,
                                           LocalDate dataValidade,
                                           String orgaoEmissor,
                                           BigDecimal valorOrcamento,
                                           BigDecimal percentualBdi,
                                           String numeroArtRrt,
                                           String observacoes) {
        if (numeroDocumento != null) {
            this.numeroDocumentoComprobatorio = numeroDocumento.trim();
        }
        if (dataValidade != null) {
            this.dataValidade = dataValidade;
        }
        if (orgaoEmissor != null) {
            this.orgaoEmissor = orgaoEmissor.trim();
        }
        if (valorOrcamento != null) {
            this.valorOrcamentoAprovadoCaixa = valorOrcamento;
        }
        if (percentualBdi != null) {
            this.percentualBdiAprovado = percentualBdi;
        }
        if (numeroArtRrt != null) {
            this.numeroArtRrt = numeroArtRrt.trim();
        }
        if (observacoes != null) {
            this.observacoesAnaliseCaixa = observacoes;
        }
        this.updatedAt = Instant.now();
    }

    /**
     * Vincula o documento ou laudo armazenado com segurança no storage.
     */
    public void vincularDocumento(String s3Key) {
        if (s3Key == null || s3Key.trim().isEmpty()) {
            throw new RegraNegocioClausulaSuspensivaException("Chave de documento inválida.");
        }
        this.s3KeyDocumento = s3Key.trim();
        this.updatedAt = Instant.now();
    }

    public boolean isAprovado() {
        return this.status == StatusCondicionanteSuspensiva.APROVADO;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getConvenioId() {
        return convenioId;
    }

    public TipoCondicionanteSuspensiva getTipoCondicionante() {
        return tipoCondicionante;
    }

    public StatusCondicionanteSuspensiva getStatus() {
        return status;
    }

    public String getNumeroDocumentoComprobatorio() {
        return numeroDocumentoComprobatorio;
    }

    public LocalDate getDataAprovacao() {
        return dataAprovacao;
    }

    public LocalDate getDataValidade() {
        return dataValidade;
    }

    public String getObservacoesAnaliseCaixa() {
        return observacoesAnaliseCaixa;
    }

    public String getS3KeyDocumento() {
        return s3KeyDocumento;
    }

    public LocalDate getDataLimiteSaneamento() {
        return dataLimiteSaneamento;
    }

    public String getS3KeyLaudoPendencias() {
        return s3KeyLaudoPendencias;
    }

    public BigDecimal getValorOrcamentoAprovadoCaixa() {
        return valorOrcamentoAprovadoCaixa;
    }

    public BigDecimal getPercentualBdiAprovado() {
        return percentualBdiAprovado;
    }

    public String getNumeroArtRrt() {
        return numeroArtRrt;
    }

    public String getOrgaoEmissor() {
        return orgaoEmissor;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
