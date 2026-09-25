package br.com.govflow.transferegov.persistence.repository;

import br.com.govflow.transferegov.persistence.entity.EmendaEspecialPlanoTrabalhoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmendaEspecialPlanoTrabalhoRepository extends JpaRepository<EmendaEspecialPlanoTrabalhoEntity, UUID> {

    Optional<EmendaEspecialPlanoTrabalhoEntity> findByIdPlanoTrabalho(Long idPlanoTrabalho);

    List<EmendaEspecialPlanoTrabalhoEntity> findByIdPlanoAcao(Long idPlanoAcao);
}
