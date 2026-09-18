package br.com.govflow.core.domain.model;

public record BoundingBox(
        double ymin,
        double xmin,
        double ymax,
        double xmax
) {
    public BoundingBox {
        ymin = Math.max(0.0, Math.min(1.0, ymin));
        xmin = Math.max(0.0, Math.min(1.0, xmin));
        ymax = Math.max(0.0, Math.min(1.0, ymax));
        xmax = Math.max(0.0, Math.min(1.0, xmax));
    }
}
