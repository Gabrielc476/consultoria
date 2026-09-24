package br.com.govflow.core.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Value Object encapsulando os dados fiscais de liquidação do documento hábil.
 * Elimina o code smell de Data Clumps e centraliza a geração de snapshots para auditoria.
 */
public record DadosFiscais(
        TipoDocumentoHabil tipoDocumento,
        String numeroDocumento,
        String serieDocumento,
        String chaveAcessoNfe,
        LocalDate dataEmissao,
        String cnpjCredor,
        String razaoSocialCredor,
        String descricaoServico,
        String numeroEmpenho,
        BigDecimal valorBruto,
        BigDecimal valorTotalDeducoes,
        BigDecimal valorLiquido,
        List<RetencaoTributaria> retencoes
) {
    public DadosFiscais {
        retencoes = retencoes != null ? Collections.unmodifiableList(new ArrayList<>(retencoes)) : Collections.emptyList();
    }

    /**
     * Gera o mapa de snapshot para registro imutável na trilha de auditoria.
     */
    public Map<String, Object> gerarSnapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("tipoDocumento", tipoDocumento != null ? tipoDocumento.name() : null);
        snapshot.put("numeroDocumento", numeroDocumento);
        snapshot.put("serieDocumento", serieDocumento);
        snapshot.put("chaveAcessoNfe", chaveAcessoNfe);
        snapshot.put("dataEmissao", dataEmissao != null ? dataEmissao.toString() : null);
        snapshot.put("cnpjCredor", cnpjCredor);
        snapshot.put("razaoSocialCredor", razaoSocialCredor);
        snapshot.put("descricaoServico", descricaoServico);
        snapshot.put("numeroEmpenho", numeroEmpenho);
        snapshot.put("valorBruto", valorBruto != null ? valorBruto.toPlainString() : null);
        snapshot.put("valorTotalDeducoes", valorTotalDeducoes != null ? valorTotalDeducoes.toPlainString() : null);
        snapshot.put("valorLiquido", valorLiquido != null ? valorLiquido.toPlainString() : null);
        snapshot.put("retencoes", retencoes);
        return snapshot;
    }

    /**
     * Valida o preenchimento de campos obrigatórios conforme as regras fiscais de liquidação.
     */
    public void validarCamposObrigatorios() {
        List<String> faltantes = new ArrayList<>();

        if (tipoDocumento == null) {
            faltantes.add("tipoDocumento");
        }
        if (numeroDocumento == null || numeroDocumento.trim().isEmpty()) {
            faltantes.add("numeroDocumento");
        }
        if (dataEmissao == null) {
            faltantes.add("dataEmissao");
        }
        if (cnpjCredor == null || cnpjCredor.trim().isEmpty()) {
            faltantes.add("cnpjCredor");
        } else {
            try {
                new Cnpj(cnpjCredor);
            } catch (Exception e) {
                faltantes.add("cnpjCredor (CNPJ inválido: " + e.getMessage() + ")");
            }
        }
        if (razaoSocialCredor == null || razaoSocialCredor.trim().isEmpty()) {
            faltantes.add("razaoSocialCredor");
        }
        if (valorBruto == null || valorBruto.compareTo(BigDecimal.ZERO) <= 0) {
            faltantes.add("valorBruto (deve ser maior que zero)");
        }
        if (valorLiquido == null || valorLiquido.compareTo(BigDecimal.ZERO) < 0) {
            faltantes.add("valorLiquido (não pode ser negativo)");
        }

        if (!faltantes.isEmpty()) {
            throw new br.com.govflow.core.domain.exception.CamposObrigatoriosAusentesException(faltantes);
        }
    }

    /**
     * Valida a integridade aritmética entre valor bruto, deduções/retenções e valor líquido.
     */
    public void validarConsistenciaMatematica() {
        if (valorBruto == null || valorLiquido == null) {
            return;
        }

        BigDecimal somaRetencoes = retencoes.stream()
                .map(RetencaoTributaria::valor)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal deducoes = valorTotalDeducoes;

        if (!retencoes.isEmpty() && deducoes != null && deducoes.compareTo(BigDecimal.ZERO) > 0) {
            if (somaRetencoes.compareTo(deducoes) != 0) {
                throw new br.com.govflow.core.domain.exception.InconsistenciaMatematicaException(
                        String.format("A soma das retenções tributárias discriminadas (%s) difere do valor total de deduções informado (%s).",
                                somaRetencoes, deducoes)
                );
            }
        }

        if (deducoes == null || deducoes.compareTo(BigDecimal.ZERO) == 0) {
            deducoes = somaRetencoes;
        }

        BigDecimal valorLiquidoEsperado = valorBruto.subtract(deducoes);
        BigDecimal diferenca = valorLiquidoEsperado.subtract(valorLiquido).abs().setScale(2, java.math.RoundingMode.HALF_UP);

        if (diferenca.compareTo(BigDecimal.ZERO) > 0) {
            throw new br.com.govflow.core.domain.exception.InconsistenciaMatematicaException(valorBruto, deducoes, valorLiquido, diferenca);
        }
    }

    /**
     * Valida consistência completa (campos obrigatórios e aritmética).
     */
    public void validarConsistencia() {
        validarCamposObrigatorios();
        validarConsistenciaMatematica();
    }
}
