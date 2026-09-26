package br.com.govflow.core.application.port.in;

import br.com.govflow.core.domain.model.convenio.Convenio;

import java.util.UUID;

public interface SuperarClausulaSuspensivaUseCase {

    Convenio superarClausulaSuspensiva(UUID convenioId, String s3KeyTermoRetirada);
}
