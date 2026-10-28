package br.com.govflow.core.application.port.in;

import br.com.govflow.core.domain.model.licitacao.CriterioJulgamento;
import br.com.govflow.core.domain.model.licitacao.Licitacao;
import br.com.govflow.core.domain.model.licitacao.ModalidadeLicitacao;
import br.com.govflow.core.domain.model.licitacao.RegimeExecucao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface CadastrarLicitacaoUseCase {

    Licitacao cadastrar(CadastrarLicitacaoCommand command);

    record CadastrarLicitacaoCommand(
            UUID convenioId,
            String numeroProcessoAdministrativo,
            String numeroLicitacao,
            ModalidadeLicitacao modalidade,
            CriterioJulgamento criterioJulgamento,
            RegimeExecucao regimeExecucao,
            String objeto,
            BigDecimal valorEstimado,
            LocalDate dataPublicacaoEdital,
            LocalDate dataAberturaPropostas,
            String linkPncp,
            String linkTransferegov,
            String linkSistemaCompras
    ) {}
}
