package br.com.govflow.whatsapp.domain.repository;

import br.com.govflow.whatsapp.domain.entity.MensagemOutboundEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MensagemOutboundRepository extends JpaRepository<MensagemOutboundEntity, UUID> {

    List<MensagemOutboundEntity> findByStatus(String status);

    List<MensagemOutboundEntity> findByRecipientPhone(String recipientPhone);
}
