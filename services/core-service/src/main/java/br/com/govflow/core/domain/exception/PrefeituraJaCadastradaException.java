package br.com.govflow.core.domain.exception;

public class PrefeituraJaCadastradaException extends DomainException {

    public PrefeituraJaCadastradaException(String message) {
        super("PREFEITURA_JA_CADASTRADA", message);
    }
}
