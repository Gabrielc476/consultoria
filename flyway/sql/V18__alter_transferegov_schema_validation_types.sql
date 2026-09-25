-- ==============================================================================
-- V18__alter_transferegov_schema_validation_types.sql
-- Descrição: Ajuste de tipos CHAR(2) para VARCHAR(2) para validação estrita do Hibernate 6
-- ==============================================================================

ALTER TABLE transferegov_schema.tb_sincronizacao_convenio 
    ALTER COLUMN uf TYPE VARCHAR(2);

ALTER TABLE transferegov_schema.tb_emenda_especial_plano_acao 
    ALTER COLUMN uf_beneficiario TYPE VARCHAR(2);
