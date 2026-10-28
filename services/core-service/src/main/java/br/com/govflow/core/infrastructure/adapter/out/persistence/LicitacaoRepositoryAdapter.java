package br.com.govflow.core.infrastructure.adapter.out.persistence;

import br.com.govflow.core.application.port.out.LicitacaoRepositoryPort;
import br.com.govflow.core.domain.model.licitacao.Licitacao;
import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.LicitacaoJpaEntity;
import br.com.govflow.core.infrastructure.adapter.out.persistence.mapper.LicitacaoPersistenceMapper;
import br.com.govflow.core.infrastructure.adapter.out.persistence.repository.SpringDataLicitacaoRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class LicitacaoRepositoryAdapter implements LicitacaoRepositoryPort {

    private final SpringDataLicitacaoRepository repository;
    private final LicitacaoPersistenceMapper mapper;

    public LicitacaoRepositoryAdapter(SpringDataLicitacaoRepository repository,
                                      LicitacaoPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Licitacao salvar(Licitacao licitacao) {
        LicitacaoJpaEntity saved = repository.save(mapper.toEntity(licitacao));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Licitacao> buscarPorId(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Licitacao> listarPorConvenioId(UUID convenioId) {
        return repository.findByConvenioIdOrderByCreatedAtAsc(convenioId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existePorConvenioIdENumeroLicitacao(UUID convenioId, String numeroLicitacao) {
        return repository.existsByConvenioIdAndNumeroLicitacao(convenioId, numeroLicitacao);
    }
}
