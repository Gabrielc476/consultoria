-- ==============================================================================
-- V1__init_whatsapp_schema.sql
-- Descrição: Estrutura inicial do schema de integração do WhatsApp (whatsapp_schema)
-- Serviço: whatsapp-service / GovFlow
-- ==============================================================================

CREATE SCHEMA IF NOT EXISTS whatsapp_schema;

-- 1. Tabela de Instâncias do WhatsApp conectadas via Evolution API
CREATE TABLE IF NOT EXISTS whatsapp_schema.tb_instancias_whatsapp (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instance_name VARCHAR(100) NOT NULL UNIQUE,
    provider VARCHAR(30) NOT NULL DEFAULT 'EVOLUTION', -- 'EVOLUTION' ou 'META_CLOUD'
    phone_number VARCHAR(30),
    status VARCHAR(30) NOT NULL DEFAULT 'DISCONNECTED', -- 'CONNECTED', 'DISCONNECTED', 'CONNECTING'
    api_key VARCHAR(255),
    webhook_url VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Tabela de Mapeamento de Contatos Municipais (Roteamento Prefeitura / Secretaria)
CREATE TABLE IF NOT EXISTS whatsapp_schema.tb_contatos_prefeitura (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    prefeitura_id UUID,
    phone_number VARCHAR(30) NOT NULL UNIQUE, -- E.164 (ex: 5583999999999)
    nome_contato VARCHAR(150) NOT NULL,
    cargo VARCHAR(100), -- 'SECRETARIO_FINANCAS', 'SECRETARIO_OBRAS', 'FISCAL_ENGENHEIRO'
    departamento VARCHAR(100),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_contatos_phone ON whatsapp_schema.tb_contatos_prefeitura (phone_number);
CREATE INDEX IF NOT EXISTS idx_contatos_tenant ON whatsapp_schema.tb_contatos_prefeitura (tenant_id);

-- 3. Tabela de Mensagens Inbound (Recebidas via Webhook da Evolution API)
CREATE TABLE IF NOT EXISTS whatsapp_schema.tb_mensagens_inbound (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID,
    prefeitura_id UUID,
    instance_name VARCHAR(100) NOT NULL,
    external_message_id VARCHAR(150) NOT NULL, -- ID original da mensagem no WhatsApp
    sender_phone VARCHAR(30) NOT NULL,
    sender_name VARCHAR(150),
    message_type VARCHAR(30) NOT NULL, -- 'TEXT', 'AUDIO', 'DOCUMENT', 'IMAGE', 'POLL_RESPONSE'
    content_text TEXT,
    s3_bucket VARCHAR(100),
    s3_key VARCHAR(500),
    media_url TEXT,
    media_mimetype VARCHAR(100),
    file_size_bytes BIGINT,
    raw_payload JSONB,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    processing_error TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_inbound_msg_id ON whatsapp_schema.tb_mensagens_inbound (external_message_id);
CREATE INDEX IF NOT EXISTS idx_inbound_sender ON whatsapp_schema.tb_mensagens_inbound (sender_phone);
CREATE INDEX IF NOT EXISTS idx_inbound_processed ON whatsapp_schema.tb_mensagens_inbound (processed);

-- 4. Tabela de Mensagens Outbound (Enviadas pelo Analista / Sistema)
CREATE TABLE IF NOT EXISTS whatsapp_schema.tb_mensagens_outbound (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID,
    instance_name VARCHAR(100) NOT NULL,
    recipient_phone VARCHAR(30) NOT NULL,
    message_type VARCHAR(30) NOT NULL, -- 'TEXT', 'DOCUMENT', 'IMAGE'
    content_text TEXT,
    s3_key VARCHAR(500),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'SENT', 'DELIVERED', 'READ', 'FAILED'
    provider_message_id VARCHAR(150),
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_outbound_status ON whatsapp_schema.tb_mensagens_outbound (status);
CREATE INDEX IF NOT EXISTS idx_outbound_recipient ON whatsapp_schema.tb_mensagens_outbound (recipient_phone);
