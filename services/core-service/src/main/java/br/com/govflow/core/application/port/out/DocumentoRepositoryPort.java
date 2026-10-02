package br.com.govflow.core.application.port.out;

import br.com.govflow.core.domain.model.Documento;
import br.com.govflow.core.domain.model.StatusDocumento;
import br.com.govflow.core.domain.model.documento.DocumentoAuditoria;
import br.com.govflow.core.domain.model.documento.DocumentoHabilDados;
import br.com.govflow.core.domain.model.documento.FaseCicloVida;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentoRepositoryPort {

    Documento salvar(Documento documento);

    Optional<Documento> buscarPorId(UUID id);

    List<Documento> listar(int page, int size, StatusDocumento status);

    long contarPorStatus(StatusDocumento status);

    List<Documento> listarPorConvenioId(UUID convenioId);

    List<Documento> listarPorConvenioIdEFase(UUID convenioId, FaseCicloVida fase);

    void excluir(UUID id);

    void salvarAuditoria(DocumentoAuditoria auditoria);

    List<DocumentoAuditoria> listarAuditorias(UUID documentoId);

    void salvarDadosHabeis(DocumentoHabilDados dadosHabeis);

    Optional<DocumentoHabilDados> buscarDadosHabeis(UUID documentoId);
}
