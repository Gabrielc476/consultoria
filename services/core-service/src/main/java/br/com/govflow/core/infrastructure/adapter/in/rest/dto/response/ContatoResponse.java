package br.com.govflow.core.infrastructure.adapter.in.rest.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ContatoResponse(
        UUID id,
        UUID tenantId,
        String phoneNumber,
        String nome,
        String papel,
        String empresaOuOrgao,
        boolean ativo,
        Instant createdAt,
        List<ContatoConvenioResponse> convenios
) {
}
