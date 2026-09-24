package br.com.govflow.transferegov.domain.model;

public record ProponenteInfo(
        String idProponente,
        String cnpj,
        String nome,
        String municipio,
        String uf
) {}
