package br.com.govflow.transferegov.sync.pipeline;

import br.com.govflow.transferegov.domain.model.ConvenioSincronizado;
import br.com.govflow.transferegov.domain.model.ProponenteInfo;
import br.com.govflow.transferegov.domain.model.PropostaInfo;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class SiconvCsvParser {

    private static final Logger log = LoggerFactory.getLogger(SiconvCsvParser.class);

    private static final DateTimeFormatter FORMATTER_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATTER_ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final CSVFormat SICONV_CSV_FORMAT = CSVFormat.DEFAULT.builder()
            .setDelimiter(';')
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreHeaderCase(true)
            .setTrim(true)
            .setAllowMissingColumnNames(true)
            .build();

    /**
     * Extrai a data de carga do arquivo sentinela data_carga_siconv.zip em memória.
     */
    public String readSentinelaDataCarga(InputStream zipInputStream) throws IOException {
        ZipInputStream zis = new ZipInputStream(zipInputStream);
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            if (entry.getName().toLowerCase().endsWith(".csv") || entry.getName().toLowerCase().endsWith(".txt")) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(zis, StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.equalsIgnoreCase("DATA_CARGA_SICONV")) {
                        return line;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Faz o streaming de siconv_proponentes.zip em memória e filtra em voo registros da UF ou CNPJs alvo.
     */
    public Map<String, ProponenteInfo> streamProponentes(
            InputStream zipInputStream,
            String ufFilter,
            Set<String> targetCnpjs
    ) throws IOException {
        Map<String, ProponenteInfo> proponentes = new HashMap<>();
        String normalizedUf = (ufFilter != null) ? ufFilter.trim().toUpperCase() : "PB";

        ZipInputStream zis = new ZipInputStream(zipInputStream);
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            if (entry.getName().toLowerCase().endsWith(".csv")) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(zis, StandardCharsets.UTF_8));
                CSVParser parser = SICONV_CSV_FORMAT.parse(reader);

                for (CSVRecord record : parser) {
                    String uf = getField(record, "UF_PROPONENTE");
                    String cnpj = getField(record, "IDENTIF_PROPONENTE");

                    boolean matchUf = uf != null && uf.equalsIgnoreCase(normalizedUf);
                    boolean matchCnpj = targetCnpjs != null && cnpj != null && targetCnpjs.contains(cnpj);

                    if (matchUf || matchCnpj) {
                        String idProponente = getField(record, "ID_PROPONENTE");
                        String nome = getField(record, "NM_PROPONENTE");
                        String municipio = getField(record, "MUNICIPIO_PROPONENTE");

                        if (idProponente != null && !idProponente.isBlank()) {
                            proponentes.put(idProponente, new ProponenteInfo(
                                    idProponente,
                                    cnpj != null ? cnpj : "",
                                    nome != null ? nome : "",
                                    municipio != null ? municipio : "",
                                    uf != null ? uf : normalizedUf
                            ));
                        }
                    }
                }
            }
        }

        log.info("Streaming de proponentes concluído. Proponentes retidos em memória: {}", proponentes.size());
        return proponentes;
    }

    /**
     * Faz o streaming de siconv_proposta.zip em memória e retém propostas associadas aos proponentes filtrados.
     */
    public Map<String, PropostaInfo> streamPropostas(
            InputStream zipInputStream,
            Set<String> targetProponenteIds
    ) throws IOException {
        Map<String, PropostaInfo> propostas = new HashMap<>();

        ZipInputStream zis = new ZipInputStream(zipInputStream);
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            if (entry.getName().toLowerCase().endsWith(".csv")) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(zis, StandardCharsets.UTF_8));
                CSVParser parser = SICONV_CSV_FORMAT.parse(reader);

                for (CSVRecord record : parser) {
                    String idProponente = getField(record, "ID_PROPONENTE");

                    if (idProponente != null && targetProponenteIds.contains(idProponente)) {
                        String idProposta = getField(record, "ID_PROPOSTA");
                        String objeto = getField(record, "OBJETO_PROPOSTA");

                        if (idProposta != null && !idProposta.isBlank()) {
                            propostas.put(idProposta, new PropostaInfo(idProposta, idProponente, objeto));
                        }
                    }
                }
            }
        }

        log.info("Streaming de propostas concluído. Propostas retidas em memória: {}", propostas.size());
        return propostas;
    }

    /**
     * Faz o streaming de siconv_convenio.zip, cruza com propostas e proponentes em memória e
     * despacha cada convênio consolidado para o consumidor de processamento em voo.
     */
    public void streamConvenios(
            InputStream zipInputStream,
            Map<String, ProponenteInfo> proponentes,
            Map<String, PropostaInfo> propostas,
            String dataCargaSiconv,
            BiConsumer<ConvenioSincronizado, Long> convenioConsumer
    ) throws IOException {
        ZipInputStream zis = new ZipInputStream(zipInputStream);
        ZipEntry entry;
        long rowNumber = 0;

        while ((entry = zis.getNextEntry()) != null) {
            if (entry.getName().toLowerCase().endsWith(".csv")) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(zis, StandardCharsets.UTF_8));
                CSVParser parser = SICONV_CSV_FORMAT.parse(reader);

                for (CSVRecord record : parser) {
                    rowNumber++;
                    String idProposta = getField(record, "ID_PROPOSTA");

                    if (idProposta != null && propostas.containsKey(idProposta)) {
                        PropostaInfo proposta = propostas.get(idProposta);
                        ProponenteInfo proponente = proponentes.get(proposta.idProponente());

                        ConvenioSincronizado convenio = buildConvenio(record, proposta, proponente, dataCargaSiconv);
                        convenioConsumer.accept(convenio, rowNumber);
                    }
                }
            }
        }

        log.info("Streaming de convênios finalizado. Total de linhas avaliadas: {}", rowNumber);
    }

    private ConvenioSincronizado buildConvenio(
            CSVRecord record,
            PropostaInfo proposta,
            ProponenteInfo proponente,
            String dataCargaSiconv
    ) {
        String nrConvenio = getField(record, "NR_CONVENIO");
        String situacao = getField(record, "SIT_CONVENIO");
        boolean ativo = parseBoolean(getField(record, "INSTRUMENTO_ATIVO"));

        LocalDate dataInicio = parseDate(getField(record, "DIA_INIC_VIGENC_CONV"));
        LocalDate dataFim = parseDate(getField(record, "DIA_FIM_VIGENC_CONV"));
        LocalDate dataLimitePrest = parseDate(getField(record, "DIA_LIMITE_PREST_CONTAS"));
        LocalDate dataSuspensiva = parseDate(getField(record, "DATA_SUSPENSIVA"));

        BigDecimal valorGlobal = parseMoney(getField(record, "VL_GLOBAL_CONV"));
        BigDecimal valorRepasse = parseMoney(getField(record, "VL_REPASSE_CONV"));
        BigDecimal valorContrapartida = parseMoney(getField(record, "VL_CONTRAPARTIDA_CONV"));
        BigDecimal valorSaldoConta = parseMoney(getField(record, "VL_SALDO_CONTA"));

        String objeto = proposta != null ? proposta.objeto() : null;
        if (objeto == null || objeto.isBlank()) {
            objeto = getField(record, "OBJETO_CONVENIO");
        }

        return new ConvenioSincronizado(
                nrConvenio,
                proposta != null ? proposta.idProposta() : getField(record, "ID_PROPOSTA"),
                proponente != null ? proponente.cnpj() : "",
                proponente != null ? proponente.nome() : "",
                proponente != null ? proponente.municipio() : "",
                proponente != null ? proponente.uf() : "PB",
                situacao,
                ativo,
                dataInicio,
                dataFim,
                dataLimitePrest,
                dataSuspensiva,
                valorGlobal,
                valorRepasse,
                valorContrapartida,
                valorSaldoConta,
                objeto,
                dataCargaSiconv
        );
    }

    public static String getField(CSVRecord record, String fieldName) {
        try {
            if (record.isMapped(fieldName)) {
                String val = record.get(fieldName);
                return val != null ? val.trim() : null;
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static LocalDate parseDate(String val) {
        if (val == null || val.isBlank()) {
            return null;
        }
        val = val.trim();
        try {
            if (val.contains("/")) {
                return LocalDate.parse(val, FORMATTER_BR);
            } else if (val.contains("-")) {
                return LocalDate.parse(val, FORMATTER_ISO);
            }
        } catch (DateTimeParseException e) {
            log.debug("Data não pôde ser convertida: '{}'", val);
        }
        return null;
    }

    public static BigDecimal parseMoney(String val) {
        if (val == null || val.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            String clean = val.trim();
            // Tratar formato brasileiro "1.250.000,50" -> "1250000.50"
            if (clean.contains(",")) {
                clean = clean.replace(".", "").replace(",", ".");
            }
            return new BigDecimal(clean);
        } catch (Exception e) {
            log.debug("Valor monetário inválido: '{}'", val);
            return BigDecimal.ZERO;
        }
    }

    public static boolean parseBoolean(String val) {
        if (val == null || val.isBlank()) {
            return true;
        }
        String clean = val.trim().toUpperCase();
        return clean.equals("SIM") || clean.equals("S") || clean.equals("TRUE") || clean.equals("1") || clean.equals("ATIVO");
    }
}
