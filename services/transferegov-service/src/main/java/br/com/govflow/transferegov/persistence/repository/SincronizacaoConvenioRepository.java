package br.com.govflow.transferegov.persistence.repository;

import br.com.govflow.transferegov.persistence.entity.SincronizacaoConvenioEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SincronizacaoConvenioRepository extends JpaRepository<SincronizacaoConvenioEntity, UUID> {

    Optional<SincronizacaoConvenioEntity> findByNrConvenio(String nrConvenio);

    java.util.List<SincronizacaoConvenioEntity> findByNrConvenioIn(java.util.Collection<String> nrConvenios);

    boolean existsByNrConvenio(String nrConvenio);

    Page<SincronizacaoConvenioEntity> findByUf(String uf, Pageable pageable);

    Page<SincronizacaoConvenioEntity> findByCnpjProponente(String cnpjProponente, Pageable pageable);

    @Query("SELECT c FROM SincronizacaoConvenioEntity c WHERE " +
           "(:uf IS NULL OR c.uf = :uf) AND " +
           "(:cnpj IS NULL OR c.cnpjProponente = :cnpj) AND " +
           "(:situacao IS NULL OR c.situacaoConvenio = :situacao)")
    Page<SincronizacaoConvenioEntity> findByFiltros(
            @Param("uf") String uf,
            @Param("cnpj") String cnpj,
            @Param("situacao") String situacao,
            Pageable pageable
    );

    @Query("SELECT c FROM SincronizacaoConvenioEntity c WHERE " +
           "c.instrumentoAtivo = true AND " +
           "(:uf IS NULL OR c.uf = :uf) AND " +
           "(:cnpj IS NULL OR c.cnpjProponente = :cnpj) AND " +
           "(:municipio IS NULL OR LOWER(c.municipio) LIKE LOWER(CONCAT('%', :municipio, '%')))")
    java.util.List<SincronizacaoConvenioEntity> findAtivosPorFiltros(
            @Param("uf") String uf,
            @Param("cnpj") String cnpj,
            @Param("municipio") String municipio
    );

    java.util.List<SincronizacaoConvenioEntity> findByInstrumentoAtivoTrue();
}
