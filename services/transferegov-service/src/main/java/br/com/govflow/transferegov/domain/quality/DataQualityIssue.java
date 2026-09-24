package br.com.govflow.transferegov.domain.quality;

public record DataQualityIssue(
        DataQualityDimension dimension,
        String field,
        String description,
        String observedValue
) {}
