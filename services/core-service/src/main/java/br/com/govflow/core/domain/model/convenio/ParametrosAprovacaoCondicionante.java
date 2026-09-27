package br.com.govflow.core.domain.model.convenio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Value Object que encapsula os parâmetros técnicos e documentais da aprovação
 * de uma condicionante suspensiva (Manual MN AE099 Caixa GIGOV / TCU).
 */
public final class ParametrosAprovacaoCondicionante {

    private final String numeroDocumentoComprobatorio;
    private final LocalDate dataAprovacao;
    private final LocalDate dataValidade;
    private final BigDecimal valorOrcamentoAprovado;
    private final BigDecimal percentualBdiAprovado;
    private final String numeroArtRrt;
    private final String orgaoEmissor;
    private final String s3KeyDocumento;

    public ParametrosAprovacaoCondicionante(
            String numeroDocumentoComprobatorio,
            LocalDate dataAprovacao,
            LocalDate dataValidade,
            BigDecimal valorOrcamentoAprovado,
            BigDecimal percentualBdiAprovado,
            String numeroArtRrt,
            String orgaoEmissor,
            String s3KeyDocumento) {
        this.numeroDocumentoComprobatorio = Objects.requireNonNull(numeroDocumentoComprobatorio, "Número do documento comprobatório é obrigatório");
        this.dataAprovacao = dataAprovacao != null ? dataAprovacao : LocalDate.now();
        this.dataValidade = dataValidade;
        this.valorOrcamentoAprovado = valorOrcamentoAprovado;
        this.percentualBdiAprovado = percentualBdiAprovado;
        this.numeroArtRrt = numeroArtRrt;
        this.orgaoEmissor = orgaoEmissor != null ? orgaoEmissor : "Mandatária Caixa GIGOV";
        this.s3KeyDocumento = s3KeyDocumento;
    }

    public static Builder builder() {
        return new Builder();
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

    public BigDecimal getValorOrcamentoAprovado() {
        return valorOrcamentoAprovado;
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

    public String getS3KeyDocumento() {
        return s3KeyDocumento;
    }

    public static class Builder {
        private String numeroDocumentoComprobatorio;
        private LocalDate dataAprovacao = LocalDate.now();
        private LocalDate dataValidade;
        private BigDecimal valorOrcamentoAprovado;
        private BigDecimal percentualBdiAprovado;
        private String numeroArtRrt;
        private String orgaoEmissor = "Mandatária Caixa GIGOV";
        private String s3KeyDocumento;

        public Builder numeroDocumentoComprobatorio(String numeroDocumentoComprobatorio) {
            this.numeroDocumentoComprobatorio = numeroDocumentoComprobatorio;
            return this;
        }

        public Builder dataAprovacao(LocalDate dataAprovacao) {
            this.dataAprovacao = dataAprovacao;
            return this;
        }

        public Builder dataValidade(LocalDate dataValidade) {
            this.dataValidade = dataValidade;
            return this;
        }

        public Builder valorOrcamentoAprovado(BigDecimal valorOrcamentoAprovado) {
            this.valorOrcamentoAprovado = valorOrcamentoAprovado;
            return this;
        }

        public Builder percentualBdiAprovado(BigDecimal percentualBdiAprovado) {
            this.percentualBdiAprovado = percentualBdiAprovado;
            return this;
        }

        public Builder numeroArtRrt(String numeroArtRrt) {
            this.numeroArtRrt = numeroArtRrt;
            return this;
        }

        public Builder orgaoEmissor(String orgaoEmissor) {
            this.orgaoEmissor = orgaoEmissor;
            return this;
        }

        public Builder s3KeyDocumento(String s3KeyDocumento) {
            this.s3KeyDocumento = s3KeyDocumento;
            return this;
        }

        public ParametrosAprovacaoCondicionante build() {
            return new ParametrosAprovacaoCondicionante(
                    numeroDocumentoComprobatorio,
                    dataAprovacao,
                    dataValidade,
                    valorOrcamentoAprovado,
                    percentualBdiAprovado,
                    numeroArtRrt,
                    orgaoEmissor,
                    s3KeyDocumento
            );
        }
    }
}
