package br.com.govflow.whatsapp.domain.repository;

import br.com.govflow.whatsapp.domain.entity.MensagemInboundEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MensagemInboundRepository extends JpaRepository<MensagemInboundEntity, UUID> {

    boolean existsByExternalMessageId(String externalMessageId);

    Optional<MensagemInboundEntity> findByExternalMessageId(String externalMessageId);
}
