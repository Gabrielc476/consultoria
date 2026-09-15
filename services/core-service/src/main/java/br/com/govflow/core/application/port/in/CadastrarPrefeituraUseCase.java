package br.com.govflow.core.application.port.in;

import br.com.govflow.core.domain.model.PorteMunicipio;
import br.com.govflow.core.domain.model.Prefeitura;

import java.time.LocalDate;
import java.util.UUID;

public interface CadastrarPrefeituraUseCase {

    Prefeitura cadastrar(CadastrarPrefeituraCommand command);

    record CadastrarPrefeituraCommand(
            UUID tenantId,
            String cnpj,
            String razaoSocial,
            String nomeMunicipio,
            String uf,
            String codigoIbge,
            PorteMunicipio porteMunicipio,
            String nomePrefeito,
            String cpfPrefeito,
            LocalDate inicioMandato,
            LocalDate fimMandato
    ) {}
}
