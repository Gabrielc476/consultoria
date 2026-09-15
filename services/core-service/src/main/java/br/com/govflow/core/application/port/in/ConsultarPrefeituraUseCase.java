package br.com.govflow.core.application.port.in;

import br.com.govflow.core.domain.model.Prefeitura;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsultarPrefeituraUseCase {

    Optional<Prefeitura> buscarPorId(UUID id);

    List<Prefeitura> listar(int page, int size, Boolean ativo);

    long contar(Boolean ativo);
}
