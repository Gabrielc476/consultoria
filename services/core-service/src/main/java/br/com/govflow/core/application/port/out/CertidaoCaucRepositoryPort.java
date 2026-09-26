package br.com.govflow.core.application.port.out;

import br.com.govflow.core.domain.model.CertidaoCauc;
import br.com.govflow.core.domain.model.StatusCertidao;
import br.com.govflow.core.domain.model.TipoExigenciaCauc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CertidaoCaucRepositoryPort {

    CertidaoCauc salvar(CertidaoCauc certidao);

    List<CertidaoCauc> salvarTodas(List<CertidaoCauc> certidoes);

    Optional<CertidaoCauc> buscarPorId(UUID id);

    Optional<CertidaoCauc> buscarPorPrefeituraIdETipoExigencia(UUID prefeituraId, TipoExigenciaCauc tipoExigencia);

    List<CertidaoCauc> listarPorPrefeituraId(UUID prefeituraId);

    List<CertidaoCauc> listarTodasPorTenant(UUID tenantId);

    List<CertidaoCauc> listarTodas();

    long contarPorPrefeituraIdESituacao(UUID prefeituraId, StatusCertidao situacao);
}
