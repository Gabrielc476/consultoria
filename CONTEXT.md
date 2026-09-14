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

**Analista**:  
O operador técnico da consultoria responsável por auditar documentos lado a lado e realizar lançamentos no Transferegov.  
_Avoid_: Digitador, Contador, Usuário comum.

**Fiscal de Obras**:  
O engenheiro ou técnico municipal designado por portaria para atestar boletins de medição e relatórios fotográficos.  
_Avoid_: Engenheiro da obra, Mestre de obras, Secretário.

**Mandatária**:  
A instituição financeira pública oficial (exclusivamente Caixa Econômica Federal / GIGOV ou Banco do Brasil) delegada pela União para operacionalizar e auditar contratos de repasse de engenharia.  
_Avoid_: Banco repassador, Agência da Caixa, Gerente.

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
