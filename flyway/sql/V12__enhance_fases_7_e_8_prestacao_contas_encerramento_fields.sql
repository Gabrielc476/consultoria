-- ==============================================================================
-- V12__enhance_fases_7_e_8_prestacao_contas_encerramento_fields.sql
-- Descrição: Adiciona colunas para monitoramento do prazo de 60 dias do RCO,
--            pareceres conclusivos do concedente (técnico e financeiro),
--            despacho decisório de homologação e dossiê de encerramento bancário
--            (Fases 7 e 8 - Prestação de Contas Final e Encerramento)
-- Baseado em: docs/architecture/18_deep_research_fase_7_e_8_prestacao_contas_e_encerramento.md
-- ==============================================================================

ALTER TABLE core_schema.tb_convenios
    ADD COLUMN IF NOT EXISTS data_envio_prestacao_contas DATE,                -- Data da transmissão formal do RCO no Transferegov (Portaria 33, art. 82)
    ADD COLUMN IF NOT EXISTS status_parecer_tecnico VARCHAR(30) DEFAULT 'EM_ANALISE', -- 'EM_ANALISE', 'APROVADO_SEM_RESSALVA', 'APROVADO_COM_RESSALVA', 'REJEITADO'
    ADD COLUMN IF NOT EXISTS status_parecer_financeiro VARCHAR(30) DEFAULT 'EM_ANALISE', -- 'EM_ANALISE', 'APROVADO_SEM_RESSALVA', 'APROVADO_COM_RESSALVA', 'REJEITADO'
    ADD COLUMN IF NOT EXISTS data_homologacao_prestacao_contas DATE,          -- Data do despacho decisório final emitido pelo concedente
    ADD COLUMN IF NOT EXISTS s3_key_relatorio_cumprimento_objeto VARCHAR(500),-- PDF do Relatório de Cumprimento do Objeto (RCO) assinado e protocolado
    ADD COLUMN IF NOT EXISTS s3_key_termo_encerramento_conta VARCHAR(500),    -- Declaração bancária da Caixa/BB de saldo zerado e encerramento da conta Op 006
    ADD COLUMN IF NOT EXISTS s3_key_parecer_tecnico_concedente VARCHAR(500),  -- Laudo e Parecer Técnico Conclusivo de Engenharia da Caixa/Ministério
    ADD COLUMN IF NOT EXISTS s3_key_parecer_financeiro_concedente VARCHAR(500);-- Parecer Financeiro Conclusivo de Conformidade Contábil do Concedente

CREATE INDEX IF NOT EXISTS idx_convenios_prestacao_contas_status
    ON core_schema.tb_convenios (situacao_prestacao_contas, data_limite_prestacao_contas);
