package br.com.govflow.core.domain.exception;

public class LimitePrefeiturasExcedidoException extends DomainException {

    public LimitePrefeiturasExcedidoException(int limite) {
        super("LIMITE_PREFEITURAS_EXCEDIDO", "O limite de prefeituras do plano contratado (" + limite + ") foi atingido.");
    }
}
