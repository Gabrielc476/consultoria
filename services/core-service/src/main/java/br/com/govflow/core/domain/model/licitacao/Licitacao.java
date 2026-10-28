package br.com.govflow.core.domain.model.licitacao;

import br.com.govflow.core.domain.exception.AioNaoEmitidaException;
import br.com.govflow.core.domain.exception.RegraNegocioLicitacaoException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class Licitacao {

    private static final BigDecimal CEM = new BigDecimal("100");

    private final UUID id;
    private final UUID tenantId;
    private final UUID convenioId;
    private final UUID prefeituraId;
    private String numeroProcessoAdministrativo;
    private String numeroLicitacao;
    private ModalidadeLicitacao modalidade;
    private CriterioJulgamento criterioJulgamento;
    private RegimeExecucao regimeExecucao;
    private String objeto;
    private BigDecimal valorEstimado;
    private BigDecimal valorHomologado;
    private BigDecimal percentualDesconto;
    private SituacaoLicitacao situacao;
    private LocalDate dataPublicacaoEdital;
    private LocalDate dataAberturaPropostas;
    private LocalDate dataHomologacao;
    private String linkPncp;
    private String linkTransferegov;
    private String linkSistemaCompras;
    private String numeroVrplTransferegov;
    private StatusVrpl statusVrpl;
    private LocalDate dataEnvioVrpl;
    private LocalDate dataAceiteVrpl;
    private String parecerTecnicoVrpl;
    private String observacoesDiligenciaVrpl;
    private String numeroAio;
    private LocalDate dataEmissaoAio;
    private StatusAio statusAio;
    private String cnpjVencedor;
    private String razaoSocialVencedor;
    private String s3KeyEdital;
    private String s3KeyTermoHomologacao;
    private String s3KeyPropostaVencedora;
    private String s3KeyAtaSessao;
    private String s3KeyParecerVrpl;
    private String s3KeyAutorizacaoAio;
    private final Instant createdAt;
    private Instant updatedAt;

    public Licitacao(UUID id,
                     UUID tenantId,
                     UUID convenioId,
                     UUID prefeituraId,
                     String numeroProcessoAdministrativo,
                     String numeroLicitacao,
                     ModalidadeLicitacao modalidade,
                     CriterioJulgamento criterioJulgamento,
                     RegimeExecucao regimeExecucao,
                     String objeto,
                     BigDecimal valorEstimado,
                     BigDecimal valorHomologado,
                     BigDecimal percentualDesconto,
                     SituacaoLicitacao situacao,
                     LocalDate dataPublicacaoEdital,
                     LocalDate dataAberturaPropostas,
                     LocalDate dataHomologacao,
                     String linkPncp,
                     String linkTransferegov,
                     String linkSistemaCompras,
                     String numeroVrplTransferegov,
                     StatusVrpl statusVrpl,
                     LocalDate dataEnvioVrpl,
                     LocalDate dataAceiteVrpl,
                     String parecerTecnicoVrpl,
                     String observacoesDiligenciaVrpl,
                     String numeroAio,
                     LocalDate dataEmissaoAio,
                     StatusAio statusAio,
                     String cnpjVencedor,
                     String razaoSocialVencedor,
                     String s3KeyEdital,
                     String s3KeyTermoHomologacao,
                     String s3KeyPropostaVencedora,
                     String s3KeyAtaSessao,
                     String s3KeyParecerVrpl,
                     String s3KeyAutorizacaoAio,
                     Instant createdAt,
                     Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "O id da licitação não pode ser nulo");
        this.tenantId = Objects.requireNonNull(tenantId, "O tenantId não pode ser nulo");
        this.convenioId = Objects.requireNonNull(convenioId, "O convenioId não pode ser nulo");
        this.prefeituraId = Objects.requireNonNull(prefeituraId, "O prefeituraId não pode ser nulo");
        this.numeroProcessoAdministrativo = numeroProcessoAdministrativo;
        this.numeroLicitacao = Objects.requireNonNull(numeroLicitacao, "O número da licitação não pode ser nulo");
        this.modalidade = Objects.requireNonNull(modalidade, "A modalidade não pode ser nula");
        this.criterioJulgamento = criterioJulgamento;
        this.regimeExecucao = regimeExecucao;
        this.objeto = Objects.requireNonNull(objeto, "O objeto da licitação não pode ser nulo");
        this.valorEstimado = valorEstimado != null ? valorEstimado : BigDecimal.ZERO;
        this.valorHomologado = valorHomologado;
        this.percentualDesconto = percentualDesconto;
        this.situacao = situacao != null ? situacao : SituacaoLicitacao.PLANEJAMENTO;
        this.dataPublicacaoEdital = dataPublicacaoEdital;
        this.dataAberturaPropostas = dataAberturaPropostas;
        this.dataHomologacao = dataHomologacao;
        this.linkPncp = linkPncp;
        this.linkTransferegov = linkTransferegov;
        this.linkSistemaCompras = linkSistemaCompras;
        this.numeroVrplTransferegov = numeroVrplTransferegov;
        this.statusVrpl = statusVrpl != null ? statusVrpl : StatusVrpl.NAO_ENVIADO;
        this.dataEnvioVrpl = dataEnvioVrpl;
        this.dataAceiteVrpl = dataAceiteVrpl;
        this.parecerTecnicoVrpl = parecerTecnicoVrpl;
        this.observacoesDiligenciaVrpl = observacoesDiligenciaVrpl;
        this.numeroAio = numeroAio;
        this.dataEmissaoAio = dataEmissaoAio;
        this.statusAio = statusAio != null ? statusAio : StatusAio.NAO_EMITIDO;
        this.cnpjVencedor = cnpjVencedor;
        this.razaoSocialVencedor = razaoSocialVencedor;
        this.s3KeyEdital = s3KeyEdital;
        this.s3KeyTermoHomologacao = s3KeyTermoHomologacao;
        this.s3KeyPropostaVencedora = s3KeyPropostaVencedora;
        this.s3KeyAtaSessao = s3KeyAtaSessao;
        this.s3KeyParecerVrpl = s3KeyParecerVrpl;
        this.s3KeyAutorizacaoAio = s3KeyAutorizacaoAio;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    public static Licitacao cadastrar(UUID tenantId,
                                      UUID convenioId,
                                      UUID prefeituraId,
                                      String numeroProcessoAdministrativo,
                                      String numeroLicitacao,
                                      ModalidadeLicitacao modalidade,
                                      CriterioJulgamento criterioJulgamento,
                                      RegimeExecucao regimeExecucao,
                                      String objeto,
                                      BigDecimal valorEstimado,
                                      LocalDate dataPublicacaoEdital,
                                      LocalDate dataAberturaPropostas,
                                      String linkPncp,
                                      String linkTransferegov,
                                      String linkSistemaCompras) {
        if (numeroLicitacao == null || numeroLicitacao.isBlank()) {
            throw new RegraNegocioLicitacaoException("O número da licitação é obrigatório.");
        }
        if (objeto == null || objeto.isBlank()) {
            throw new RegraNegocioLicitacaoException("O objeto da licitação é obrigatório.");
        }
        if (valorEstimado == null || valorEstimado.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraNegocioLicitacaoException("O valor estimado deve ser maior que zero.");
        }

        SituacaoLicitacao situacaoInicial = dataPublicacaoEdital != null
                ? SituacaoLicitacao.EDITAL_PUBLICADO
                : SituacaoLicitacao.PLANEJAMENTO;
        Instant agora = Instant.now();

        return new Licitacao(
                UUID.randomUUID(),
                tenantId,
                convenioId,
                prefeituraId,
                numeroProcessoAdministrativo,
                numeroLicitacao.trim(),
                modalidade,
                criterioJulgamento,
                regimeExecucao,
                objeto.trim(),
                valorEstimado,
                null,
                null,
                situacaoInicial,
                dataPublicacaoEdital,
                dataAberturaPropostas,
                null,
                linkPncp,
                linkTransferegov,
                linkSistemaCompras,
                null,
                StatusVrpl.NAO_ENVIADO,
                null,
                null,
                null,
                null,
                null,
                null,
                StatusAio.NAO_EMITIDO,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                agora,
                agora
        );
    }

    public void homologar(BigDecimal valorHomologado,
                          LocalDate dataHomologacao,
                          String cnpjVencedor,
                          String razaoSocialVencedor) {
        garantirNaoEncerrada();
        if (valorHomologado == null || valorHomologado.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraNegocioLicitacaoException("O valor homologado deve ser maior que zero.");
        }
        if (valorHomologado.compareTo(this.valorEstimado) > 0) {
            throw new RegraNegocioLicitacaoException("O valor homologado não pode exceder o valor estimado da SPA.");
        }
        if (dataHomologacao == null) {
            throw new RegraNegocioLicitacaoException("A data de homologação é obrigatória.");
        }
        if (cnpjVencedor == null || cnpjVencedor.isBlank()) {
            throw new RegraNegocioLicitacaoException("O CNPJ do vencedor é obrigatório para homologar.");
        }
        if (razaoSocialVencedor == null || razaoSocialVencedor.isBlank()) {
            throw new RegraNegocioLicitacaoException("A razão social do vencedor é obrigatória para homologar.");
        }

        this.valorHomologado = valorHomologado;
        this.percentualDesconto = calcularEconomia(this.valorEstimado, valorHomologado);
        this.dataHomologacao = dataHomologacao;
        this.cnpjVencedor = cnpjVencedor.trim();
        this.razaoSocialVencedor = razaoSocialVencedor.trim();
        this.situacao = SituacaoLicitacao.HOMOLOGADA;
        this.updatedAt = Instant.now();
    }

    public void submeterVrpl(String numeroVrplTransferegov, LocalDate dataEnvioVrpl) {
        if (this.situacao != SituacaoLicitacao.HOMOLOGADA) {
            throw new RegraNegocioLicitacaoException("O VRPL só pode ser submetido após a homologação municipal.");
        }
        if (this.statusVrpl == StatusVrpl.ACEITO_HOMOLOGADO) {
            throw new RegraNegocioLicitacaoException("O VRPL já foi aceito pela Caixa.");
        }
        if (this.statusVrpl == StatusVrpl.REJEITADO) {
            throw new RegraNegocioLicitacaoException("O VRPL foi rejeitado e não pode ser reenviado neste certame.");
        }
        if (numeroVrplTransferegov == null || numeroVrplTransferegov.isBlank()) {
            throw new RegraNegocioLicitacaoException("O número de protocolo do VRPL no Transferegov é obrigatório.");
        }
        if (dataEnvioVrpl == null) {
            throw new RegraNegocioLicitacaoException("A data de envio do VRPL é obrigatória.");
        }

        this.numeroVrplTransferegov = numeroVrplTransferegov.trim();
        this.dataEnvioVrpl = dataEnvioVrpl;
        this.statusVrpl = StatusVrpl.EM_ANALISE_CAIXA;
        this.updatedAt = Instant.now();
    }

    public void registrarDiligencia(String observacoes) {
        if (this.statusVrpl != StatusVrpl.EM_ANALISE_CAIXA) {
            throw new RegraNegocioLicitacaoException("Diligência só pode ser registrada com VRPL em análise pela Caixa.");
        }
        if (observacoes == null || observacoes.isBlank()) {
            throw new RegraNegocioLicitacaoException("As observações da diligência da Caixa são obrigatórias.");
        }
        this.statusVrpl = StatusVrpl.DILIGENCIA;
        this.observacoesDiligenciaVrpl = observacoes.trim();
        this.updatedAt = Instant.now();
    }

    public void aceitarVrpl(String parecerTecnico, LocalDate dataAceiteVrpl) {
        if (this.statusVrpl != StatusVrpl.EM_ANALISE_CAIXA && this.statusVrpl != StatusVrpl.DILIGENCIA) {
            throw new RegraNegocioLicitacaoException("O VRPL só pode ser aceito quando estiver em análise ou em diligência.");
        }
        if (parecerTecnico == null || parecerTecnico.isBlank()) {
            throw new RegraNegocioLicitacaoException("O parecer técnico da Caixa é obrigatório para aceitar o VRPL.");
        }
        if (dataAceiteVrpl == null) {
            throw new RegraNegocioLicitacaoException("A data de aceite do VRPL é obrigatória.");
        }
        this.statusVrpl = StatusVrpl.ACEITO_HOMOLOGADO;
        this.parecerTecnicoVrpl = parecerTecnico.trim();
        this.dataAceiteVrpl = dataAceiteVrpl;
        this.updatedAt = Instant.now();
    }

    public void rejeitarVrpl(String parecerTecnico) {
        if (this.statusVrpl != StatusVrpl.EM_ANALISE_CAIXA && this.statusVrpl != StatusVrpl.DILIGENCIA) {
            throw new RegraNegocioLicitacaoException("O VRPL só pode ser rejeitado quando estiver em análise ou em diligência.");
        }
        if (parecerTecnico == null || parecerTecnico.isBlank()) {
            throw new RegraNegocioLicitacaoException("O parecer técnico da Caixa é obrigatório para rejeitar o VRPL.");
        }
        this.statusVrpl = StatusVrpl.REJEITADO;
        this.parecerTecnicoVrpl = parecerTecnico.trim();
        this.updatedAt = Instant.now();
    }

    public void solicitarAio() {
        garantirVrplAceitoParaAio();
        if (this.statusAio == StatusAio.EMITIDO) {
            throw new RegraNegocioLicitacaoException("A AIO já foi emitida para esta licitação.");
        }
        this.statusAio = StatusAio.SOLICITADO;
        this.updatedAt = Instant.now();
    }

    public void emitirAio(String numeroAio, LocalDate dataEmissaoAio) {
        garantirVrplAceitoParaAio();
        if (this.statusAio == StatusAio.EMITIDO) {
            throw new RegraNegocioLicitacaoException("A AIO já foi emitida para esta licitação.");
        }
        if (numeroAio == null || numeroAio.isBlank()) {
            throw new RegraNegocioLicitacaoException("O número da AIO é obrigatório.");
        }
        if (dataEmissaoAio == null) {
            throw new RegraNegocioLicitacaoException("A data de emissão da AIO é obrigatória.");
        }
        this.numeroAio = numeroAio.trim();
        this.dataEmissaoAio = dataEmissaoAio;
        this.statusAio = StatusAio.EMITIDO;
        this.updatedAt = Instant.now();
    }

    public boolean podeCriarMedicao() {
        return this.statusAio == StatusAio.EMITIDO;
    }

    public void garantirAioEmitidaParaMedicao() {
        if (!podeCriarMedicao()) {
            throw new AioNaoEmitidaException(this.id);
        }
    }

    public BigDecimal calcularValorEconomia() {
        if (this.valorHomologado == null) {
            return BigDecimal.ZERO;
        }
        return this.valorEstimado.subtract(this.valorHomologado).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calcularEconomia(BigDecimal valorEstimado, BigDecimal valorHomologado) {
        if (valorEstimado == null || valorEstimado.compareTo(BigDecimal.ZERO) == 0 || valorHomologado == null) {
            return BigDecimal.ZERO;
        }
        return valorEstimado.subtract(valorHomologado)
                .multiply(CEM)
                .divide(valorEstimado, 2, RoundingMode.HALF_UP);
    }

    private void garantirVrplAceitoParaAio() {
        if (this.statusVrpl != StatusVrpl.ACEITO_HOMOLOGADO) {
            throw new RegraNegocioLicitacaoException("A AIO só pode ser emitida com VRPL em ACEITO_HOMOLOGADO.");
        }
    }

    private void garantirNaoEncerrada() {
        if (this.situacao == SituacaoLicitacao.REVOGADA
                || this.situacao == SituacaoLicitacao.ANULADA
                || this.situacao == SituacaoLicitacao.DESERTA
                || this.situacao == SituacaoLicitacao.FRACASSADA) {
            throw new RegraNegocioLicitacaoException("Licitação encerrada não admite homologação.");
        }
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getConvenioId() { return convenioId; }
    public UUID getPrefeituraId() { return prefeituraId; }
    public String getNumeroProcessoAdministrativo() { return numeroProcessoAdministrativo; }
    public String getNumeroLicitacao() { return numeroLicitacao; }
    public ModalidadeLicitacao getModalidade() { return modalidade; }
    public CriterioJulgamento getCriterioJulgamento() { return criterioJulgamento; }
    public RegimeExecucao getRegimeExecucao() { return regimeExecucao; }
    public String getObjeto() { return objeto; }
    public BigDecimal getValorEstimado() { return valorEstimado; }
    public BigDecimal getValorHomologado() { return valorHomologado; }
    public BigDecimal getPercentualDesconto() { return percentualDesconto; }
    public SituacaoLicitacao getSituacao() { return situacao; }
    public LocalDate getDataPublicacaoEdital() { return dataPublicacaoEdital; }
    public LocalDate getDataAberturaPropostas() { return dataAberturaPropostas; }
    public LocalDate getDataHomologacao() { return dataHomologacao; }
    public String getLinkPncp() { return linkPncp; }
    public String getLinkTransferegov() { return linkTransferegov; }
    public String getLinkSistemaCompras() { return linkSistemaCompras; }
    public String getNumeroVrplTransferegov() { return numeroVrplTransferegov; }
    public StatusVrpl getStatusVrpl() { return statusVrpl; }
    public LocalDate getDataEnvioVrpl() { return dataEnvioVrpl; }
    public LocalDate getDataAceiteVrpl() { return dataAceiteVrpl; }
    public String getParecerTecnicoVrpl() { return parecerTecnicoVrpl; }
    public String getObservacoesDiligenciaVrpl() { return observacoesDiligenciaVrpl; }
    public String getNumeroAio() { return numeroAio; }
    public LocalDate getDataEmissaoAio() { return dataEmissaoAio; }
    public StatusAio getStatusAio() { return statusAio; }
    public String getCnpjVencedor() { return cnpjVencedor; }
    public String getRazaoSocialVencedor() { return razaoSocialVencedor; }
    public String getS3KeyEdital() { return s3KeyEdital; }
    public String getS3KeyTermoHomologacao() { return s3KeyTermoHomologacao; }
    public String getS3KeyPropostaVencedora() { return s3KeyPropostaVencedora; }
    public String getS3KeyAtaSessao() { return s3KeyAtaSessao; }
    public String getS3KeyParecerVrpl() { return s3KeyParecerVrpl; }
    public String getS3KeyAutorizacaoAio() { return s3KeyAutorizacaoAio; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
