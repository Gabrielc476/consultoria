package br.com.govflow.transferegov.sync.mock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MockSiconvArchiveGenerator {

    public static byte[] createZipArchive(String entryName, String content) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry(entryName);
            zos.putNextEntry(entry);
            zos.write(content.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return baos.toByteArray();
    }

    public static byte[] generateSentinelaZip(String timestamp) throws IOException {
        String csv = "DATA_CARGA_SICONV\n" + timestamp + "\n";
        return createZipArchive("data_carga_siconv.csv", csv);
    }

    public static byte[] generateProponentesZip() throws IOException {
        String csv = """
                ID_PROPONENTE;IDENTIF_PROPONENTE;NM_PROPONENTE;UF_PROPONENTE;MUNICIPIO_PROPONENTE
                1001;08923456000112;PREFEITURA MUNICIPAL DE MASSARANDUBA;PB;Massaranduba
                1002;08123456000199;PREFEITURA MUNICIPAL DE JOAO PESSOA;PB;Joao Pessoa
                9999;11222333000144;PREFEITURA MUNICIPAL DE CAMPINAS;SP;Campinas
                """;
        return createZipArchive("siconv_proponentes.csv", csv);
    }

    public static byte[] generatePropostasZip() throws IOException {
        String csv = """
                ID_PROPOSTA;ID_PROPONENTE;OBJETO_PROPOSTA
                5001;1001;Pavimentacao em paralelepipedo de diversas ruas
                5002;1002;Construcao de creche tipo B Proinfancia
                8888;9999;Recapeamento asfaltico em Campinas SP
                """;
        return createZipArchive("siconv_proposta.csv", csv);
    }

    public static byte[] generateConveniosZip() throws IOException {
        String csv = """
                NR_CONVENIO;ID_PROPOSTA;SIT_CONVENIO;INSTRUMENTO_ATIVO;DIA_INIC_VIGENC_CONV;DIA_FIM_VIGENC_CONV;DIA_LIMITE_PREST_CONTAS;DATA_SUSPENSIVA;VL_GLOBAL_CONV;VL_REPASSE_CONV;VL_CONTRAPARTIDA_CONV;VL_SALDO_CONTA;OBJETO_CONVENIO
                912345;5001;Em Execucao;SIM;01/01/2024;31/12/2026;01/03/2027;30/06/2024;500.000,00;450.000,00;50.000,00;120.000,00;Pavimentacao Massaranduba
                912346;5002;Prestacao de Contas;SIM;15/03/2023;15/03/2025;15/05/2025;01/09/2023;1.200.000,00;1.000.000,00;200.000,00;0,00;Creche Proinfancia JP
                912347;5001;Cancelado;NAO;01/01/2024;01/01/2023;01/01/2023;;100.000,00;90.000,00;10.000,00;0,00;Convenio Inconsistente Cronologia
                999999;8888;Em Execucao;SIM;01/01/2024;31/12/2025;28/02/2026;;300.000,00;250.000,00;50.000,00;0,00;Convenio Campinas SP Fora PB
                """;
        return createZipArchive("siconv_convenio.csv", csv);
    }
}
