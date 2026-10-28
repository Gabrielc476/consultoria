package br.com.govflow.core.application.port.in;

import br.com.govflow.core.domain.model.licitacao.Licitacao;

import java.util.List;
import java.util.UUID;

public interface ConsultarLicitacoesConvenioUseCase {

    List<Licitacao> listarPorConvenio(UUID convenioId);

    Licitacao buscarPorId(UUID convenioId, UUID licitacaoId);
}
