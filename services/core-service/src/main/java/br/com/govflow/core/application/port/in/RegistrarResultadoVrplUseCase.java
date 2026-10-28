package br.com.govflow.core.application.port.in;

import br.com.govflow.core.domain.model.licitacao.Licitacao;
import br.com.govflow.core.domain.model.licitacao.StatusVrpl;

import java.time.LocalDate;
import java.util.UUID;

public interface RegistrarResultadoVrplUseCase {

    Licitacao registrar(RegistrarResultadoVrplCommand command);

    record RegistrarResultadoVrplCommand(
            UUID convenioId,
            UUID licitacaoId,
            StatusVrpl resultado,
            String parecerTecnico,
            String observacoesDiligencia,
            LocalDate dataAceiteVrpl
    ) {}
}
