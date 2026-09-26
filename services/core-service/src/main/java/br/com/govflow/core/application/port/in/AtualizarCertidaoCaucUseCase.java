package br.com.govflow.core.application.port.in;

import br.com.govflow.core.domain.model.CertidaoCauc;
import br.com.govflow.core.domain.model.StatusCertidao;
import br.com.govflow.core.domain.model.TipoExigenciaCauc;

import java.time.LocalDate;
import java.util.UUID;

public interface AtualizarCertidaoCaucUseCase {

    CertidaoCauc cadastrarOuAtualizarCertidao(CadastrarCertidaoCommand command);

    record CadastrarCertidaoCommand(
            UUID tenantId,
            UUID prefeituraId,
            TipoExigenciaCauc tipoExigencia,
            String numeroCertidao,
            LocalDate dataEmissao,
            LocalDate dataValidade,
            String s3KeyComprovante,
            StatusCertidao situacaoForcada
    ) {}
}
