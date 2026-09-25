package br.com.govflow.transferegov.query.dto;

import br.com.govflow.transferegov.domain.radar.AlertaConvenioDTO;

import java.util.List;

/**
 * Payload consolidado do Radar de Prazos entregue pelo endpoint REST para o Frontend Angular.
 */
public record RadarPrazosResponseDTO(
        ResumoRadarDTO resumo,
        List<MunicipioRadarDTO> agrupamentoPorMunicipio,
        List<AlertaConvenioDTO> alertas
) {
}
