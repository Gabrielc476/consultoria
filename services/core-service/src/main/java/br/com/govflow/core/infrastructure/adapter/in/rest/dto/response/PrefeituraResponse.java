package br.com.govflow.core.infrastructure.adapter.in.rest.dto.response;

import br.com.govflow.core.domain.model.PorteMunicipio;
import br.com.govflow.core.domain.model.Prefeitura;
import br.com.govflow.core.domain.model.StatusCauc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PrefeituraResponse(
        UUID id,
        UUID tenantId,
        String cnpj,
        String razaoSocial,
        String nomeMunicipio,
        String uf,
        String codigoIbge,
        PorteMunicipio porteMunicipio,
        String nomePrefeito,
        String cpfPrefeito,
        LocalDate inicioMandato,
        LocalDate fimMandato,
        StatusCauc statusCauc,
        boolean ativo,
        Instant createdAt,
        Instant updatedAt
) {
    public static PrefeituraResponse fromDomain(Prefeitura domain) {
        if (domain == null) return null;
        return new PrefeituraResponse(
                domain.getId(),
                domain.getTenantId(),
                domain.getCnpj().getFormatted(),
                domain.getRazaoSocial(),
                domain.getNomeMunicipio(),
                domain.getUf().name(),
                domain.getCodigoIbge().getValue(),
                domain.getPorteMunicipio(),
                domain.getNomePrefeito(),
                domain.getCpfPrefeito() != null ? domain.getCpfPrefeito().getFormatted() : null,
                domain.getInicioMandato(),
                domain.getFimMandato(),
                domain.getStatusCauc(),
                domain.isAtivo(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}
