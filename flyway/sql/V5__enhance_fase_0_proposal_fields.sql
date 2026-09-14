-- ==============================================================================
-- V5__enhance_fase_0_proposal_fields.sql
-- Descrição: Refinamento dos campos da Fase 0 (Pré-Convênio, Origem e Proposta)
--            Conforme Deep Research Especializado da Fase 0 (docs/architecture/11)
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. EXPANSÃO DE tb_convenios COM METADADOS OPERACIONAIS DA FASE 0
-- ------------------------------------------------------------------------------
ALTER TABLE core_schema.tb_convenios
    -- Enquadramento Orçamentário e Prevenção de Impedimentos Técnicos
    ADD COLUMN IF NOT EXISTS gnd_despesa VARCHAR(30) DEFAULT 'GND_4_INVESTIMENTOS', -- 'GND_3_DESPESAS_CORRENTES', 'GND_4_INVESTIMENTOS'
    
    -- Contrapartida e Dotação Orçamentária na LOA Municipal
    ADD COLUMN IF NOT EXISTS dotacao_orcamentaria_contrapartida_loa VARCHAR(100), -- Ex: '02.05.10.301.0012.2045.4.4.90.51.00 - Fonte 1.500.0000'
    ADD COLUMN IF NOT EXISTS percentual_contrapartida_calculado NUMERIC(5, 2), -- Percentual mínimo exigido pela LDO vigente
    
    -- Controle de Análise e Janela Fatal de Diligências Preliminares (SIOP / SOF)
    ADD COLUMN IF NOT EXISTS status_analise_proposta VARCHAR(35) NOT NULL DEFAULT 'EM_CADASTRAMENTO', -- 'EM_CADASTRAMENTO', 'ENVIADA_PARA_ANALISE', 'EM_DILIGENCIA', 'APROVADA_EMPENHADA', 'IMPEDIMENTO_TECNICO'
    ADD COLUMN IF NOT EXISTS data_limite_saneamento_diligencia DATE, -- Prazo fatal de 15 a 30 dias para não perder a emenda
    
    -- Armazenamento Seguro do Dossiê Preparatório da Proposta (MinIO / S3)
    ADD COLUMN IF NOT EXISTS s3_key_plano_trabalho_proposta VARCHAR(500), -- PDF gerado do Plano de Trabalho submetido
    ADD COLUMN IF NOT EXISTS s3_key_dossie_declaracoes_proposta VARCHAR(500); -- Pacote de 6 declarações + Folha da LOA assinados digitalmente

-- ------------------------------------------------------------------------------
-- 2. ÍNDICE OPERACIONAL PARA MONITORAMENTO DE DILIGÊNCIAS PREVENTIVAS
-- ------------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_convenios_diligencia_proposta 
    ON core_schema.tb_convenios (status_analise_proposta, data_limite_saneamento_diligencia);
