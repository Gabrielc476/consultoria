-- ==============================================================================
-- V16__create_transferegov_sincronizacao_tables.sql
-- Descrição: Tabelas do Pipeline de Ingestão e Streaming SICONV/Transferegov
--            (TASK-09: Dumps CSV/ZIP diários com Data Quality Framework e Quarentena)
-- Baseado em: docs/architecture/03_arquitetura_transferegov_service_cqrs.md e
--             docs/architecture/07_deep_research_transferegov_csv_vs_api.md
-- ==============================================================================

-- 1. Tabela Principal de Convênios Sincronizados
CREATE TABLE IF NOT EXISTS transferegov_schema.tb_sincronizacao_convenio (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nr_convenio VARCHAR(30) NOT NULL UNIQUE,
    id_proposta VARCHAR(30),
    cnpj_proponente VARCHAR(18) NOT NULL,
    nome_proponente VARCHAR(200) NOT NULL,
    municipio VARCHAR(100) NOT NULL,
    uf CHAR(2) NOT NULL DEFAULT 'PB',
    situacao_convenio VARCHAR(50),
    instrumento_ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_inicio_vigencia DATE,
    data_fim_vigencia DATE,
    data_limite_prestacao_contas DATE,
    data_suspensiva DATE,
    valor_global NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    valor_repasse NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    valor_contrapartida NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    valor_saldo_conta NUMERIC(15, 2) DEFAULT 0.00,
    objeto TEXT,
    data_carga_siconv VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sinc_convenio_nr ON transferegov_schema.tb_sincronizacao_convenio (nr_convenio);
CREATE INDEX IF NOT EXISTS idx_sinc_convenio_cnpj ON transferegov_schema.tb_sincronizacao_convenio (cnpj_proponente);
CREATE INDEX IF NOT EXISTS idx_sinc_convenio_uf ON transferegov_schema.tb_sincronizacao_convenio (uf);
CREATE INDEX IF NOT EXISTS idx_sinc_convenio_fim_vigencia ON transferegov_schema.tb_sincronizacao_convenio (data_fim_vigencia);
CREATE INDEX IF NOT EXISTS idx_sinc_convenio_suspensiva ON transferegov_schema.tb_sincronizacao_convenio (data_suspensiva);
CREATE INDEX IF NOT EXISTS idx_sinc_convenio_prest_contas ON transferegov_schema.tb_sincronizacao_convenio (data_limite_prestacao_contas);

-- 2. Tabela de Logs e Telemetria de Sincronização (Data Quality Pipeline)
CREATE TABLE IF NOT EXISTS transferegov_schema.tb_sincronizacao_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo_sincronizacao VARCHAR(50) NOT NULL, -- 'SICONV_CSV_DUMP', 'MANUAL_TRIGGER'
    data_inicio TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_fim TIMESTAMP WITH TIME ZONE,
    status VARCHAR(30) NOT NULL DEFAULT 'EM_ANDAMENTO', -- 'EM_ANDAMENTO', 'SUCESSO', 'ALERTA', 'FALHA'
    data_carga_siconv_referencia VARCHAR(50),
    total_registros_lidos BIGINT NOT NULL DEFAULT 0,
    total_registros_filtrados BIGINT NOT NULL DEFAULT 0,
    total_registros_persistidos BIGINT NOT NULL DEFAULT 0,
    total_anomalias BIGINT NOT NULL DEFAULT 0,
    tempo_execucao_ms BIGINT,
    detalhes_execucao_json JSONB,
    mensagem_erro TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sinc_log_data_inicio ON transferegov_schema.tb_sincronizacao_log (data_inicio DESC);
CREATE INDEX IF NOT EXISTS idx_sinc_log_status ON transferegov_schema.tb_sincronizacao_log (status);

-- 3. Tabela de Quarentena e Auditoria de Anomalias (Data Quality Quarantine / Dead-Letter)
CREATE TABLE IF NOT EXISTS transferegov_schema.tb_sincronizacao_anomalias (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    log_id UUID REFERENCES transferegov_schema.tb_sincronizacao_log(id) ON DELETE CASCADE,
    origem_arquivo VARCHAR(100) NOT NULL, -- 'siconv_convenio.csv', 'siconv_proponentes.csv', etc.
    numero_linha BIGINT,
    identificador_registro VARCHAR(100), -- ex: NR_CONVENIO ou ID_PROPOSTA
    dimensao_qualidade VARCHAR(50) NOT NULL, -- 'COMPLETENESS', 'VALIDITY', 'CONSISTENCY', 'ACCURACY'
    descricao_falha TEXT NOT NULL,
    conteudo_bruto TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sinc_anomalias_log ON transferegov_schema.tb_sincronizacao_anomalias (log_id);
CREATE INDEX IF NOT EXISTS idx_sinc_anomalias_dimensao ON transferegov_schema.tb_sincronizacao_anomalias (dimensao_qualidade);
