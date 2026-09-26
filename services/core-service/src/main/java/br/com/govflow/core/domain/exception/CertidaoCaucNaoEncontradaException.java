package br.com.govflow.core.domain.exception;

import java.util.UUID;

public class CertidaoCaucNaoEncontradaException extends DomainException {

    public CertidaoCaucNaoEncontradaException(UUID id) {
        super("CERTIDAO_CAUC_NAO_ENCONTRADA", "Certidão CAUC com ID " + id + " não foi encontrada.");
    }

    public CertidaoCaucNaoEncontradaException(String message) {
        super("CERTIDAO_CAUC_NAO_ENCONTRADA", message);
    }
}
