package br.com.govflow.core.infrastructure.adapter.in.rest.dto.response;

import br.com.govflow.core.application.port.in.ConsultarCaucUseCase.ItemCertidaoDto;
import br.com.govflow.core.domain.model.CertidaoCauc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CertidaoCaucResponse(
        UUID id,
        String codigo,
        String grupo,
        String tipoExigencia,
        String nome,
        String orgaoEmissor,
        String numeroCertidao,
        LocalDate dataEmissao,
        LocalDate dataValidade,
        String status,
        Integer diasParaVencer,
        String s3KeyComprovante,
        Instant updatedAt
) {
    public static CertidaoCaucResponse fromDto(ItemCertidaoDto dto) {
        if (dto == null) return null;
        return new CertidaoCaucResponse(
                dto.id(),
                dto.codigo(),
                dto.grupo() != null ? dto.grupo().name() : null,
                dto.tipoExigencia() != null ? dto.tipoExigencia().name() : null,
                dto.nome(),
                dto.orgaoEmissor(),
                dto.numeroCertidao(),
                dto.dataEmissao(),
                dto.dataValidade(),
                dto.status() != null ? dto.status().name() : "REGULAR",
                dto.diasParaVencer(),
                dto.s3KeyComprovante(),
                dto.updatedAt()
        );
    }

    public static CertidaoCaucResponse fromDomain(CertidaoCauc domain) {
        if (domain == null) return null;
        return new CertidaoCaucResponse(
                domain.getId(),
                domain.getTipoExigencia().getCodigo(),
                domain.getTipoExigencia().getGrupo().name(),
                domain.getTipoExigencia().name(),
                domain.getTipoExigencia().getNome(),
                domain.getTipoExigencia().getOrgaoEmissor(),
                domain.getNumeroCertidao(),
                domain.getDataEmissao(),
                domain.getDataValidade(),
                domain.getSituacao().name(),
                domain.getDiasParaVencer(),
                domain.getS3KeyComprovante(),
                domain.getUpdatedAt()
        );
    }
}
