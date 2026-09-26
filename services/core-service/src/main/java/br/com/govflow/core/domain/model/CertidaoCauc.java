package br.com.govflow.core.domain.model;

import br.com.govflow.core.domain.exception.DataValidadeInvalidaException;
import br.com.govflow.core.domain.exception.DomainException;
import br.com.govflow.core.domain.exception.TenantInvalidoException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade de Domínio que representa uma das 16 certidões fiscais e orçamentárias
 * do Radar CAUC vinculadas a um município convenente.
 */
public class CertidaoCauc {

    private final UUID id;
    private final UUID tenantId;
    private final UUID prefeituraId;
    private final TipoExigenciaCauc tipoExigencia;
    private String numeroCertidao;
    private LocalDate dataEmissao;
    private LocalDate dataValidade;
    private StatusCertidao situacao;
    private Integer diasParaVencer;
    private String s3KeyComprovante;
    private final Instant createdAt;
    private Instant updatedAt;

    public CertidaoCauc(UUID id,
                        UUID tenantId,
                        UUID prefeituraId,
                        TipoExigenciaCauc tipoExigencia,
                        String numeroCertidao,
                        LocalDate dataEmissao,
                        LocalDate dataValidade,
                        StatusCertidao situacao,
                        Integer diasParaVencer,
                        String s3KeyComprovante,
                        Instant createdAt,
                        Instant updatedAt) {
        this.id = id != null ? id : UUID.randomUUID();
        if (tenantId == null) {
            throw new TenantInvalidoException("Tenant ID da certidão CAUC não pode ser nulo.");
        }
        this.tenantId = tenantId;
        this.prefeituraId = Objects.requireNonNull(prefeituraId, "PrefeituraId é obrigatório para CertidaoCauc.");
        this.tipoExigencia = Objects.requireNonNull(tipoExigencia, "TipoExigencia é obrigatório para CertidaoCauc.");
        this.numeroCertidao = numeroCertidao;
        this.dataEmissao = Objects.requireNonNull(dataEmissao, "Data de emissão é obrigatória.");
        this.dataValidade = Objects.requireNonNull(dataValidade, "Data de validade é obrigatória.");
        this.situacao = situacao != null ? situacao : StatusCertidao.REGULAR;
        this.diasParaVencer = diasParaVencer;
        this.s3KeyComprovante = s3KeyComprovante;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;

        validarInvariantes();
    }

    public static CertidaoCauc criarPadrao(UUID tenantId,
                                           UUID prefeituraId,
                                           TipoExigenciaCauc tipo,
                                           LocalDate dataReferencia) {
        LocalDate hoje = dataReferencia != null ? dataReferencia : LocalDate.now();
        LocalDate validadePadrao = hoje.plusDays(90);
        CertidaoCauc cert = new CertidaoCauc(
                UUID.randomUUID(),
                tenantId,
                prefeituraId,
                tipo,
                "PADRAO-" + tipo.getCodigo(),
                hoje,
                validadePadrao,
                StatusCertidao.REGULAR,
                90,
                null,
                Instant.now(),
                Instant.now()
        );
        cert.reavaliarSituacao(hoje);
        return cert;
    }

    private void validarInvariantes() {
        if (tenantId == null) {
            throw new TenantInvalidoException("Tenant ID da certidão CAUC não pode ser nulo.");
        }
        if (dataValidade.isBefore(dataEmissao)) {
            throw new DataValidadeInvalidaException("Data de validade (" + dataValidade + ") não pode ser anterior à data de emissão (" + dataEmissao + ").");
        }
    }

    /**
     * Reavalia a situação do semáforo com base na data de referência (hoje).
     */
    public void reavaliarSituacao(LocalDate dataReferencia) {
        LocalDate referencia = dataReferencia != null ? dataReferencia : LocalDate.now();
        long dias = ChronoUnit.DAYS.between(referencia, this.dataValidade);
        this.diasParaVencer = (int) dias;

        if (dias < 0) {
            this.situacao = StatusCertidao.VENCIDA;
        } else if (dias <= 10) {
            this.situacao = StatusCertidao.ALERTA;
        } else {
            this.situacao = StatusCertidao.REGULAR;
        }
        this.updatedAt = Instant.now();
    }

    /**
     * Atualiza os dados da certidão e recalcula a criticidade.
     */
    public void atualizarDados(String numeroCertidao,
                               LocalDate dataEmissao,
                               LocalDate dataValidade,
                               String s3KeyComprovante,
                               StatusCertidao situacaoForcada,
                               LocalDate dataReferencia) {
        if (numeroCertidao != null && !numeroCertidao.isBlank()) {
            this.numeroCertidao = numeroCertidao.trim();
        }
        if (dataEmissao != null) {
            this.dataEmissao = dataEmissao;
        }
        if (dataValidade != null) {
            this.dataValidade = dataValidade;
        }
        if (s3KeyComprovante != null && !s3KeyComprovante.isBlank()) {
            this.s3KeyComprovante = s3KeyComprovante.trim();
        }

        validarInvariantes();

        if (situacaoForcada != null) {
            this.situacao = situacaoForcada;
            LocalDate referencia = dataReferencia != null ? dataReferencia : LocalDate.now();
            this.diasParaVencer = (int) ChronoUnit.DAYS.between(referencia, this.dataValidade);
            this.updatedAt = Instant.now();
        } else {
            reavaliarSituacao(dataReferencia);
        }
    }

    /**
     * Verifica se a certidão atende aos critérios para emissão de alerta programado:
     * - Vencimento em exatamente 10 dias (D-10)
     * - Vencimento em exatamente 5 dias (D-5)
     * - Já vencida / expirada
     */
    public boolean isCriticaParaAlerta() {
        if (situacao == StatusCertidao.VENCIDA) {
            return true;
        }
        if (diasParaVencer != null) {
            return diasParaVencer == 10 || diasParaVencer == 5;
        }
        return false;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getPrefeituraId() {
        return prefeituraId;
    }

    public TipoExigenciaCauc getTipoExigencia() {
        return tipoExigencia;
    }

    public String getNumeroCertidao() {
        return numeroCertidao;
    }

    public LocalDate getDataEmissao() {
        return dataEmissao;
    }

    public LocalDate getDataValidade() {
        return dataValidade;
    }

    public StatusCertidao getSituacao() {
        return situacao;
    }

    public Integer getDiasParaVencer() {
        return diasParaVencer;
    }

    public String getS3KeyComprovante() {
        return s3KeyComprovante;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
