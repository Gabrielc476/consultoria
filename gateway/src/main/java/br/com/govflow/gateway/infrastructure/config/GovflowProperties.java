package br.com.govflow.gateway.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "govflow")
public class GovflowProperties {

    private final Jwt jwt = new Jwt();
    private final Routes routes = new Routes();

    public Jwt getJwt() {
        return jwt;
    }

    public Routes getRoutes() {
        return routes;
    }

    public static class Jwt {
        private String secret = "GovFlowLocalDevJwtSecretKeyChangeMe32b!";

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }
    }

    public static class Routes {
        private String coreUri = "http://localhost:8081";
        private String transferegovUri = "http://localhost:8082";
        private String whatsappUri = "http://localhost:8083";
        private String aiUri = "http://localhost:8000";

        public String getCoreUri() {
            return coreUri;
        }

        public void setCoreUri(String coreUri) {
            this.coreUri = coreUri;
        }

        public String getTransferegovUri() {
            return transferegovUri;
        }

        public void setTransferegovUri(String transferegovUri) {
            this.transferegovUri = transferegovUri;
        }

        public String getWhatsappUri() {
            return whatsappUri;
        }

        public void setWhatsappUri(String whatsappUri) {
            this.whatsappUri = whatsappUri;
        }

        public String getAiUri() {
            return aiUri;
        }

        public void setAiUri(String aiUri) {
            this.aiUri = aiUri;
        }
    }
}
