package br.com.govflow.core.domain.exception;

import java.math.BigDecimal;

public class InconsistenciaMatematicaException extends DomainException {

    private final BigDecimal valorBruto;
    private final BigDecimal totalDeducoes;
    private final BigDecimal valorLiquido;
    private final BigDecimal diferenca;

    public InconsistenciaMatematicaException(BigDecimal valorBruto, BigDecimal totalDeducoes, BigDecimal valorLiquido, BigDecimal diferenca) {
        super("INCONSISTENCIA_MATEMATICA_FISCAL",
                String.format("Inconsistência matemática nos valores fiscais: Bruto (%s) - Deduções (%s) != Líquido (%s). Diferença calculada: %s (tolerância máxima: R$ 0.00).",
                        valorBruto, totalDeducoes, valorLiquido, diferenca));
        this.valorBruto = valorBruto;
        this.totalDeducoes = totalDeducoes;
        this.valorLiquido = valorLiquido;
        this.diferenca = diferenca;
    }

    public InconsistenciaMatematicaException(String mensagem) {
        super("INCONSISTENCIA_MATEMATICA_FISCAL", mensagem);
        this.valorBruto = BigDecimal.ZERO;
        this.totalDeducoes = BigDecimal.ZERO;
        this.valorLiquido = BigDecimal.ZERO;
        this.diferenca = BigDecimal.ZERO;
    }

    public BigDecimal getValorBruto() {
        return valorBruto;
    }

    public BigDecimal getTotalDeducoes() {
        return totalDeducoes;
    }

    public BigDecimal getValorLiquido() {
        return valorLiquido;
    }

    public BigDecimal getDiferenca() {
        return diferenca;
    }
}
