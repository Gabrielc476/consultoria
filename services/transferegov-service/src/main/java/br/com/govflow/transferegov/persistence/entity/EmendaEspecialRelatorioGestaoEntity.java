package br.com.govflow.transferegov.persistence.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_emenda_especial_relatorio_gestao", schema = "transferegov_schema")
public class EmendaEspecialRelatorioGestaoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "id_relatorio_gestao_novo", nullable = false, unique = true)
    private Long idRelatorioGestaoNovo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plano_acao_id", nullable = false)
    private EmendaEspecialPlanoAcaoEntity planoAcao;

    @Column(name = "id_plano_acao", nullable = false)
    private Long idPlanoAcao;

    @Column(name = "tipo_relatorio", nullable = false, length = 30)
    private String tipoRelatorio;

    @Column(name = "situacao_relatorio", nullable = false, length = 50)
    private String situacaoRelatorio;

    @Column(name = "data_relatorio")
    private LocalDate dataRelatorio;

    @Column(name = "valor_executado", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorExecutado = BigDecimal.ZERO;

    @Column(name = "valor_pendente", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorPendente = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public EmendaEspecialRelatorioGestaoEntity() {}

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Long getIdRelatorioGestaoNovo() { return idRelatorioGestaoNovo; }
    public void setIdRelatorioGestaoNovo(Long idRelatorioGestaoNovo) { this.idRelatorioGestaoNovo = idRelatorioGestaoNovo; }

    public EmendaEspecialPlanoAcaoEntity getPlanoAcao() { return planoAcao; }
    public void setPlanoAcao(EmendaEspecialPlanoAcaoEntity planoAcao) { this.planoAcao = planoAcao; }

    public Long getIdPlanoAcao() { return idPlanoAcao; }
    public void setIdPlanoAcao(Long idPlanoAcao) { this.idPlanoAcao = idPlanoAcao; }

    public String getTipoRelatorio() { return tipoRelatorio; }
    public void setTipoRelatorio(String tipoRelatorio) { this.tipoRelatorio = tipoRelatorio; }

    public String getSituacaoRelatorio() { return situacaoRelatorio; }
    public void setSituacaoRelatorio(String situacaoRelatorio) { this.situacaoRelatorio = situacaoRelatorio; }

    public LocalDate getDataRelatorio() { return dataRelatorio; }
    public void setDataRelatorio(LocalDate dataRelatorio) { this.dataRelatorio = dataRelatorio; }

    public BigDecimal getValorExecutado() { return valorExecutado; }
    public void setValorExecutado(BigDecimal valorExecutado) { this.valorExecutado = valorExecutado; }

    public BigDecimal getValorPendente() { return valorPendente; }
    public void setValorPendente(BigDecimal valorPendente) { this.valorPendente = valorPendente; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static EmendaEspecialRelatorioGestaoEntity fromDto(
            EmendaEspecialPlanoAcaoEntity planoAcao,
            br.com.govflow.transferegov.sync.client.dto.RelatorioGestaoEspecialDTO dto
    ) {
        EmendaEspecialRelatorioGestaoEntity entity = new EmendaEspecialRelatorioGestaoEntity();
        entity.setPlanoAcao(planoAcao);
        entity.setIdPlanoAcao(planoAcao != null ? planoAcao.getIdPlanoAcao() : null);
        entity.updateFromDto(dto);
        return entity;
    }

    public void updateFromDto(br.com.govflow.transferegov.sync.client.dto.RelatorioGestaoEspecialDTO dto) {
        this.idRelatorioGestaoNovo = dto.idRelatorioGestaoNovo();
        this.tipoRelatorio = dto.tipoRelatorioGestaoNovo() != null ? dto.tipoRelatorioGestaoNovo() : "Final";
        this.situacaoRelatorio = dto.situacaoRelatorioGestaoNovo() != null ? dto.situacaoRelatorioGestaoNovo() : "EM_ELABORACAO";
        this.dataRelatorio = dto.dataRelatorioGestaoNovo();
        this.valorExecutado = dto.valorExecutadoRelatorioGestaoNovo() != null ? dto.valorExecutadoRelatorioGestaoNovo() : BigDecimal.ZERO;
        this.valorPendente = dto.valorPendenteRelatorioGestaoNovo() != null ? dto.valorPendenteRelatorioGestaoNovo() : BigDecimal.ZERO;
        this.updatedAt = OffsetDateTime.now();
    }
}
