package br.com.govflow.core.domain.exception;

public class AcessoNegadoException extends DomainException {

    public AcessoNegadoException(String message) {
        super("ACESSO_NEGADO", message);
    }
}
