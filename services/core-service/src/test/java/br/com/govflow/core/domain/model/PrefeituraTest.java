package br.com.govflow.core.domain.model;

import br.com.govflow.core.domain.exception.CodigoIbgeInvalidoException;
import br.com.govflow.core.domain.exception.TenantInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PrefeituraTest {

    private final UUID tenantId = UUID.randomUUID();
    private final Cnpj cnpjMassaranduba = new Cnpj("08.923.456/0001-35");
    private final CodigoIbge ibgeMassaranduba = new CodigoIbge("2509701");

    @Test
    @DisplayName("Deve criar com sucesso uma prefeitura com todos os dados válidos")
    void deveCriarPrefeituraComSucesso() {
        Prefeitura prefeitura = Prefeitura.criarNova(
                tenantId,
                new Cnpj("08.778.326/0001-56"),
                "Prefeitura Municipal de João Pessoa",
                "João Pessoa",
                Uf.PB,
                new CodigoIbge("2507507"),
                PorteMunicipio.GRANDE_PORTE,
                "Cícero Lucena",
                null,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2028, 12, 31)
        );

        assertNotNull(prefeitura.getId());
        assertEquals(tenantId, prefeitura.getTenantId());
        assertEquals("08.778.326/0001-56", prefeitura.getCnpj().getFormatted());
        assertEquals(Uf.PB, prefeitura.getUf());
        assertEquals(PorteMunicipio.GRANDE_PORTE, prefeitura.getPorteMunicipio());
        assertEquals(StatusCauc.ADIMPLENTE, prefeitura.getStatusCauc());
        assertTrue(prefeitura.isAtivo());
    }

    @Test
    @DisplayName("Deve rejeitar prefeitura com código IBGE incompatível com a UF")
    void deveRejeitarPrefeituraComIbgeIncompativel() {
        // IBGE de São Paulo (3550308) com UF da Paraíba (PB)
        assertThrows(CodigoIbgeInvalidoException.class, () -> Prefeitura.criarNova(
                tenantId,
                new Cnpj("08.778.326/0001-56"),
                "Prefeitura Teste",
                "Teste",
                Uf.PB,
                new CodigoIbge("3550308"), // SP
                PorteMunicipio.PEQUENO_PORTE_1,
                null,
                null,
                null,
                null
        ));
    }

    @Test
    @DisplayName("Deve rejeitar prefeitura com data final de mandato anterior à inicial")
    void deveRejeitarDatasDeMandatoInvertidas() {
        assertThrows(IllegalArgumentException.class, () -> Prefeitura.criarNova(
                tenantId,
                new Cnpj("08.778.326/0001-56"),
                "Prefeitura Teste",
                "Teste",
                Uf.PB,
                new CodigoIbge("2507507"),
                PorteMunicipio.PEQUENO_PORTE_1,
                null,
                null,
                LocalDate.of(2028, 1, 1),
                LocalDate.of(2025, 1, 1) // Invertida
        ));
    }

    @Test
    @DisplayName("Deve alternar corretamente o status ativo/inativo e CAUC")
    void deveAlternarStatusAtivoECauc() {
        Prefeitura prefeitura = Prefeitura.criarNova(
                tenantId,
                new Cnpj("08.778.326/0001-56"),
                "Prefeitura Teste",
                "Teste",
                Uf.PB,
                new CodigoIbge("2507507"),
                PorteMunicipio.PEQUENO_PORTE_1,
                null,
                null,
                null,
                null
        );

        assertTrue(prefeitura.isAtivo());
        prefeitura.inativar();
        assertFalse(prefeitura.isAtivo());

        prefeitura.ativar();
        assertTrue(prefeitura.isAtivo());

        assertEquals(StatusCauc.ADIMPLENTE, prefeitura.getStatusCauc());
        prefeitura.atualizarStatusCauc(StatusCauc.BLOQUEADO);
        assertEquals(StatusCauc.BLOQUEADO, prefeitura.getStatusCauc());
    }
}
