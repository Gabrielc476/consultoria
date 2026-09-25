package br.com.govflow.transferegov.persistence.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_emenda_especial_plano_trabalho", schema = "transferegov_schema")
public class EmendaEspecialPlanoTrabalhoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "id_plano_trabalho", nullable = false, unique = true)
    private Long idPlanoTrabalho;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plano_acao_id", nullable = false)
    private EmendaEspecialPlanoAcaoEntity planoAcao;

    @Column(name = "id_plano_acao", nullable = false)
    private Long idPlanoAcao;

    @Column(name = "situacao_plano_trabalho", nullable = false, length = 50)
    private String situacaoPlanoTrabalho;

    @Column(name = "data_inicio_execucao")
    private LocalDate dataInicioExecucao;

    @Column(name = "data_fim_execucao")
    private LocalDate dataFimExecucao;

    @Column(name = "prazo_execucao_meses")
    private Integer prazoExecucaoMeses;

    @Column(name = "data_aprovacao")
    private OffsetDateTime dataAprovacao;

    @Column(name = "ind_orgao_analises_pendentes", length = 10)
    private String indOrgaoAnalisesPendentes;

    @Column(name = "classificacao_orcamentaria", columnDefinition = "TEXT")
    private String classificacaoOrcamentaria;

    @Column(name = "justificativa_prorrogacao", columnDefinition = "TEXT")
    private String justificativaProrrogacao;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public EmendaEspecialPlanoTrabalhoEntity() {}

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Long getIdPlanoTrabalho() { return idPlanoTrabalho; }
    public void setIdPlanoTrabalho(Long idPlanoTrabalho) { this.idPlanoTrabalho = idPlanoTrabalho; }

    public EmendaEspecialPlanoAcaoEntity getPlanoAcao() { return planoAcao; }
    public void setPlanoAcao(EmendaEspecialPlanoAcaoEntity planoAcao) { this.planoAcao = planoAcao; }

    public Long getIdPlanoAcao() { return idPlanoAcao; }
    public void setIdPlanoAcao(Long idPlanoAcao) { this.idPlanoAcao = idPlanoAcao; }

    public String getSituacaoPlanoTrabalho() { return situacaoPlanoTrabalho; }
    public void setSituacaoPlanoTrabalho(String situacaoPlanoTrabalho) { this.situacaoPlanoTrabalho = situacaoPlanoTrabalho; }

    public LocalDate getDataInicioExecucao() { return dataInicioExecucao; }
    public void setDataInicioExecucao(LocalDate dataInicioExecucao) { this.dataInicioExecucao = dataInicioExecucao; }

    public LocalDate getDataFimExecucao() { return dataFimExecucao; }
    public void setDataFimExecucao(LocalDate dataFimExecucao) { this.dataFimExecucao = dataFimExecucao; }

    public Integer getPrazoExecucaoMeses() { return prazoExecucaoMeses; }
    public void setPrazoExecucaoMeses(Integer prazoExecucaoMeses) { this.prazoExecucaoMeses = prazoExecucaoMeses; }

    public OffsetDateTime getDataAprovacao() { return dataAprovacao; }
    public void setDataAprovacao(OffsetDateTime dataAprovacao) { this.dataAprovacao = dataAprovacao; }

    public String getIndOrgaoAnalisesPendentes() { return indOrgaoAnalisesPendentes; }
    public void setIndOrgaoAnalisesPendentes(String indOrgaoAnalisesPendentes) { this.indOrgaoAnalisesPendentes = indOrgaoAnalisesPendentes; }

    public String getClassificacaoOrcamentaria() { return classificacaoOrcamentaria; }
    public void setClassificacaoOrcamentaria(String classificacaoOrcamentaria) { this.classificacaoOrcamentaria = classificacaoOrcamentaria; }

    public String getJustificativaProrrogacao() { return justificativaProrrogacao; }
    public void setJustificativaProrrogacao(String justificativaProrrogacao) { this.justificativaProrrogacao = justificativaProrrogacao; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static EmendaEspecialPlanoTrabalhoEntity fromDto(
            EmendaEspecialPlanoAcaoEntity planoAcao,
            br.com.govflow.transferegov.sync.client.dto.PlanoTrabalhoEspecialDTO dto
    ) {
        EmendaEspecialPlanoTrabalhoEntity entity = new EmendaEspecialPlanoTrabalhoEntity();
        entity.setPlanoAcao(planoAcao);
        entity.setIdPlanoAcao(planoAcao != null ? planoAcao.getIdPlanoAcao() : null);
        entity.updateFromDto(dto);
        return entity;
    }

    public void updateFromDto(br.com.govflow.transferegov.sync.client.dto.PlanoTrabalhoEspecialDTO dto) {
        this.idPlanoTrabalho = dto.idPlanoTrabalho();
        this.situacaoPlanoTrabalho = dto.situacaoPlanoTrabalho() != null ? dto.situacaoPlanoTrabalho() : "EM_ELABORACAO";
        this.dataInicioExecucao = dto.dataInicioExecucaoPlanoTrabalho();
        this.dataFimExecucao = dto.dataFimExecucaoPlanoTrabalho();
        this.prazoExecucaoMeses = dto.prazoExecucaoMesesPlanoTrabalho();
        this.indOrgaoAnalisesPendentes = dto.indOrgaoAnalisesPendentes();
        this.classificacaoOrcamentaria = dto.classificacaoOrcamentariaPt();
        this.justificativaProrrogacao = dto.justificativaProrrogacaoParalizacaoPt();
        this.updatedAt = OffsetDateTime.now();
    }
}
