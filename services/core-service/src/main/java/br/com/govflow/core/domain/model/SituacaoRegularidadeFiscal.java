package br.com.govflow.core.domain.model;

/**
 * Representa a Situação de Regularidade Fiscal do município conforme vocabulário ubíquo do CONTEXT.md.
 * Valores: ADIMPLENTE, BLOQUEADO.
 */
public enum SituacaoRegularidadeFiscal {
    ADIMPLENTE,
    BLOQUEADO;

    public static SituacaoRegularidadeFiscal fromStatusCauc(StatusCauc statusCauc) {
        if (statusCauc == null) {
            return ADIMPLENTE;
        }
        return switch (statusCauc) {
            case ADIMPLENTE -> ADIMPLENTE;
            case BLOQUEADO -> BLOQUEADO;
        };
    }

    public StatusCauc toStatusCauc() {
        return switch (this) {
            case ADIMPLENTE -> StatusCauc.ADIMPLENTE;
            case BLOQUEADO -> StatusCauc.BLOQUEADO;
        };
    }
}
