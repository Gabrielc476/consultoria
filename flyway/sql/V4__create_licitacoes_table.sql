-- ==============================================================================
-- V4__create_licitacoes_table.sql
-- Descrição: Criação da tabela dedicada de Licitações (Fase 3 - Contratação Pública)
--            Permite múltiplos certames licitatórios para um único Convênio (1:N)
-- Baseado em: Lei nº 14.133/2021 (NLLC) e Portaria Conjunta MGI/MF/CGU nº 33/2023
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. CRIAÇÃO DA TABELA DE LICITAÇÕES
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS core_schema.tb_licitacoes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    convenio_id UUID NOT NULL REFERENCES core_schema.tb_convenios(id) ON DELETE CASCADE,
    prefeitura_id UUID NOT NULL REFERENCES core_schema.tb_prefeituras(id) ON DELETE CASCADE,
    
    -- Identificação e Processo Administrativo
    numero_processo_administrativo VARCHAR(50),
    numero_licitacao VARCHAR(50) NOT NULL, -- Ex: "PE 012/2024", "CC 002/2024", "DISP 005/2024"
    modalidade VARCHAR(50) NOT NULL, -- 'PREGAO_ELETRONICO', 'CONCORRENCIA_ELETRONICA', 'CONCURSO', 'LEILAO', 'DIALOGO_COMPETITIVO', 'DISPENSA', 'INEXIGIBILIDADE'
    criterio_julgamento VARCHAR(50), -- 'MENOR_PRECO', 'MAIOR_DESCONTO', 'MELHOR_TECNICA', 'TECNICA_E_PRECO', 'MAIOR_LANCE'
    regime_execucao VARCHAR(50), -- 'EMPREITADA_PRECO_GLOBAL', 'EMPREITADA_PRECO_UNITARIO', 'CONTRATACAO_INTEGRADA', 'CONTRATACAO_SEMI_INTEGRADA', 'FORNECIMENTO'
    objeto TEXT NOT NULL,
    
    -- Valores Financeiros e Economia
    valor_estimado NUMERIC(15, 2) NOT NULL,
    valor_homologado NUMERIC(15, 2),
    percentual_desconto NUMERIC(5, 2),
    
    -- Ciclo de Andamento Municipal (Lei 14.133/2021)
    situacao VARCHAR(50) NOT NULL DEFAULT 'PLANEJAMENTO', -- 'PLANEJAMENTO', 'EDITAL_PUBLICADO', 'IMPUGNADO', 'EM_DISPUTA', 'JULGAMENTO', 'HOMOLOGADA', 'DESERTA', 'FRACASSADA', 'REVOGADA', 'ANULADA'
    data_publicacao_edital DATE,
    data_abertura_propostas DATE,
    data_homologacao DATE,
    
    -- Portais e Sistemas Eletrônicos
    link_pncp VARCHAR(500), -- Portal Nacional de Contratações Públicas
    link_transferegov VARCHAR(500),
    link_sistema_compras VARCHAR(500), -- Ex: Compras.gov.br, BBMNET, BLL Compras
    
    -- Validação e Autorização Federal Transferegov / Mandatária (Caixa GIGOV)
    numero_vrpl_transferegov VARCHAR(50), -- Verificação do Resultado do Processo Licitatório
    status_vrpl VARCHAR(30) NOT NULL DEFAULT 'NAO_ENVIADO', -- 'NAO_ENVIADO', 'EM_ANALISE_CAIXA', 'DILIGENCIA', 'ACEITO_HOMOLOGADO', 'REJEITADO'
    data_envio_vrpl DATE,
    data_aceite_vrpl DATE,
    numero_aio VARCHAR(50), -- Autorização de Início de Objeto
    data_emissao_aio DATE,
    status_aio VARCHAR(30) NOT NULL DEFAULT 'NAO_EMITIDO', -- 'NAO_EMITIDO', 'SOLICITADO', 'EMITIDO'
    
    -- Armazenamento Seguro de Documentos (S3 / MinIO)
    s3_key_edital VARCHAR(500),
    s3_key_termo_homologacao VARCHAR(500),
    s3_key_parecer_vrpl VARCHAR(500),
    s3_key_autorizacao_aio VARCHAR(500),
    
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_licitacao_convenio_numero UNIQUE (convenio_id, numero_licitacao)
);

-- Índices de Consulta e Desempenho
CREATE INDEX IF NOT EXISTS idx_licitacoes_convenio ON core_schema.tb_licitacoes (convenio_id);
CREATE INDEX IF NOT EXISTS idx_licitacoes_prefeitura ON core_schema.tb_licitacoes (prefeitura_id);
CREATE INDEX IF NOT EXISTS idx_licitacoes_status ON core_schema.tb_licitacoes (situacao, status_vrpl);
CREATE INDEX IF NOT EXISTS idx_licitacoes_tenant ON core_schema.tb_licitacoes (tenant_id);

-- ------------------------------------------------------------------------------
-- 2. VÍNCULO DE CONTRATOS DE EXECUÇÃO À LICITAÇÃO ORIGINÁRIA
-- ------------------------------------------------------------------------------
ALTER TABLE core_schema.tb_contratos_execucao
    ADD COLUMN IF NOT EXISTS licitacao_id UUID REFERENCES core_schema.tb_licitacoes(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_contratos_licitacao ON core_schema.tb_contratos_execucao (licitacao_id);
