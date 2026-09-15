package br.com.govflow.core.application.port.in;

import br.com.govflow.core.domain.model.PorteMunicipio;
import br.com.govflow.core.domain.model.Prefeitura;
import br.com.govflow.core.domain.model.StatusCauc;

import java.time.LocalDate;
import java.util.UUID;

public interface AtualizarPrefeituraUseCase {

    Prefeitura atualizar(UUID id, AtualizarPrefeituraCommand command);

    void inativar(UUID id);

    void ativar(UUID id);

    record AtualizarPrefeituraCommand(
            String razaoSocial,
            String nomeMunicipio,
            PorteMunicipio porteMunicipio,
            String nomePrefeito,
            String cpfPrefeito,
            LocalDate inicioMandato,
            LocalDate fimMandato,
            StatusCauc statusCauc
    ) {}
}
