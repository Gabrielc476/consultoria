package br.com.govflow.whatsapp.domain.repository;

import br.com.govflow.whatsapp.domain.entity.InstanciaWhatsAppEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstanciaWhatsAppRepository extends JpaRepository<InstanciaWhatsAppEntity, UUID> {

    Optional<InstanciaWhatsAppEntity> findByInstanceName(String instanceName);
}
