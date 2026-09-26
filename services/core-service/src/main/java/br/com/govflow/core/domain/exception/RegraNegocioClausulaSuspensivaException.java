package br.com.govflow.core.domain.exception;

public class RegraNegocioClausulaSuspensivaException extends DomainException {

    public RegraNegocioClausulaSuspensivaException(String message) {
        super("REGRA_NEGOCIO_CLAUSULA_SUSPENSIVA", message);
    }
}
