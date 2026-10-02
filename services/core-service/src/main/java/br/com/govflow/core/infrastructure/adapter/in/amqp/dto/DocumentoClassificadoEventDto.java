package br.com.govflow.core.infrastructure.adapter.in.amqp.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DocumentoClassificadoEventDto(
        @JsonProperty("eventId") @JsonAlias("event_id") UUID eventId,
        @JsonProperty("correlationId") @JsonAlias("correlation_id") UUID correlationId,
        @JsonProperty("tenantId") @JsonAlias("tenant_id") UUID tenantId,
        @JsonProperty("prefeituraId") @JsonAlias("prefeitura_id") UUID prefeituraId,
        @JsonProperty("convenioId") @JsonAlias("convenio_id") UUID convenioId,
        @JsonProperty("documentoId") @JsonAlias("documento_id") UUID documentoId,
        @JsonProperty("s3Bucket") @JsonAlias("s3_bucket") String s3Bucket,
        @JsonProperty("s3Key") @JsonAlias("s3_key") String s3Key,
        @JsonProperty("nomeArquivoOriginal") @JsonAlias({"nome_arquivo_original", "fileName"}) String nomeArquivoOriginal,
        @JsonProperty("contentType") @JsonAlias({"content_type", "mediaMimetype"}) String contentType,
        @JsonProperty("tamanhoBytes") @JsonAlias({"tamanho_bytes", "fileSizeBytes"}) Long tamanhoBytes,
        @JsonProperty("hashSha256") @JsonAlias("hash_sha256") String hashSha256,
        @JsonProperty("faseSugerida") @JsonAlias({"fase_sugerida", "faseCicloVida"}) String faseSugerida,
        @JsonProperty("categoriaSugerida") @JsonAlias({"categoria_sugerida", "categoriaDocumento"}) String categoriaSugerida,
        @JsonProperty("pastaVirtualSugerida") @JsonAlias("pasta_virtual_sugerida") String pastaVirtualSugerida,
        @JsonProperty("confidenceScore") @JsonAlias("confidence_score") BigDecimal confidenceScore,
        @JsonProperty("remetente") @JsonAlias({"remetentePhone", "remetente_phone"}) String remetente,
        @JsonProperty("tags") List<String> tags,
        @JsonProperty("metadados") @JsonAlias("extracao") Map<String, Object> metadados,
        @JsonProperty("timestamp") @JsonAlias("occurredAt") Instant timestamp,
        @JsonProperty("mensagemInboundId") @JsonAlias("mensagem_inbound_id") UUID mensagemInboundId,
        @JsonProperty("senderName") @JsonAlias("remetente_name") String senderName,
        @JsonProperty("remetenteNovo") @JsonAlias("remetente_novo") Boolean remetenteNovo,
        @JsonProperty("motivoAmbiguidade") @JsonAlias("motivo_ambiguidade") String motivoAmbiguidade,
        @JsonProperty("conteudoResumo") @JsonAlias("conteudo_resumo") String conteudoResumo,
        @JsonProperty("direcionarTriagem") @JsonAlias("direcionar_triagem") Boolean direcionarTriagem,
        @JsonProperty("payload") Map<String, Object> payload
) {

    public DocumentoClassificadoEventDto(
            UUID eventId,
            UUID correlationId,
            UUID tenantId,
            UUID prefeituraId,
            UUID convenioId,
            UUID documentoId,
            String s3Bucket,
            String s3Key,
            String nomeArquivoOriginal,
            String contentType,
            Long tamanhoBytes,
            String hashSha256,
            String faseSugerida,
            String categoriaSugerida,
            String pastaVirtualSugerida,
            BigDecimal confidenceScore,
            String remetente,
            List<String> tags,
            Map<String, Object> metadados,
            Instant timestamp
    ) {
        this(eventId, correlationId, tenantId, prefeituraId, convenioId, documentoId,
                s3Bucket, s3Key, nomeArquivoOriginal, contentType, tamanhoBytes,
                hashSha256, faseSugerida, categoriaSugerida, pastaVirtualSugerida,
                confidenceScore, remetente, tags, metadados, timestamp,
                null, null, null, null, null, null, null);
    }

    @JsonIgnore
    public UUID getEfetivoTenantId() {
        if (tenantId != null) return tenantId;
        if (payload != null && payload.get("tenantId") != null) {
            return UUID.fromString(payload.get("tenantId").toString());
        }
        return null;
    }

    @JsonIgnore
    public UUID getEfetivoDocumentoId() {
        if (documentoId != null) return documentoId;
        if (payload != null && payload.get("documentoId") != null) {
            return UUID.fromString(payload.get("documentoId").toString());
        }
        return correlationId != null ? correlationId : UUID.randomUUID();
    }

    @JsonIgnore
    public UUID getEfetivoMensagemInboundId() {
        if (mensagemInboundId != null) return mensagemInboundId;
        if (payload != null && payload.get("mensagemInboundId") != null) {
            return UUID.fromString(payload.get("mensagemInboundId").toString());
        }
        return correlationId;
    }

    @JsonIgnore
    public UUID getEfetivoConvenioId() {
        if (convenioId != null) return convenioId;
        if (payload != null && payload.get("convenioId") != null) {
            return UUID.fromString(payload.get("convenioId").toString());
        }
        return null;
    }

    @JsonIgnore
    public UUID getEfetivoPrefeituraId() {
        if (prefeituraId != null) return prefeituraId;
        if (payload != null && payload.get("prefeituraId") != null) {
            return UUID.fromString(payload.get("prefeituraId").toString());
        }
        return null;
    }

    @JsonIgnore
    public String getEfetivoS3Bucket() {
        if (s3Bucket != null && !s3Bucket.isBlank()) return s3Bucket;
        if (payload != null && payload.get("s3Bucket") != null) return payload.get("s3Bucket").toString();
        return "govflow-docs";
    }

    @JsonIgnore
    public String getEfetivoS3Key() {
        if (s3Key != null && !s3Key.isBlank()) return s3Key;
        if (payload != null && payload.get("s3Key") != null) return payload.get("s3Key").toString();
        return "";
    }

    @JsonIgnore
    public String getEfetivoNomeArquivoOriginal() {
        if (nomeArquivoOriginal != null) return nomeArquivoOriginal;
        if (payload != null && payload.get("nomeArquivoOriginal") != null) return payload.get("nomeArquivoOriginal").toString();
        return "documento.pdf";
    }

    @JsonIgnore
    public String getEfetivoContentType() {
        if (contentType != null) return contentType;
        if (payload != null && payload.get("contentType") != null) return payload.get("contentType").toString();
        return "application/pdf";
    }

    @JsonIgnore
    public Long getEfetivoTamanhoBytes() {
        if (tamanhoBytes != null) return tamanhoBytes;
        if (payload != null && payload.get("tamanhoBytes") != null) {
            return Long.valueOf(payload.get("tamanhoBytes").toString());
        }
        return 0L;
    }

    @JsonIgnore
    public String getEfetivoHashSha256() {
        if (hashSha256 != null) return hashSha256;
        if (payload != null && payload.get("hashSha256") != null) return payload.get("hashSha256").toString();
        return "";
    }

    @JsonIgnore
    public String getEfetivoFaseSugerida() {
        if (faseSugerida != null) return faseSugerida;
        if (payload != null && payload.get("faseCicloVida") != null) return payload.get("faseCicloVida").toString();
        return "04_EXECUCAO_FISICA_E_MEDICOES";
    }

    @JsonIgnore
    public String getEfetivoCategoriaSugerida() {
        if (categoriaSugerida != null) return categoriaSugerida;
        if (payload != null && payload.get("categoriaDocumento") != null) return payload.get("categoriaDocumento").toString();
        return "DOCUMENTO_HABIL";
    }

    @JsonIgnore
    public BigDecimal getEfetivoConfidenceScore() {
        if (confidenceScore != null) return confidenceScore;
        if (payload != null && payload.get("confidenceScore") != null) {
            return new BigDecimal(payload.get("confidenceScore").toString());
        }
        return BigDecimal.ZERO;
    }

    @JsonIgnore
    public String getEfetivoRemetentePhone() {
        if (remetente != null) return remetente;
        if (payload != null && payload.get("remetentePhone") != null) return payload.get("remetentePhone").toString();
        return null;
    }

    @JsonIgnore
    public String getEfetivoSenderName() {
        if (senderName != null) return senderName;
        if (payload != null && payload.get("remetenteName") != null) return payload.get("remetenteName").toString();
        return null;
    }

    @JsonIgnore
    public boolean isEfetivoRemetenteNovo() {
        if (remetenteNovo != null) return remetenteNovo;
        if (payload != null && payload.get("remetenteNovo") != null) {
            return Boolean.parseBoolean(payload.get("remetenteNovo").toString());
        }
        return false;
    }

    @JsonIgnore
    public String getEfetivoMotivoAmbiguidade() {
        if (motivoAmbiguidade != null) return motivoAmbiguidade;
        if (payload != null && payload.get("motivoAmbiguidade") != null) return payload.get("motivoAmbiguidade").toString();
        return null;
    }

    @JsonIgnore
    public String getEfetivoConteudoResumo() {
        if (conteudoResumo != null) return conteudoResumo;
        if (payload != null && payload.get("conteudoResumo") != null) return payload.get("conteudoResumo").toString();
        return null;
    }

    @JsonIgnore
    public boolean isEfetivoDirecionarTriagem() {
        if (direcionarTriagem != null) return direcionarTriagem;
        if (payload != null && payload.get("direcionarTriagem") != null) {
            return Boolean.parseBoolean(payload.get("direcionarTriagem").toString());
        }
        return false;
    }
}
