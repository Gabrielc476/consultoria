package br.com.govflow.whatsapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "govflow.whatsapp")
public class WhatsAppProperties {

    private String activeProvider = "EVOLUTION";
    private Evolution evolution = new Evolution();
    private S3 s3 = new S3();
    private RabbitMq rabbitmq = new RabbitMq();

    public static class Evolution {
        private String baseUrl = "http://localhost:8084";
        private String apiKey = "GovFlowSuperSecretApiKey2026";
        private String instanceName = "govflow-consultoria";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getInstanceName() {
            return instanceName;
        }

        public void setInstanceName(String instanceName) {
            this.instanceName = instanceName;
        }
    }

    public static class S3 {
        private String endpoint = "http://localhost:9000";
        private String region = "us-east-1";
        private String accessKey = "minioadmin";
        private String secretKey = "minioadmin123";
        private String bucketDocuments = "govflow-documents";
        private boolean pathStyleAccess = true;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getBucketDocuments() {
            return bucketDocuments;
        }

        public void setBucketDocuments(String bucketDocuments) {
            this.bucketDocuments = bucketDocuments;
        }

        public boolean isPathStyleAccess() {
            return pathStyleAccess;
        }

        public void setPathStyleAccess(boolean pathStyleAccess) {
            this.pathStyleAccess = pathStyleAccess;
        }
    }

    public static class RabbitMq {
        private String exchange = "govflow.events";
        private String dlx = "govflow.dlx";
        private String queueDocumentos = "fila.documentos.extrair";
        private String routingKeyDocumentos = "whatsapp.documento.recebido";
        private String queueAudios = "fila.audios.transcrever";
        private String routingKeyAudios = "whatsapp.audio.recebido";

        public String getExchange() {
            return exchange;
        }

        public void setExchange(String exchange) {
            this.exchange = exchange;
        }

        public String getDlx() {
            return dlx;
        }

        public void setDlx(String dlx) {
            this.dlx = dlx;
        }

        public String getQueueDocumentos() {
            return queueDocumentos;
        }

        public void setQueueDocumentos(String queueDocumentos) {
            this.queueDocumentos = queueDocumentos;
        }

        public String getRoutingKeyDocumentos() {
            return routingKeyDocumentos;
        }

        public void setRoutingKeyDocumentos(String routingKeyDocumentos) {
            this.routingKeyDocumentos = routingKeyDocumentos;
        }

        public String getQueueAudios() {
            return queueAudios;
        }

        public void setQueueAudios(String queueAudios) {
            this.queueAudios = queueAudios;
        }

        public String getRoutingKeyAudios() {
            return routingKeyAudios;
        }

        public void setRoutingKeyAudios(String routingKeyAudios) {
            this.routingKeyAudios = routingKeyAudios;
        }
    }

    public String getActiveProvider() {
        return activeProvider;
    }

    public void setActiveProvider(String activeProvider) {
        this.activeProvider = activeProvider;
    }

    public Evolution getEvolution() {
        return evolution;
    }

    public void setEvolution(Evolution evolution) {
        this.evolution = evolution;
    }

    public S3 getS3() {
        return s3;
    }

    public void setS3(S3 s3) {
        this.s3 = s3;
    }

    public RabbitMq getRabbitmq() {
        return rabbitmq;
    }

    public void setRabbitmq(RabbitMq rabbitmq) {
        this.rabbitmq = rabbitmq;
    }
}
