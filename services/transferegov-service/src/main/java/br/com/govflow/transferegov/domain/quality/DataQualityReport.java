package br.com.govflow.transferegov.domain.quality;

import java.util.EnumMap;
import java.util.Map;

public class DataQualityReport {

    private long totalRead;
    private long totalFiltered;
    private long totalValid;
    private long totalAnomalies;
    private final Map<DataQualityDimension, Long> anomaliesByDimension = new EnumMap<>(DataQualityDimension.class);
    private long executionTimeMs;
    private String dataCargaSiconv;

    public DataQualityReport() {
        for (DataQualityDimension dimension : DataQualityDimension.values()) {
            anomaliesByDimension.put(dimension, 0L);
        }
    }

    public synchronized void incrementRead() {
        totalRead++;
    }

    public synchronized void incrementFiltered() {
        totalFiltered++;
    }

    public synchronized void incrementValid() {
        totalValid++;
    }

    public synchronized void recordIssue(DataQualityIssue issue) {
        totalAnomalies++;
        anomaliesByDimension.compute(issue.dimension(), (dim, count) -> (count == null ? 0L : count) + 1L);
    }

    public long getTotalRead() {
        return totalRead;
    }

    public void setTotalRead(long totalRead) {
        this.totalRead = totalRead;
    }

    public long getTotalFiltered() {
        return totalFiltered;
    }

    public void setTotalFiltered(long totalFiltered) {
        this.totalFiltered = totalFiltered;
    }

    public long getTotalValid() {
        return totalValid;
    }

    public void setTotalValid(long totalValid) {
        this.totalValid = totalValid;
    }

    public long getTotalAnomalies() {
        return totalAnomalies;
    }

    public Map<DataQualityDimension, Long> getAnomaliesByDimension() {
        return anomaliesByDimension;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public String getDataCargaSiconv() {
        return dataCargaSiconv;
    }

    public void setDataCargaSiconv(String dataCargaSiconv) {
        this.dataCargaSiconv = dataCargaSiconv;
    }

    public double getSuccessRate() {
        if (totalFiltered == 0) return 100.0;
        return (double) totalValid / totalFiltered * 100.0;
    }
}
