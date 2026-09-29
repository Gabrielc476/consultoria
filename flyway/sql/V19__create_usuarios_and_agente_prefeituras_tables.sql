-- ==============================================================================
-- V19__create_usuarios_and_agente_prefeituras_tables.sql
-- Descrição: Tabelas de Usuários (IAM), Vínculo N:N com Prefeituras e Seed de ADMIN
-- Serviço: core-service / GovFlow
-- Bounded Context: Módulo 1 (IAM & Onboarding)
-- ==============================================================================

-- 1. Tabela de Usuários do Sistema (IAM)
CREATE TABLE IF NOT EXISTS core_schema.tb_usuarios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES core_schema.tb_consultorias(id) ON DELETE CASCADE,
    nome VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,
    telefone_celular VARCHAR(30),
    role VARCHAR(30) NOT NULL DEFAULT 'AGENTE',
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_usuarios_email UNIQUE (email),
    CONSTRAINT chk_usuarios_role CHECK (role IN ('ADMIN', 'AGENTE'))
);

CREATE INDEX IF NOT EXISTS idx_usuarios_tenant ON core_schema.tb_usuarios (tenant_id);
CREATE INDEX IF NOT EXISTS idx_usuarios_email ON core_schema.tb_usuarios (email);
CREATE INDEX IF NOT EXISTS idx_usuarios_celular ON core_schema.tb_usuarios (telefone_celular);

-- 2. Tabela de Vínculo N:N entre Usuários/Agentes e Prefeituras
CREATE TABLE IF NOT EXISTS core_schema.tb_usuario_prefeituras (
    usuario_id UUID NOT NULL REFERENCES core_schema.tb_usuarios(id) ON DELETE CASCADE,
    prefeitura_id UUID NOT NULL REFERENCES core_schema.tb_prefeituras(id) ON DELETE CASCADE,
    atribuido_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_usuario_prefeituras PRIMARY KEY (usuario_id, prefeitura_id)
);

CREATE INDEX IF NOT EXISTS idx_usuario_pref_usuario ON core_schema.tb_usuario_prefeituras (usuario_id);
CREATE INDEX IF NOT EXISTS idx_usuario_pref_prefeitura ON core_schema.tb_usuario_prefeituras (prefeitura_id);

-- 3. Ajuste de integridade referencial: FK de tb_prefeituras para tb_consultorias
-- Saneia eventuais prefeituras órfãs de consultoria em ambientes existentes/legados
INSERT INTO core_schema.tb_consultorias (id, cnpj, razao_social, nome_fantasia, email_contato, telefone_contato, plano, status, limite_prefeituras)
SELECT DISTINCT
    p.tenant_id,
    '00.000.000/0001-' || LPAD((ROW_NUMBER() OVER ())::TEXT, 2, '0'),
    'Consultoria Demo ' || SUBSTRING(p.tenant_id::TEXT, 1, 8),
    'Consultoria Demo',
    'admin.' || SUBSTRING(p.tenant_id::TEXT, 1, 8) || '@govflow.com.br',
    '+5583999990000',
    'ENTERPRISE',
    'ATIVO',
    999
FROM core_schema.tb_prefeituras p
WHERE NOT EXISTS (
    SELECT 1 FROM core_schema.tb_consultorias c WHERE c.id = p.tenant_id
)
ON CONFLICT (id) DO NOTHING;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_prefeituras_consultoria'
    ) THEN
        ALTER TABLE core_schema.tb_prefeituras
        ADD CONSTRAINT fk_prefeituras_consultoria
        FOREIGN KEY (tenant_id) REFERENCES core_schema.tb_consultorias(id) ON DELETE CASCADE;
    END IF;
END $$;

-- 4. Seed de compatibilidade para garantir que consultorias existentes possuam usuário ADMIN
INSERT INTO core_schema.tb_usuarios (id, tenant_id, nome, email, senha_hash, telefone_celular, role, ativo)
SELECT
    gen_random_uuid(),
    c.id,
    c.razao_social,
    c.email_contato,
    '$2a$12$0UjRVZCVPBXUbShbtvg6ieWjZKznC5HjItIS6GobKxtgAM1n/uXlW', -- BCrypt de 'GovFlow2026!'
    c.telefone_contato,
    'ADMIN',
    TRUE
FROM core_schema.tb_consultorias c
WHERE NOT EXISTS (
    SELECT 1 FROM core_schema.tb_usuarios u WHERE u.email = c.email_contato
)
ON CONFLICT (email) DO NOTHING;
