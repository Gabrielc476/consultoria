package br.com.govflow.transferegov.sync.rest;

import br.com.govflow.transferegov.persistence.entity.SincronizacaoLogEntity;
import br.com.govflow.transferegov.sync.pipeline.EmendasEspeciaisSyncPipeline;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Testes do Controller de Sincronização de Emendas Especiais (Write Side / CQRS-Light)")
class EmendaEspecialSyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmendasEspeciaisSyncPipeline syncPipeline;

    @Test
    @DisplayName("POST /sincronizar - Síncrono deve retornar 200 OK com dados do log")
    void deveDispararSincronizacaoSincrona() throws Exception {
        SincronizacaoLogEntity log = new SincronizacaoLogEntity();
        log.setId(UUID.randomUUID());
        log.setStatus("SUCESSO");
        log.setDataInicio(OffsetDateTime.now());
        log.setDataFim(OffsetDateTime.now());

        when(syncPipeline.executeSync(anyBoolean())).thenReturn(log);

        mockMvc.perform(post("/api/v1/transferegov/emendas-especiais/sincronizar")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUCESSO")))
                .andExpect(jsonPath("$.logId", notNullValue()))
                .andExpect(jsonPath("$.message", containsString("Sincronização de Emendas Especiais finalizada com status: SUCESSO")));
    }

    @Test
    @DisplayName("POST /sincronizar?async=true - Assíncrono deve retornar 202 Accepted")
    void deveDispararSincronizacaoAssincrona() throws Exception {
        mockMvc.perform(post("/api/v1/transferegov/emendas-especiais/sincronizar")
                        .param("async", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status", is("INICIADO_ASSINCRONO")))
                .andExpect(jsonPath("$.message", containsString("iniciado em segundo plano")));
    }
}
