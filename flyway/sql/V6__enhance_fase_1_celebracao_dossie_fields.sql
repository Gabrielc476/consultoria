-- ==============================================================================
-- V6__enhance_fase_1_celebracao_dossie_fields.sql
-- Descrição: Adiciona colunas para armazenamento de documentos comprobatórios
--            (Dossiê da Fase 1: Celebração, Publicidade no DOU e Conta Vinculada)
-- Baseado em: docs/architecture/12_deep_research_fase_1_celebracao_formalizacao_e_custodia.md
-- ==============================================================================

-- 1. Armazenamento de Arquivos da Celebração e Eficácia no S3/MinIO
ALTER TABLE core_schema.tb_convenios
    ADD COLUMN IF NOT EXISTS s3_key_termo_convenio_assinado VARCHAR(500), -- Cópia integral em PDF do Termo de Convênio/Contrato de Repasse com assinaturas Gov.br
    ADD COLUMN IF NOT EXISTS s3_key_extrato_dou VARCHAR(500);            -- Certidão/Página em PDF da publicação oficial no Diário Oficial da União

-- 2. Armazenamento da Ficha Cadastral da Conta Corrente Bloqueada
ALTER TABLE core_schema.tb_contas_bancarias_vinculadas
    ADD COLUMN IF NOT EXISTS s3_key_comprovante_abertura VARCHAR(500);   -- Ficha cadastral / termo de abertura bancária emitido pela Caixa (Op 006) ou Banco do Brasil
