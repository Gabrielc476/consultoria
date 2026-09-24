package br.com.govflow.transferegov.domain.quality;

import br.com.govflow.transferegov.domain.model.ConvenioSincronizado;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class ConsistencyQualityRule implements DataQualityRule {

    private static final BigDecimal TOLERANCIA_SOMA = new BigDecimal("2.00");

    @Override
    public List<DataQualityIssue> evaluate(ConvenioSincronizado convenio, long rowNumber) {
        List<DataQualityIssue> issues = new ArrayList<>();

        // Invariante temporal: data final de vigência deve ser posterior ou igual à inicial
        if (convenio.dataInicioVigencia() != null && convenio.dataFimVigencia() != null) {
            if (convenio.dataFimVigencia().isBefore(convenio.dataInicioVigencia())) {
                issues.add(new DataQualityIssue(
                        DataQualityDimension.CONSISTENCY,
                        "data_fim_vigencia",
                        "Data de fim de vigência (" + convenio.dataFimVigencia() +
                                ") não pode ser anterior à data de início (" + convenio.dataInicioVigencia() + ")",
                        convenio.dataFimVigencia().toString()
                ));
            }
        }

        // Invariante temporal: limite de prestação de contas deve ser posterior ou igual ao fim da vigência
        if (convenio.dataFimVigencia() != null && convenio.dataLimitePrestacaoContas() != null) {
            if (convenio.dataLimitePrestacaoContas().isBefore(convenio.dataFimVigencia())) {
                issues.add(new DataQualityIssue(
                        DataQualityDimension.CONSISTENCY,
                        "data_limite_prestacao_contas",
                        "Data limite de prestação de contas (" + convenio.dataLimitePrestacaoContas() +
                                ") não pode ser anterior ao fim da vigência (" + convenio.dataFimVigencia() + ")",
                        convenio.dataLimitePrestacaoContas().toString()
                ));
            }
        }

        // Invariante financeira: Repasse + Contrapartida deve aproximar-se do Valor Global
        if (convenio.valorGlobal() != null && convenio.valorRepasse() != null && convenio.valorContrapartida() != null) {
            BigDecimal somaFontes = convenio.valorRepasse().add(convenio.valorContrapartida());
            BigDecimal diferenca = somaFontes.subtract(convenio.valorGlobal()).abs();

            if (diferenca.compareTo(TOLERANCIA_SOMA) > 0 && convenio.valorGlobal().compareTo(BigDecimal.ZERO) > 0) {
                issues.add(new DataQualityIssue(
                        DataQualityDimension.CONSISTENCY,
                        "valor_global",
                        "Inconsistência na composição financeira: Repasse (" + convenio.valorRepasse() +
                                ") + Contrapartida (" + convenio.valorContrapartida() + ") = " + somaFontes +
                                ", divergente do Valor Global (" + convenio.valorGlobal() + ")",
                        convenio.valorGlobal().toString()
                ));
            }
        }

        return issues;
    }
}
