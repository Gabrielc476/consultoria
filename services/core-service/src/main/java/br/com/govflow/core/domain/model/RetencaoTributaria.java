package br.com.govflow.core.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public record RetencaoTributaria(
        TipoRetencao tipo,
        BigDecimal aliquota,
        BigDecimal valor,
        double confianca,
        BoundingBox coordenadas
) {
    public RetencaoTributaria {
        Objects.requireNonNull(tipo, "Tipo de retenção é obrigatório.");
        if (aliquota == null) {
            aliquota = BigDecimal.ZERO;
        }
        if (valor == null) {
            valor = BigDecimal.ZERO;
        }
        confianca = Math.max(0.0, Math.min(1.0, confianca));
    }
}
