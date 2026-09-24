package br.com.govflow.transferegov.domain.quality;

import java.util.Collections;
import java.util.List;

public record DataQualityResult(
        boolean valid,
        List<DataQualityIssue> issues
) {
    public static DataQualityResult ok() {
        return new DataQualityResult(true, Collections.emptyList());
    }

    public static DataQualityResult withIssues(List<DataQualityIssue> issues) {
        return new DataQualityResult(false, issues != null ? issues : Collections.emptyList());
    }
}
