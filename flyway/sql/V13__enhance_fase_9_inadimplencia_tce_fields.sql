-- ==============================================================================
-- V13__enhance_fase_9_inadimplencia_tce_fields.sql
-- Descrição: Adiciona colunas para tipificação de inadimplência, ciclo de vida
--            da Tomada de Contas Especial (TCE), cálculo do débito com taxa SELIC,
--            rastreamento de blindagem da Súmula 230 do TCU e guarda do contencioso
--            (Fase 9 - Inadimplência, Notificação de 45 Dias e TCE)
-- Baseado em: docs/architecture/19_deep_research_fase_9_inadimplencia_tce_e_sumula_230.md
-- ==============================================================================

ALTER TABLE core_schema.tb_convenios
    ADD COLUMN IF NOT EXISTS motivo_inadimplencia VARCHAR(50),                      -- 'OMISSAO_PRESTACAO_CONTAS', 'GLOSA_REJEICAO_CONTAS', 'FALTA_FUNCIONALIDADE', 'DESVIO_FINALIDADE', 'DANO_AO_ERARIO'
    ADD COLUMN IF NOT EXISTS fase_tce VARCHAR(30) NOT NULL DEFAULT 'NAO_INSTAURADA',-- 'NAO_INSTAURADA', 'NOTIFICACAO_PREVIA', 'FASE_INTERNA_MINISTERIO', 'AUDITORIA_CGU', 'FASE_EXTERNA_TCU', 'JULGADA_CONDENATORIA', 'ARQUIVADA_QUITADA', 'PRESCRITA'
    ADD COLUMN IF NOT EXISTS valor_debito_atualizado_selic NUMERIC(15, 2),          -- Débito com atualização monetária e juros SELIC (Portaria 33, art. 91)
    ADD COLUMN IF NOT EXISTS data_ajuizamento_sumula_230 DATE,                     -- Data da ação judicial ou representação MPF ajuizada pelo prefeito sucessor
    ADD COLUMN IF NOT EXISTS s3_key_notificacao_cgu VARCHAR(500),                  -- PDF do ofício formal de notificação de 45 dias
    ADD COLUMN IF NOT EXISTS s3_key_defesa_previa VARCHAR(500),                    -- PDF da defesa técnica administrativa no prazo de 45 dias
    ADD COLUMN IF NOT EXISTS s3_key_peticao_judicial_sucessor VARCHAR(500),        -- Petição inicial e certidão judicial para suspensão no CAUC (Súmula 230 TCU)
    ADD COLUMN IF NOT EXISTS s3_key_acordao_tcu VARCHAR(500);                      -- Cópia digital do Acórdão proferido pelo Plenário/Câmaras do TCU

CREATE INDEX IF NOT EXISTS idx_convenios_inadimplencia_tce
    ON core_schema.tb_convenios (status_inadimplencia_siafi, fase_tce, data_limite_defesa_45_dias);
