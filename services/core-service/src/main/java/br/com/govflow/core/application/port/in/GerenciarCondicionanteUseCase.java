package br.com.govflow.core.application.port.in;

import br.com.govflow.core.domain.model.convenio.CondicionanteSuspensiva;
import br.com.govflow.core.domain.model.convenio.TipoCondicionanteSuspensiva;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface GerenciarCondicionanteUseCase {

    CondicionanteSuspensiva submeterParaAnaliseCaixa(UUID convenioId, TipoCondicionanteSuspensiva tipo);

    CondicionanteSuspensiva registrarDiligenciaCaixa(RegistrarDiligenciaCommand command);

    CondicionanteSuspensiva aprovarCondicionante(AprovarCondicionanteCommand command);

    CondicionanteSuspensiva atualizarParametrosTecnicos(AtualizarCondicionanteCommand command);

    record RegistrarDiligenciaCommand(
            UUID convenioId,
            TipoCondicionanteSuspensiva tipo,
            String observacoes,
            String s3KeyLaudoPendencias,
            LocalDate dataLimiteSaneamento
    ) {}

    record AprovarCondicionanteCommand(
            UUID convenioId,
            TipoCondicionanteSuspensiva tipo,
            String numeroDocumentoComprobatorio,
            LocalDate dataAprovacao,
            LocalDate dataValidade,
            BigDecimal valorOrcamentoAprovado,
            BigDecimal percentualBdiAprovado,
            String numeroArtRrt,
            String orgaoEmissor,
            String s3KeyDocumento
    ) {}

    record AtualizarCondicionanteCommand(
            UUID convenioId,
            TipoCondicionanteSuspensiva tipo,
            String numeroDocumentoComprobatorio,
            LocalDate dataValidade,
            String orgaoEmissor,
            BigDecimal valorOrcamentoAprovado,
            BigDecimal percentualBdiAprovado,
            String numeroArtRrt,
            String observacoes
    ) {}
}
