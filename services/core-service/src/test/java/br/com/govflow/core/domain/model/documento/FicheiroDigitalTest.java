package br.com.govflow.core.domain.model.documento;

import br.com.govflow.core.domain.model.ArmazenamentoArquivo;
import br.com.govflow.core.domain.model.Documento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Domínio: Agregado FicheiroDigital e Enums do Ciclo de Vida")
class FicheiroDigitalTest {

    @Test
    @DisplayName("Deve inicializar as 10 Fases corretamente e calcular totalizadores")
    void deveInicializarFasesECalcularTotalizadores() {
        UUID convenioId = UUID.randomUUID();
        UUID prefeituraId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        Documento doc1 = Documento.criarNovo(
                UUID.randomUUID(), tenantId, prefeituraId, convenioId,
                FaseCicloVida.FASE_02_CLAUSULA_SUSPENSIVA,
                CategoriaDocumento.PROJETO_ENGENHARIA,
                "/02_Clausula_Suspensiva_e_Engenharia",
                new ArmazenamentoArquivo("bucket", "key1", "projeto.pdf", "application/pdf", 1024L),
                "hash1",
                OrigemCanal.UPLOAD_MANUAL,
                List.of("projeto", "arquitetura"),
                null,
                null
        );

        Documento doc2 = Documento.criarNovo(
                UUID.randomUUID(), tenantId, prefeituraId, convenioId,
                FaseCicloVida.FASE_04_EXECUCAO_FISICA,
                CategoriaDocumento.BOLETIM_MEDICAO,
                "/04_Execucao_Fisica_e_Medicoes",
                new ArmazenamentoArquivo("bucket", "key2", "bm01.pdf", "application/pdf", 2048L),
                "hash2",
                OrigemCanal.WHATSAPP,
                List.of("bm"),
                null,
                null
        );

        FicheiroDigital.PastaFase pasta02 = new FicheiroDigital.PastaFase(
                FaseCicloVida.FASE_02_CLAUSULA_SUSPENSIVA,
                "Fase 02",
                "02_Clausula_Suspensiva_e_Engenharia",
                "Engenharia",
                1,
                1024L,
                List.of(doc1)
        );

        FicheiroDigital.PastaFase pasta04 = new FicheiroDigital.PastaFase(
                FaseCicloVida.FASE_04_EXECUCAO_FISICA,
                "Fase 04",
                "04_Execucao_Fisica_e_Medicoes",
                "Medições",
                1,
                2048L,
                List.of(doc2)
        );

        FicheiroDigital ficheiro = new FicheiroDigital(
                convenioId, prefeituraId, tenantId,
                "954120/2024", "Construção de Creche Municipal",
                List.of(pasta02, pasta04)
        );

        assertEquals(2, ficheiro.getTotalArquivos());
        assertEquals(3072L, ficheiro.getTamanhoTotalBytes());
        assertEquals("954120/2024", ficheiro.getNumeroSiconv());
        assertNotNull(ficheiro.obterPasta(FaseCicloVida.FASE_02_CLAUSULA_SUSPENSIVA));
        assertNull(ficheiro.obterPasta(FaseCicloVida.FASE_00_PROPOSTA));
    }

    @Test
    @DisplayName("Deve mapear códigos e nomes flexíveis de fases no enum FaseCicloVida")
    void deveMapearFaseCicloVidaFlexivel() {
        assertEquals(FaseCicloVida.FASE_00_PROPOSTA, FaseCicloVida.fromCodigoOuNome("0"));
        assertEquals(FaseCicloVida.FASE_00_PROPOSTA, FaseCicloVida.fromCodigoOuNome("Fase 00"));
        assertEquals(FaseCicloVida.FASE_02_CLAUSULA_SUSPENSIVA, FaseCicloVida.fromCodigoOuNome("02_Clausula_Suspensiva_e_Engenharia"));
        assertEquals(FaseCicloVida.FASE_04_EXECUCAO_FISICA, FaseCicloVida.fromCodigoOuNome("FASE_04_EXECUCAO_FISICA"));
        assertEquals(FaseCicloVida.FASE_05_EXECUCAO_FINANCEIRA, FaseCicloVida.fromCodigoOuNome(null));
    }

    @Test
    @DisplayName("Deve mover pasta e marcar exclusão lógica no agregado Documento")
    void deveMoverPastaEMarcarExclusao() {
        Documento doc = Documento.criarNovo(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                FaseCicloVida.FASE_05_EXECUCAO_FINANCEIRA,
                CategoriaDocumento.DOCUMENTO_HABIL,
                "/",
                new ArmazenamentoArquivo("bucket", "key", "nota.pdf", "application/pdf", 500L),
                "hash",
                OrigemCanal.UPLOAD_MANUAL,
                null,
                null,
                null
        );

        doc.moverPasta(FaseCicloVida.FASE_04_EXECUCAO_FISICA, "/04_Execucao_Fisica_e_Medicoes/Medicoes_2026");
        assertEquals(FaseCicloVida.FASE_04_EXECUCAO_FISICA, doc.getFaseCicloVida());
        assertEquals("/04_Execucao_Fisica_e_Medicoes/Medicoes_2026", doc.getPastaVirtual());

        doc.marcarExcluido("Documento anexado por engano");
        assertTrue(doc.isFinalizado());
        assertEquals("Documento anexado por engano", doc.getMotivoRejeicao());
    }
}
