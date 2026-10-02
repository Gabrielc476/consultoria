package br.com.govflow.whatsapp.domain.repository;

import br.com.govflow.whatsapp.domain.entity.ContatoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContatoRepository extends JpaRepository<ContatoEntity, UUID> {

    Optional<ContatoEntity> findByPhoneNumberAndAtivoTrue(String phoneNumber);

    Optional<ContatoEntity> findByPhoneNumber(String phoneNumber);

    List<ContatoEntity> findByTenantId(UUID tenantId);

    boolean existsByPhoneNumber(String phoneNumber);
}
