# GovFlow Core Domain

Copiloto de gestão operacional do Transferegov para consultorias municipais, cobrindo a recepção de documentos fiscais, auditoria de convênios federais e liquidação de contratos administrativos de obras e serviços ao longo de todo o ciclo de vida (Fases 0 a 9).

## Language

### Entes e Agentes

**Prefeitura**:  
O ente federativo municipal convenente atendido pela consultoria de gestão pública.  
_Avoid_: Cliente, Cidade, Município contratante, Órgão local.

**Consultoria**:  
A empresa privada prestadora de serviços técnicos em gestão municipal detentora da assinatura do software (Tenant).  
_Avoid_: Empresa usuária, Escritório, Agência.

**Administrador**:  
O usuário gestor responsável pela conta da Consultoria. Possui prerrogativa de cadastrar Prefeituras atendidas, cadastrar e gerenciar Agentes, alocar vínculos de prefeituras e configurar números de celular operacionais.  
_Avoid_: Superuser genérico, Root, Dono do sistema.

**Agente**:  
O operador técnico e analista da consultoria alocado para gerenciar e auditar convênios de prefeituras específicas. Possui número de celular cadastrado (utilizado para interações no WhatsApp, envio/recebimento de documentos e recebimento de notificações críticas) e opera com visão restrita às prefeituras vinculadas a ele.  
_Avoid_: Digitador, Contador, Usuário comum.

**Contato Externo**:  
Pessoa física externa à consultoria (secretário municipal, fiscal de engenharia, engenheiro da empreiteira, fornecedor) que interage via WhatsApp e canais digitais. Não possui vínculo 1:1 com prefeitura ou convênio: um mesmo contato pode atuar em múltiplos convênios (1:N) de uma ou mais prefeituras.  
_Avoid_: Contato da prefeitura, Usuário externo fixo.

**Fiscal de Obras**:  
O engenheiro ou técnico municipal designado por portaria para atestar boletins de medição e relatórios fotográficos.  
_Avoid_: Engenheiro da obra, Mestre de obras, Secretário.

**Mandatária**:  
A instituição financeira pública oficial (exclusivamente Caixa Econômica Federal / GIGOV ou Banco do Brasil) delegada pela União para operacionalizar e auditar contratos de repasse de engenharia.  
_Avoid_: Banco repassador, Agência da Caixa, Gerente.

---

### Arquitetura de Módulos (Bounded Contexts)

**Módulo 1: Identidade, Organização e Acesso (IAM & Onboarding)**:  
Gestão do ciclo de vida da Consultoria (Tenant), Administradores, Agentes (com seus números de celular e vínculos N:N com Prefeituras), autenticação robusta (BCrypt + JWT) e isolamento multi-tenant de dados.

**Módulo 2: GED & Ficheiro Digital do Convênio**:  
Sistema oficial de gestão documental eletrônica estruturado por Convênio e Fases do Ciclo de Vida (Fases 0 a 9). Substitui integralmente a pasta do Windows das consultorias, suportando todos os tipos documentais (engenharia, licitações, medições, financeiro, aditivos e prestação de contas) com versionamento, tags, busca textual, preview inline e download em lote.

**Módulo 3: Comunicação & WhatsApp (Roteamento 1:N & IA de Contexto)**:  
Camada de mensageria omnicanal. Desacopla contatos de prefeituras fixas (relação 1:N convênios). Implementa ingestão agnóstica na entrada (áudios, textos e arquivos de qualquer tipo), com IA que analisa o histórico recente da conversa para montar o contexto de inferência, e classificação na saída com triagem rápida na Inbox do Agente em caso de ambiguidade.

**Módulo 4: Cockpit de Ciclo de Vida & Hub Operacional**:  
Interface central e intuitiva de acompanhamento do convênio. Organiza o fluxo em torno de cada uma das 10 Fases, provendo para cada fase: 1) Checklist de Pendências / Travas Legais; 2) Ficheiro Digital da Fase; e 3) Cronômetro regressivo de Prazos Fatais e ações recomendadas.

**Módulo 5: Integração Transferegov & Inteligência**:  
Mecanismos de sincronização e ingestão batch/webhook de dados abertos do portal Transferegov.br e pipelines de inteligência artificial especializada.

---

### Habilitação e Pré-Convênio (Fase 0)

**CertidaoCauc**:  
Cada uma das 16 exigências fiscais e orçamentárias mandatórias do art. 25 da LRF monitoradas pelo Radar CAUC com data de emissão e validade individualizadas (Receita Federal/PGFN, FGTS, CNDT, RREO, RGF, SICONFI, limites de pessoal, saúde e educação).  
_Avoid_: Status do CAUC, Certidão genérica, Folha corrida municipal.

**ImpedimentoTecnico**:  
A objeção formal registrada pela Secretaria de Orçamento Federal (SOF/MPO) no SIOP e Transferegov quando uma emenda parlamentar ou proposta municipal possui inconsistências técnicas ou legais não sanadas no prazo fatal de 15 a 30 dias, gerando cancelamento orçamentário.  
_Avoid_: Trava comum, Pendência leve, Erro de digitação.

**GND**:  
Grupo de Natureza de Despesa da classificação orçamentária federal (GND 3 para Despesas Correntes/Custeio e GND 4 para Investimentos/Capital e Obras). A proposta municipal deve ser estritamente compatível com o GND da emenda sob pena de impedimento técnico imediato.  
_Avoid_: Tipo de dinheiro, Categoria solta.

**ContrapartidaLDO**:  
O percentual e montante financeiro mínimo que a prefeitura é legalmente obrigada a aportar com recursos próprios na execução do convênio, apurado conforme as faixas populacionais e critérios regionais da Lei de Diretrizes Orçamentárias federal e garantido por dotação na LOA municipal.  
_Avoid_: Aporte voluntário, Ajuda do município.

**DossieDeclaracoes**:  
O conjunto documental padronizado obrigatório instruído pela consultoria (Capacidade Técnica, Previsão Orçamentária na LOA, Não Duplicidade, Sustentabilidade, Transparência) assinado digitalmente pelo Prefeito e Contador via Gov.br para anexação no Transferegov.  
_Avoid_: Papelada, Declarações avulsas.

---

### Instrumentos, Contratos e Governança

**Convenio**:  
O instrumento jurídico de transferência voluntária ou especial firmado entre a União (Ministério/Caixa) e a Prefeitura no portal Transferegov.br.  
_Avoid_: Contrato federal, Acordo, Termo genérico.

**ContratoExecucao**:  
O contrato administrativo municipal decorrente de processo licitatório firmado entre a Prefeitura e a empresa contratada/empreiteira (Lei nº 14.133/2021).  
_Avoid_: Contrato do Transferegov, Convênio da obra, Acordo comercial.

**ClausulaSuspensiva**:  
A condição legal imposta ao convênio federal exigindo superação de projetos de engenharia, licenças ambientais e titularidade do terreno em prazo fatal sob pena de rescisão e perda do repasse.  
_Avoid_: Pendência simples, Exigência burocrática, Trava.

**CondicionanteSuspensiva**:  
Cada um dos três pilares técnicos obrigatórios perante a Caixa para superação da cláusula suspensiva: Projetos/Orçamento SINAPI (SPA/LAE), Licenciamento Ambiental e Titularidade do Imóvel.  
_Avoid_: Documento da Caixa, Exigência solta.

**TermoAditivo**:  
O instrumento formal bilateral para prorrogação de vigência ou alteração de valor/metas, registrado no Transferegov ou no contrato municipal.  
_Avoid_: Prorrogação informal, Renovação, Ajuste simples.

---

### Licitação e Autorizações Federais (Fase 3)

**Licitacao**:  
O certame ou procedimento formal de contratação pública municipal (Pregão, Concorrência, Dispensa, Inexigibilidade regido pela Lei nº 14.133/2021) vinculado ao convênio federal, cujo resultado é submetido ao Concedente para homologação (VRPL) antes da emissão da AIO. Um convênio pode comportar múltiplos certames independentes (relação 1:N).  
_Avoid_: Edital solto, Compra direta genérica, Licitação isolada sem vínculo.

**VRPL**:  
Verificação do Resultado do Processo Licitatório realizada pela Caixa ou Ministério para atestar a conformidade do certame municipal com a SPA e a preservação do desconto.  
_Avoid_: Análise de licitação, Checagem de edital.

**AIO**:  
Autorização de Início de Objeto formal expedida pelo Concedente permitindo o início da execução física da obra após aprovação da licitação.  
_Avoid_: Liberação de obra, Autorização genérica.

---

### Execução Física e Engenharia (Fase 4)

**Medicao**:  
A apuração periódica cumulativa dos serviços físicos executados pela contratada em determinado intervalo de tempo, atestada formalmente pelo fiscal.  
_Avoid_: Parcela, Fatura, Etapa solta.

**RAE**:  
Relatório de Acompanhamento de Engenharia emitido pelo engenheiro fiscal da Caixa Econômica Federal chancelando o percentual de avanço físico da intervenção.  
_Avoid_: Laudo da Caixa, Vistoria simples.

---

### Alterações Contratuais e Governança (Fase 6)

**TermoAditivo**:  
Instrumento formal bilateral para alteração de vigência, valor (acréscimo/supressão até os limites de 25% da Lei nº 14.133/2021) ou reprogramação do plano de trabalho com prévia anuência técnica da Caixa GIGOV.  
_Avoid_: Emenda contratual, Prorrogação informal, Ajuste de contrato.

**ApostilamentoReajuste**:  
Ato unilateral da Administração Pública para aplicação de reajuste anual por índices oficiais (INCC/IPCA após 12 meses) previsto no edital/contrato, dispensando termo aditivo formal.  
_Avoid_: Aditivo de reajuste, Repactuação genérica.

---

### Execução Financeira e Liquidação (Fase 5)

**DocumentoHabil**:  
O documento fiscal formal (NFS-e, NF-e ou recibo legal) cadastrado no Transferegov contendo discriminação exata de valores brutos e retenções tributárias.  
_Avoid_: Nota fiscal comum, Boleto, Comprovante de despesa.

**RetencaoTributaria**:  
O montante financeiro deduzido do valor bruto da nota fiscal relativo a obrigações legais (INSS, ISS, IRRF - Tema 1.130/STF) a ser recolhido pela Prefeitura via guia própria (DARF/DAM).  
_Avoid_: Desconto, Glosa, Abatimento.

**OrdemPagamento (OBTV)**:  
O comando de liquidação financeira e pagamento bancário exclusivamente eletrônico emitido a partir da conta vinculada (Op 006) com assinatura em duplo comando (Prefeito + Secretário de Finanças).  
_Avoid_: TED, Pix avulso, Cheque, Transferência bancária comum.

---

### Prestação de Contas, Encerramento e Passivo (Fases 7 a 9)

**TermoRecebimentoDefinitivo**:  
Ato lavrado por comissão municipal após vistoria técnica e período de observação (até 90 dias - art. 140 da Lei nº 14.133/2021), atestando a perfeita entrega da obra para fins de prestação de contas.  
_Avoid_: Recebimento simples, Termo de entrega informal.

**RelatorioCumprimentoObjeto (RCO)**:  
Peça formal protocolada no Transferegov no prazo improrrogável de até 60 dias após a vigência, contendo fotos georreferenciadas da obra funcional com placa de inauguração e conciliação bancária integral.  
_Avoid_: Prestação de contas genérica, Balancete final.

**EncerramentoContaOp006**:  
Procedimento bancário compulsório na Caixa/BB após recolhimento dos saldos remanescentes e da totalidade dos rendimentos via Guia de Recolhimento da União (GRU), comprovando saldo R$ 0,00.  
_Avoid_: Conta zerada, Fechamento de agência.

**NotificacaoSELIC45Dias**:  
Notificação expedida pelo Concedente assinalando o prazo fatal e improrrogável de 45 dias para regularização de pendência ou recolhimento do débito atualizado pela taxa SELIC antes da trava no CAUC.  
_Avoid_: Cobrança de dívida, Notificação simples.

**Sumula230TCU**:  
Mecanismo jurídico pelo qual o prefeito sucessor ajuíza Ação de Ressarcimento ao Erário ou Representação no MPF contra o ex-prefeito omisso, obtendo a suspensão imediata da inadimplência do município no SIAFI/CAUC.  
_Avoid_: Desbloqueio de certidão, Ação contra prefeitura.

**TomadaDeContasEspecial (TCE)**:  
Processo administrativo formal de exceção (IN TCU nº 71/2012) instaurado pelo Concedente e julgado pelo TCU para ressarcimento de dano ao erário, aplicação de multas e declaração de inelegibilidade pessoal do gestor.  
_Avoid_: Processo administrativo comum, Auditoria de rotina.

---

### GED & Ficheiro Digital do Convênio (Módulo 2)

**FicheiroDigital**:  
Repositório eletrônico e visão agregada oficial de todos os arquivos e peças processuais vinculados a um Convênio, estruturado e organizado de forma determinística nas 10 Fases do Ciclo de Vida da Transferência Voluntária. Substitui em definitivo a gestão dispersa de pastas locais do Windows das consultorias.  
_Avoid_: Repositório de arquivos avulsos, Pasta do convênio, Drive compartilhado.

**FaseCicloVida**:  
Cada uma das 10 etapas oficiais pelas quais um instrumento formal de transferência voluntária ou especial transita: Fase 00 (Proposta e Plano de Trabalho), Fase 01 (Celebração e Formalização), Fase 02 (Cláusula Suspensiva e Engenharia), Fase 03 (Licitação e Contratação), Fase 04 (Execução Física e Medições), Fase 05 (Execução Financeira e Pagamentos), Fase 06 (Alterações Contratuais e Aditivos), Fase 07 (Prestação de Contas Final - RCO), Fase 08 (Encerramento e Devolução de Saldo) e Fase 09 (Passivo Jurídico e TCE).  
_Avoid_: Etapa de projeto, Passo de cadastro, Fase informal.

**CategoriaDocumento**:  
Taxonomia e classificação arquivística formal atribuída ao documento (ex: `PROPOSTA_PLANO_TRABALHO`, `TERMO_CONVENIO`, `PROJETO_ENGENHARIA`, `DOCUMENTO_HABIL`, `ORDEM_BANCARIA_OBTV`, `BOLETIM_MEDICAO`, etc.), governando regras de conformidade, indexação por IA e sugestão de pasta no repositório.  
_Avoid_: Tipo de anexo, Extensão de arquivo.

**PastaFase**:  
A pasta virtual raiz associada a uma Fase do Ciclo de Vida dentro do Ficheiro Digital do convênio, contendo contadores atômicos de documentos arquivados, volumetria consolidada em bytes e a coleção de documentos ativos.  
_Avoid_: Diretório do disco, Subpasta solta.

**DocumentoAuditoria**:  
Trilha imutável de auditoria registrada em tabela própria (`tb_documentos_auditoria`) para cada evento de ciclo de vida do documento (upload, classificação por IA, movimentação entre fases/pastas, substituição ou exclusão lógica), registrando tenant, usuário/agente operador, timestamp, justificativa e estado anterior/novo.  
_Avoid_: Log de sistema descartável, Histórico simples.

**HashSha256**:  
Resumo criptográfico calculado em tempo real durante o streaming de upload no MinIO/S3 e gravado na base de dados para atestar a integridade e não repúdio do documento municipal.  
_Avoid_: Checksum simples, ID do arquivo.

**StreamingZip**:  
Processo não bloqueante de exportação em lote (dossiê completo ou pasta de fase específica) transmitido diretamente na resposta HTTP via `StreamingResponseBody`, sem bufferização de grandes payloads na memória Heap da JVM e preservando a árvore oficial de pastas SICONV.  
_Avoid_: Download de zip em memória, Compactação estática em disco temporário.

**StatusDocumento**:  
O estado do ciclo de vida documental na esteira e no Ficheiro Digital (`RECEBIDO`, `EM_ANALISE_IA`, `CLASSIFICADO`, `ARQUIVADO`, `REJEITADO`). Indica aos agentes e consultores o nível de processamento e prontidão do arquivo.  
_Avoid_: Estado solto, Flag de arquivo.

**EmAnaliseIA**:  
O estado transitório ativo no qual o motor de inteligência artificial multimodal (OCR + LLM) está identificando metadados, fase e campos fiscais de um arquivo recém-recebido, permitindo auditoria humana concorrente e exibindo o nome original do arquivo com badge indicador visual.  
_Avoid_: Arquivo travado, Processando genérico.

**ContatoVinculado**:  
Contato externo cadastrado e ativo em `tb_contatos` que possui vínculo formal comprovado com um ou mais convênios em `tb_contato_convenios`. Apenas documentos e mídias originados de Contatos Vinculados são admitidos para extração por IA ou encaminhados à Caixa de Triagem; quaisquer arquivos de números desconhecidos ou não vinculados são sumariamente descartados na borda.  
_Avoid_: Contato solto, Remetente avulso, Número qualquer.

**AuditorPolimorfico**:  
A interface side-by-side de conferência documental (`/documentos/:id/revisar`) que adapta dinamicamente seus pilares de integridade, formulários de extração e cálculos conforme a Categoria Documental e Fase do Ciclo de Vida: exibe campos fiscais e retenções tributárias para `DOCUMENTO_HABIL` (Fase 05) e aferição físico-financeira, ART e atesto técnico para `BOLETIM_MEDICAO` (Fase 04).  
_Avoid_: Tela engessada de nota fiscal, Formulário único para tudo.

**BoletimMedicao**:  
Documento oficial comprobatório da execução física de obras e serviços de engenharia (Fase 04), composto por número do boletim, contrato municipal, período medido, apuração físico-financeira acumulada, percentual executado e atesto formal com ART do engenheiro fiscal. Não possui retenções tributárias diretas nem exige nota de empenho na sua conferência física.  
_Avoid_: Nota fiscal de medição, Relatório informal de obra.

**NotificacaoTempoReal**:  
Infraestrutura reativa no frontend (via `NotificacaoDocumentoService`) com polling a cada 5 segundos, sinos dinâmicos no cabeçalho com atalho direto de auditoria ("Auditar Lado a Lado"), badges vivos com contadores de pendências na barra lateral (Esteira e Triagem), síntese sonora via Web Audio API e toasts de alerta imediato quando novos documentos ou ambiguidades são processados.  
_Avoid_: Polling pesado bloqueante, Alerta estático manual.

---

### Estado Atual dos Módulos (Milestones)

- **Módulo 1: Identidade, Organização e Acesso (IAM & Onboarding)**: ✅ **100% Concluído e Validado** (Migrações Flyway, Auth JWT, isolamento Multi-Tenant e vínculos N:N).
- **Módulo 2: GED & Ficheiro Digital do Convênio**: ✅ **100% Concluído e Validado na Interface Real** (Tickets GED-01 a GED-08, MinIO/S3, Presigned URLs, streaming de ZIP, Explorer das 10 Fases e Auditor Side-by-Side Polimórfico de Engenharia e Fiscal).
- **Módulo 3: Comunicação Omnicanal, WhatsApp & IA de Contexto**: ✅ **100% Concluído e Validado** (Tickets WPP-01 a WPP-06, Contatos 1:N, Ingestão Agnóstica, Contexto Multimodal, Caixa de Triagem e Feedback Reativo em Tempo Real para o Agente).
- **Módulo 4: Cockpit de Ciclo de Vida & Hub Operacional**: 🚀 **Próximo Módulo** (Esteira Kanban das 10 Fases, Alertas CAUC e Linha do Tempo).
- **Módulo 5: Integração Transferegov & Inteligência**: ⏳ Na fila de execução.

