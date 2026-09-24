package br.com.govflow.transferegov.domain.quality;

import br.com.govflow.transferegov.domain.model.ConvenioSincronizado;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CompletenessQualityRule implements DataQualityRule {

    @Override
    public List<DataQualityIssue> evaluate(ConvenioSincronizado convenio, long rowNumber) {
        List<DataQualityIssue> issues = new ArrayList<>();

        if (convenio.nrConvenio() == null || convenio.nrConvenio().isBlank()) {
            issues.add(new DataQualityIssue(
                    DataQualityDimension.COMPLETENESS,
                    "nr_convenio",
                    "Número do convênio é obrigatório e não pode ser nulo ou vazio",
                    String.valueOf(convenio.nrConvenio())
            ));
        }

        if (convenio.idProposta() == null || convenio.idProposta().isBlank()) {
            issues.add(new DataQualityIssue(
                    DataQualityDimension.COMPLETENESS,
                    "id_proposta",
                    "ID da proposta é obrigatório e não pode ser nulo ou vazio",
                    String.valueOf(convenio.idProposta())
            ));
        }

        if (convenio.cnpjProponente() == null || convenio.cnpjProponente().isBlank()) {
            issues.add(new DataQualityIssue(
                    DataQualityDimension.COMPLETENESS,
                    "cnpj_proponente",
                    "CNPJ do proponente é obrigatório para identificação municipal",
                    String.valueOf(convenio.cnpjProponente())
            ));
        }

        if (convenio.municipio() == null || convenio.municipio().isBlank()) {
            issues.add(new DataQualityIssue(
                    DataQualityDimension.COMPLETENESS,
                    "municipio",
                    "Nome do município é obrigatório",
                    String.valueOf(convenio.municipio())
            ));
        }

        if (convenio.uf() == null || convenio.uf().isBlank()) {
            issues.add(new DataQualityIssue(
                    DataQualityDimension.COMPLETENESS,
                    "uf",
                    "UF do convenente é obrigatória",
                    String.valueOf(convenio.uf())
            ));
        }

        if (convenio.valorGlobal() == null) {
            issues.add(new DataQualityIssue(
                    DataQualityDimension.COMPLETENESS,
                    "valor_global",
                    "Valor global do convênio não pode ser nulo",
                    "null"
            ));
        }

        return issues;
    }
}
