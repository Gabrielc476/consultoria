-- ==============================================================================
-- V3__complete_core_lifecycle_schema.sql
-- Descrição: Expansão e consolidação do schema de domínio (core_schema)
--            com 100% de cobertura do ciclo de vida de convênios federais (Transferegov.br)
-- Baseado em: docs/architecture/10_deep_research_ciclo_de_vida_convenios.md
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. ATUALIZAÇÃO DE TABELAS EXISTENTES (FASES 0, 1, 2, 3, 7, 8 e 9)
-- ------------------------------------------------------------------------------

-- 1.1 Expansão de tb_convenios (Ciclo de Vida Completo)
ALTER TABLE core_schema.tb_convenios
    -- Fase 0: Origem Orçamentária e Proposta
    ADD COLUMN IF NOT EXISTS numero_proposta_siconv VARCHAR(30),
    ADD COLUMN IF NOT EXISTS tipo_instrumento VARCHAR(50) NOT NULL DEFAULT 'CONVENIO_TRADICIONAL', -- 'CONVENIO_TRADICIONAL', 'CONTRATO_REPASSE_CAIXA', 'TERMO_COMPROMISSO_FNDE', 'EMENDA_ESPECIAL_PIX'
    ADD COLUMN IF NOT EXISTS modalidade_emenda VARCHAR(30), -- 'INDIVIDUAL_RP6', 'BANCADA_RP7', 'COMISSAO_RP8', 'EXTRAEMENDA_RP2'
    ADD COLUMN IF NOT EXISTS nome_parlamentar_autor VARCHAR(150),
    ADD COLUMN IF NOT EXISTS numero_emenda VARCHAR(30),
    ADD COLUMN IF NOT EXISTS numero_nota_empenho_siafi VARCHAR(50),
    ADD COLUMN IF NOT EXISTS data_empenho DATE,

    -- Fase 1: Celebração & Publicidade Oficial
    ADD COLUMN IF NOT EXISTS mandataria VARCHAR(50) DEFAULT 'CAIXA_GIGOV', -- 'CAIXA_GIGOV', 'BANCO_DO_BRASIL', 'DIRETO_MINISTERIO'
    ADD COLUMN IF NOT EXISTS data_assinatura DATE,
    ADD COLUMN IF NOT EXISTS data_publicacao_dou DATE,
    ADD COLUMN IF NOT EXISTS link_dou VARCHAR(500),

    -- Fase 2: Gestão de Cláusula Suspensiva
    ADD COLUMN IF NOT EXISTS status_clausula_suspensiva VARCHAR(30) NOT NULL DEFAULT 'NAO_APLICA', -- 'NAO_APLICA', 'PENDENTE', 'SUPERADA', 'VENCIDA_EXTINTA'
    ADD COLUMN IF NOT EXISTS data_superacao_clausula_suspensiva DATE,

    -- Fase 7 e 8: Prestação de Contas Final & Julgamento do Concedente
    ADD COLUMN IF NOT EXISTS data_limite_prestacao_contas DATE, -- 60 dias após término de vigência
    ADD COLUMN IF NOT EXISTS data_envio_prestacao_contas DATE,
    ADD COLUMN IF NOT EXISTS situacao_prestacao_contas VARCHAR(50) NOT NULL DEFAULT 'EM_EXECUCAO', -- 'EM_EXECUCAO', 'AGUARDANDO_PRESTACAO_CONTAS', 'PRESTACAO_CONTAS_ENVIADA', 'APROVADO_INTEGRAL', 'APROVADO_COM_RESSALVA', 'REJEITADO_COM_GLOSA'
    ADD COLUMN IF NOT EXISTS tipo_procedimento_analise VARCHAR(30) DEFAULT 'CONVENCIONAL', -- 'INFORMATIZADO_60D', 'CONVENCIONAL_180D'
    ADD COLUMN IF NOT EXISTS valor_saldo_remanescente_devolvido NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS valor_rendimentos_devolvidos NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS numero_gru_devolucao VARCHAR(50),
    ADD COLUMN IF NOT EXISTS s3_key_comprovante_gru VARCHAR(500),
    ADD COLUMN IF NOT EXISTS s3_key_termo_recebimento_definitivo VARCHAR(500),

    -- Fase 9: Fase Sancionatória, Notificações e Tomada de Contas Especial (TCE)
    ADD COLUMN IF NOT EXISTS status_inadimplencia_siafi VARCHAR(30) NOT NULL DEFAULT 'ADIMPLENTE', -- 'ADIMPLENTE', 'NOTIFICADO_45_DIAS', 'INADIMPLENTE_SUSPENSO'
    ADD COLUMN IF NOT EXISTS data_notificacao_cgu DATE,
    ADD COLUMN IF NOT EXISTS data_limite_defesa_45_dias DATE,
    ADD COLUMN IF NOT EXISTS valor_glosa_apurado NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS processo_tce_numero_tcu VARCHAR(50),
    ADD COLUMN IF NOT EXISTS amparado_sumula_230_tcu BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS numero_processo_judicial_sucessor VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_convenios_prestacao_contas ON core_schema.tb_convenios (data_limite_prestacao_contas, situacao_prestacao_contas);
CREATE INDEX IF NOT EXISTS idx_convenios_inadimplencia ON core_schema.tb_convenios (status_inadimplencia_siafi);

-- 1.2 Expansão de tb_contratos_execucao (Vínculo com Licitação Federal, VRPL e AIO)
ALTER TABLE core_schema.tb_contratos_execucao
    ADD COLUMN IF NOT EXISTS prefeitura_id UUID REFERENCES core_schema.tb_prefeituras(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS objeto_contratado TEXT,
    ADD COLUMN IF NOT EXISTS dados_bancarios_credor JSONB,
    ADD COLUMN IF NOT EXISTS valor_contratado_atual NUMERIC(15, 2),
    ADD COLUMN IF NOT EXISTS valor_acumulado_medido NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS valor_acumulado_pago NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS saldo_contratual_restante NUMERIC(15, 2),
    ADD COLUMN IF NOT EXISTS percentual_execucao_fisica NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS data_ordem_servico DATE,
    ADD COLUMN IF NOT EXISTS data_inicio_vigencia DATE,
    -- Validação e Autorização Federal da Licitação
    ADD COLUMN IF NOT EXISTS numero_vrpl_transferegov VARCHAR(50), -- Verificação do Resultado do Processo Licitatório
    ADD COLUMN IF NOT EXISTS data_aceite_licitacao_concedente DATE,
    ADD COLUMN IF NOT EXISTS desconto_licitacao_percentual NUMERIC(5, 2),
    ADD COLUMN IF NOT EXISTS numero_aio VARCHAR(50), -- Autorização de Início de Objeto
    ADD COLUMN IF NOT EXISTS data_emissao_aio DATE,
    ADD COLUMN IF NOT EXISTS s3_key_contrato_assinado VARCHAR(500);

-- Preenchimento padrão para compatibilidade de dados existentes
UPDATE core_schema.tb_contratos_execucao
SET valor_contratado_atual = valor_total_contrato
WHERE valor_contratado_atual IS NULL;

-- ------------------------------------------------------------------------------
-- 2. CRIAÇÃO DAS NOVAS ENTIDADES DO CICLO DE VIDA INTEGRAL
-- ------------------------------------------------------------------------------

-- 2.1 Fase 0: Tabela de Certidões do CAUC (Radar Granular de Regularidade Fiscal da LRF)
CREATE TABLE IF NOT EXISTS core_schema.tb_certidoes_cauc (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    prefeitura_id UUID NOT NULL REFERENCES core_schema.tb_prefeituras(id) ON DELETE CASCADE,
    tipo_exigencia VARCHAR(50) NOT NULL, -- 'RECEITA_FEDERAL_PGFN', 'FGTS', 'CNDT_TRABALHISTA', 'PRESTACAO_CONTAS_SIAFI', 'RREO_RGF_SICONFI', 'LIMITE_PESSOAL_LRF', 'LIMITE_SAUDE_15', 'LIMITE_EDUCACAO_25', 'CADIN'
    numero_certidao VARCHAR(100),
    data_emissao DATE NOT NULL,
    data_validade DATE NOT NULL,
    situacao VARCHAR(30) NOT NULL DEFAULT 'REGULAR', -- 'REGULAR', 'IRREGULAR', 'EM_RISCO'
    dias_para_vencer INTEGER GENERATED ALWAYS AS (data_validade - CURRENT_DATE) STORED,
    s3_key_comprovante VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_prefeitura_tipo_certidao UNIQUE (prefeitura_id, tipo_exigencia)
);

CREATE INDEX IF NOT EXISTS idx_cauc_validade ON core_schema.tb_certidoes_cauc (data_validade);
CREATE INDEX IF NOT EXISTS idx_cauc_situacao ON core_schema.tb_certidoes_cauc (situacao);

-- 2.2 Fase 2: Tabela de Condicionantes Suspensivas (Checklist Caixa / GIGOV)
CREATE TABLE IF NOT EXISTS core_schema.tb_condicionantes_suspensivas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    convenio_id UUID NOT NULL REFERENCES core_schema.tb_convenios(id) ON DELETE CASCADE,
    tipo_condicionante VARCHAR(50) NOT NULL, -- 'ENGENHARIA_PROJETOS_SINAPI', 'LICENCIAMENTO_AMBIENTAL', 'TITULARIDADE_IMOVEL'
    status VARCHAR(30) NOT NULL DEFAULT 'PENDENTE', -- 'PENDENTE', 'EM_ANALISE_CAIXA', 'DILIGENCIA_EMITIDA', 'APROVADO'
    numero_documento_comprobatorio VARCHAR(100), -- Nº da SPA/LAE, Nº da Licença ou Matrícula no Cartório de Imóveis
    data_aprovacao DATE,
    data_validade DATE,
    observacoes_analise_caixa TEXT,
    s3_key_documento VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_convenio_tipo_condicionante UNIQUE (convenio_id, tipo_condicionante)
);

CREATE INDEX IF NOT EXISTS idx_condicionantes_convenio ON core_schema.tb_condicionantes_suspensivas (convenio_id);

-- 2.3 Fase 1 & 5: Contas Bancárias Vinculadas e Gestão de Rendimentos
CREATE TABLE IF NOT EXISTS core_schema.tb_contas_bancarias_vinculadas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    convenio_id UUID NOT NULL REFERENCES core_schema.tb_convenios(id) ON DELETE CASCADE,
    banco VARCHAR(50) NOT NULL, -- '001_BANCO_DO_BRASIL', '104_CAIXA_ECONOMICA'
    agencia VARCHAR(10) NOT NULL,
    numero_conta VARCHAR(20) NOT NULL,
    tipo_bloqueio VARCHAR(30) NOT NULL DEFAULT 'CONTA_BLOQUEADA_OBTV',
    saldo_total_disponivel NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    saldo_repasse_disponivel NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    saldo_contrapartida_disponivel NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    rendimentos_aplicacao_acumulados NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total_desembolsado_uniao NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total_aportado_contrapartida NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    data_ultimo_extrato DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_convenio_conta_bancaria UNIQUE (convenio_id, banco, agencia, numero_conta)
);

CREATE INDEX IF NOT EXISTS idx_contas_convenio ON core_schema.tb_contas_bancarias_vinculadas (convenio_id);

-- 2.4 Fase 0, 4 e 5: Metas e Etapas do Plano de Trabalho Aprovado
CREATE TABLE IF NOT EXISTS core_schema.tb_metas_plano_trabalho (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    convenio_id UUID NOT NULL REFERENCES core_schema.tb_convenios(id) ON DELETE CASCADE,
    numero_meta INTEGER NOT NULL,
    titulo_meta VARCHAR(200) NOT NULL,
    descricao TEXT,
    valor_previsto NUMERIC(15, 2) NOT NULL,
    valor_executado_acumulado NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    percentual_fisico_concluido NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_convenio_numero_meta UNIQUE (convenio_id, numero_meta)
);

CREATE INDEX IF NOT EXISTS idx_metas_convenio ON core_schema.tb_metas_plano_trabalho (convenio_id);

-- 2.5 Fase 6: Governança de Alterações e Termos Aditivos
CREATE TABLE IF NOT EXISTS core_schema.tb_termos_aditivos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    tipo_instrumento_aditivado VARCHAR(30) NOT NULL, -- 'CONVENIO_FEDERAL', 'CONTRATO_MUNICIPAL'
    convenio_id UUID REFERENCES core_schema.tb_convenios(id) ON DELETE CASCADE,
    contrato_id UUID REFERENCES core_schema.tb_contratos_execucao(id) ON DELETE CASCADE,
    numero_aditivo INTEGER NOT NULL, -- 1 para 1º Termo Aditivo, etc.
    tipo_aditivo VARCHAR(50) NOT NULL, -- 'PRORROGACAO_VIGENCIA', 'VALOR_ACRESCIMO', 'VALOR_SUPRESSAO', 'REMANEJAMENTO_METAS', 'REEQUILIBRIO_ECONOMICO'
    data_assinatura DATE NOT NULL,
    data_publicacao_dou_ou_dom DATE,
    nova_data_fim_vigencia DATE,
    valor_aditado NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    justificativa TEXT NOT NULL,
    s3_key_aditivo VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_aditivos_convenio ON core_schema.tb_termos_aditivos (convenio_id);
CREATE INDEX IF NOT EXISTS idx_aditivos_contrato ON core_schema.tb_termos_aditivos (contrato_id);

-- 2.6 Fase 4: Boletins de Medição de Obras
CREATE TABLE IF NOT EXISTS core_schema.tb_medicoes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    contrato_id UUID NOT NULL REFERENCES core_schema.tb_contratos_execucao(id) ON DELETE CASCADE,
    convenio_id UUID NOT NULL REFERENCES core_schema.tb_convenios(id) ON DELETE CASCADE,
    numero_medicao INTEGER NOT NULL,
    data_inicio_periodo DATE NOT NULL,
    data_fim_periodo DATE NOT NULL,
    valor_medicao_periodo NUMERIC(15, 2) NOT NULL,
    valor_medicao_acumulado NUMERIC(15, 2) NOT NULL,
    percentual_medicao_periodo NUMERIC(5, 2) NOT NULL,
    percentual_acumulado_obra NUMERIC(5, 2) NOT NULL,
    status_medicao VARCHAR(30) NOT NULL DEFAULT 'EM_CONFERENCIA', -- 'EM_CONFERENCIA', 'ATESTADA_FISCAL', 'AFERIDA_CAIXA', 'REJEITADA'
    nome_fiscal_atestante VARCHAR(150),
    registro_crea_fiscal VARCHAR(30),
    numero_art_fiscalizacao VARCHAR(40),
    data_atesto_fiscal DATE,
    numero_rae_caixa VARCHAR(50), -- Relatório de Acompanhamento de Engenharia da Caixa
    percentual_aferido_caixa NUMERIC(5, 2),
    data_vistoria_caixa DATE,
    s3_key_planilha_medicao VARCHAR(500) NOT NULL,
    s3_key_relatorio_fotografico VARCHAR(500),
    georreferenciamento_validado BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_contrato_num_medicao UNIQUE (contrato_id, numero_medicao)
);

CREATE INDEX IF NOT EXISTS idx_medicoes_contrato ON core_schema.tb_medicoes (contrato_id);
CREATE INDEX IF NOT EXISTS idx_medicoes_convenio ON core_schema.tb_medicoes (convenio_id);

-- 2.7 Fase 5: Documentos Hábeis (Notas Fiscais Eletrônicas)
CREATE TABLE IF NOT EXISTS core_schema.tb_documentos_habeis (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    medicao_id UUID NOT NULL REFERENCES core_schema.tb_medicoes(id) ON DELETE CASCADE,
    contrato_id UUID NOT NULL REFERENCES core_schema.tb_contratos_execucao(id) ON DELETE CASCADE,
    convenio_id UUID NOT NULL REFERENCES core_schema.tb_convenios(id) ON DELETE CASCADE,
    tipo_documento_habil VARCHAR(30) NOT NULL, -- 'NOTA_FISCAL_SERVICOS', 'NOTA_FISCAL_MERCADORIAS', 'RECIBO_LEGAL'
    numero_documento VARCHAR(50) NOT NULL,
    serie_documento VARCHAR(10),
    chave_acesso_nfe VARCHAR(44),
    data_emissao DATE NOT NULL,
    cnpj_favorecido VARCHAR(18) NOT NULL,
    razao_social_favorecido VARCHAR(200) NOT NULL,
    valor_bruto NUMERIC(15, 2) NOT NULL,
    valor_total_deducoes NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    valor_liquido NUMERIC(15, 2) NOT NULL,
    status_validacao_matematica BOOLEAN NOT NULL DEFAULT FALSE,
    confidence_score_geral NUMERIC(4, 3) NOT NULL DEFAULT 1.000,
    bounding_boxes_json JSONB,
    status_revisao VARCHAR(30) NOT NULL DEFAULT 'PENDENTE_REVISAO', -- 'PENDENTE_REVISAO', 'APROVADO_ANALISTA', 'REJEITADO_DEVOLVIDO', 'INJETADO_NO_GOVERNO'
    analista_responsavel_id UUID,
    data_aprovacao_revisao TIMESTAMP WITH TIME ZONE,
    injetado_transferegov BOOLEAN NOT NULL DEFAULT FALSE,
    s3_key_pdf_original VARCHAR(500) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_doc_habil_medicao ON core_schema.tb_documentos_habeis (medicao_id);
CREATE INDEX IF NOT EXISTS idx_doc_habil_revisao ON core_schema.tb_documentos_habeis (status_revisao);

-- 2.8 Fase 5: Deduções e Retenções Tributárias
CREATE TABLE IF NOT EXISTS core_schema.tb_retencoes_tributarias (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    documento_habil_id UUID NOT NULL REFERENCES core_schema.tb_documentos_habeis(id) ON DELETE CASCADE,
    tipo_tributo VARCHAR(20) NOT NULL, -- 'INSS', 'ISS', 'IRRF', 'PIS', 'COFINS', 'CSLL'
    base_calculo NUMERIC(15, 2) NOT NULL,
    aliquota_percentual NUMERIC(5, 2) NOT NULL,
    valor_retido NUMERIC(15, 2) NOT NULL,
    codigo_receita_darf VARCHAR(20), -- Código oficial de recolhimento (ex: 0588 para IRRF, 2631 para INSS)
    confidence_score NUMERIC(4, 3) NOT NULL DEFAULT 1.000,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_retencoes_doc_habil ON core_schema.tb_retencoes_tributarias (documento_habil_id);

-- 2.9 Fase 5 & 7: Ordens de Pagamento (OBTV Fornecedor, Tributos e Devolução GRU)
CREATE TABLE IF NOT EXISTS core_schema.tb_ordens_pagamento (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    documento_habil_id UUID REFERENCES core_schema.tb_documentos_habeis(id) ON DELETE SET NULL,
    convenio_id UUID NOT NULL REFERENCES core_schema.tb_convenios(id) ON DELETE CASCADE,
    conta_bancaria_id UUID REFERENCES core_schema.tb_contas_bancarias_vinculadas(id) ON DELETE SET NULL,
    numero_obtv VARCHAR(50), -- Número oficial gerado pelo Transferegov
    tipo_obtv VARCHAR(30) NOT NULL DEFAULT 'OBTV_FORNECEDOR', -- 'OBTV_FORNECEDOR', 'OBTV_TRIBUTOS_DARF_DAM', 'OBTV_DEVOLUCAO_SALDO_GRU'
    favorecido_nome VARCHAR(200) NOT NULL,
    favorecido_cnpj_cpf VARCHAR(18) NOT NULL,
    dados_bancarios_destino JSONB,
    codigo_barras_guia VARCHAR(100), -- Para DARF, DAM ou GRU
    valor_pago NUMERIC(15, 2) NOT NULL,
    data_emissao_obtv DATE NOT NULL,
    data_debito_efetivo DATE,
    situacao_obtv VARCHAR(40) NOT NULL DEFAULT 'AGUARDANDO_ASSINATURA_GESTOR', -- 'AGUARDANDO_ASSINATURA_GESTOR', 'ENVIADA_BANCO', 'PAGA_CONFIRMADA', 'ESTORNADA'
    autenticacao_bancaria VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_obtv_doc_habil ON core_schema.tb_ordens_pagamento (documento_habil_id);
CREATE INDEX IF NOT EXISTS idx_obtv_convenio ON core_schema.tb_ordens_pagamento (convenio_id);
CREATE INDEX IF NOT EXISTS idx_obtv_situacao ON core_schema.tb_ordens_pagamento (situacao_obtv);
