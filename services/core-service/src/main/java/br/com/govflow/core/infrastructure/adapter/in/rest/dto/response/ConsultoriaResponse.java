package br.com.govflow.core.infrastructure.adapter.in.rest.dto.response;

import br.com.govflow.core.domain.model.Consultoria;
import br.com.govflow.core.domain.model.PlanoConsultoria;
import br.com.govflow.core.domain.model.StatusConsultoria;

import java.time.Instant;
import java.util.UUID;

public record ConsultoriaResponse(
        UUID id,
        String cnpj,
        String razaoSocial,
        String nomeFantasia,
        String emailContato,
        String telefoneContato,
        PlanoConsultoria plano,
        StatusConsultoria status,
        int limitePrefeituras,
        Instant createdAt,
        Instant updatedAt
) {
    public static ConsultoriaResponse fromDomain(Consultoria domain) {
        if (domain == null) return null;
        return new ConsultoriaResponse(
                domain.getId(),
                domain.getCnpj().getFormatted(),
                domain.getRazaoSocial(),
                domain.getNomeFantasia(),
                domain.getEmailContato(),
                domain.getTelefoneContato(),
                domain.getPlano(),
                domain.getStatus(),
                domain.getLimitePrefeituras(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}
