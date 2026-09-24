package br.com.govflow.transferegov.domain.quality;

import br.com.govflow.transferegov.domain.model.ConvenioSincronizado;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class ValidityQualityRule implements DataQualityRule {

    private static final Pattern CNPJ_DIGITS = Pattern.compile("^\\d{14}$");
    private static final Pattern CNPJ_MASKED = Pattern.compile("^\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}$");
    private static final Pattern UF_PATTERN = Pattern.compile("^[A-Z]{2}$");

    @Override
    public List<DataQualityIssue> evaluate(ConvenioSincronizado convenio, long rowNumber) {
        List<DataQualityIssue> issues = new ArrayList<>();

        // Validação de formato de CNPJ
        if (convenio.cnpjProponente() != null && !convenio.cnpjProponente().isBlank()) {
            String cleanCnpj = convenio.cnpjProponente().trim();
            if (!CNPJ_DIGITS.matcher(cleanCnpj).matches() && !CNPJ_MASKED.matcher(cleanCnpj).matches()) {
                issues.add(new DataQualityIssue(
                        DataQualityDimension.VALIDITY,
                        "cnpj_proponente",
                        "Formato de CNPJ inválido (esperado 14 dígitos numéricos ou máscara padrão)",
                        cleanCnpj
                ));
            }
        }

        // Validação de UF
        if (convenio.uf() != null && !convenio.uf().isBlank()) {
            if (!UF_PATTERN.matcher(convenio.uf().trim().toUpperCase()).matches()) {
                issues.add(new DataQualityIssue(
                        DataQualityDimension.VALIDITY,
                        "uf",
                        "Sigla de UF deve conter exatamente 2 letras maiúsculas",
                        convenio.uf()
                ));
            }
        }

        // Validações de valores não negativos
        checkNonNegative(issues, "valor_global", convenio.valorGlobal());
        checkNonNegative(issues, "valor_repasse", convenio.valorRepasse());
        checkNonNegative(issues, "valor_contrapartida", convenio.valorContrapartida());
        checkNonNegative(issues, "valor_saldo_conta", convenio.valorSaldoConta());

        return issues;
    }

    private void checkNonNegative(List<DataQualityIssue> issues, String fieldName, BigDecimal value) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            issues.add(new DataQualityIssue(
                    DataQualityDimension.VALIDITY,
                    fieldName,
                    "Valor monetário não pode ser negativo",
                    value.toString()
            ));
        }
    }
}
