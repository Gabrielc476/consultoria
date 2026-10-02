-- ==============================================================================
-- V21__decouple_whatsapp_contatos_and_create_triagem_inbox.sql
-- Descrição: Desacoplamento da entidade Contato de prefeitura única (relação 1:N com convênios),
--            criação da tabela associativa tb_contato_convenios, evolução da tabela
--            tb_mensagens_inbound e criação da Caixa de Triagem do Agente (tb_triagem_inbox).
-- Referência: docs/architecture/22_modulo_comunicacao_whatsapp_e_ia_contexto.md
-- Tickets: TICKET-WPP-01
-- ==============================================================================

-- 1. Tabela de Contatos Externos Desacoplados (whatsapp_schema.tb_contatos)
CREATE TABLE IF NOT EXISTS whatsapp_schema.tb_contatos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    phone_number VARCHAR(30) NOT NULL UNIQUE, -- E.164 sanitizado (ex: 5583999998888)
    nome VARCHAR(150) NOT NULL,
    papel VARCHAR(100), -- 'FISCAL_ENGENHEIRO', 'SECRETARIO_MUNICIPAL', 'REPRESENTANTE_EMPREITEIRA', 'AGENTE_CONSULTORIA'
    empresa_ou_orgao VARCHAR(150),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_contatos_phone ON whatsapp_schema.tb_contatos(phone_number);
CREATE INDEX IF NOT EXISTS idx_contatos_tenant ON whatsapp_schema.tb_contatos(tenant_id);

-- Migração de dados de compatibilidade da tabela legada tb_contatos_prefeitura
DO $$
BEGIN
    IF EXISTS (
        SELECT FROM information_schema.tables 
        WHERE table_schema = 'whatsapp_schema' 
          AND table_name = 'tb_contatos_prefeitura'
    ) THEN
        INSERT INTO whatsapp_schema.tb_contatos (id, tenant_id, phone_number, nome, papel, empresa_ou_orgao, ativo, created_at, updated_at)
        SELECT id, tenant_id, phone_number, nome_contato, cargo, departamento, ativo, created_at, updated_at
        FROM whatsapp_schema.tb_contatos_prefeitura
        ON CONFLICT (phone_number) DO NOTHING;
    END IF;
END $$;

-- 2. Tabela de Associação N:N entre Contato e Convênios (whatsapp_schema.tb_contato_convenios)
CREATE TABLE IF NOT EXISTS whatsapp_schema.tb_contato_convenios (
    contato_id UUID NOT NULL REFERENCES whatsapp_schema.tb_contatos(id) ON DELETE CASCADE,
    convenio_id UUID NOT NULL,
    prefeitura_id UUID NOT NULL,
    papel_especifico VARCHAR(100), -- ex: 'RESPONSAVEL_TECNICO_OBRA', 'FISCAL_TITULAR'
    principal BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (contato_id, convenio_id)
);

CREATE INDEX IF NOT EXISTS idx_contato_conv_convenio ON whatsapp_schema.tb_contato_convenios(convenio_id);
CREATE INDEX IF NOT EXISTS idx_contato_conv_prefeitura ON whatsapp_schema.tb_contato_convenios(prefeitura_id);

-- 3. Evolução de whatsapp_schema.tb_mensagens_inbound com rastreabilidade de contato e áudio
ALTER TABLE whatsapp_schema.tb_mensagens_inbound
    ADD COLUMN IF NOT EXISTS contato_id UUID REFERENCES whatsapp_schema.tb_contatos(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS audio_transcription TEXT,
    ADD COLUMN IF NOT EXISTS remetente_novo BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_inbound_contato ON whatsapp_schema.tb_mensagens_inbound(contato_id);

-- 4. Fila / Caixa de Triagem do Agente (core_schema.tb_triagem_inbox)
CREATE TABLE IF NOT EXISTS core_schema.tb_triagem_inbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES core_schema.tb_consultorias(id) ON DELETE CASCADE,
    agente_responsavel_id UUID REFERENCES core_schema.tb_usuarios(id) ON DELETE SET NULL,
    mensagem_inbound_id UUID,
    documento_id UUID REFERENCES core_schema.tb_documentos(id) ON DELETE CASCADE,
    
    -- Sugestão da IA
    convenio_sugerido_id UUID REFERENCES core_schema.tb_convenios(id) ON DELETE SET NULL,
    fase_sugerida VARCHAR(40),
    confidence_score NUMERIC(4, 3) NOT NULL DEFAULT 0.000,
    motivo_ambiguidade TEXT,
    
    -- Metadados rápidos para exibição e triagem
    phone_number VARCHAR(30),
    sender_name VARCHAR(150),
    push_name VARCHAR(150),
    remetente_novo BOOLEAN NOT NULL DEFAULT FALSE,
    conteudo_resumo TEXT,
    
    -- Status da Triagem
    status VARCHAR(30) NOT NULL DEFAULT 'PENDENTE', -- 'PENDENTE', 'RESOLVIDO', 'IGNORADO'
    resolvido_em TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_triagem_agente ON core_schema.tb_triagem_inbox(agente_responsavel_id);
CREATE INDEX IF NOT EXISTS idx_triagem_status ON core_schema.tb_triagem_inbox(status);
CREATE INDEX IF NOT EXISTS idx_triagem_tenant ON core_schema.tb_triagem_inbox(tenant_id);
CREATE INDEX IF NOT EXISTS idx_triagem_phone ON core_schema.tb_triagem_inbox(phone_number);

-- 5. View de interoperabilidade para consultas em whatsapp_schema.tb_triagem_inbox
CREATE OR REPLACE VIEW whatsapp_schema.tb_triagem_inbox AS
SELECT * FROM core_schema.tb_triagem_inbox;
