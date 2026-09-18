package br.com.govflow.core.application.port.in;

import br.com.govflow.core.domain.model.AuditoriaRevisao;
import br.com.govflow.core.domain.model.Documento;
import br.com.govflow.core.domain.model.StatusDocumento;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsultarDocumentoUseCase {

    Optional<Documento> buscarPorId(UUID id);

    List<Documento> listar(int page, int size, StatusDocumento status);

    long contar(StatusDocumento status);

    List<AuditoriaRevisao> listarAuditorias(UUID documentoId);
}
