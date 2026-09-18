package br.com.govflow.core.application.port.out;

import br.com.govflow.core.domain.model.Documento;
import br.com.govflow.core.domain.model.StatusDocumento;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentoRepositoryPort {

    Documento salvar(Documento documento);

    Optional<Documento> buscarPorId(UUID id);

    List<Documento> listar(int page, int size, StatusDocumento status);

    long contarPorStatus(StatusDocumento status);
}
