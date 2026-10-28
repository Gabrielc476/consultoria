package br.com.govflow.core.application.port.out;

import br.com.govflow.core.domain.model.licitacao.Licitacao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LicitacaoRepositoryPort {

    Licitacao salvar(Licitacao licitacao);

    Optional<Licitacao> buscarPorId(UUID id);

    List<Licitacao> listarPorConvenioId(UUID convenioId);

    boolean existePorConvenioIdENumeroLicitacao(UUID convenioId, String numeroLicitacao);
}
