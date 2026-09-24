package br.com.govflow.transferegov.sync.pipeline;

import br.com.govflow.transferegov.domain.model.ConvenioSincronizado;
import br.com.govflow.transferegov.persistence.entity.SincronizacaoConvenioEntity;
import br.com.govflow.transferegov.persistence.repository.SincronizacaoConvenioRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Responsável pela persistência e sincronização em lote (batch UPSERT) dos convênios recebidos no pipeline ETL.
 * Elimina o code smell de Divergent Change do SiconvStreamingPipeline.
 */
@Component
public class SiconvConvenioBatchWriter {

    private final SincronizacaoConvenioRepository convenioRepository;

    public SiconvConvenioBatchWriter(SincronizacaoConvenioRepository convenioRepository) {
        this.convenioRepository = convenioRepository;
    }

    @Transactional
    public void writeBatch(List<ConvenioSincronizado> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return;
        }

        List<String> nrConvenios = dtos.stream().map(ConvenioSincronizado::nrConvenio).toList();
        Map<String, SincronizacaoConvenioEntity> existingMap = convenioRepository.findByNrConvenioIn(nrConvenios)
                .stream()
                .collect(Collectors.toMap(SincronizacaoConvenioEntity::getNrConvenio, Function.identity()));

        List<SincronizacaoConvenioEntity> toSave = new ArrayList<>(dtos.size());

        for (ConvenioSincronizado dto : dtos) {
            SincronizacaoConvenioEntity entity = existingMap.get(dto.nrConvenio());
            if (entity == null) {
                entity = new SincronizacaoConvenioEntity();
                entity.setNrConvenio(dto.nrConvenio());
            }
            updateEntityFromDto(entity, dto);
            toSave.add(entity);
        }

        convenioRepository.saveAll(toSave);
    }

    private void updateEntityFromDto(SincronizacaoConvenioEntity entity, ConvenioSincronizado dto) {
        entity.setIdProposta(dto.idProposta());
        entity.setCnpjProponente(dto.cnpjProponente());
        entity.setNomeProponente(dto.nomeProponente());
        entity.setMunicipio(dto.municipio());
        entity.setUf(dto.uf());
        entity.setSituacaoConvenio(dto.situacaoConvenio());
        entity.setInstrumentoAtivo(dto.instrumentoAtivo());
        entity.setDataInicioVigencia(dto.dataInicioVigencia());
        entity.setDataFimVigencia(dto.dataFimVigencia());
        entity.setDataLimitePrestacaoContas(dto.dataLimitePrestacaoContas());
        entity.setDataSuspensiva(dto.dataSuspensiva());
        entity.setValorGlobal(dto.valorGlobal() != null ? dto.valorGlobal() : BigDecimal.ZERO);
        entity.setValorRepasse(dto.valorRepasse() != null ? dto.valorRepasse() : BigDecimal.ZERO);
        entity.setValorContrapartida(dto.valorContrapartida() != null ? dto.valorContrapartida() : BigDecimal.ZERO);
        entity.setValorSaldoConta(dto.valorSaldoConta() != null ? dto.valorSaldoConta() : BigDecimal.ZERO);
        entity.setObjeto(dto.objeto());
        entity.setDataCargaSiconv(dto.dataCargaSiconv());
    }
}
