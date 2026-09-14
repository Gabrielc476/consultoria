-- ==============================================================================
-- V10__enhance_fase_5_execucao_financeira_obtv_fields.sql
-- Descrição: Adiciona colunas para liquidação formal, armazenamento do XML fiscal,
--            auditoria de retenções tributárias, rastreamento de duplo comando
--            e conciliação da conta vinculada (Fase 5 - Execução Financeira & OBTV)
-- Baseado em: docs/architecture/16_deep_research_fase_5_execucao_financeira_e_obtv.md
-- ==============================================================================

-- 1. Documentos Hábeis (Liquidação formal e guarda do XML da NF-e / NFS-e)
ALTER TABLE core_schema.tb_documentos_habeis
    ADD COLUMN IF NOT EXISTS data_atesto_liquidacao DATE,                -- Data do atesto do secretário/fiscal (art. 63 da Lei 4.320/64)
    ADD COLUMN IF NOT EXISTS s3_key_xml_documento VARCHAR(500);          -- Arquivo XML original da NF-e / NFS-e para auditoria e parsing

-- 2. Retenções Tributárias (Linha digitável, vencimento e guarda da guia DARF/DAM)
ALTER TABLE core_schema.tb_retencoes_tributarias
    ADD COLUMN IF NOT EXISTS codigo_barras_guia VARCHAR(100),            -- Linha digitável da guia de recolhimento tributário (DARF/DAM)
    ADD COLUMN IF NOT EXISTS data_vencimento_guia DATE,                  -- Prazo fatal para pagamento da guia tributária sem juros/multa
    ADD COLUMN IF NOT EXISTS s3_key_guia_recolhimento VARCHAR(500);      -- PDF da guia de recolhimento tributário emitida (DARF DCTFWeb / DAM)

-- 3. Ordens de Pagamento OBTV (Duplo Comando Gov.br, Pix e comprovante bancário)
ALTER TABLE core_schema.tb_ordens_pagamento
    ADD COLUMN IF NOT EXISTS assinante_1_nome VARCHAR(150),              -- 1º Comando: Prefeito Municipal ou Ordenador de Despesas
    ADD COLUMN IF NOT EXISTS data_assinatura_1 TIMESTAMP WITH TIME ZONE, -- Data/hora da assinatura do 1º comando no Transferegov
    ADD COLUMN IF NOT EXISTS assinante_2_nome VARCHAR(150),              -- 2º Comando: Secretário de Finanças ou Tesoureiro Municipal
    ADD COLUMN IF NOT EXISTS data_assinatura_2 TIMESTAMP WITH TIME ZONE, -- Data/hora da assinatura do 2º comando no Transferegov
    ADD COLUMN IF NOT EXISTS chave_pix_favorecido VARCHAR(100),          -- Chave Pix do credor para liquidação eletrônica instantânea
    ADD COLUMN IF NOT EXISTS s3_key_comprovante_obtv VARCHAR(500);       -- Comprovante bancário definitivo do débito/liquidação da OBTV

-- 4. Contas Bancárias Vinculadas (Conciliação da Conta Bloqueada Op 006)
ALTER TABLE core_schema.tb_contas_bancarias_vinculadas
    ADD COLUMN IF NOT EXISTS s3_key_ultimo_extrato VARCHAR(500);         -- PDF do extrato bancário oficial da conta vinculada (Op 006)
