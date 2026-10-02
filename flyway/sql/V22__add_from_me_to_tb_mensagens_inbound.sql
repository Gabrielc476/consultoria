-- V22: Adicionar coluna from_me em tb_mensagens_inbound para suportar chat bidirecional na Central do WhatsApp
ALTER TABLE whatsapp_schema.tb_mensagens_inbound
ADD COLUMN IF NOT EXISTS from_me BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_mensagens_phone_created
ON whatsapp_schema.tb_mensagens_inbound (sender_phone, created_at ASC);
