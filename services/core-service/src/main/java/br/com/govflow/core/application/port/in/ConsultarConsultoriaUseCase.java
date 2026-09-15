package br.com.govflow.core.application.port.in;

import br.com.govflow.core.domain.model.Consultoria;

import java.util.Optional;
import java.util.UUID;

public interface ConsultarConsultoriaUseCase {

    Optional<Consultoria> buscarPorId(UUID id);

    Optional<Consultoria> buscarPorCnpj(String cnpj);
}
