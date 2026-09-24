package br.com.govflow.transferegov.domain.quality;

/**
 * Dimensões fundamentais de qualidade de dados conforme o Data Quality Framework.
 */
public enum DataQualityDimension {
    COMPLETENESS("Completude: ausência de valores obrigatórios nulos ou vazios"),
    UNIQUENESS("Unicidade: garantia de chaves primárias e sem duplicidade de registros"),
    VALIDITY("Validade: conformidade com formatos, tipos de dados, máscaras e intervalos"),
    CONSISTENCY("Consistência: ausência de contradições lógicas entre campos relacionados"),
    ACCURACY("Acurácia: integridade relacional entre entidades dependentes"),
    TIMELINESS("Atualidade: frescor dos dados e verificação de sentinelas de carga");

    private final String description;

    DataQualityDimension(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
