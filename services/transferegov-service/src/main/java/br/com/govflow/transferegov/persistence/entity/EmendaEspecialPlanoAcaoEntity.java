package br.com.govflow.transferegov.persistence.entity;

import br.com.govflow.transferegov.domain.compliance.StatusAdpf854;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tb_emenda_especial_plano_acao", schema = "transferegov_schema")
public class EmendaEspecialPlanoAcaoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "id_plano_acao", nullable = false, unique = true)
    private Long idPlanoAcao;

    @Column(name = "codigo_plano_acao", length = 50)
    private String codigoPlanoAcao;

    @Column(name = "ano_plano_acao")
    private Integer anoPlanoAcao;

    @Column(name = "modalidade_plano_acao", length = 50)
    private String modalidadePlanoAcao;

    @Column(name = "situacao_plano_acao", nullable = false, length = 50)
    private String situacaoPlanoAcao = "CIENTE";

    @Column(name = "data_aceite_plano_acao")
    private LocalDate dataAceitePlanoAcao;

    @Column(name = "cnpj_beneficiario", nullable = false, length = 18)
    private String cnpjBeneficiario;

    @Column(name = "nome_beneficiario", nullable = false, length = 200)
    private String nomeBeneficiario;

    @Column(name = "uf_beneficiario", nullable = false, length = 2)
    private String ufBeneficiario = "PB";

    @Column(name = "id_beneficiario")
    private Long idBeneficiario;

    @Column(name = "nome_parlamentar", length = 200)
    private String nomeParlamentar;

    @Column(name = "ano_emenda")
    private Integer anoEmenda;

    @Column(name = "numero_emenda")
    private Integer numeroEmenda;

    @Column(name = "codigo_emenda_formatado", length = 50)
    private String codigoEmendaFormatado;

    @Column(name = "categoria_despesa", length = 50)
    private String categoriaDespesa;

    @Column(name = "valor_custeio", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorCusteio = BigDecimal.ZERO;

    @Column(name = "valor_investimento", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorInvestimento = BigDecimal.ZERO;

    @Column(name = "valor_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorTotal = BigDecimal.ZERO;

    @Column(name = "nome_objeto", columnDefinition = "TEXT")
    private String nomeObjeto;

    @Column(name = "detalhamento_objeto", columnDefinition = "TEXT")
    private String detalhamentoObjeto;

    @Column(name = "area_politica_publica", columnDefinition = "TEXT")
    private String areaPoliticaPublica;

    @Column(name = "motivo_impedimento", columnDefinition = "TEXT")
    private String motivoImpedimento;

    @Column(name = "codigo_banco", length = 20)
    private String codigoBanco;

    @Column(name = "nome_banco", length = 100)
    private String nomeBanco;

    @Column(name = "numero_agencia", length = 20)
    private String numeroAgencia;

    @Column(name = "dv_agencia", length = 5)
    private String dvAgencia;

    @Column(name = "numero_conta", length = 30)
    private String numeroConta;

    @Column(name = "dv_conta", length = 5)
    private String dvConta;

    @Column(name = "situacao_dado_bancario", length = 100)
    private String situacaoDadoBancario;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_adpf854", nullable = false, length = 30)
    private StatusAdpf854 statusAdpf854 = StatusAdpf854.ALERTA;

    @OneToMany(mappedBy = "planoAcao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EmendaEspecialPlanoTrabalhoEntity> planosTrabalho = new ArrayList<>();

    @OneToMany(mappedBy = "planoAcao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EmendaEspecialRelatorioGestaoEntity> relatoriosGestao = new ArrayList<>();

    @OneToMany(mappedBy = "planoAcao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EmendaEspecialInconformidadeEntity> inconformidades = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public EmendaEspecialPlanoAcaoEntity() {}

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Long getIdPlanoAcao() { return idPlanoAcao; }
    public void setIdPlanoAcao(Long idPlanoAcao) { this.idPlanoAcao = idPlanoAcao; }

    public String getCodigoPlanoAcao() { return codigoPlanoAcao; }
    public void setCodigoPlanoAcao(String codigoPlanoAcao) { this.codigoPlanoAcao = codigoPlanoAcao; }

    public Integer getAnoPlanoAcao() { return anoPlanoAcao; }
    public void setAnoPlanoAcao(Integer anoPlanoAcao) { this.anoPlanoAcao = anoPlanoAcao; }

    public String getModalidadePlanoAcao() { return modalidadePlanoAcao; }
    public void setModalidadePlanoAcao(String modalidadePlanoAcao) { this.modalidadePlanoAcao = modalidadePlanoAcao; }

    public String getSituacaoPlanoAcao() { return situacaoPlanoAcao; }
    public void setSituacaoPlanoAcao(String situacaoPlanoAcao) { this.situacaoPlanoAcao = situacaoPlanoAcao; }

    public LocalDate getDataAceitePlanoAcao() { return dataAceitePlanoAcao; }
    public void setDataAceitePlanoAcao(LocalDate dataAceitePlanoAcao) { this.dataAceitePlanoAcao = dataAceitePlanoAcao; }

    public String getCnpjBeneficiario() { return cnpjBeneficiario; }
    public void setCnpjBeneficiario(String cnpjBeneficiario) { this.cnpjBeneficiario = cnpjBeneficiario; }

    public String getNomeBeneficiario() { return nomeBeneficiario; }
    public void setNomeBeneficiario(String nomeBeneficiario) { this.nomeBeneficiario = nomeBeneficiario; }

    public String getUfBeneficiario() { return ufBeneficiario; }
    public void setUfBeneficiario(String ufBeneficiario) { this.ufBeneficiario = ufBeneficiario; }

    public Long getIdBeneficiario() { return idBeneficiario; }
    public void setIdBeneficiario(Long idBeneficiario) { this.idBeneficiario = idBeneficiario; }

    public String getNomeParlamentar() { return nomeParlamentar; }
    public void setNomeParlamentar(String nomeParlamentar) { this.nomeParlamentar = nomeParlamentar; }

    public Integer getAnoEmenda() { return anoEmenda; }
    public void setAnoEmenda(Integer anoEmenda) { this.anoEmenda = anoEmenda; }

    public Integer getNumeroEmenda() { return numeroEmenda; }
    public void setNumeroEmenda(Integer numeroEmenda) { this.numeroEmenda = numeroEmenda; }

    public String getCodigoEmendaFormatado() { return codigoEmendaFormatado; }
    public void setCodigoEmendaFormatado(String codigoEmendaFormatado) { this.codigoEmendaFormatado = codigoEmendaFormatado; }

    public String getCategoriaDespesa() { return categoriaDespesa; }
    public void setCategoriaDespesa(String categoriaDespesa) { this.categoriaDespesa = categoriaDespesa; }

    public BigDecimal getValorCusteio() { return valorCusteio; }
    public void setValorCusteio(BigDecimal valorCusteio) { this.valorCusteio = valorCusteio; }

    public BigDecimal getValorInvestimento() { return valorInvestimento; }
    public void setValorInvestimento(BigDecimal valorInvestimento) { this.valorInvestimento = valorInvestimento; }

    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }

    public String getNomeObjeto() { return nomeObjeto; }
    public void setNomeObjeto(String nomeObjeto) { this.nomeObjeto = nomeObjeto; }

    public String getDetalhamentoObjeto() { return detalhamentoObjeto; }
    public void setDetalhamentoObjeto(String detalhamentoObjeto) { this.detalhamentoObjeto = detalhamentoObjeto; }

    public String getAreaPoliticaPublica() { return areaPoliticaPublica; }
    public void setAreaPoliticaPublica(String areaPoliticaPublica) { this.areaPoliticaPublica = areaPoliticaPublica; }

    public String getMotivoImpedimento() { return motivoImpedimento; }
    public void setMotivoImpedimento(String motivoImpedimento) { this.motivoImpedimento = motivoImpedimento; }

    public String getCodigoBanco() { return codigoBanco; }
    public void setCodigoBanco(String codigoBanco) { this.codigoBanco = codigoBanco; }

    public String getNomeBanco() { return nomeBanco; }
    public void setNomeBanco(String nomeBanco) { this.nomeBanco = nomeBanco; }

    public String getNumeroAgencia() { return numeroAgencia; }
    public void setNumeroAgencia(String numeroAgencia) { this.numeroAgencia = numeroAgencia; }

    public String getDvAgencia() { return dvAgencia; }
    public void setDvAgencia(String dvAgencia) { this.dvAgencia = dvAgencia; }

    public String getNumeroConta() { return numeroConta; }
    public void setNumeroConta(String numeroConta) { this.numeroConta = numeroConta; }

    public String getDvConta() { return dvConta; }
    public void setDvConta(String dvConta) { this.dvConta = dvConta; }

    public String getSituacaoDadoBancario() { return situacaoDadoBancario; }
    public void setSituacaoDadoBancario(String situacaoDadoBancario) { this.situacaoDadoBancario = situacaoDadoBancario; }

    public StatusAdpf854 getStatusAdpf854() { return statusAdpf854; }
    public void setStatusAdpf854(StatusAdpf854 statusAdpf854) { this.statusAdpf854 = statusAdpf854; }

    public List<EmendaEspecialPlanoTrabalhoEntity> getPlanosTrabalho() { return planosTrabalho; }
    public void setPlanosTrabalho(List<EmendaEspecialPlanoTrabalhoEntity> planosTrabalho) { this.planosTrabalho = planosTrabalho; }

    public List<EmendaEspecialRelatorioGestaoEntity> getRelatoriosGestao() { return relatoriosGestao; }
    public void setRelatoriosGestao(List<EmendaEspecialRelatorioGestaoEntity> relatoriosGestao) { this.relatoriosGestao = relatoriosGestao; }

    public List<EmendaEspecialInconformidadeEntity> getInconformidades() { return inconformidades; }
    public void setInconformidades(List<EmendaEspecialInconformidadeEntity> inconformidades) { this.inconformidades = inconformidades; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static EmendaEspecialPlanoAcaoEntity fromDto(
            br.com.govflow.transferegov.sync.client.dto.PlanoAcaoEspecialDTO planoDto,
            br.com.govflow.transferegov.sync.client.dto.BeneficiarioEspecialDTO benef,
            StatusAdpf854 status
    ) {
        EmendaEspecialPlanoAcaoEntity entity = new EmendaEspecialPlanoAcaoEntity();
        entity.updateFromDto(planoDto, benef, status);
        return entity;
    }

    public void updateFromDto(
            br.com.govflow.transferegov.sync.client.dto.PlanoAcaoEspecialDTO planoDto,
            br.com.govflow.transferegov.sync.client.dto.BeneficiarioEspecialDTO benef,
            StatusAdpf854 status
    ) {
        this.idPlanoAcao = planoDto.idPlanoAcao();
        this.codigoPlanoAcao = planoDto.codigoPlanoAcao();
        this.anoPlanoAcao = planoDto.anoPlanoAcao();
        this.modalidadePlanoAcao = planoDto.modalidadePlanoAcao();
        this.situacaoPlanoAcao = planoDto.situacaoPlanoAcao() != null ? planoDto.situacaoPlanoAcao() : "CIENTE";
        this.dataAceitePlanoAcao = planoDto.dataAceitePlanoAcao();

        if (benef != null) {
            this.cnpjBeneficiario = benef.cnpjBeneficiario() != null ? benef.cnpjBeneficiario() : "00000000000000";
            this.nomeBeneficiario = benef.nomeBeneficiario() != null ? benef.nomeBeneficiario() : "MUNICÍPIO NÃO IDENTIFICADO";
            this.ufBeneficiario = benef.ufBeneficiario() != null ? benef.ufBeneficiario() : "PB";
            this.idBeneficiario = benef.idBeneficiario();
        }

        this.nomeParlamentar = planoDto.nomeParlamentarEmendaPlanoAcao();
        this.anoEmenda = planoDto.anoEmendaParlamentarPlanoAcao();
        this.numeroEmenda = planoDto.numeroEmendaParlamentarPlanoAcao();
        this.codigoEmendaFormatado = planoDto.codigoEmendaParlamentarFormatadoPlanoAcao();

        this.categoriaDespesa = planoDto.categoriaDespesaPlanoAcao();
        this.valorCusteio = planoDto.valorCusteioPlanoAcao() != null ? planoDto.valorCusteioPlanoAcao() : BigDecimal.ZERO;
        this.valorInvestimento = planoDto.valorInvestimentoPlanoAcao() != null ? planoDto.valorInvestimentoPlanoAcao() : BigDecimal.ZERO;
        this.valorTotal = planoDto.valorTotal();

        this.nomeObjeto = planoDto.nomeObjeto();
        this.detalhamentoObjeto = planoDto.detalhamentoObjeto();
        this.areaPoliticaPublica = planoDto.areasPoliticasPublicasPlanoAcao();
        this.motivoImpedimento = planoDto.motivoImpedimentoPlanoAcao();

        this.codigoBanco = planoDto.codigoBancoPlanoAcao();
        this.nomeBanco = planoDto.nomeBancoPlanoAcao();
        this.numeroAgencia = planoDto.numeroAgenciaPlanoAcao();
        this.dvAgencia = planoDto.dvAgenciaPlanoAcao();
        this.numeroConta = planoDto.numeroContaPlanoAcao();
        this.dvConta = planoDto.dvContaPlanoAcao();
        this.situacaoDadoBancario = planoDto.descricaoSituacaoDadoBancarioPlanoAcao();

        this.statusAdpf854 = status;
        this.updatedAt = OffsetDateTime.now();
    }
}
