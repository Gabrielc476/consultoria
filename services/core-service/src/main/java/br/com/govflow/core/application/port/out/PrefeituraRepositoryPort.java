package br.com.govflow.core.application.port.out;

import br.com.govflow.core.domain.model.Cnpj;
import br.com.govflow.core.domain.model.Prefeitura;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PrefeituraRepositoryPort {

    Prefeitura salvar(Prefeitura prefeitura);

    Optional<Prefeitura> buscarPorId(UUID id);

    List<Prefeitura> listar(int page, int size, Boolean ativo);

    List<Prefeitura> listarPorIds(Collection<UUID> ids, int page, int size, Boolean ativo);

    long contar(Boolean ativo);

    long contarPorIds(Collection<UUID> ids, Boolean ativo);

    boolean existePorCnpjETenantId(Cnpj cnpj, UUID tenantId);

    long contarPorTenantId(UUID tenantId);
}
