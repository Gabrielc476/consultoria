package br.com.govflow.core.infrastructure.adapter.in.rest.dto.response;

import br.com.govflow.core.application.port.in.ConsultarClausulaSuspensivaUseCase.ItemCondicionanteDto;
import br.com.govflow.core.domain.model.convenio.CondicionanteSuspensiva;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CondicionanteSuspensivaResponse(
        UUID id,
        String tipo,
        String descricaoTipo,
        String status,
        String statusDescricao,
        String numeroDocumentoComprobatorio,
        LocalDate dataAprovacao,
        LocalDate dataValidade,
        String observacoesAnaliseCaixa,
        String s3KeyDocumento,
        LocalDate dataLimiteSaneamento,
        String s3KeyLaudoPendencias,
        BigDecimal valorOrcamentoAprovadoCaixa,
        BigDecimal percentualBdiAprovado,
        String numeroArtRrt,
        String orgaoEmissor
) {

    public static CondicionanteSuspensivaResponse fromDomain(CondicionanteSuspensiva c) {
        if (c == null) return null;
        return new CondicionanteSuspensivaResponse(
                c.getId(),
                c.getTipoCondicionante().name(),
                c.getTipoCondicionante().getDescricao(),
                c.getStatus().name(),
                c.getStatus().getDescricao(),
                c.getNumeroDocumentoComprobatorio(),
                c.getDataAprovacao(),
                c.getDataValidade(),
                c.getObservacoesAnaliseCaixa(),
                c.getS3KeyDocumento(),
                c.getDataLimiteSaneamento(),
                c.getS3KeyLaudoPendencias(),
                c.getValorOrcamentoAprovadoCaixa(),
                c.getPercentualBdiAprovado(),
                c.getNumeroArtRrt(),
                c.getOrgaoEmissor()
        );
    }

    public static CondicionanteSuspensivaResponse fromDto(ItemCondicionanteDto dto) {
        if (dto == null) return null;
        return new CondicionanteSuspensivaResponse(
                dto.id(),
                dto.tipo().name(),
                dto.descricaoTipo(),
                dto.status().name(),
                dto.statusDescricao(),
                dto.numeroDocumento(),
                dto.dataAprovacao(),
                dto.dataValidade(),
                dto.observacoes(),
                dto.s3KeyDocumento(),
                dto.dataLimiteSaneamento(),
                dto.s3KeyLaudoPendencias(),
                dto.valorOrcamentoAprovado(),
                dto.percentualBdi(),
                dto.numeroArtRrt(),
                dto.orgaoEmissor()
        );
    }
}
