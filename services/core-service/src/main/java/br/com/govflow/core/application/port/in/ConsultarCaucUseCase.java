package br.com.govflow.core.application.port.in;

import br.com.govflow.core.domain.model.GrupoCauc;
import br.com.govflow.core.domain.model.StatusCauc;
import br.com.govflow.core.domain.model.StatusCertidao;
import br.com.govflow.core.domain.model.TipoExigenciaCauc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ConsultarCaucUseCase {

    DossieCaucDto obterDossiePrefeitura(UUID prefeituraId);

    ResumoCaucDto obterResumoConsultoria(UUID tenantId);

    record ItemCertidaoDto(
            UUID id,
            String codigo,
            GrupoCauc grupo,
            TipoExigenciaCauc tipoExigencia,
            String nome,
            String orgaoEmissor,
            String numeroCertidao,
            LocalDate dataEmissao,
            LocalDate dataValidade,
            StatusCertidao status,
            Integer diasParaVencer,
            String s3KeyComprovante,
            Instant updatedAt
    ) {}

    record DossieCaucDto(
            UUID prefeituraId,
            String nomeMunicipio,
            String uf,
            String cnpj,
            StatusCauc statusGeral,
            int certidoesRegulares,
            int certidoesAlerta,
            int certidoesVencidas,
            List<ItemCertidaoDto> certidoes
    ) {}

    record PrazoFatalDto(
            String descricao,
            int diasRestantes,
            LocalDate dataLimite,
            String tipo
    ) {}

    record MunicipioRiscoDto(
            UUID id,
            String nome,
            String uf,
            int conveniosAtivos,
            int certidoesRegulares,
            int certidoesAlerta,
            int certidoesVencidas,
            PrazoFatalDto proximoPrazoFatal,
            List<ItemCertidaoDto> certidoes
    ) {}

    record ResumoCaucDto(
            int totalMunicipios,
            int totalRegulares,
            int totalAlerta,
            int totalVencidas,
            List<MunicipioRiscoDto> municipios
    ) {}
}
