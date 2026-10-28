-- ==============================================================================
-- V23__add_parecer_tecnico_vrpl.sql
-- Descrição: Resultado textual da análise de VRPL pela Caixa GIGOV (TASK-14).
--            O dossiê em PDF permanece em s3_key_parecer_vrpl (upload fora desta task).
-- ==============================================================================

ALTER TABLE core_schema.tb_licitacoes
    ADD COLUMN IF NOT EXISTS parecer_tecnico_vrpl TEXT,
    ADD COLUMN IF NOT EXISTS observacoes_diligencia_vrpl TEXT;
