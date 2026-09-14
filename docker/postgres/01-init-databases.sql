-- Script executado na primeira inicialização do container PostgreSQL (/docker-entrypoint-initdb.d/)

-- 1. Criação da base de dados dedicada para a Evolution API (isolamento do Prisma ORM)
SELECT 'CREATE DATABASE evolution'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'evolution')\gexec

-- 2. Concede permissões para o usuário padrão
GRANT ALL PRIVILEGES ON DATABASE evolution TO postgres;

-- 3. Criação das extensões essenciais na base padrão (govflow)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 4. Criação dos Schemas de Domínio do GovFlow
CREATE SCHEMA IF NOT EXISTS core_schema;
CREATE SCHEMA IF NOT EXISTS whatsapp_schema;
CREATE SCHEMA IF NOT EXISTS transferegov_schema;

-- Permissões nos schemas
GRANT ALL ON SCHEMA core_schema TO postgres;
GRANT ALL ON SCHEMA whatsapp_schema TO postgres;
GRANT ALL ON SCHEMA transferegov_schema TO postgres;
