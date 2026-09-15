-- ==============================================================================
-- V14__create_consultorias_table.sql
-- Descrição: Tabela de Consultorias Municipais (Tenants do GovFlow)
-- Serviço: core-service / GovFlow
-- ==============================================================================

CREATE TABLE IF NOT EXISTS core_schema.tb_consultorias (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cnpj VARCHAR(18) NOT NULL,
    razao_social VARCHAR(200) NOT NULL,
    nome_fantasia VARCHAR(150) NOT NULL,
    email_contato VARCHAR(150) NOT NULL,
    telefone_contato VARCHAR(20),
    plano VARCHAR(30) NOT NULL DEFAULT 'STARTER',
    status VARCHAR(30) NOT NULL DEFAULT 'ATIVO',
    limite_prefeituras INTEGER NOT NULL DEFAULT 5,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_consultoria_cnpj UNIQUE (cnpj)
);

CREATE INDEX IF NOT EXISTS idx_consultorias_cnpj ON core_schema.tb_consultorias (cnpj);
CREATE INDEX IF NOT EXISTS idx_consultorias_status ON core_schema.tb_consultorias (status);
