package br.com.govflow.core.domain.exception;

import br.com.govflow.core.domain.model.Cnpj;

public class ConsultoriaJaCadastradaException extends DomainException {

    public ConsultoriaJaCadastradaException(String message) {
        super("CONSULTORIA_JA_CADASTRADA", message);
    }

    public ConsultoriaJaCadastradaException(Cnpj cnpj) {
        super("CONSULTORIA_JA_CADASTRADA", "Já existe uma consultoria cadastrada com o CNPJ: " + cnpj.getFormatted());
    }
}
