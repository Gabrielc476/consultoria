package br.com.govflow.transferegov.persistence.entity;

import br.com.govflow.transferegov.domain.compliance.Adpf854ComplianceResult.InconformidadeItem;
import br.com.govflow.transferegov.domain.compliance.SeveridadeInconformidade;
import br.com.govflow.transferegov.domain.compliance.TipoInconformidadeAdpf854;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_emenda_especial_inconformidade", schema = "transferegov_schema")
public class EmendaEspecialInconformidadeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plano_acao_id", nullable = false)
    private EmendaEspecialPlanoAcaoEntity planoAcao;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_inconformidade", nullable = false, length = 80)
    private TipoInconformidadeAdpf854 tipoInconformidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "severidade", nullable = false, length = 20)
    private SeveridadeInconformidade severidade;

    @Column(name = "descricao", nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "resolvido", nullable = false)
    private boolean resolvido = false;

    @Column(name = "data_deteccao", nullable = false)
    private OffsetDateTime dataDeteccao = OffsetDateTime.now();

    @Column(name = "data_resolucao")
    private OffsetDateTime dataResolucao;

    public EmendaEspecialInconformidadeEntity() {}

    public EmendaEspecialInconformidadeEntity(
            EmendaEspecialPlanoAcaoEntity planoAcao,
            TipoInconformidadeAdpf854 tipoInconformidade,
            SeveridadeInconformidade severidade,
            String descricao
    ) {
        this.planoAcao = planoAcao;
        this.tipoInconformidade = tipoInconformidade;
        this.severidade = severidade;
        this.descricao = descricao;
        this.resolvido = false;
        this.dataDeteccao = OffsetDateTime.now();
    }

    public static EmendaEspecialInconformidadeEntity fromCompliance(
            EmendaEspecialPlanoAcaoEntity planoAcao,
            InconformidadeItem item
    ) {
        return new EmendaEspecialInconformidadeEntity(
                planoAcao,
                item.tipo(),
                item.severidade(),
                item.descricao()
        );
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public EmendaEspecialPlanoAcaoEntity getPlanoAcao() { return planoAcao; }
    public void setPlanoAcao(EmendaEspecialPlanoAcaoEntity planoAcao) { this.planoAcao = planoAcao; }

    public TipoInconformidadeAdpf854 getTipoInconformidade() { return tipoInconformidade; }
    public void setTipoInconformidade(TipoInconformidadeAdpf854 tipoInconformidade) { this.tipoInconformidade = tipoInconformidade; }

    public SeveridadeInconformidade getSeveridade() { return severidade; }
    public void setSeveridade(SeveridadeInconformidade severidade) { this.severidade = severidade; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public boolean isResolvido() { return resolvido; }
    public void setResolvido(boolean resolvido) { this.resolvido = resolvido; }

    public OffsetDateTime getDataDeteccao() { return dataDeteccao; }
    public void setDataDeteccao(OffsetDateTime dataDeteccao) { this.dataDeteccao = dataDeteccao; }

    public OffsetDateTime getDataResolucao() { return dataResolucao; }
    public void setDataResolucao(OffsetDateTime dataResolucao) { this.dataResolucao = dataResolucao; }
}
