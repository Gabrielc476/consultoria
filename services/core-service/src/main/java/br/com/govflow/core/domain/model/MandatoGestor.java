package br.com.govflow.core.domain.model;

import java.time.LocalDate;

/**
 * Value Object que encapsula os dados do mandato do prefeito/gestor municipal.
 * Elimina o code smell de Data Clumps e valida invariantes temporais do mandato.
 */
public record MandatoGestor(
        String nomePrefeito,
        Cpf cpfPrefeito,
        LocalDate inicioMandato,
        LocalDate fimMandato
) {
    public MandatoGestor {
        if (inicioMandato != null && fimMandato != null && fimMandato.isBefore(inicioMandato)) {
            throw new IllegalArgumentException("A data final do mandato não pode ser anterior à data inicial.");
        }
    }

    public static MandatoGestor of(String nomePrefeito, Cpf cpfPrefeito, LocalDate inicioMandato, LocalDate fimMandato) {
        return new MandatoGestor(nomePrefeito, cpfPrefeito, inicioMandato, fimMandato);
    }
}
