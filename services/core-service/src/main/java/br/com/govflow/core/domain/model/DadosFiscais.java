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
}
