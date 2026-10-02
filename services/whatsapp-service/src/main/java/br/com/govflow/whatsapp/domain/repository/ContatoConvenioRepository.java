package br.com.govflow.whatsapp.domain.repository;

import br.com.govflow.whatsapp.domain.entity.ContatoConvenioEntity;
import br.com.govflow.whatsapp.domain.entity.ContatoConvenioId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContatoConvenioRepository extends JpaRepository<ContatoConvenioEntity, ContatoConvenioId> {

    List<ContatoConvenioEntity> findByIdContatoId(UUID contatoId);

    List<ContatoConvenioEntity> findByIdConvenioId(UUID convenioId);

    List<ContatoConvenioEntity> findByPrefeituraId(UUID prefeituraId);

    void deleteByIdContatoId(UUID contatoId);
}
