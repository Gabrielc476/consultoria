package br.com.govflow.transferegov.query.dto;

import br.com.govflow.transferegov.domain.compliance.StatusAdpf854;
import br.com.govflow.transferegov.persistence.entity.EmendaEspecialPlanoAcaoEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record EmendaEspecialResumoDTO(
        UUID id,
        Long idPlanoAcao,
        String codigoPlanoAcao,
        Integer anoPlanoAcao,
        String situacaoPlanoAcao,
        LocalDate dataAceitePlanoAcao,
        String cnpjBeneficiario,
        String nomeBeneficiario,
        String ufBeneficiario,
        String nomeParlamentar,
        Integer anoEmenda,
        String categoriaDespesa,
        BigDecimal valorCusteio,
        BigDecimal valorInvestimento,
        BigDecimal valorTotal,
        String nomeObjeto,
        StatusAdpf854 statusAdpf854
) {
    public static EmendaEspecialResumoDTO fromEntity(EmendaEspecialPlanoAcaoEntity entity) {
        if (entity == null) return null;
        return new EmendaEspecialResumoDTO(
                entity.getId(),
                entity.getIdPlanoAcao(),
                entity.getCodigoPlanoAcao(),
                entity.getAnoPlanoAcao(),
                entity.getSituacaoPlanoAcao(),
                entity.getDataAceitePlanoAcao(),
                entity.getCnpjBeneficiario(),
                entity.getNomeBeneficiario(),
                entity.getUfBeneficiario(),
                entity.getNomeParlamentar(),
                entity.getAnoEmenda(),
                entity.getCategoriaDespesa(),
                entity.getValorCusteio(),
                entity.getValorInvestimento(),
                entity.getValorTotal(),
                entity.getNomeObjeto(),
                entity.getStatusAdpf854()
        );
    }
}
