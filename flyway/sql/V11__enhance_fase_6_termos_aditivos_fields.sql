-- ==============================================================================
-- V11__enhance_fase_6_termos_aditivos_fields.sql
-- Descrição: Adiciona colunas para validação de tetos da Lei 14.133/21 (25% e 50%),
--            rastreamento de reprogramação Caixa GIGOV, radar tempestivo de 30 dias
--            e armazenamento do dossiê documental (Fase 6 - Termos Aditivos e Reequilíbrio)
-- Baseado em: docs/architecture/17_deep_research_fase_6_termos_aditivos_e_reequilibrio.md
-- ==============================================================================

ALTER TABLE core_schema.tb_termos_aditivos
    ADD COLUMN IF NOT EXISTS percentual_aditado NUMERIC(5, 2),                  -- Percentual de acréscimo ou supressão calculado sobre o valor original
    ADD COLUMN IF NOT EXISTS parecer_tecnico_caixa VARCHAR(50),                 -- Protocolo do Parecer Técnico de Engenharia (PTE) ou SPA da Caixa GIGOV
    ADD COLUMN IF NOT EXISTS status_aprovacao_concedente VARCHAR(30) NOT NULL DEFAULT 'EM_ELABORACAO', -- 'EM_ELABORACAO', 'SUBMETIDO_CAIXA', 'DILIGENCIA_CAIXA', 'APROVADO_CONCEDENTE', 'REJEITADO'
    ADD COLUMN IF NOT EXISTS data_limite_solicitacao_tempestiva DATE,           -- Prazo fatal de 30 dias antes do fim da vigência (Portaria 33, art. 57)
    ADD COLUMN IF NOT EXISTS s3_key_justificativa_tecnica VARCHAR(500),         -- Laudo e memória de cálculo do engenheiro fiscal municipal
    ADD COLUMN IF NOT EXISTS s3_key_planilha_comparativa VARCHAR(500),          -- Planilha orçamentária comparativa SINAPI (previsto x proposto)
    ADD COLUMN IF NOT EXISTS s3_key_parecer_caixa VARCHAR(500),                 -- Laudo de Reprogramação da Caixa GIGOV em PDF
    ADD COLUMN IF NOT EXISTS s3_key_extrato_publicacao VARCHAR(500);            -- Certidão de publicação oficial no DOU ou PNCP em PDF

CREATE INDEX IF NOT EXISTS idx_aditivos_status_tempestivo
    ON core_schema.tb_termos_aditivos (status_aprovacao_concedente, data_limite_solicitacao_tempestiva);
