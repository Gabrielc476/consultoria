# Módulo 4: Cockpit de Ciclo de Vida & Hub Operacional

## 1. Visão Geral e Responsabilidades
O Módulo de Cockpit de Ciclo de Vida é a camada de experiência do usuário (Frontend Angular + Backend Orchestration) onde os analistas e consultores passam a maior parte do seu dia de trabalho.

### Principais Dores Solucionadas
1. **Interface Desconexa e Pouco Intuitiva**: O layout anterior utilizava abas genéricas com listas desconectadas de documentos e dados mockados em código, sem oferecer uma visão clara do que está travando o convênio.
2. **Dados Mockados (`MOCK_DOCS`, `MOCK_PRAZOS`)**: Substituição completa dos arrays estáticos no frontend por chamadas reativas a endpoints reais que consultam o banco de dados e o Ficheiro Digital (GED).
3. **Foco no "Dossiê da Fase"**: Ao selecionar qualquer uma das 10 Fases da esteira, a interface abre o **Hub Operacional da Fase**, congregando em uma única tela:
   - **Checklist de Travas Legais** (o que precisa ser cumprido para superar a fase).
   - **Ficheiro da Fase** (arquivos anexados, validados ou pendentes do GED).
   - **Prazos Fatais Regressivos** (cronômetro de dias restantes antes de perda de repasse ou trava no CAUC).
   - **Barra de Ações Rápidas** (Upload direto, aprovação técnica e disparo de mensagens no WhatsApp).

---

## 2. As 10 Fases Operacionais do Convênio (Ciclo de Vida)

| Fase | Denominação Oficial | Principais Travas e Condicionantes Legais | Documentos Mandatórios no GED |
| :--- | :--- | :--- | :--- |
| **00** | **Habilitação & Proposta** | Regularidade das 16 certidões do CAUC (art. 25 LRF), compatibilidade de GND (GND 3 vs GND 4), contrapartida da LDO. | Dossiê de Declarações Gov.br, Certidões CAUC, Proposta no Transferegov. |
| **01** | **Celebração & Formalização** | Publicação no DOU, abertura da Conta Vinculada exclusiva (Op 006). | Termo de Convênio/Repasse assinado, Notificação bancária Caixa/BB. |
| **02** | **Cláusula Suspensiva** | Cronômetro fatal de **180 dias** da Caixa GIGOV: Projetos SINAPI/BDI, Licenças Ambientais, Titularidade do Imóvel. | SPA (Síntese do Projeto Aprovado), LAE, Plantas, ART/RRT, Matrícula. |
| **03** | **Licitação & Contratação** | Lei nº 14.133/2021, homologação do resultado pela Mandatária (VRPL), emissão da AIO. | Edital, Parecer Jurídico, Atas, Contrato Administrativo, Parecer VRPL, AIO. |
| **04** | **Execução Física & Medições** | Atesto pelo fiscal municipal, compatibilidade de avanço físico com cronograma, emissão do RAE pela Caixa. | Boletins de Medição (BM), Relatório Fotográfico Georreferenciado, Diário de Obra, RAE. |
| **05** | **Execução Financeira & OBTV** | Conferência de Documento Hábil (retenções tributárias IRRF/INSS/ISS), emissão de OBTV em duplo comando. | Notas Fiscais (NF-e/NFS-e), Guias DAM/DARF, Comprovantes de Ordem Bancária (OBTV). |
| **06** | **Aditivos & Alterações** | Limite legal de 25% (acréscimo/supressão), repactuação de metas, apostilamento de reajuste anual (INCC/IPCA). | Termos Aditivos assinados, Parecer Técnico de Reengenharia, Apostilamentos. |
| **07** | **Prestação de Contas Final** | Vistoria final (art. 140 Lei 14.133/21), prazo fatal de **60 dias** após término da vigência para envio do RCO. | Termo de Recebimento Definitivo, Relatório de Cumprimento do Objeto (RCO), Foto da Placa. |
| **08** | **Encerramento & Saldos** | Recolhimento de saldo remanescente e rendimentos de aplicação via GRU, comprovação de saldo R$ 0,00. | Comprovante de recolhimento da GRU, Extrato bancário final zerado. |
| **09** | **Passivo & Notificações** | Prazo fatal de **45 dias** da Notificação SELIC antes de inscrição no CAUC/SIAFI; instrução de Súmula 230 TCU ou TCE. | Notificações do Concedente, Guias SELIC, Petições de Ação de Ressarcimento (Súmula 230 TCU). |

---

## 3. Arquitetura da Interface do Cockpit (Frontend Angular 19+)

### 3.1 Componentes e Layout
```
+---------------------------------------------------------------------------------------------------+
| HEADER GLOBAL: [Consultoria XPTO]  |  [Seletor de Prefeituras]  |  [Status WhatsApp]  | [Perfil] |
+---------------------------------------------------------------------------------------------------+
| CONTEXTO DO CONVÊNIO:                                                                             |
| Convênio: SICONV #954120/2026 - Pavimentação Asfáltica Bairro Centro (Esperança - PB)            |
| Concedente: Ministério das Cidades / Caixa GIGOV  |  Valor: R$ 1.500.000,00  |  Vigência: 12/2027|
+---------------------------------------------------------------------------------------------------+
| ESTEIRA DE FASES (Phase Stepper):                                                                 |
| [✓ 00] -> [✓ 01] -> [● 02: Cláusula Suspensiva (54 dias rest.)] -> [ 03 ] -> [ 04 ] -> ...      |
+---------------------------------------------------------------------------------------------------+
| HUB OPERACIONAL DA FASE SELECIONADA (Fase 02):                                                    |
|                                                                                                   |
|  [COLUNA 1: CHECKLIST DE TRAVAS LEGAIS]    |  [COLUNA 2: FICHEIRO DIGITAL DA FASE]                |
|  - [✓] Projeto de Engenharia e Orçamento   |  Documentos anexados nesta fase:                     |
|        Aprovado pela Caixa (SPA emitido)   |  📄 Memorial_Descritivo_Rev02.pdf (12 MB) [Preview]  |
|  - [!] Licença Ambiental de Instalação     |  📄 Planilha_Orcamentaria_SINAPI.xlsx (1.4 MB)       |
|        STATUS: Vencendo em 12 dias         |  📄 ART_Engenheiro_Responsavel.pdf [Validado]        |
|  - [X] Titularidade do Imóvel              |                                                      |
|        STATUS: Pendente Certidão de Ônus   |  [+ Fazer Upload de Novo Documento nesta Fase]       |
|                                            |                                                      |
|  -----------------------------------------------------------------------------------------------  |
|  [LINHA INFERIOR: PRAZOS FATAIS & AÇÕES RÁPIDAS]                                                  |
|  ⏳ 54 dias restantes para rescisão unilateral do repasse pela Caixa GIGOV                       |
|  [Botão: Notificar Engenheiro via WhatsApp]  |  [Botão: Solicitar Prorrogação de Prazo no SICONV]  |
+---------------------------------------------------------------------------------------------------+
```

---

## 4. Endpoints de Suporte ao Cockpit (Backend `core-service`)

- `GET /api/v1/convenios/{id}/cockpit`:
  - Retorna o status consolidado do convênio, a fase ativa atual, a lista das 10 fases com percentual de conclusão e indicadores gerais de risco.
- `GET /api/v1/convenios/{id}/fases/{numeroFase}/dossie`:
  - Retorna o payload completo da fase: checklist de pendências, itens de trava legal, documentos associados do GED e alertas de prazos.
- `PUT /api/v1/convenios/{id}/fases/{numeroFase}/itens/{itemId}/status`:
  - Atualiza o status de um item do checklist (ex: `SUPERADO`, `PENDENTE`, `EM_ANALISE_CAIXA`).
- `POST /api/v1/convenios/{id}/fases/{numeroFase}/notificar-contato`:
  - Dispara mensagem automática pré-formatada via WhatsApp para o contato externo responsável pela pendência.

---

## 5. Auditor Side-by-Side Multidocumental (Especialização Dinâmica por Tipo de Documento)

O recurso de **Auditoria Lado a Lado (Human-in-the-Loop)** — onde o documento original é exibido à esquerda e o formulário com checklist de auditoria à direita — deixa de ser exclusivo de Notas Fiscais e é generalizado para atender a todos os documentos críticos do ciclo de vida:

```
+---------------------------------------------------------------------------------------------------+
| AUDITOR SIDE-BY-SIDE MULTIDOCUMENTAL: [Documento: BM_03_Pavimentacao.pdf]  | Categoria: MEDICAO   |
+-------------------------------------------------------------------+-------------------------------+
| VISUALIZADOR DE MÍDIA & BOUNDING BOXES (Esquerda)                 | FORMULÁRIO DINÂMICO & AUDITORIA|
|                                                                   | (Direita - Adaptável por Tipo)|
| +---------------------------------------------------------------+ |                               |
| | PREFEITURA MUNICIPAL DE ESPERANÇA                             | | 📋 DADOS EXTRAÍDOS PELA IA:   |
| | BOLETIM DE MEDIÇÃO Nº 03                                      | | Número BM: [ BM-03/2026   ] |
| | [Contrato: 045/2026] [Empresa: Construtora Alvorada Ltda]     | | Período: [01/09 a 30/09/26] |
| | Valor Medido: R$ 125.400,00                                   | | Valor Medido: [R$ 125.400]  |
| | Avanço Físico Acumulado: 45.2%                                | | % Acumulado: [ 45.2%      ] |
| |                                                               | | Fiscal: [ Eng. Roberto C. ] |
| | [Assinado Digitalmente por Eng. Roberto Carlos - CREA 12345]  | | CREA: [ 12345-D/PB        ] |
| +---------------------------------------------------------------+ | ----------------------------- |
|                                                                   | 🔍 CHECKLIST DE AUDITORIA IA: |
|                                                                   | [✓] Atesto do Fiscal Presente |
|                                                                   | [✓] Valor dentro do Saldo Cont|
|                                                                   | [!] Avanço físico 3% abaixo do|
|                                                                   |     cronograma previsto Caixa |
|                                                                   | ----------------------------- |
|                                                                   | [ Salvar e Atualizar Medição] |
+-------------------------------------------------------------------+-------------------------------+
```

### 5.1 Especializações de Extração e Checklist por Categoria Documental

| Categoria Documental | Campos Extraídos pela IA (JSONB / Relacional) | Checklist e Regras de Validação Automatizada da IA | Efeito da Aprovação no Ciclo de Vida |
| :--- | :--- | :--- | :--- |
| **Boletim de Medição (BM)** | Número do BM, Período de Medição, Contrato Vinculado, Valor do Período, Percentual Físico Acumulado (%), Fiscal Designado, Registro CREA. | - Assinatura/atesto do fiscal municipal válida.<br/>- Valor do período não excede saldo do contrato.<br/>- Comparativo com cronograma físico da Caixa. | Alimenta a entidade `tb_medicoes` do convênio e libera a fase para emissão da NF e OBTV (Fase 05). |
| **ART / RRT** | Número da ART, Conselho Regional (CREA/CAU), Engenheiro/Arquiteto Responsável, RNP, Objeto da Obra, Endereço, Quitação da Taxa. | - Engenheiro confere com a portaria municipal.<br/>- Comprovante de pagamento bancário da taxa anexado e quitado. | Supera o pilar **1. Projetos de Engenharia** da Cláusula Suspensiva (Fase 02). |
| **Licença Ambiental (LP, LI, LO)** | Órgão Emissor (SUDEMA/IBAMA), Tipo de Licença, Número, Data de Emissão, Data de Validade, Condicionantes Ambientais. | - Licença vigente (data de validade > data atual).<br/>- Alerta se faltar menos de 60 dias para o vencimento. | Supera o pilar **2. Licenciamento Ambiental** da Cláusula Suspensiva (Fase 02). |
| **Certidões CAUC (Fase 00)** | Órgão Emissor, Número da Certidão, Data de Emissão, Data de Validade, Código de Controle de Autenticidade, Situação. | - Validação de regularidade fiscal.<br/>- Vigência atende à janela fatal de repasse. | Atualiza o indicador do item correspondente no **Radar CAUC**. |
| **Contrato Administrativo** | Número do Contrato, Processo Licitatório, Razão Social e CNPJ da Contratada, Valor Global, Vigência Contratual. | - Compatibilidade de valor com o limite homologado na licitação e na SPA da Caixa. | Registra a contratação formal (Fase 03) para início do acompanhamento de medições. |
| **Documento Hábil (NF-e/NFS-e)** | Número, Série, Chave 44 dígitos, Data de Emissão, CNPJ Credor, Valor Bruto, Retenções (INSS, ISS, IRRF), Valor Líquido. | - Validação matemática: `Bruto - Deduções == Líquido`.<br/>- Chave da NF-e válida e consultável na SEFAZ. | Vincula à medição correspondente e gera a minuta da Ordem de Pagamento OBTV (Fase 05). |

### 5.2 Persistência e Propagação de Dados Validados

1. **Repositório Central (`tb_documentos`)**:
   - Todo documento aprovado grava seu payload completo validado no campo `metadados_json` (JSONB) da `tb_documentos`.
2. **Propagação Automática de Domínio**:
   - Ao aprovar um documento de medição: o sistema atualiza automaticamente a linha correspondente em `core_schema.tb_medicoes`.
   - Ao aprovar ART, Licença Ambiental ou Matrícula de Imóvel: o sistema marca automaticamente o pilar correspondente como `SUPERADO` em `core_schema.tb_condicionantes_suspensivas`, avançando o progresso da Fase 02.
   - Ao aprovar Documento Hábil: persiste em `core_schema.tb_documentos_habeis_dados` com retenções tributárias discriminadas para posterior emissão da guia DAM/DARF.

---

## 6. Roteiro de Teste Manual Passo a Passo (Playbook Operacional do Módulo 4)

### 6.1 Teste Manual de Eliminação de Mocks e Consulta do Dossiê Real
1. **Verificação de Ausência de Dados Mockados**:
   - Abrir o navegador em `http://localhost:4200/convenios/{convenioId}/cockpit`.
   - Pressionar F12 e abrir a aba "Console".
   - *Validação*: Nenhuma ocorrência de avisos ou objetos de `MOCK_DOCS` ou `MOCK_PRAZOS`.
2. **Inspeção de Chamada do Dossiê na Aba Network**:
   - Constatar requisição `GET /api/v1/convenios/{id}/fases/02/dossie`.
   - *Validação*: Payload JSON retornado contendo dados reais do banco:
     - Situação das condicionantes suspensivas (Engenharia, Ambiental, Titularidade).
     - Lista real de arquivos anexados no GED para aquela fase.
     - Prazos fatais calculados dinamicamente com base nas datas de vigência do convênio.

### 6.2 Teste Manual de Navegação nas 10 Fases no Cockpit
1. **Navegação Interativa**:
   - Clicar sequencialmente nos chips da esteira:
     - Fase 00 (Radar CAUC & Proposta)
     - Fase 01 (Celebração e Publicação)
     - Fase 02 (Superação de Cláusula Suspensiva)
     - Fase 04 (Execução Física e Medições)
   - *Validação*: A interface atualiza o Dossiê Operacional instantaneamente, renderizando os cards de pendências, prazos e documentos específicos de cada fase.

### 6.3 Teste Manual de Auditoria Side-by-Side de Boletim de Medição (BM)
1. **Abrir Documento de Medição Pendente**:
   - Na lista de pendências da Fase 04, clicar no botão "Auditar Documento" de um Boletim de Medição recém-recebido.
2. **Conferência da Interface Side-by-Side**:
   - Lado Esquerdo: Visualizador de PDF com o arquivo original da medição digitalizado.
   - Lado Direito: Formulário dinâmico com campos extraídos pela IA:
     - Número do BM: `BM-03/2026`
     - Período: `01/09/2026 a 30/09/2026`
     - Valor Medido: `R$ 125.400,00`
     - % Físico Acumulado: `45.2%`
     - Nome do Fiscal: `Eng. Roberto Carlos`
     - CREA: `12345-D/PB`
3. **Checagem do Checklist de Validação da IA**:
   - `[✓] Atesto do Fiscal Presente`: ícone verde confirmando assinatura digital detectada no PDF.
   - `[✓] Valor Compatível com Saldo Contratual`: verificação automática contra o saldo do contrato.
4. **Aprovação e Propagação de Domínio**:
   - Clicar em "Aprovar e Atualizar Medição".
   - *Validação*: No banco de dados:
     ```sql
     SELECT id, numero_medicao, valor_medido, percentual_executado, situacao FROM core_schema.tb_medicoes WHERE convenio_id = '<ID_CONVENIO>';
     ```
     Constatar novo registro aprovado com valor `125400.00` e percentual `45.2`.
   - Ao retornar ao Cockpit, o gráfico de progresso físico do convênio avança de 32% para 45.2%.

### 6.4 Teste Manual de Superação de Cláusula Suspensiva (ART e Licença Ambiental)
1. **Auditar ART de Engenharia**:
   - Abrir no Side-by-Side uma ART enviada para a Fase 02.
   - Conferir se os campos de número de ART, profissional e quitação bancária foram extraídos.
   - Clicar em "Aprovar Documento".
2. **Auditar Licença Ambiental**:
   - Abrir no Side-by-Side a Licença de Instalação (LI).
   - Clicar em "Aprovar Documento".
3. **Conferência da Esteira Operacional**:
   - Consultar no banco:
     ```sql
     SELECT pilar, situacao, superado_em FROM core_schema.tb_condicionantes_suspensivas WHERE convenio_id = '<ID_CONVENIO>';
     ```
   - *Validação*: Os pilares `PROJETOS_ENGENHARIA` e `LICENCIAMENTO_AMBIENTAL` passam para `SUPERADO`.
   - No Cockpit, a Fase 02 exibe badge verde "2 de 3 Pilares Superados", restando apenas a Titularidade Dominial para conclusão integral da cláusula suspensiva.


