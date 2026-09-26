package br.com.govflow.core.application.port.in;

import br.com.govflow.core.domain.model.convenio.CriticidadePrazoSuspensiva;
import br.com.govflow.core.domain.model.convenio.StatusCondicionanteSuspensiva;
import br.com.govflow.core.domain.model.convenio.TipoCondicionanteSuspensiva;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ConsultarClausulaSuspensivaUseCase {

    DossieClausulaSuspensivaDto obterDossiePorConvenioId(UUID convenioId);

    DossieClausulaSuspensivaDto obterDossiePorNumeroSiconv(String numeroSiconv);

    record DossieClausulaSuspensivaDto(
            UUID convenioId,
            UUID prefeituraId,
            String numeroSiconv,
            String numeroProcesso,
            String orgaoConcedente,
            String objeto,
            BigDecimal valorGlobal,
            BigDecimal valorRepasse,
            BigDecimal valorContrapartida,
            boolean possuiClausulaSuspensiva,
            LocalDate prazoOriginal,
            boolean prorrogacaoSolicitada,
            LocalDate novoPrazoProrrogado,
            LocalDate prazoFatalEfetivo,
            long diasRestantes,
            CriticidadePrazoSuspensiva criticidade,
            boolean superada,
            String s3KeyTermoRetirada,
            List<ItemCondicionanteDto> condicionantes
    ) {}

    record ItemCondicionanteDto(
            UUID id,
            TipoCondicionanteSuspensiva tipo,
            String descricaoTipo,
            StatusCondicionanteSuspensiva status,
            String statusDescricao,
            String numeroDocumento,
            LocalDate dataAprovacao,
            LocalDate dataValidade,
            String observacoes,
            String s3KeyDocumento,
            LocalDate dataLimiteSaneamento,
            String s3KeyLaudoPendencias,
            BigDecimal valorOrcamentoAprovado,
            BigDecimal percentualBdi,
            String numeroArtRrt,
            String orgaoEmissor
    ) {}
}
