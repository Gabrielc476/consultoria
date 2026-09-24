package br.com.govflow.transferegov.domain.quality;

import br.com.govflow.transferegov.domain.model.ConvenioSincronizado;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SiconvDataQualityValidator {

    private final List<DataQualityRule> rules;

    public SiconvDataQualityValidator(List<DataQualityRule> rules) {
        this.rules = rules;
    }

    public DataQualityResult validate(ConvenioSincronizado convenio, long rowNumber) {
        List<DataQualityIssue> allIssues = new ArrayList<>();

        for (DataQualityRule rule : rules) {
            List<DataQualityIssue> issues = rule.evaluate(convenio, rowNumber);
            if (issues != null && !issues.isEmpty()) {
                allIssues.addAll(issues);
            }
        }

        if (allIssues.isEmpty()) {
            return DataQualityResult.ok();
        }

        return DataQualityResult.withIssues(allIssues);
    }
}
