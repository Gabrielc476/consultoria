package br.com.govflow.core.application.port.out;

import br.com.govflow.core.domain.model.AuditoriaRevisao;

import java.util.List;
import java.util.UUID;

public interface AuditoriaRevisaoRepositoryPort {

    AuditoriaRevisao salvar(AuditoriaRevisao auditoria);

    List<AuditoriaRevisao> listarPorDocumentoId(UUID documentoId);
}
