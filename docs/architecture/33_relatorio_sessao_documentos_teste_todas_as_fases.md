# Relatório de Sessão Técnica: Criação dos Documentos de Teste das 10 Fases Oficiais do Ciclo de Vida

**Data**: 01/10/2026  
**Status**: Concluído  
**Módulos Impactados**: `documentos_teste/`, `services/ai-service`, `frontend`

---

## 1. Contexto e Objetivo
Para permitir a validação integral da esteira, do Ficheiro Digital, da classificação polimórfica da IA e do sistema de notificações em tempo real, foi construído um conjunto de documentos modelo oficiais, cobrindo integralmente as 10 Fases do Ciclo de Vida dos Convênios Federais (Fases 00 a 09) para o convênio ativo **nº 914250/2023** (Creche Proinfância - Patos/PB).

Cada documento foi elaborado no padrão visual governamental em HTML com layout de folha A4 e botão automático de impressão/salvamento em PDF (`@media print`), permitindo tanto a emissão manual pelo usuário para envio via WhatsApp quanto a ingestão por upload na esteira.

---

## 2. Acervo de Documentos Gerados

| Fase | Código Oficial | Categoria Documental | Arquivo Criado | Objeto / Finalidade |
|:---:|---|---|---|---|
| **00** | `FASE_00_PROPOSTA` | `PROPOSTA_PLANO_TRABALHO` | `fase_00_proposta_plano_de_trabalho.html` | Plano de Trabalho nº 012458/2023 com detalhamento de metas, justificativa técnica e cronograma de desembolso no SICONV. |
| **01** | `FASE_01_CELEBRACAO` | `TERMO_CONVENIO` | `fase_01_termo_de_convenio_celebrado.html` | Termo de Convênio formalizado com publicação no DOU Seção 3, conta bancária Op 006 e prazos de vigência. |
| **02** | `FASE_02_CLAUSULA_SUSPENSIVA` | `SPA_LAE_CAIXA` | `fase_02_laudo_engenharia_spa_lae_caixa.html` | Laudo de Análise de Engenharia (LAE/SPA) da Caixa GIGOV/JP aprovando projetos SINAPI, licença SUDEMA e superando a cláusula suspensiva. |
| **03** | `FASE_03_LICITACAO` | `CONTRATO_ADMINISTRATIVO` | `fase_03_contrato_administrativo_execucao.html` | Contrato nº 042/2024 de execução de obra com a Construtora Alvorada Ltda após homologação do certame e emissão de AIO Caixa. |
| **04** | `FASE_04_EXECUCAO_FISICA` | `BOLETIM_MEDICAO` | `fase_04_boletim_medicao_bm04.html` | 4º Boletim de Medição de Obras (BM-04) no valor de R$ 182.400,00 com atesto formal do Engenheiro Fiscal do Município. |
| **05** | `FASE_05_EXECUCAO_FINANCEIRA` | `DOCUMENTO_HABIL` | `fase_05_nota_fiscal_servicos_nfe.html` | Nota Fiscal Eletrônica NFS-e nº 2045 discriminando valor bruto, retenções de INSS/ISS e líquido de R$ 166.896,00. |
| **06** | `FASE_06_ALTERACOES_CONTRATUAIS` | `TERMO_ADITIVO` | `fase_06_termo_aditivo_prazo_vigencia.html` | 1º Termo Aditivo de Prorrogação de Vigência (+180 dias) fundamentado em laudo técnico de reprogramação da Mandatária Caixa. |
| **07** | `FASE_07_PRESTACAO_CONTAS` | `TERMO_RECEBIMENTO` | `fase_07_termo_recebimento_definitivo.html` | Termo de Recebimento Definitivo da Obra atestando 100% de conclusão física da Creche Proinfância para instruir o RCO. |
| **08** | `FASE_08_ENCERRAMENTO_FINANCEIRO` | `GUIA_RECOLHIMENTO_UNIAO` | `fase_08_gru_recolhimento_saldo_remanescente.html` | GRU Simples no valor de R$ 23.493,10 com autenticação bancária de liquidação e atesto de saldo zero na conta bancária vinculada. |
| **09** | `FASE_09_PASSIVO_JURIDICO` | `NOTIFICACAO_DILIGENCIA` | `fase_09_notificacao_diligencia_defesa.html` | Notificação formal nº 098/2026 da Diretoria Financeira do FNDE com apontamentos de diligência e prazo de 30 dias para manifestação. |

---

## 3. Hub Central e Atualização do AI Service
1. **Hub Central**: Criada a página `documentos_teste/index.html`, servida também em `http://localhost:4200/documentos-teste/index.html`, permitindo ao usuário navegar visualmente pelos documentos de cada fase.
2. **Classificador Multimodal Atualizado**: O pipeline do `ai-service` foi expandido em `document_pipeline.py` para reconhecer e categorizar deterministicamente os termos-chave de todas as 10 fases do ciclo de vida.
