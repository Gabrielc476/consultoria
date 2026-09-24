package br.com.govflow.transferegov;

import br.com.govflow.transferegov.sync.client.SiconvStreamingClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Teste de Carregamento de Contexto da Aplicação")
class TransferegovServiceApplicationTests {

    @MockBean
    private SiconvStreamingClient streamingClient;

    @Test
    void contextLoads() {
    }
}
