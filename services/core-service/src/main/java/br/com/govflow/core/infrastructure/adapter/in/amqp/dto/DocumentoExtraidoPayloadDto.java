package br.com.govflow.core.infrastructure.adapter.in.amqp.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DocumentoExtraidoPayloadDto(
        @JsonProperty("documentoId")
        @JsonAlias({"documento_id", "mensagemInboundId"})
        UUID documentoId,

        @JsonProperty("s3Bucket")
        @JsonAlias("s3_bucket")
        String s3Bucket,

        @JsonProperty("s3Key")
        @JsonAlias("s3_key")
        String s3Key,

        @JsonProperty("prefeituraId")
        @JsonAlias("prefeitura_id")
        UUID prefeituraId,

        @JsonProperty("convenioId")
        @JsonAlias("convenio_id")
        UUID convenioId,

        @JsonProperty("nomeArquivoOriginal")
        @JsonAlias({"nome_arquivo_original", "fileName"})
        String nomeArquivoOriginal,

        @JsonProperty("contentType")
        @JsonAlias({"content_type", "mediaMimetype"})
        String contentType,

        @JsonProperty("tamanhoBytes")
        @JsonAlias({"tamanho_bytes", "fileSizeBytes"})
        Long tamanhoBytes,

        @JsonProperty("extracao")
        Map<String, Object> extracao,

        @JsonProperty("validacaoMatematica")
        @JsonAlias("validacao_matematica")
        Map<String, Object> validacaoMatematica,

        @JsonProperty("confidenceScoreGeral")
        @JsonAlias("confidence_score_geral")
        Double confidenceScoreGeral,

        @JsonProperty("boundingBoxes")
        @JsonAlias("bounding_boxes")
        Map<String, Object> boundingBoxes
) {
}
