-- ==============================================================================
-- V15__create_documentos_and_auditoria_tables.sql
-- Descrição: Tabelas do Agregado Documento e Ciclo de Aprovação Auditada
--            (TASK-06: Human-in-the-Loop com isolamento multi-tenant)
-- Baseado em: docs/architecture/02_arquitetura_core_service_hexagonal.md e
--             docs/architecture/09_modelo_de_dominio_e_entidades.md
-- ==============================================================================

-- 1. Tabela de Documentos (Ingestão, Extração IA e Ciclo Operacional)
CREATE TABLE IF NOT EXISTS core_schema.tb_documentos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    prefeitura_id UUID REFERENCES core_schema.tb_prefeituras(id) ON DELETE SET NULL,
    convenio_id UUID REFERENCES core_schema.tb_convenios(id) ON DELETE SET NULL,
    contrato_id UUID REFERENCES core_schema.tb_contratos_execucao(id) ON DELETE SET NULL,
    medicao_id UUID REFERENCES core_schema.tb_medicoes(id) ON DELETE SET NULL,
    s3_bucket VARCHAR(100),
    s3_key VARCHAR(500),
    nome_arquivo_original VARCHAR(255),
    content_type VARCHAR(100),
    tamanho_bytes BIGINT,
    status VARCHAR(30) NOT NULL DEFAULT 'RECEBIDO', -- 'RECEBIDO', 'EM_CONFERENCIA', 'PRONTO_PARA_TRANSFEREGOV', 'REJEITADO'
    tipo_documento_habil VARCHAR(30), -- 'NOTA_FISCAL_SERVICOS', 'NOTA_FISCAL_MERCADORIAS', 'RECIBO_LEGAL'
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
    confidence_score_geral NUMERIC(4, 3) NOT NULL DEFAULT 0.000,
    dados_extracao_json JSONB,
    bounding_boxes_json JSONB,
    dados_revisao_json JSONB,
    motivo_rejeicao TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_documentos_tenant ON core_schema.tb_documentos (tenant_id);
CREATE INDEX IF NOT EXISTS idx_documentos_status ON core_schema.tb_documentos (status);
CREATE INDEX IF NOT EXISTS idx_documentos_cnpj ON core_schema.tb_documentos (cnpj_credor);
CREATE INDEX IF NOT EXISTS idx_documentos_prefeitura ON core_schema.tb_documentos (prefeitura_id);
CREATE INDEX IF NOT EXISTS idx_documentos_convenio ON core_schema.tb_documentos (convenio_id);
CREATE INDEX IF NOT EXISTS idx_documentos_contrato ON core_schema.tb_documentos (contrato_id);
CREATE INDEX IF NOT EXISTS idx_documentos_medicao ON core_schema.tb_documentos (medicao_id);

-- 2. Tabela de Auditorias de Revisão (Trilha Imutável com Comparativo Diff)
CREATE TABLE IF NOT EXISTS core_schema.tb_auditorias_revisao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    documento_id UUID NOT NULL REFERENCES core_schema.tb_documentos(id) ON DELETE CASCADE,
    analista_id UUID NOT NULL,
    acao VARCHAR(30) NOT NULL, -- 'APROVACAO', 'REJEICAO'
    data_revisao TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    justificativa TEXT,
    diff_alteracoes_json JSONB,
    valores_originais_json JSONB,
    valores_revisados_json JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_auditorias_tenant ON core_schema.tb_auditorias_revisao (tenant_id);
CREATE INDEX IF NOT EXISTS idx_auditorias_documento ON core_schema.tb_auditorias_revisao (documento_id);
CREATE INDEX IF NOT EXISTS idx_auditorias_analista ON core_schema.tb_auditorias_revisao (analista_id);
