package br.com.govflow.core.application.port.in;

import br.com.govflow.core.domain.model.convenio.Convenio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsultarConveniosUseCase {

    Optional<Convenio> buscarPorId(UUID id);

    Optional<Convenio> buscarPorNumeroSiconv(String numeroSiconv);

    List<Convenio> listarPorPrefeitura(UUID prefeituraId);

    List<Convenio> listarPorTenant(UUID tenantId);
}
