package br.com.govflow.core.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tb_licitacoes", schema = "core_schema")
public class LicitacaoJpaEntity extends BaseTenantEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "convenio_id", nullable = false)
    private UUID convenioId;

    @Column(name = "prefeitura_id", nullable = false)
    private UUID prefeituraId;

    @Column(name = "numero_processo_administrativo", length = 50)
    private String numeroProcessoAdministrativo;

    @Column(name = "numero_licitacao", length = 50, nullable = false)
    private String numeroLicitacao;

    @Column(name = "modalidade", length = 50, nullable = false)
    private String modalidade;

    @Column(name = "criterio_julgamento", length = 50)
    private String criterioJulgamento;

    @Column(name = "regime_execucao", length = 50)
    private String regimeExecucao;

    @Column(name = "objeto", nullable = false, columnDefinition = "TEXT")
    private String objeto;

    @Column(name = "valor_estimado", precision = 15, scale = 2, nullable = false)
    private BigDecimal valorEstimado;

    @Column(name = "valor_homologado", precision = 15, scale = 2)
    private BigDecimal valorHomologado;

    @Column(name = "percentual_desconto", precision = 5, scale = 2)
    private BigDecimal percentualDesconto;

    @Column(name = "situacao", length = 50, nullable = false)
    private String situacao;

    @Column(name = "data_publicacao_edital")
    private LocalDate dataPublicacaoEdital;

    @Column(name = "data_abertura_propostas")
    private LocalDate dataAberturaPropostas;

    @Column(name = "data_homologacao")
    private LocalDate dataHomologacao;

    @Column(name = "link_pncp", length = 500)
    private String linkPncp;

    @Column(name = "link_transferegov", length = 500)
    private String linkTransferegov;

    @Column(name = "link_sistema_compras", length = 500)
    private String linkSistemaCompras;

    @Column(name = "numero_vrpl_transferegov", length = 50)
    private String numeroVrplTransferegov;

    @Column(name = "status_vrpl", length = 30, nullable = false)
    private String statusVrpl;

    @Column(name = "data_envio_vrpl")
    private LocalDate dataEnvioVrpl;

    @Column(name = "data_aceite_vrpl")
    private LocalDate dataAceiteVrpl;

    @Column(name = "parecer_tecnico_vrpl", columnDefinition = "TEXT")
    private String parecerTecnicoVrpl;

    @Column(name = "observacoes_diligencia_vrpl", columnDefinition = "TEXT")
    private String observacoesDiligenciaVrpl;

    @Column(name = "numero_aio", length = 50)
    private String numeroAio;

    @Column(name = "data_emissao_aio")
    private LocalDate dataEmissaoAio;

    @Column(name = "status_aio", length = 30, nullable = false)
    private String statusAio;

    @Column(name = "cnpj_vencedor", length = 18)
    private String cnpjVencedor;

    @Column(name = "razao_social_vencedor", length = 200)
    private String razaoSocialVencedor;

    @Column(name = "s3_key_edital", length = 500)
    private String s3KeyEdital;

    @Column(name = "s3_key_termo_homologacao", length = 500)
    private String s3KeyTermoHomologacao;

    @Column(name = "s3_key_proposta_vencedora", length = 500)
    private String s3KeyPropostaVencedora;

    @Column(name = "s3_key_ata_sessao", length = 500)
    private String s3KeyAtaSessao;

    @Column(name = "s3_key_parecer_vrpl", length = 500)
    private String s3KeyParecerVrpl;

    @Column(name = "s3_key_autorizacao_aio", length = 500)
    private String s3KeyAutorizacaoAio;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public LicitacaoJpaEntity() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getConvenioId() { return convenioId; }
    public void setConvenioId(UUID convenioId) { this.convenioId = convenioId; }
    public UUID getPrefeituraId() { return prefeituraId; }
    public void setPrefeituraId(UUID prefeituraId) { this.prefeituraId = prefeituraId; }
    public String getNumeroProcessoAdministrativo() { return numeroProcessoAdministrativo; }
    public void setNumeroProcessoAdministrativo(String numeroProcessoAdministrativo) { this.numeroProcessoAdministrativo = numeroProcessoAdministrativo; }
    public String getNumeroLicitacao() { return numeroLicitacao; }
    public void setNumeroLicitacao(String numeroLicitacao) { this.numeroLicitacao = numeroLicitacao; }
    public String getModalidade() { return modalidade; }
    public void setModalidade(String modalidade) { this.modalidade = modalidade; }
    public String getCriterioJulgamento() { return criterioJulgamento; }
    public void setCriterioJulgamento(String criterioJulgamento) { this.criterioJulgamento = criterioJulgamento; }
    public String getRegimeExecucao() { return regimeExecucao; }
    public void setRegimeExecucao(String regimeExecucao) { this.regimeExecucao = regimeExecucao; }
    public String getObjeto() { return objeto; }
    public void setObjeto(String objeto) { this.objeto = objeto; }
    public BigDecimal getValorEstimado() { return valorEstimado; }
    public void setValorEstimado(BigDecimal valorEstimado) { this.valorEstimado = valorEstimado; }
    public BigDecimal getValorHomologado() { return valorHomologado; }
    public void setValorHomologado(BigDecimal valorHomologado) { this.valorHomologado = valorHomologado; }
    public BigDecimal getPercentualDesconto() { return percentualDesconto; }
    public void setPercentualDesconto(BigDecimal percentualDesconto) { this.percentualDesconto = percentualDesconto; }
    public String getSituacao() { return situacao; }
    public void setSituacao(String situacao) { this.situacao = situacao; }
    public LocalDate getDataPublicacaoEdital() { return dataPublicacaoEdital; }
    public void setDataPublicacaoEdital(LocalDate dataPublicacaoEdital) { this.dataPublicacaoEdital = dataPublicacaoEdital; }
    public LocalDate getDataAberturaPropostas() { return dataAberturaPropostas; }
    public void setDataAberturaPropostas(LocalDate dataAberturaPropostas) { this.dataAberturaPropostas = dataAberturaPropostas; }
    public LocalDate getDataHomologacao() { return dataHomologacao; }
    public void setDataHomologacao(LocalDate dataHomologacao) { this.dataHomologacao = dataHomologacao; }
    public String getLinkPncp() { return linkPncp; }
    public void setLinkPncp(String linkPncp) { this.linkPncp = linkPncp; }
    public String getLinkTransferegov() { return linkTransferegov; }
    public void setLinkTransferegov(String linkTransferegov) { this.linkTransferegov = linkTransferegov; }
    public String getLinkSistemaCompras() { return linkSistemaCompras; }
    public void setLinkSistemaCompras(String linkSistemaCompras) { this.linkSistemaCompras = linkSistemaCompras; }
    public String getNumeroVrplTransferegov() { return numeroVrplTransferegov; }
    public void setNumeroVrplTransferegov(String numeroVrplTransferegov) { this.numeroVrplTransferegov = numeroVrplTransferegov; }
    public String getStatusVrpl() { return statusVrpl; }
    public void setStatusVrpl(String statusVrpl) { this.statusVrpl = statusVrpl; }
    public LocalDate getDataEnvioVrpl() { return dataEnvioVrpl; }
    public void setDataEnvioVrpl(LocalDate dataEnvioVrpl) { this.dataEnvioVrpl = dataEnvioVrpl; }
    public LocalDate getDataAceiteVrpl() { return dataAceiteVrpl; }
    public void setDataAceiteVrpl(LocalDate dataAceiteVrpl) { this.dataAceiteVrpl = dataAceiteVrpl; }
    public String getParecerTecnicoVrpl() { return parecerTecnicoVrpl; }
    public void setParecerTecnicoVrpl(String parecerTecnicoVrpl) { this.parecerTecnicoVrpl = parecerTecnicoVrpl; }
    public String getObservacoesDiligenciaVrpl() { return observacoesDiligenciaVrpl; }
    public void setObservacoesDiligenciaVrpl(String observacoesDiligenciaVrpl) { this.observacoesDiligenciaVrpl = observacoesDiligenciaVrpl; }
    public String getNumeroAio() { return numeroAio; }
    public void setNumeroAio(String numeroAio) { this.numeroAio = numeroAio; }
    public LocalDate getDataEmissaoAio() { return dataEmissaoAio; }
    public void setDataEmissaoAio(LocalDate dataEmissaoAio) { this.dataEmissaoAio = dataEmissaoAio; }
    public String getStatusAio() { return statusAio; }
    public void setStatusAio(String statusAio) { this.statusAio = statusAio; }
    public String getCnpjVencedor() { return cnpjVencedor; }
    public void setCnpjVencedor(String cnpjVencedor) { this.cnpjVencedor = cnpjVencedor; }
    public String getRazaoSocialVencedor() { return razaoSocialVencedor; }
    public void setRazaoSocialVencedor(String razaoSocialVencedor) { this.razaoSocialVencedor = razaoSocialVencedor; }
    public String getS3KeyEdital() { return s3KeyEdital; }
    public void setS3KeyEdital(String s3KeyEdital) { this.s3KeyEdital = s3KeyEdital; }
    public String getS3KeyTermoHomologacao() { return s3KeyTermoHomologacao; }
    public void setS3KeyTermoHomologacao(String s3KeyTermoHomologacao) { this.s3KeyTermoHomologacao = s3KeyTermoHomologacao; }
    public String getS3KeyPropostaVencedora() { return s3KeyPropostaVencedora; }
    public void setS3KeyPropostaVencedora(String s3KeyPropostaVencedora) { this.s3KeyPropostaVencedora = s3KeyPropostaVencedora; }
    public String getS3KeyAtaSessao() { return s3KeyAtaSessao; }
    public void setS3KeyAtaSessao(String s3KeyAtaSessao) { this.s3KeyAtaSessao = s3KeyAtaSessao; }
    public String getS3KeyParecerVrpl() { return s3KeyParecerVrpl; }
    public void setS3KeyParecerVrpl(String s3KeyParecerVrpl) { this.s3KeyParecerVrpl = s3KeyParecerVrpl; }
    public String getS3KeyAutorizacaoAio() { return s3KeyAutorizacaoAio; }
    public void setS3KeyAutorizacaoAio(String s3KeyAutorizacaoAio) { this.s3KeyAutorizacaoAio = s3KeyAutorizacaoAio; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
