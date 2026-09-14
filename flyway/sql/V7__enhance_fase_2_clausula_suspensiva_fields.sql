-- ==============================================================================
-- V7__enhance_fase_2_clausula_suspensiva_fields.sql
-- Descrição: Refinamento dos campos da Fase 2 (Gestão e Superação da Cláusula Suspensiva)
--            Adiciona controle de prorrogação de prazo, diligências da Caixa GIGOV
--            e parâmetros técnicos da Síntese do Projeto Aprovado (SPA).
-- Baseado em: docs/architecture/13_deep_research_fase_2_gestao_clausula_suspensiva.md
-- ==============================================================================

-- 1. Expansão de tb_convenios: Prorrogação de Prazo e Termo de Retirada
ALTER TABLE core_schema.tb_convenios
    ADD COLUMN IF NOT EXISTS prorrogacao_solicitada BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS novo_prazo_prorrogado DATE,
    ADD COLUMN IF NOT EXISTS s3_key_termo_retirada_suspensiva VARCHAR(500);

-- 2. Expansão de tb_condicionantes_suspensivas: Gestão de Diligências Caixa & Parâmetros SPA
ALTER TABLE core_schema.tb_condicionantes_suspensivas
    ADD COLUMN IF NOT EXISTS data_limite_saneamento DATE,
    ADD COLUMN IF NOT EXISTS s3_key_laudo_pendencias VARCHAR(500),
    ADD COLUMN IF NOT EXISTS valor_orcamento_aprovado_caixa NUMERIC(15, 2),
    ADD COLUMN IF NOT EXISTS percentual_bdi_aprovado NUMERIC(5, 2),
    ADD COLUMN IF NOT EXISTS numero_art_rrt VARCHAR(50),
    ADD COLUMN IF NOT EXISTS orgao_emissor VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_condicionantes_saneamento 
    ON core_schema.tb_condicionantes_suspensivas (status, data_limite_saneamento);
