package br.com.govflow.core.application.port.in;

import br.com.govflow.core.domain.model.licitacao.Licitacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface HomologarLicitacaoUseCase {

    Licitacao homologar(HomologarLicitacaoCommand command);

    record HomologarLicitacaoCommand(
            UUID convenioId,
            UUID licitacaoId,
            BigDecimal valorHomologado,
            LocalDate dataHomologacao,
            String cnpjVencedor,
            String razaoSocialVencedor
    ) {}
}
