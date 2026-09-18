package br.com.govflow.core.infrastructure.adapter.out.persistence.mapper;

import br.com.govflow.core.domain.model.AcaoAuditoria;
import br.com.govflow.core.domain.model.AuditoriaRevisao;
import br.com.govflow.core.domain.model.DiffRevisao;
import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.AuditoriaRevisaoJpaEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Component
public class AuditoriaRevisaoPersistenceMapper {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaRevisaoPersistenceMapper.class);

    private final ObjectMapper objectMapper;

    public AuditoriaRevisaoPersistenceMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AuditoriaRevisao toDomain(AuditoriaRevisaoJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        AcaoAuditoria acao = AcaoAuditoria.valueOf(entity.getAcao());

        DiffRevisao diff = new DiffRevisao(Collections.emptyMap());
        if (entity.getDiffAlteracoesJson() != null && !entity.getDiffAlteracoesJson().isBlank()) {
            try {
                diff = objectMapper.readValue(entity.getDiffAlteracoesJson(), DiffRevisao.class);
            } catch (JsonProcessingException e) {
                log.error("Erro ao desserializar diffAlteracoesJson da auditoria {}", entity.getId(), e);
            }
        }

        Map<String, Object> originais = Collections.emptyMap();
        if (entity.getValoresOriginaisJson() != null && !entity.getValoresOriginaisJson().isBlank()) {
            try {
                originais = objectMapper.readValue(entity.getValoresOriginaisJson(), new TypeReference<Map<String, Object>>() {});
            } catch (JsonProcessingException e) {
                log.error("Erro ao desserializar valoresOriginaisJson da auditoria {}", entity.getId(), e);
            }
        }

        Map<String, Object> revisados = Collections.emptyMap();
        if (entity.getValoresRevisadosJson() != null && !entity.getValoresRevisadosJson().isBlank()) {
            try {
                revisados = objectMapper.readValue(entity.getValoresRevisadosJson(), new TypeReference<Map<String, Object>>() {});
            } catch (JsonProcessingException e) {
                log.error("Erro ao desserializar valoresRevisadosJson da auditoria {}", entity.getId(), e);
            }
        }

        return new AuditoriaRevisao(
                entity.getId(),
                entity.getTenantId(),
                entity.getDocumentoId(),
                entity.getAnalistaId(),
                acao,
                entity.getDataRevisao(),
                entity.getJustificativa(),
                diff,
                originais,
                revisados
        );
    }

    public AuditoriaRevisaoJpaEntity toEntity(AuditoriaRevisao domain) {
        if (domain == null) {
            return null;
        }

        AuditoriaRevisaoJpaEntity entity = new AuditoriaRevisaoJpaEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setDocumentoId(domain.getDocumentoId());
        entity.setAnalistaId(domain.getAnalistaId());
        entity.setAcao(domain.getAcao().name());
        entity.setDataRevisao(domain.getDataRevisao());
        entity.setJustificativa(domain.getJustificativa());
        entity.setCreatedAt(domain.getDataRevisao());

        if (domain.getDiff() != null) {
            try {
                entity.setDiffAlteracoesJson(objectMapper.writeValueAsString(domain.getDiff()));
            } catch (JsonProcessingException e) {
                log.error("Erro ao serializar diffAlteracoesJson da auditoria {}", domain.getId(), e);
            }
        }

        if (domain.getValoresOriginais() != null && !domain.getValoresOriginais().isEmpty()) {
            try {
                entity.setValoresOriginaisJson(objectMapper.writeValueAsString(domain.getValoresOriginais()));
            } catch (JsonProcessingException e) {
                log.error("Erro ao serializar valoresOriginaisJson da auditoria {}", domain.getId(), e);
            }
        }

        if (domain.getValoresRevisados() != null && !domain.getValoresRevisados().isEmpty()) {
            try {
                entity.setValoresRevisadosJson(objectMapper.writeValueAsString(domain.getValoresRevisados()));
            } catch (JsonProcessingException e) {
                log.error("Erro ao serializar valoresRevisadosJson da auditoria {}", domain.getId(), e);
            }
        }

        return entity;
    }
}
