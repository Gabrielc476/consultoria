package br.com.govflow.core.infrastructure.adapter.out.persistence.mapper;

import br.com.govflow.core.domain.model.CertidaoCauc;
import br.com.govflow.core.domain.model.StatusCertidao;
import br.com.govflow.core.domain.model.TipoExigenciaCauc;
import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.CertidaoCaucJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class CertidaoCaucPersistenceMapper {

    public CertidaoCauc toDomain(CertidaoCaucJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        TipoExigenciaCauc tipo;
        try {
            tipo = TipoExigenciaCauc.valueOf(entity.getTipoExigencia());
        } catch (IllegalArgumentException e) {
            tipo = TipoExigenciaCauc.fromCodigo(entity.getTipoExigencia())
                    .orElse(TipoExigenciaCauc.RECEITA_FEDERAL_PGFN);
        }

        StatusCertidao situacao = StatusCertidao.fromSituacaoBanco(entity.getSituacao());

        return new CertidaoCauc(
                entity.getId(),
                entity.getTenantId(),
                entity.getPrefeituraId(),
                tipo,
                entity.getNumeroCertidao(),
                entity.getDataEmissao(),
                entity.getDataValidade(),
                situacao,
                entity.getDiasParaVencer(),
                entity.getS3KeyComprovante(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public CertidaoCaucJpaEntity toEntity(CertidaoCauc domain) {
        if (domain == null) {
            return null;
        }
        CertidaoCaucJpaEntity entity = new CertidaoCaucJpaEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setPrefeituraId(domain.getPrefeituraId());
        entity.setTipoExigencia(domain.getTipoExigencia().name());
        entity.setNumeroCertidao(domain.getNumeroCertidao());
        entity.setDataEmissao(domain.getDataEmissao());
        entity.setDataValidade(domain.getDataValidade());
        entity.setSituacao(domain.getSituacao().name());
        entity.setDiasParaVencer(domain.getDiasParaVencer());
        entity.setS3KeyComprovante(domain.getS3KeyComprovante());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
