-- ==============================================================================
-- V2__init_core_schema.sql
-- Descrição: Estrutura inicial do schema central de domínio (core_schema)
-- Baseado em: docs/architecture/09_modelo_de_dominio_e_entidades.md
-- ==============================================================================

CREATE SCHEMA IF NOT EXISTS core_schema;

-- 1. Tabela de Prefeituras (Ente Convenente)
CREATE TABLE IF NOT EXISTS core_schema.tb_prefeituras (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    cnpj VARCHAR(18) NOT NULL,
    razao_social VARCHAR(200) NOT NULL,
    nome_municipio VARCHAR(100) NOT NULL,
    uf CHAR(2) NOT NULL,
    codigo_ibge VARCHAR(7) NOT NULL,
    porte_municipio VARCHAR(30) NOT NULL DEFAULT 'PEQUENO_PORTE_1',
    nome_prefeito VARCHAR(150),
    cpf_prefeito VARCHAR(14),
    inicio_mandato DATE,
    fim_mandato DATE,
    status_cauc VARCHAR(30) NOT NULL DEFAULT 'ADIMPLENTE',
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_prefeitura_cnpj_tenant UNIQUE (cnpj, tenant_id)
);

CREATE INDEX IF NOT EXISTS idx_prefeituras_tenant ON core_schema.tb_prefeituras (tenant_id);
CREATE INDEX IF NOT EXISTS idx_prefeituras_ibge ON core_schema.tb_prefeituras (codigo_ibge);

-- 2. Tabela de Convênios Federais (Transferegov / SICONV)
CREATE TABLE IF NOT EXISTS core_schema.tb_convenios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    prefeitura_id UUID NOT NULL REFERENCES core_schema.tb_prefeituras(id) ON DELETE CASCADE,
    numero_siconv VARCHAR(30) NOT NULL,
    numero_processo VARCHAR(50),
    orgao_concedente VARCHAR(150) NOT NULL,
    objeto TEXT NOT NULL,
    valor_global NUMERIC(15, 2) NOT NULL,
    valor_repasse NUMERIC(15, 2) NOT NULL,
    valor_contrapartida NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    situacao VARCHAR(50) NOT NULL,
    possui_clausula_suspensiva BOOLEAN NOT NULL DEFAULT FALSE,
    prazo_clausula_suspensiva DATE,
    data_inicio_vigencia DATE,
    data_fim_vigencia DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_convenio_siconv_tenant UNIQUE (numero_siconv, tenant_id)
);

CREATE INDEX IF NOT EXISTS idx_convenios_prefeitura ON core_schema.tb_convenios (prefeitura_id);
CREATE INDEX IF NOT EXISTS idx_convenios_suspensiva ON core_schema.tb_convenios (possui_clausula_suspensiva, prazo_clausula_suspensiva);

-- 3. Tabela de Contratos de Execução (Contrato Administrativo / Licitação Municipal)
CREATE TABLE IF NOT EXISTS core_schema.tb_contratos_execucao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    convenio_id UUID NOT NULL REFERENCES core_schema.tb_convenios(id) ON DELETE CASCADE,
    numero_contrato_municipal VARCHAR(50) NOT NULL,
    numero_licitacao VARCHAR(50),
    modalidade_licitacao VARCHAR(50),
    cnpj_contratada VARCHAR(18) NOT NULL,
    razao_social_contratada VARCHAR(200) NOT NULL,
    valor_total_contrato NUMERIC(15, 2) NOT NULL,
    data_assinatura DATE,
    data_fim_vigencia DATE,
    status_contrato VARCHAR(30) NOT NULL DEFAULT 'VIGENTE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_contratos_convenio ON core_schema.tb_contratos_execucao (convenio_id);
