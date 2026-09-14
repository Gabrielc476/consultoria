-- ==============================================================================
-- V8__enhance_fase_3_licitacoes_vrpl_fields.sql
-- Descrição: Refinamento dos campos da Fase 3 (Licitações Municipais e VRPL Transferegov)
--            Adiciona identificação da empresa vencedora adjudicada e arquivos
--            do dossiê de proposta e ata de lances para auditoria da Caixa GIGOV.
-- Baseado em: docs/architecture/14_deep_research_fase_3_licitacoes_vrpl_e_aio.md
-- ==============================================================================

ALTER TABLE core_schema.tb_licitacoes
    ADD COLUMN IF NOT EXISTS cnpj_vencedor VARCHAR(18),
    ADD COLUMN IF NOT EXISTS razao_social_vencedor VARCHAR(200),
    ADD COLUMN IF NOT EXISTS s3_key_proposta_vencedora VARCHAR(500), -- Proposta e Planilha de Preços com desconto adjudicado
    ADD COLUMN IF NOT EXISTS s3_key_ata_sessao VARCHAR(500);         -- Ata da sessão pública eletrônica com histórico de lances

CREATE INDEX IF NOT EXISTS idx_licitacoes_vencedor 
    ON core_schema.tb_licitacoes (cnpj_vencedor);
