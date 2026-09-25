package br.com.govflow.transferegov.domain.radar;

import br.com.govflow.transferegov.persistence.entity.SincronizacaoConvenioEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Motor de domínio para cálculo de proximidade e criticidade de prazos do SICONV/Transferegov.
 * Avalia fim de vigência, cláusula suspensiva e prestação de contas, determinando o semáforo de risco.
 */
@Service
public class DeadlineDetectorService {

    public AlertaConvenioDTO avaliar(SincronizacaoConvenioEntity convenio) {
        return avaliar(convenio, LocalDate.now());
    }

    public AlertaConvenioDTO avaliar(SincronizacaoConvenioEntity convenio, LocalDate dataReferencia) {
        if (convenio == null) {
            return null;
        }

        LocalDate ref = dataReferencia != null ? dataReferencia : LocalDate.now();

        Long diasFimVigencia = calcularDias(ref, convenio.getDataFimVigencia());
        Long diasSuspensiva = calcularDias(ref, convenio.getDataSuspensiva());
        Long diasPrestacaoContas = calcularDias(ref, convenio.getDataLimitePrestacaoContas());

        List<MarcoTemporal> marcos = new ArrayList<>(3);
        if (convenio.getDataSuspensiva() != null && diasSuspensiva != null) {
            marcos.add(new MarcoTemporal(TipoPrazo.CLAUSULA_SUSPENSIVA, convenio.getDataSuspensiva(), diasSuspensiva));
        }
        if (convenio.getDataFimVigencia() != null && diasFimVigencia != null) {
            marcos.add(new MarcoTemporal(TipoPrazo.FIM_VIGENCIA, convenio.getDataFimVigencia(), diasFimVigencia));
        }
        if (convenio.getDataLimitePrestacaoContas() != null && diasPrestacaoContas != null) {
            marcos.add(new MarcoTemporal(TipoPrazo.PRESTACAO_CONTAS, convenio.getDataLimitePrestacaoContas(), diasPrestacaoContas));
        }

        NivelRisco nivelRisco = NivelRisco.REGULAR;
        TipoPrazo tipoPrazoMaisProximo = null;
        LocalDate prazoMaisProximo = null;
        Long diasRestantes = null;

        if (!marcos.isEmpty()) {
            marcos.sort(Comparator.comparingLong(MarcoTemporal::diasRestantes));
            MarcoTemporal maisCritico = marcos.getFirst();
            prazoMaisProximo = maisCritico.data();
            tipoPrazoMaisProximo = maisCritico.tipo();
            diasRestantes = maisCritico.diasRestantes();
            nivelRisco = NivelRisco.fromDiasRestantes(diasRestantes);
        }

        return new AlertaConvenioDTO(
                convenio.getId(),
                convenio.getNrConvenio(),
                convenio.getIdProposta(),
                convenio.getMunicipio(),
                convenio.getUf(),
                convenio.getCnpjProponente(),
                convenio.getNomeProponente(),
                convenio.getObjeto(),
                convenio.getSituacaoConvenio(),
                nivelRisco,
                tipoPrazoMaisProximo,
                prazoMaisProximo,
                diasRestantes,
                convenio.getDataFimVigencia(),
                diasFimVigencia,
                convenio.getDataSuspensiva(),
                diasSuspensiva,
                convenio.getDataLimitePrestacaoContas(),
                diasPrestacaoContas,
                convenio.getValorGlobal(),
                convenio.getValorRepasse()
        );
    }

    private Long calcularDias(LocalDate referencia, LocalDate alvo) {
        if (alvo == null) {
            return null;
        }
        return ChronoUnit.DAYS.between(referencia, alvo);
    }

    private record MarcoTemporal(TipoPrazo tipo, LocalDate data, long diasRestantes) {}
}
