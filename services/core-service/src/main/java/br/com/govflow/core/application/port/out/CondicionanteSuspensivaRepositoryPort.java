package br.com.govflow.core.application.port.out;

import br.com.govflow.core.domain.model.convenio.CondicionanteSuspensiva;
import br.com.govflow.core.domain.model.convenio.TipoCondicionanteSuspensiva;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CondicionanteSuspensivaRepositoryPort {

    List<CondicionanteSuspensiva> buscarPorConvenioId(UUID convenioId);

    Optional<CondicionanteSuspensiva> buscarPorConvenioETipo(UUID convenioId, TipoCondicionanteSuspensiva tipo);

    Optional<CondicionanteSuspensiva> buscarPorId(UUID id);

    CondicionanteSuspensiva salvar(CondicionanteSuspensiva condicionante);

    List<CondicionanteSuspensiva> salvarTodas(List<CondicionanteSuspensiva> condicionantes);
}
