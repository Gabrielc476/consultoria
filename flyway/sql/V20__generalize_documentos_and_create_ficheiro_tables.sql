-- ==============================================================================
-- V20__generalize_documentos_and_create_ficheiro_tables.sql
-- Descrição: Generalização da tabela tb_documentos para suportar todos os tipos
--            documentais do Ficheiro Digital (Fases 00 a 09), criação da tabela
--            satélite tb_documentos_habeis_dados para especialização fiscal e
--            criação da trilha de auditoria documental tb_documentos_auditoria.
-- Referência: docs/architecture/21_modulo_ged_ficheiro_digital_convenio.md
-- ==============================================================================

-- 1. Generalização de core_schema.tb_documentos para o Ficheiro Digital
ALTER TABLE core_schema.tb_documentos
    ADD COLUMN IF NOT EXISTS fase_ciclo_vida VARCHAR(40) NOT NULL DEFAULT 'FASE_05_EXECUCAO_FINANCEIRA',
    ADD COLUMN IF NOT EXISTS categoria_documento VARCHAR(50) NOT NULL DEFAULT 'DOCUMENTO_HABIL',
    ADD COLUMN IF NOT EXISTS pasta_virtual VARCHAR(255) NOT NULL DEFAULT '/',
    ADD COLUMN IF NOT EXISTS tags TEXT[],
    ADD COLUMN IF NOT EXISTS hash_sha256 VARCHAR(64),
    ADD COLUMN IF NOT EXISTS metadados_json JSONB,
    ADD COLUMN IF NOT EXISTS origem_canal VARCHAR(30) NOT NULL DEFAULT 'UPLOAD_MANUAL',
    ADD COLUMN IF NOT EXISTS criado_por_usuario_id UUID REFERENCES core_schema.tb_usuarios(id) ON DELETE SET NULL;

-- Índices de consulta otimizada no Ficheiro Digital
CREATE INDEX IF NOT EXISTS idx_documentos_convenio_fase ON core_schema.tb_documentos(convenio_id, fase_ciclo_vida);
CREATE INDEX IF NOT EXISTS idx_documentos_categoria ON core_schema.tb_documentos(categoria_documento);
CREATE INDEX IF NOT EXISTS idx_documentos_origem ON core_schema.tb_documentos(origem_canal);
CREATE INDEX IF NOT EXISTS idx_documentos_hash ON core_schema.tb_documentos(hash_sha256);

-- 2. Tabela Satélite Especializada para Documentos Hábeis / Fiscais (1:1 com tb_documentos)
CREATE TABLE IF NOT EXISTS core_schema.tb_documentos_habeis_dados (
    documento_id UUID PRIMARY KEY REFERENCES core_schema.tb_documentos(id) ON DELETE CASCADE,
    tipo_documento_habil VARCHAR(30) NOT NULL, -- 'NOTA_FISCAL_SERVICOS', 'NOTA_FISCAL_MERCADORIAS', 'RECIBO_LEGAL'
    numero_documento VARCHAR(50),
    serie_documento VARCHAR(10),
    chave_acesso_nfe VARCHAR(44),
    data_emissao DATE,
    cnpj_credor VARCHAR(18),
    razao_social_credor VARCHAR(200),
    descricao_servico TEXT,
    valor_bruto NUMERIC(15, 2),
    valor_total_deducoes NUMERIC(15, 2) DEFAULT 0.00,
    valor_liquido NUMERIC(15, 2),
    status_validacao_matematica BOOLEAN NOT NULL DEFAULT FALSE,
    confidence_score_ia NUMERIC(4, 3) DEFAULT 0.000,
    dados_extracao_ia_json JSONB,
    bounding_boxes_json JSONB,
    dados_revisao_json JSONB
);

-- Migração de compatibilidade: copia dados fiscais legados para a tabela satélite
INSERT INTO core_schema.tb_documentos_habeis_dados (
    documento_id, tipo_documento_habil, numero_documento, serie_documento,
    chave_acesso_nfe, data_emissao, cnpj_credor, razao_social_credor,
    descricao_servico, valor_bruto, valor_total_deducoes, valor_liquido,
    status_validacao_matematica, confidence_score_ia, dados_extracao_ia_json,
    bounding_boxes_json, dados_revisao_json
)
SELECT
    id,
    COALESCE(tipo_documento_habil, 'NOTA_FISCAL_SERVICOS'),
    numero_documento,
    serie_documento,
    chave_acesso_nfe,
    data_emissao,
    cnpj_credor,
    razao_social_credor,
    descricao_servico,
    valor_bruto,
    COALESCE(valor_total_deducoes, 0.00),
    valor_liquido,
    COALESCE(status_validacao_matematica, FALSE),
    COALESCE(confidence_score_geral, 0.000),
    dados_extracao_json,
    bounding_boxes_json,
    dados_revisao_json
FROM core_schema.tb_documentos
WHERE tipo_documento_habil IS NOT NULL
   OR chave_acesso_nfe IS NOT NULL
   OR cnpj_credor IS NOT NULL
ON CONFLICT (documento_id) DO NOTHING;

-- 3. Histórico e Trilha Imutável de Auditoria Documental
CREATE TABLE IF NOT EXISTS core_schema.tb_documentos_auditoria (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    documento_id UUID NOT NULL REFERENCES core_schema.tb_documentos(id) ON DELETE CASCADE,
    usuario_id UUID REFERENCES core_schema.tb_usuarios(id) ON DELETE SET NULL,
    acao VARCHAR(30) NOT NULL, -- 'UPLOAD', 'CLASSIFICACAO', 'APROVACAO', 'REJEICAO', 'MOVIDO_DE_PASTA', 'DOWNLOAD', 'EXCLUSAO'
    justificativa TEXT,
    snapshot_anterior_json JSONB,
    snapshot_atual_json JSONB,
    realizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_doc_auditoria_doc ON core_schema.tb_documentos_auditoria(documento_id);
CREATE INDEX IF NOT EXISTS idx_doc_auditoria_tenant ON core_schema.tb_documentos_auditoria(tenant_id);
