package br.com.govflow.core.domain.model.documento;

/**
 * Canal de origem da ingestão do documento no sistema GovFlow.
 */
public enum OrigemCanal {
    UPLOAD_MANUAL("Upload Manual Web"),
    WHATSAPP("Mensageria WhatsApp"),
    TRANSFEREGOV_CRAWLER("Crawler Oficial Transferegov"),
    EMAIL("Ingestão por E-mail"),
    OUTRO("Outro Canal");

    private final String descricao;

    OrigemCanal(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public static OrigemCanal fromString(String valor) {
        if (valor == null || valor.isBlank()) {
            return UPLOAD_MANUAL;
        }
        for (OrigemCanal o : values()) {
            if (o.name().equalsIgnoreCase(valor.trim())) {
                return o;
            }
        }
        return UPLOAD_MANUAL;
    }
}
