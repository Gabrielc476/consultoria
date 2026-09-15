package br.com.govflow.core.application.port.out;

import br.com.govflow.core.domain.model.Cnpj;
import br.com.govflow.core.domain.model.Consultoria;

import java.util.Optional;
import java.util.UUID;

public interface ConsultoriaRepositoryPort {

    Consultoria salvar(Consultoria consultoria);

    Optional<Consultoria> buscarPorId(UUID id);

    Optional<Consultoria> buscarPorCnpj(Cnpj cnpj);

    boolean existePorCnpj(Cnpj cnpj);
}
