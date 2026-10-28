package br.com.govflow.core.domain.exception;

public class RegraNegocioLicitacaoException extends DomainException {

    public RegraNegocioLicitacaoException(String message) {
        super("REGRA_NEGOCIO_LICITACAO", message);
    }
}
