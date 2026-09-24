package br.com.govflow.transferegov.query.dto;

import br.com.govflow.transferegov.persistence.entity.SincronizacaoConvenioEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Data Transfer Object para projeções e consultas de convênios sincronizados (Read Side - CQRS).
 */
public record ConvenioDTO(
        UUID id,
        String nrConvenio,
        String idProposta,
        String cnpjProponente,
        String nomeProponente,
        String municipio,
        String uf,
        String situacaoConvenio,
        boolean instrumentoAtivo,
        LocalDate dataInicioVigencia,
        LocalDate dataFimVigencia,
        LocalDate dataLimitePrestacaoContas,
        LocalDate dataSuspensiva,
        BigDecimal valorGlobal,
        BigDecimal valorRepasse,
        BigDecimal valorContrapartida,
        BigDecimal valorSaldoConta,
        String objeto,
        String dataCargaSiconv,
        OffsetDateTime updatedAt
) {
    public static ConvenioDTO fromEntity(SincronizacaoConvenioEntity entity) {
        if (entity == null) return null;
        return new ConvenioDTO(
                entity.getId(),
                entity.getNrConvenio(),
                entity.getIdProposta(),
                entity.getCnpjProponente(),
                entity.getNomeProponente(),
                entity.getMunicipio(),
                entity.getUf(),
                entity.getSituacaoConvenio(),
                entity.isInstrumentoAtivo(),
                entity.getDataInicioVigencia(),
                entity.getDataFimVigencia(),
                entity.getDataLimitePrestacaoContas(),
                entity.getDataSuspensiva(),
                entity.getValorGlobal(),
                entity.getValorRepasse(),
                entity.getValorContrapartida(),
                entity.getValorSaldoConta(),
                entity.getObjeto(),
                entity.getDataCargaSiconv(),
                entity.getUpdatedAt()
        );
    }
}
