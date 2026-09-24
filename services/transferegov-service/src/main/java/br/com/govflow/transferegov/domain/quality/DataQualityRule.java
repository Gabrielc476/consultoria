package br.com.govflow.transferegov.domain.quality;

import br.com.govflow.transferegov.domain.model.ConvenioSincronizado;

import java.util.List;

@FunctionalInterface
public interface DataQualityRule {

    List<DataQualityIssue> evaluate(ConvenioSincronizado convenio, long rowNumber);
}
