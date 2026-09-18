package br.com.govflow.core.domain.exception;

public class JustificativaObrigatoriaException extends DomainException {

    public JustificativaObrigatoriaException(String operacao) {
        super("JUSTIFICATIVA_OBRIGATORIA",
                String.format("Justificativa obrigatória para a operação '%s'.", operacao));
    }
}
