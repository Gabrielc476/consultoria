package br.com.govflow.transferegov.quality;

import br.com.govflow.transferegov.domain.model.ConvenioSincronizado;
import br.com.govflow.transferegov.domain.quality.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes Unitários do Motor de Data Quality (SiconvDataQualityValidator)")
class SiconvDataQualityValidatorTest {

    private SiconvDataQualityValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SiconvDataQualityValidator(List.of(
                new CompletenessQualityRule(),
                new ValidityQualityRule(),
                new ConsistencyQualityRule()
        ));
    }

    @Test
    @DisplayName("Deve aprovar convênio com dados íntegros e consistentes")
    void deveAprovarConvenioValido() {
        ConvenioSincronizado convenio = new ConvenioSincronizado(
                "912345",
                "5001",
                "08923456000112",
                "PREFEITURA MUNICIPAL DE MASSARANDUBA",
                "Massaranduba",
                "PB",
                "Em Execucao",
                true,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2026, 12, 31),
                LocalDate.of(2027, 2, 28),
                LocalDate.of(2024, 6, 30),
                new BigDecimal("500000.00"),
                new BigDecimal("450000.00"),
                new BigDecimal("50000.00"),
                new BigDecimal("120000.00"),
                "Pavimentação Asfáltica",
                "23/09/2026"
        );

        DataQualityResult result = validator.validate(convenio, 1L);

        assertTrue(result.valid(), "Convenio íntegro deve ser válido");
        assertTrue(result.issues().isEmpty(), "Não deve haver issues registradas");
    }

    @Test
    @DisplayName("Deve reprovar e sinalizar COMPLETENESS quando campos obrigatórios estiverem ausentes")
    void deveDetectarFalhaCompletude() {
        ConvenioSincronizado convenioIncompleto = new ConvenioSincronizado(
                "", // nrConvenio vazio
                null, // idProposta nulo
                "",
                "PREFEITURA",
                "",
                "PB",
                "Em Execucao",
                true,
                null,
                null,
                null,
                null,
                null, // valorGlobal nulo
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                null
        );

        DataQualityResult result = validator.validate(convenioIncompleto, 2L);

        assertFalse(result.valid());
        assertTrue(result.issues().stream().anyMatch(i -> i.dimension() == DataQualityDimension.COMPLETENESS));
    }

    @Test
    @DisplayName("Deve reprovar e sinalizar VALIDITY quando CNPJ tiver formato incorreto ou valores forem negativos")
    void deveDetectarFalhaValidade() {
        ConvenioSincronizado convenioInvalido = new ConvenioSincronizado(
                "912345",
                "5001",
                "123", // CNPJ com 3 dígitos (inválido)
                "PREFEITURA DE TESTE",
                "Pombal",
                "PB",
                "Em Execucao",
                true,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 3, 1),
                null,
                new BigDecimal("-500.00"), // Valor global negativo
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "Objeto",
                null
        );

        DataQualityResult result = validator.validate(convenioInvalido, 3L);

        assertFalse(result.valid());
        List<DataQualityIssue> issues = result.issues();
        assertTrue(issues.stream().anyMatch(i -> i.dimension() == DataQualityDimension.VALIDITY && i.field().equals("cnpj_proponente")));
        assertTrue(issues.stream().anyMatch(i -> i.dimension() == DataQualityDimension.VALIDITY && i.field().equals("valor_global")));
    }

    @Test
    @DisplayName("Deve reprovar e sinalizar CONSISTENCY quando vigência final for anterior à inicial")
    void deveDetectarFalhaConsistenciaCronologica() {
        ConvenioSincronizado convenioInconsistente = new ConvenioSincronizado(
                "912345",
                "5001",
                "08923456000112",
                "PREFEITURA DE TESTE",
                "Massaranduba",
                "PB",
                "Em Execucao",
                true,
                LocalDate.of(2025, 1, 1), // início
                LocalDate.of(2024, 1, 1), // fim ANTES do início
                LocalDate.of(2025, 3, 1),
                null,
                new BigDecimal("100000.00"),
                new BigDecimal("90000.00"),
                new BigDecimal("10000.00"),
                BigDecimal.ZERO,
                "Objeto",
                null
        );

        DataQualityResult result = validator.validate(convenioInconsistente, 4L);

        assertFalse(result.valid());
        assertTrue(result.issues().stream().anyMatch(i -> i.dimension() == DataQualityDimension.CONSISTENCY && i.field().equals("data_fim_vigencia")));
    }

    @Test
    @DisplayName("Deve reprovar e sinalizar CONSISTENCY quando soma de repasse + contrapartida divergir do valor global")
    void deveDetectarDivergenciaFinanceira() {
        ConvenioSincronizado convenioDivergente = new ConvenioSincronizado(
                "912345",
                "5001",
                "08923456000112",
                "PREFEITURA DE TESTE",
                "Massaranduba",
                "PB",
                "Em Execucao",
                true,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 3, 1),
                null,
                new BigDecimal("100000.00"), // Global 100k
                new BigDecimal("50000.00"),  // Repasse 50k
                new BigDecimal("10000.00"),  // Contrapartida 10k -> Soma 60k != 100k
                BigDecimal.ZERO,
                "Objeto",
                null
        );

        DataQualityResult result = validator.validate(convenioDivergente, 5L);

        assertFalse(result.valid());
        assertTrue(result.issues().stream().anyMatch(i -> i.dimension() == DataQualityDimension.CONSISTENCY && i.description().contains("Inconsistência na composição financeira")));
    }
}
