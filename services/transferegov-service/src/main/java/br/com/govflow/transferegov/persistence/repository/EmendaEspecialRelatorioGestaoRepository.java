package br.com.govflow.transferegov.persistence.repository;

import br.com.govflow.transferegov.persistence.entity.EmendaEspecialRelatorioGestaoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmendaEspecialRelatorioGestaoRepository extends JpaRepository<EmendaEspecialRelatorioGestaoEntity, UUID> {

    Optional<EmendaEspecialRelatorioGestaoEntity> findByIdRelatorioGestaoNovo(Long idRelatorioGestaoNovo);

    List<EmendaEspecialRelatorioGestaoEntity> findByIdPlanoAcao(Long idPlanoAcao);
}
