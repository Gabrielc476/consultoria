package br.com.govflow.transferegov.query.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AuditoriaAdpf854ResponseDTO(
        long totalEmendasMonitoradas,
        BigDecimal valorTotalMonitorado,
        BigDecimal valorTotalEmRisco,
        long totalConforme,
        long totalAlerta,
        long totalNaoConforme,
        double percentualConformidade,
        List<MunicipioAuditoriaResumoDTO> resumoMunicipios,
        List<AlertaInconformidadeResumoDTO> alertasCriticos
) {
    public record MunicipioAuditoriaResumoDTO(
            String municipio,
            String uf,
            String cnpj,
            long totalEmendas,
            BigDecimal valorTotal,
            long totalConforme,
            long totalAlerta,
            long totalNaoConforme,
            double percentualConformidade
    ) {}

    public record AlertaInconformidadeResumoDTO(
            UUID inconformidadeId,
            Long idPlanoAcao,
            String codigoPlanoAcao,
            String municipio,
            String nomeParlamentar,
            String tipoInconformidade,
            String severidade,
            String descricao,
            OffsetDateTime dataDeteccao
    ) {}
}
