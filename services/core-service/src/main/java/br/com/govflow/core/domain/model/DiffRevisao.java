package br.com.govflow.core.domain.model;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public record DiffRevisao(
        Map<String, AlteracaoCampo> alteracoes
) {
    public DiffRevisao {
        alteracoes = alteracoes != null ? Collections.unmodifiableMap(new LinkedHashMap<>(alteracoes)) : Collections.emptyMap();
    }

    public boolean temAlteracoes() {
        return !alteracoes.isEmpty();
    }

    public static DiffRevisao comparar(ExtracaoSugerida sugerida, DadosRevisaoAnalista revisado) {
        Map<String, AlteracaoCampo> diffs = new LinkedHashMap<>();

        if (sugerida == null || revisado == null) {
            return new DiffRevisao(diffs);
        }

        compararCampo(diffs, "tipoDocumento",
                sugerida.tipoDocumento() != null ? sugerida.tipoDocumento().name() : null,
                revisado.tipoDocumento() != null ? revisado.tipoDocumento().name() : null);

        compararCampo(diffs, "numeroDocumento", sugerida.numeroDocumento(), revisado.numeroDocumento());
        compararCampo(diffs, "serieDocumento", sugerida.serieDocumento(), revisado.serieDocumento());
        compararCampo(diffs, "chaveAcessoNfe", sugerida.chaveAcessoNfe(), revisado.chaveAcessoNfe());

        compararCampo(diffs, "dataEmissao",
                sugerida.dataEmissao() != null ? sugerida.dataEmissao().toString() : null,
                revisado.dataEmissao() != null ? revisado.dataEmissao().toString() : null);

        compararCampo(diffs, "cnpjCredor", sugerida.cnpjCredor(), revisado.cnpjCredor());
        compararCampo(diffs, "razaoSocialCredor", sugerida.razaoSocialCredor(), revisado.razaoSocialCredor());
        compararCampo(diffs, "descricaoServico", sugerida.descricaoServico(), revisado.descricaoServico());
        compararCampo(diffs, "numeroEmpenho", sugerida.numeroEmpenho(), revisado.numeroEmpenho());

        compararValorMonetario(diffs, "valorBruto", sugerida.valorBruto(), revisado.valorBruto());
        compararValorMonetario(diffs, "valorTotalDeducoes", sugerida.valorTotalDeducoes(), revisado.valorTotalDeducoes());
        compararValorMonetario(diffs, "valorLiquido", sugerida.valorLiquido(), revisado.valorLiquido());

        compararRetencoes(diffs, sugerida.retencoes(), revisado.retencoes());

        return new DiffRevisao(diffs);
    }

    private static void compararCampo(Map<String, AlteracaoCampo> diffs, String campo, String de, String para) {
        String cleanDe = de != null ? de.trim() : null;
        String cleanPara = para != null ? para.trim() : null;

        if (!Objects.equals(cleanDe, cleanPara)) {
            diffs.put(campo, new AlteracaoCampo(campo, cleanDe, cleanPara));
        }
    }

    private static void compararValorMonetario(Map<String, AlteracaoCampo> diffs, String campo, BigDecimal de, BigDecimal para) {
        if (de == null && para == null) {
            return;
        }
        if (de == null || para == null || de.compareTo(para) != 0) {
            diffs.put(campo, new AlteracaoCampo(
                    campo,
                    de != null ? de.toPlainString() : null,
                    para != null ? para.toPlainString() : null
            ));
        }
    }

    private static void compararRetencoes(Map<String, AlteracaoCampo> diffs, List<RetencaoTributaria> de, List<RetencaoTributaria> para) {
        String strDe = formatarRetencoes(de);
        String strPara = formatarRetencoes(para);

        if (!Objects.equals(strDe, strPara)) {
            diffs.put("retencoes", new AlteracaoCampo("retencoes", strDe, strPara));
        }
    }

    private static String formatarRetencoes(List<RetencaoTributaria> retencoes) {
        if (retencoes == null || retencoes.isEmpty()) {
            return "[]";
        }
        return retencoes.stream()
                .map(r -> String.format("%s(val=%s,aliq=%s)",
                        r.tipo() != null ? r.tipo().name() : "N/A",
                        r.valor() != null ? r.valor().toPlainString() : "0.00",
                        r.aliquota() != null ? r.aliquota().toPlainString() : "0.00"))
                .sorted()
                .collect(Collectors.joining(", ", "[", "]"));
    }
}
