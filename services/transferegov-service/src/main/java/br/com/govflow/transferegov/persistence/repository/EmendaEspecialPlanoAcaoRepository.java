package br.com.govflow.transferegov.persistence.repository;

import br.com.govflow.transferegov.domain.compliance.StatusAdpf854;
import br.com.govflow.transferegov.persistence.entity.EmendaEspecialPlanoAcaoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmendaEspecialPlanoAcaoRepository extends JpaRepository<EmendaEspecialPlanoAcaoEntity, UUID> {

    Optional<EmendaEspecialPlanoAcaoEntity> findByIdPlanoAcao(Long idPlanoAcao);

    Optional<EmendaEspecialPlanoAcaoEntity> findByCodigoPlanoAcao(String codigoPlanoAcao);

    @Query("""
        SELECT e FROM EmendaEspecialPlanoAcaoEntity e
        WHERE (:uf IS NULL OR UPPER(e.ufBeneficiario) = UPPER(:uf))
          AND (:cnpj IS NULL OR e.cnpjBeneficiario = :cnpj)
          AND (:municipio IS NULL OR UPPER(e.nomeBeneficiario) LIKE UPPER(CONCAT('%', :municipio, '%')))
          AND (:ano IS NULL OR e.anoEmenda = :ano)
          AND (:parlamentar IS NULL OR UPPER(e.nomeParlamentar) LIKE UPPER(CONCAT('%', :parlamentar, '%')))
          AND (:statusAdpf854 IS NULL OR e.statusAdpf854 = :statusAdpf854)
    """)
    Page<EmendaEspecialPlanoAcaoEntity> findByFiltros(
            @Param("uf") String uf,
            @Param("cnpj") String cnpj,
            @Param("municipio") String municipio,
            @Param("ano") Integer ano,
            @Param("parlamentar") String parlamentar,
            @Param("statusAdpf854") StatusAdpf854 statusAdpf854,
            Pageable pageable
    );

    long countByStatusAdpf854(StatusAdpf854 statusAdpf854);

    @Query("SELECT COALESCE(SUM(e.valorTotal), 0) FROM EmendaEspecialPlanoAcaoEntity e")
    BigDecimal sumValorTotal();

    @Query("SELECT COALESCE(SUM(e.valorTotal), 0) FROM EmendaEspecialPlanoAcaoEntity e WHERE e.statusAdpf854 = 'NAO_CONFORME'")
    BigDecimal sumValorTotalNaoConforme();

    @Query("SELECT DISTINCT e.nomeBeneficiario FROM EmendaEspecialPlanoAcaoEntity e WHERE (:uf IS NULL OR UPPER(e.ufBeneficiario) = UPPER(:uf)) ORDER BY e.nomeBeneficiario")
    List<String> findDistinctBeneficiarios(@Param("uf") String uf);

    List<EmendaEspecialPlanoAcaoEntity> findByUfBeneficiario(String ufBeneficiario);
}
