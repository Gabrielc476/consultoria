package br.com.govflow.transferegov.query.dto;

import br.com.govflow.transferegov.domain.radar.NivelRisco;

/**
 * Panorama agregado por município/prefeitura para acompanhamento da consultoria municipal.
 */
public record MunicipioRadarDTO(
        String municipio,
        String uf,
        String cnpjProponente,
        String nomeProponente,
        long totalConvenios,
        long totalCriticos,
        long totalAtencao,
        long totalRegulares,
        NivelRisco maiorRisco
) {
}
