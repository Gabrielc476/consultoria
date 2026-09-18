package br.com.govflow.core.infrastructure.adapter.in.amqp;

import br.com.govflow.core.domain.model.BoundingBox;
import br.com.govflow.core.domain.model.DadosFiscais;
import br.com.govflow.core.domain.model.ExtracaoSugerida;
import br.com.govflow.core.domain.model.RetencaoTributaria;
import br.com.govflow.core.domain.model.TipoDocumentoHabil;
import br.com.govflow.core.domain.model.TipoRetencao;
import br.com.govflow.core.infrastructure.adapter.in.amqp.dto.DocumentoExtraidoPayloadDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class DocumentoExtraidoPayloadParser {

    private static final Logger log = LoggerFactory.getLogger(DocumentoExtraidoPayloadParser.class);
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    public ExtracaoSugerida parseExtracao(DocumentoExtraidoPayloadDto payload) {
        if (payload == null || payload.extracao() == null) {
            return null;
        }

        Map<String, Object> map = payload.extracao();
        Map<String, Double> scoresCampos = new LinkedHashMap<>();

        TipoDocumentoHabil tipo = parseTipoDocumento(extractFieldString(map, "tipo_documento", "tipoDocumento"));
        extractAndPutConfidence(scoresCampos, "tipoDocumento", map, "tipo_documento", "tipoDocumento");

        String numero = extractFieldString(map, "numero_documento", "numeroDocumento");
        extractAndPutConfidence(scoresCampos, "numeroDocumento", map, "numero_documento", "numeroDocumento");

        String serie = extractFieldString(map, "serie_documento", "serieDocumento");
        extractAndPutConfidence(scoresCampos, "serieDocumento", map, "serie_documento", "serieDocumento");

        String chave = extractFieldString(map, "chave_acesso_nfe", "chaveAcessoNfe");
        extractAndPutConfidence(scoresCampos, "chaveAcessoNfe", map, "chave_acesso_nfe", "chaveAcessoNfe");

        LocalDate dataEmissao = parseLocalDate(extractFieldString(map, "data_emissao", "dataEmissao"));
        extractAndPutConfidence(scoresCampos, "dataEmissao", map, "data_emissao", "dataEmissao");

        String cnpj = extractFieldString(map, "cnpj_credor", "cnpjCredor");
        extractAndPutConfidence(scoresCampos, "cnpjCredor", map, "cnpj_credor", "cnpjCredor");

        String razaoSocial = extractFieldString(map, "razao_social_credor", "razaoSocialCredor");
        extractAndPutConfidence(scoresCampos, "razaoSocialCredor", map, "razao_social_credor", "razaoSocialCredor");

        String descricao = extractFieldString(map, "descricao_servico", "descricaoServico");
        extractAndPutConfidence(scoresCampos, "descricaoServico", map, "descricao_servico", "descricaoServico");

        String empenho = extractFieldString(map, "numero_empenho", "numeroEmpenho");
        extractAndPutConfidence(scoresCampos, "numeroEmpenho", map, "numero_empenho", "numeroEmpenho");

        BigDecimal bruto = parseBigDecimal(extractFieldString(map, "valor_bruto", "valorBruto"));
        extractAndPutConfidence(scoresCampos, "valorBruto", map, "valor_bruto", "valorBruto");

        BigDecimal deducoes = parseBigDecimal(extractFieldString(map, "valor_total_deducoes", "valorTotalDeducoes"));
        extractAndPutConfidence(scoresCampos, "valorTotalDeducoes", map, "valor_total_deducoes", "valorTotalDeducoes");

        BigDecimal liquido = parseBigDecimal(extractFieldString(map, "valor_liquido", "valorLiquido"));
        extractAndPutConfidence(scoresCampos, "valorLiquido", map, "valor_liquido", "valorLiquido");

        List<RetencaoTributaria> retencoes = parseRetencoes(map.get("retencoes"));

        boolean consistente = true;
        if (payload.validacaoMatematica() != null) {
            Object consObj = payload.validacaoMatematica().get("consistente");
            if (consObj instanceof Boolean b) {
                consistente = b;
            }
        }

        List<String> alertas = new ArrayList<>();
        Object alertasObj = map.get("alertas_inconsistencia");
        if (alertasObj instanceof List<?> list) {
            for (Object item : list) {
                if (item != null) {
                    alertas.add(item.toString());
                }
            }
        }

        double confidence = payload.confidenceScoreGeral() != null ? payload.confidenceScoreGeral() : 1.0;

        DadosFiscais dadosFiscais = new DadosFiscais(
                tipo,
                numero,
                serie,
                chave,
                dataEmissao,
                cnpj,
                razaoSocial,
                descricao,
                empenho,
                bruto,
                deducoes != null ? deducoes : BigDecimal.ZERO,
                liquido,
                retencoes
        );

        return new ExtracaoSugerida(
                dadosFiscais,
                confidence,
                scoresCampos,
                consistente,
                alertas
        );
    }

    public Map<String, BoundingBox> parseBoundingBoxes(DocumentoExtraidoPayloadDto payload) {
        Map<String, BoundingBox> result = new LinkedHashMap<>();
        if (payload == null || payload.boundingBoxes() == null) {
            return result;
        }

        Map<String, Object> rawMap = payload.boundingBoxes();
        for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
            BoundingBox box = parseSingleBoundingBox(entry.getValue());
            if (box != null) {
                result.put(entry.getKey(), box);
            }
        }

        return result;
    }

    private BoundingBox parseSingleBoundingBox(Object value) {
        if (value instanceof List<?> list && list.size() >= 4) {
            try {
                double ymin = toDouble(list.get(0));
                double xmin = toDouble(list.get(1));
                double ymax = toDouble(list.get(2));
                double xmax = toDouble(list.get(3));
                return new BoundingBox(ymin, xmin, ymax, xmax);
            } catch (Exception e) {
                return null;
            }
        } else if (value instanceof Map<?, ?> map) {
            try {
                double ymin = toDouble(map.get("ymin"));
                double xmin = toDouble(map.get("xmin"));
                double ymax = toDouble(map.get("ymax"));
                double xmax = toDouble(map.get("xmax"));
                return new BoundingBox(ymin, xmin, ymax, xmax);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private double toDouble(Object obj) {
        if (obj instanceof Number n) {
            return n.doubleValue();
        }
        if (obj != null) {
            try {
                return Double.parseDouble(obj.toString());
            } catch (Exception ignored) {
            }
        }
        return 0.0;
    }

    private String extractFieldString(Map<String, Object> map, String key1, String key2) {
        Object val = map.containsKey(key1) ? map.get(key1) : map.get(key2);
        if (val == null) {
            return null;
        }
        if (val instanceof Map<?, ?> fieldMap) {
            Object inner = fieldMap.get("valor");
            return inner != null ? inner.toString() : null;
        }
        return val.toString();
    }

    private void extractAndPutConfidence(Map<String, Double> scores, String fieldName, Map<String, Object> map, String key1, String key2) {
        Object val = map.containsKey(key1) ? map.get(key1) : map.get(key2);
        if (val instanceof Map<?, ?> fieldMap) {
            Object confObj = fieldMap.get("confianca");
            if (confObj != null) {
                scores.put(fieldName, toDouble(confObj));
            }
        }
    }

    private TipoDocumentoHabil parseTipoDocumento(String str) {
        if (str == null || str.isBlank()) {
            return TipoDocumentoHabil.NOTA_FISCAL_SERVICOS;
        }
        try {
            return TipoDocumentoHabil.valueOf(str.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return TipoDocumentoHabil.NOTA_FISCAL_SERVICOS;
        }
    }

    private LocalDate parseLocalDate(String str) {
        if (str == null || str.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(str.trim(), ISO_FORMATTER);
        } catch (Exception e) {
            log.warn("Não foi possível converter data '{}' no formato ISO", str);
            return null;
        }
    }

    private BigDecimal parseBigDecimal(String str) {
        if (str == null || str.isBlank()) {
            return null;
        }
        String cleaned = str.trim()
                .replace("R$", "")
                .replace(" ", "");
        if (cleaned.contains(",") && cleaned.contains(".")) {
            cleaned = cleaned.replace(".", "").replace(",", ".");
        } else if (cleaned.contains(",")) {
            cleaned = cleaned.replace(",", ".");
        }
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<RetencaoTributaria> parseRetencoes(Object obj) {
        if (!(obj instanceof List<?> list)) {
            return Collections.emptyList();
        }
        List<RetencaoTributaria> retencoes = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> rMap) {
                try {
                    String tipoStr = Objects.toString(rMap.get("tipo"), "ISS");
                    TipoRetencao tipo = TipoRetencao.valueOf(tipoStr.toUpperCase());
                    BigDecimal aliquota = parseBigDecimal(Objects.toString(rMap.get("aliquota"), "0.0"));
                    BigDecimal valor = parseBigDecimal(Objects.toString(rMap.get("valor"), "0.0"));
                    double confianca = toDouble(rMap.get("confianca"));
                    BoundingBox box = parseSingleBoundingBox(rMap.get("coordenadas"));

                    retencoes.add(new RetencaoTributaria(
                            tipo,
                            aliquota != null ? aliquota : BigDecimal.ZERO,
                            valor != null ? valor : BigDecimal.ZERO,
                            confianca > 0 ? confianca : 0.99,
                            box
                    ));
                } catch (Exception e) {
                    log.warn("Erro ao processar retenção individual: {}", item, e);
                }
            }
        }
        return retencoes;
    }
}
