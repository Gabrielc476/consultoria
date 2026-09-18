package br.com.govflow.core.domain.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Value Object representando o conjunto de coordenadas normalizadas [ymin, xmin, ymax, xmax]
 * identificadas pela visão computacional / IA para renderização no visualizador PDF do frontend.
 */
public record BoundingBoxesData(
        Map<String, BoundingBox> caixas
) {
    public BoundingBoxesData {
        caixas = caixas != null ? Collections.unmodifiableMap(new LinkedHashMap<>(caixas)) : Collections.emptyMap();
    }

    public static BoundingBoxesData empty() {
        return new BoundingBoxesData(Collections.emptyMap());
    }

    public static BoundingBoxesData of(Map<String, BoundingBox> map) {
        return new BoundingBoxesData(map);
    }

    public BoundingBox get(String campo) {
        return caixas.get(campo);
    }

    public boolean contem(String campo) {
        return caixas.containsKey(campo);
    }

    public Map<String, BoundingBox> toMap() {
        return caixas;
    }
}
