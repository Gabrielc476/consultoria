-- ==============================================================================
-- V9__enhance_fase_4_medicoes_e_rae_fields.sql
-- Descrição: Refinamento dos campos da Fase 4 (Execução Física, Medições e RAE Caixa)
--            Adiciona valores financeiros aferidos/glosados pela Caixa GIGOV,
--            chaves S3 para Diário de Obras Digital e Laudo RAE em tb_medicoes,
--            além de controle da Ordem de Serviço em tb_contratos_execucao.
-- Baseado em: docs/architecture/15_deep_research_fase_4_execucao_fisica_medicoes_e_rae.md
-- ==============================================================================

-- 1. Expansão de tb_medicoes: Valores do Laudo RAE e Dossiê S3
ALTER TABLE core_schema.tb_medicoes
    ADD COLUMN IF NOT EXISTS valor_aferido_caixa NUMERIC(15, 2),
    ADD COLUMN IF NOT EXISTS valor_glosado_caixa NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS s3_key_diario_obras VARCHAR(500), -- Diário de Obra Digital (art. 88 da Lei 14.133/2021)
    ADD COLUMN IF NOT EXISTS s3_key_laudo_rae VARCHAR(500);    -- Relatório de Acompanhamento de Engenharia da Caixa

-- 2. Expansão de tb_contratos_execucao: Controle de Ordem de Serviço pós-AIO
ALTER TABLE core_schema.tb_contratos_execucao
    ADD COLUMN IF NOT EXISTS data_ordem_servico DATE,
    ADD COLUMN IF NOT EXISTS s3_key_ordem_servico VARCHAR(500),
    ADD COLUMN IF NOT EXISTS numero_art_execucao VARCHAR(50);
