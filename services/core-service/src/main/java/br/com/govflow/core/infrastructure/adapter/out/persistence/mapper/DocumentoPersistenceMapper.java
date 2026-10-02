package br.com.govflow.core.infrastructure.adapter.out.persistence.mapper;

import br.com.govflow.core.domain.model.*;
import br.com.govflow.core.domain.model.documento.*;
import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.DocumentoAuditoriaJpaEntity;
import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.DocumentoHabilDadosJpaEntity;
import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.DocumentoJpaEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component
public class DocumentoPersistenceMapper {

    private static final Logger log = LoggerFactory.getLogger(DocumentoPersistenceMapper.class);

    private final ObjectMapper objectMapper;

    public DocumentoPersistenceMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Documento toDomain(DocumentoJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        StatusDocumento status;
        try {
            status = StatusDocumento.valueOf(entity.getStatus());
        } catch (Exception e) {
            status = StatusDocumento.RECEBIDO;
        }

        ExtracaoSugerida extracao = null;
        if (entity.getDadosExtracaoJson() != null && !entity.getDadosExtracaoJson().isBlank()) {
            try {
                extracao = objectMapper.readValue(entity.getDadosExtracaoJson(), ExtracaoSugerida.class);
            } catch (JsonProcessingException e) {
                log.error("Erro ao desserializar dadosExtracaoJson do documento {}", entity.getId(), e);
            }
        }

        Map<String, BoundingBox> boxes = new LinkedHashMap<>();
        if (entity.getBoundingBoxesJson() != null && !entity.getBoundingBoxesJson().isBlank()) {
            try {
                boxes = objectMapper.readValue(entity.getBoundingBoxesJson(), new TypeReference<Map<String, BoundingBox>>() {});
            } catch (JsonProcessingException e) {
                log.error("Erro ao desserializar boundingBoxesJson do documento {}", entity.getId(), e);
            }
        }

        DadosRevisaoAnalista dadosRevisao = null;
        if (entity.getDadosRevisaoJson() != null && !entity.getDadosRevisaoJson().isBlank()) {
            try {
                dadosRevisao = objectMapper.readValue(entity.getDadosRevisaoJson(), DadosRevisaoAnalista.class);
            } catch (JsonProcessingException e) {
                log.error("Erro ao desserializar dadosRevisaoJson do documento {}", entity.getId(), e);
            }
        }

        // Fallback de reconstrução para registros legados onde dadosRevisaoJson ainda não existia
        if (dadosRevisao == null && (status == StatusDocumento.PRONTO_PARA_TRANSFEREGOV || status == StatusDocumento.APROVADO) && entity.getNumeroDocumento() != null) {
            TipoDocumentoHabil tipo = entity.getTipoDocumentoHabil() != null ?
                    TipoDocumentoHabil.valueOf(entity.getTipoDocumentoHabil()) : null;

            dadosRevisao = new DadosRevisaoAnalista(
                    tipo,
                    entity.getNumeroDocumento(),
                    entity.getSerieDocumento(),
                    entity.getChaveAcessoNfe(),
                    entity.getDataEmissao(),
                    entity.getCnpjCredor(),
                    entity.getRazaoSocialCredor(),
                    entity.getDescricaoServico(),
                    null,
                    entity.getValorBruto(),
                    entity.getValorTotalDeducoes(),
                    entity.getValorLiquido(),
                    extracao != null ? extracao.retencoes() : Collections.emptyList(),
                    null
            );
        }

        ArmazenamentoArquivo armazenamento = new ArmazenamentoArquivo(
                entity.getS3Bucket(),
                entity.getS3Key(),
                entity.getNomeArquivoOriginal(),
                entity.getContentType(),
                entity.getTamanhoBytes()
        );

        FaseCicloVida fase = FaseCicloVida.fromCodigoOuNome(entity.getFaseCicloVida());
        CategoriaDocumento categoria = CategoriaDocumento.fromString(entity.getCategoriaDocumento());
        OrigemCanal origem = OrigemCanal.fromString(entity.getOrigemCanal());

        List<String> tags = entity.getTags() != null ? Arrays.asList(entity.getTags()) : Collections.emptyList();

        Map<String, Object> metadados = new HashMap<>();
        if (entity.getMetadadosJson() != null && !entity.getMetadadosJson().isBlank()) {
            try {
                metadados = objectMapper.readValue(entity.getMetadadosJson(), new TypeReference<Map<String, Object>>() {});
            } catch (JsonProcessingException e) {
                log.warn("Erro ao desserializar metadados_json do documento {}: {}", entity.getId(), e.getMessage());
            }
        }

        return new Documento(
                entity.getId(),
                entity.getTenantId(),
                entity.getPrefeituraId(),
                entity.getConvenioId(),
                entity.getContratoId(),
                entity.getMedicaoId(),
                fase,
                categoria,
                entity.getPastaVirtual() != null ? entity.getPastaVirtual() : "/",
                origem,
                entity.getHashSha256(),
                tags,
                metadados,
                entity.getCriadoPorUsuarioId(),
                armazenamento,
                status,
                extracao,
                BoundingBoxesData.of(boxes),
                dadosRevisao,
                null,
                entity.getMotivoRejeicao(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public DocumentoJpaEntity toEntity(Documento domain) {
        if (domain == null) {
            return null;
        }

        DocumentoJpaEntity entity = new DocumentoJpaEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setPrefeituraId(domain.getPrefeituraId());
        entity.setConvenioId(domain.getConvenioId());
        entity.setContratoId(domain.getContratoId());
        entity.setMedicaoId(domain.getMedicaoId());
        entity.setS3Bucket(domain.getS3Bucket());
        entity.setS3Key(domain.getS3Key());
        entity.setNomeArquivoOriginal(domain.getNomeArquivoOriginal());
        entity.setContentType(domain.getContentType());
        entity.setTamanhoBytes(domain.getTamanhoBytes());
        entity.setStatus(domain.getStatus().name());
        entity.setMotivoRejeicao(domain.getMotivoRejeicao());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());

        entity.setFaseCicloVida(domain.getFaseCicloVida() != null ? domain.getFaseCicloVida().name() : FaseCicloVida.FASE_05_EXECUCAO_FINANCEIRA.name());
        entity.setCategoriaDocumento(domain.getCategoriaDocumento() != null ? domain.getCategoriaDocumento().name() : CategoriaDocumento.DOCUMENTO_HABIL.name());
        entity.setPastaVirtual(domain.getPastaVirtual() != null ? domain.getPastaVirtual() : "/");
        entity.setOrigemCanal(domain.getOrigemCanal() != null ? domain.getOrigemCanal().name() : OrigemCanal.UPLOAD_MANUAL.name());
        entity.setHashSha256(domain.getHashSha256());
        entity.setCriadoPorUsuarioId(domain.getCriadoPorUsuarioId());

        if (domain.getTags() != null && !domain.getTags().isEmpty()) {
            entity.setTags(domain.getTags().toArray(new String[0]));
        }

        if (domain.getMetadadosJson() != null && !domain.getMetadadosJson().isEmpty()) {
            try {
                entity.setMetadadosJson(objectMapper.writeValueAsString(domain.getMetadadosJson()));
            } catch (JsonProcessingException e) {
                log.error("Erro ao serializar metadados_json do documento {}", domain.getId(), e);
            }
        }

        // Se houver dados revisados pelo analista, eles têm prioridade nos campos fiscais relacionais
        if (domain.getDadosRevisao() != null) {
            DadosRevisaoAnalista rev = domain.getDadosRevisao();
            entity.setTipoDocumentoHabil(rev.tipoDocumento() != null ? rev.tipoDocumento().name() : null);
            entity.setNumeroDocumento(rev.numeroDocumento());
            entity.setSerieDocumento(rev.serieDocumento());
            entity.setChaveAcessoNfe(rev.chaveAcessoNfe());
            entity.setDataEmissao(rev.dataEmissao());
            entity.setCnpjCredor(rev.cnpjCredor());
            entity.setRazaoSocialCredor(rev.razaoSocialCredor());
            entity.setDescricaoServico(rev.descricaoServico());
            entity.setValorBruto(rev.valorBruto());
            entity.setValorTotalDeducoes(rev.valorTotalDeducoes());
            entity.setValorLiquido(rev.valorLiquido());
            entity.setStatusValidacaoMatematica(true);

            try {
                entity.setDadosRevisaoJson(objectMapper.writeValueAsString(rev));
            } catch (JsonProcessingException e) {
                log.error("Erro ao serializar dadosRevisaoJson do documento {}", domain.getId(), e);
            }
        } else if (domain.getExtracaoSugerida() != null) {
            ExtracaoSugerida ext = domain.getExtracaoSugerida();
            entity.setTipoDocumentoHabil(ext.tipoDocumento() != null ? ext.tipoDocumento().name() : null);
            entity.setNumeroDocumento(ext.numeroDocumento());
            entity.setSerieDocumento(ext.serieDocumento());
            entity.setChaveAcessoNfe(ext.chaveAcessoNfe());
            entity.setDataEmissao(ext.dataEmissao());
            entity.setCnpjCredor(ext.cnpjCredor());
            entity.setRazaoSocialCredor(ext.razaoSocialCredor());
            entity.setDescricaoServico(ext.descricaoServico());
            entity.setValorBruto(ext.valorBruto());
            entity.setValorTotalDeducoes(ext.valorTotalDeducoes());
            entity.setValorLiquido(ext.valorLiquido());
            entity.setStatusValidacaoMatematica(ext.consistenteMatematicamente());
            entity.setConfidenceScoreGeral(BigDecimal.valueOf(ext.confidenceScoreGeral()));
        }

        if (domain.getExtracaoSugerida() != null) {
            try {
                entity.setDadosExtracaoJson(objectMapper.writeValueAsString(domain.getExtracaoSugerida()));
            } catch (JsonProcessingException e) {
                log.error("Erro ao serializar dadosExtracaoJson do documento {}", domain.getId(), e);
            }
        }

        if (domain.getBoundingBoxes() != null && !domain.getBoundingBoxes().isEmpty()) {
            try {
                entity.setBoundingBoxesJson(objectMapper.writeValueAsString(domain.getBoundingBoxes()));
            } catch (JsonProcessingException e) {
                log.error("Erro ao serializar boundingBoxesJson do documento {}", domain.getId(), e);
            }
        }

        if (entity.getConfidenceScoreGeral() == null) {
            entity.setConfidenceScoreGeral(BigDecimal.ZERO);
        }

        return entity;
    }

    public DocumentoAuditoria toDomain(DocumentoAuditoriaJpaEntity entity) {
        if (entity == null) return null;
        return new DocumentoAuditoria(
                entity.getId(),
                entity.getTenantId(),
                entity.getDocumentoId(),
                entity.getUsuarioId(),
                entity.getAcao(),
                entity.getJustificativa(),
                entity.getSnapshotAnteriorJson(),
                entity.getSnapshotAtualJson(),
                entity.getRealizadoEm()
        );
    }

    public DocumentoAuditoriaJpaEntity toEntity(DocumentoAuditoria domain) {
        if (domain == null) return null;
        DocumentoAuditoriaJpaEntity entity = new DocumentoAuditoriaJpaEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setDocumentoId(domain.getDocumentoId());
        entity.setUsuarioId(domain.getUsuarioId());
        entity.setAcao(domain.getAcao());
        entity.setJustificativa(domain.getJustificativa());
        entity.setSnapshotAnteriorJson(domain.getSnapshotAnteriorJson());
        entity.setSnapshotAtualJson(domain.getSnapshotAtualJson());
        entity.setRealizadoEm(domain.getRealizadoEm());
        return entity;
    }

    public DocumentoHabilDados toDomain(DocumentoHabilDadosJpaEntity entity) {
        if (entity == null) return null;
        TipoDocumentoHabil tipo = entity.getTipoDocumentoHabil() != null ?
                TipoDocumentoHabil.valueOf(entity.getTipoDocumentoHabil()) : TipoDocumentoHabil.NOTA_FISCAL_SERVICOS;

        return new DocumentoHabilDados(
                entity.getDocumentoId(),
                tipo,
                entity.getNumeroDocumento(),
                entity.getSerieDocumento(),
                entity.getChaveAcessoNfe(),
                entity.getDataEmissao(),
                entity.getCnpjCredor(),
                entity.getRazaoSocialCredor(),
                entity.getDescricaoServico(),
                entity.getValorBruto(),
                entity.getValorTotalDeducoes(),
                entity.getValorLiquido(),
                entity.isStatusValidacaoMatematica(),
                entity.getConfidenceScoreIa(),
                entity.getDadosExtracaoIaJson(),
                entity.getBoundingBoxesJson(),
                entity.getDadosRevisaoJson()
        );
    }

    public DocumentoHabilDadosJpaEntity toEntity(DocumentoHabilDados domain) {
        if (domain == null) return null;
        DocumentoHabilDadosJpaEntity entity = new DocumentoHabilDadosJpaEntity();
        entity.setDocumentoId(domain.getDocumentoId());
        entity.setTipoDocumentoHabil(domain.getTipoDocumentoHabil() != null ? domain.getTipoDocumentoHabil().name() : TipoDocumentoHabil.NOTA_FISCAL_SERVICOS.name());
        entity.setNumeroDocumento(domain.getNumeroDocumento());
        entity.setSerieDocumento(domain.getSerieDocumento());
        entity.setChaveAcessoNfe(domain.getChaveAcessoNfe());
        entity.setDataEmissao(domain.getDataEmissao());
        entity.setCnpjCredor(domain.getCnpjCredor());
        entity.setRazaoSocialCredor(domain.getRazaoSocialCredor());
        entity.setDescricaoServico(domain.getDescricaoServico());
        entity.setValorBruto(domain.getValorBruto());
        entity.setValorTotalDeducoes(domain.getValorTotalDeducoes());
        entity.setValorLiquido(domain.getValorLiquido());
        entity.setStatusValidacaoMatematica(domain.isStatusValidacaoMatematica());
        entity.setConfidenceScoreIa(domain.getConfidenceScoreIa());
        entity.setDadosExtracaoIaJson(domain.getDadosExtracaoIaJson());
        entity.setBoundingBoxesJson(domain.getBoundingBoxesJson());
        entity.setDadosRevisaoJson(domain.getDadosRevisaoJson());
        return entity;
    }
}
