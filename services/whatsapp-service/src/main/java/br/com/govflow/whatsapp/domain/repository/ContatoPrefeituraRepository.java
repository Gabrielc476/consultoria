package br.com.govflow.whatsapp.domain.repository;

import br.com.govflow.whatsapp.domain.entity.ContatoPrefeituraEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContatoPrefeituraRepository extends JpaRepository<ContatoPrefeituraEntity, UUID> {

    Optional<ContatoPrefeituraEntity> findByPhoneNumberAndAtivoTrue(String phoneNumber);

    Optional<ContatoPrefeituraEntity> findByPhoneNumber(String phoneNumber);
}
