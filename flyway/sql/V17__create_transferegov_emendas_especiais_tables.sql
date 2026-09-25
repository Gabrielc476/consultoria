-- ==============================================================================
-- V17__create_transferegov_emendas_especiais_tables.sql
-- Descrição: Tabelas de Ingestão e Auditoria de Emendas Especiais (Emendas Pix)
--            (TASK-11: API REST Federal /especiais e Conformidade STF ADPF 854)
-- ==============================================================================

-- 1. Tabela Principal de Planos de Ação das Emendas Especiais (Pix)
CREATE TABLE IF NOT EXISTS transferegov_schema.tb_emenda_especial_plano_acao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_plano_acao BIGINT NOT NULL UNIQUE,
    codigo_plano_acao VARCHAR(50),
    ano_plano_acao INTEGER,
    modalidade_plano_acao VARCHAR(50),
    situacao_plano_acao VARCHAR(50) NOT NULL DEFAULT 'CIENTE',
    data_aceite_plano_acao DATE,
    cnpj_beneficiario VARCHAR(18) NOT NULL,
    nome_beneficiario VARCHAR(200) NOT NULL,
    uf_beneficiario CHAR(2) NOT NULL DEFAULT 'PB',
    id_beneficiario BIGINT,
    nome_parlamentar VARCHAR(200),
    ano_emenda INTEGER,
    numero_emenda INTEGER,
    codigo_emenda_formatado VARCHAR(50),
    categoria_despesa VARCHAR(50), -- 'CUSTEIO', 'INVESTIMENTO', 'CUSTEIO E INVESTIMENTO'
    valor_custeio NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    valor_investimento NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    valor_total NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    nome_objeto TEXT,
    detalhamento_objeto TEXT,
    area_politica_publica TEXT,
    motivo_impedimento TEXT,
    codigo_banco VARCHAR(20),
    nome_banco VARCHAR(100),
    numero_agencia VARCHAR(20),
    dv_agencia VARCHAR(5),
    numero_conta VARCHAR(30),
    dv_conta VARCHAR(5),
    situacao_dado_bancario VARCHAR(100),
    status_adpf854 VARCHAR(30) NOT NULL DEFAULT 'PENDENTE_AVALIACAO', -- 'CONFORME', 'ALERTA', 'NAO_CONFORME'
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_emenda_especial_cnpj ON transferegov_schema.tb_emenda_especial_plano_acao (cnpj_beneficiario);
CREATE INDEX IF NOT EXISTS idx_emenda_especial_uf ON transferegov_schema.tb_emenda_especial_plano_acao (uf_beneficiario);
CREATE INDEX IF NOT EXISTS idx_emenda_especial_status_adpf ON transferegov_schema.tb_emenda_especial_plano_acao (status_adpf854);
CREATE INDEX IF NOT EXISTS idx_emenda_especial_parlamentar ON transferegov_schema.tb_emenda_especial_plano_acao (nome_parlamentar);
CREATE INDEX IF NOT EXISTS idx_emenda_especial_ano ON transferegov_schema.tb_emenda_especial_plano_acao (ano_emenda);

-- 2. Tabela de Planos de Trabalho Vinculados
CREATE TABLE IF NOT EXISTS transferegov_schema.tb_emenda_especial_plano_trabalho (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_plano_trabalho BIGINT NOT NULL UNIQUE,
    plano_acao_id UUID NOT NULL REFERENCES transferegov_schema.tb_emenda_especial_plano_acao(id) ON DELETE CASCADE,
    id_plano_acao BIGINT NOT NULL,
    situacao_plano_trabalho VARCHAR(50) NOT NULL, -- 'APROVADO', 'EM_ELABORACAO', 'ENVIADO_PARA_ANALISE', etc.
    data_inicio_execucao DATE,
    data_fim_execucao DATE,
    prazo_execucao_meses INTEGER,
    data_aprovacao TIMESTAMP WITH TIME ZONE,
    ind_orgao_analises_pendentes VARCHAR(10),
    classificacao_orcamentaria TEXT,
    justificativa_prorrogacao TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_emenda_pt_plano_acao_id ON transferegov_schema.tb_emenda_especial_plano_trabalho (plano_acao_id);
CREATE INDEX IF NOT EXISTS idx_emenda_pt_situacao ON transferegov_schema.tb_emenda_especial_plano_trabalho (situacao_plano_trabalho);
CREATE INDEX IF NOT EXISTS idx_emenda_pt_fim_execucao ON transferegov_schema.tb_emenda_especial_plano_trabalho (data_fim_execucao);

-- 3. Tabela de Relatórios de Gestão Novos (Transparência Ativa / Prestação de Contas)
CREATE TABLE IF NOT EXISTS transferegov_schema.tb_emenda_especial_relatorio_gestao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_relatorio_gestao_novo BIGINT NOT NULL UNIQUE,
    plano_acao_id UUID NOT NULL REFERENCES transferegov_schema.tb_emenda_especial_plano_acao(id) ON DELETE CASCADE,
    id_plano_acao BIGINT NOT NULL,
    tipo_relatorio VARCHAR(30) NOT NULL, -- 'Final', 'Parcial'
    situacao_relatorio VARCHAR(50) NOT NULL, -- 'DISPONIBILIZADO', 'EM_ELABORACAO'
    data_relatorio DATE,
    valor_executado NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    valor_pendente NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_emenda_rg_plano_acao_id ON transferegov_schema.tb_emenda_especial_relatorio_gestao (plano_acao_id);
CREATE INDEX IF NOT EXISTS idx_emenda_rg_situacao ON transferegov_schema.tb_emenda_especial_relatorio_gestao (situacao_relatorio);

-- 4. Tabela de Inconformidades e Auditoria STF (ADPF 854)
CREATE TABLE IF NOT EXISTS transferegov_schema.tb_emenda_especial_inconformidade (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plano_acao_id UUID NOT NULL REFERENCES transferegov_schema.tb_emenda_especial_plano_acao(id) ON DELETE CASCADE,
    tipo_inconformidade VARCHAR(80) NOT NULL,
    severidade VARCHAR(20) NOT NULL, -- 'CRITICO', 'ALERTA', 'INFO'
    descricao TEXT NOT NULL,
    resolvido BOOLEAN NOT NULL DEFAULT FALSE,
    data_deteccao TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_resolucao TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_emenda_inconf_plano_acao_id ON transferegov_schema.tb_emenda_especial_inconformidade (plano_acao_id);
CREATE INDEX IF NOT EXISTS idx_emenda_inconf_severidade ON transferegov_schema.tb_emenda_especial_inconformidade (severidade);
CREATE INDEX IF NOT EXISTS idx_emenda_inconf_resolvido ON transferegov_schema.tb_emenda_especial_inconformidade (resolvido);
