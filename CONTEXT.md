# GovFlow Core Domain

Copiloto de gestão operacional do Transferegov para consultorias municipais, cobrindo a recepção de documentos fiscais, auditoria de convênios federais e liquidação de contratos administrativos de obras e serviços.

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

---

### Instrumentos e Contratos

**Convenio**:  
O instrumento jurídico de transferência voluntária ou especial firmado entre a União (Ministério/Caixa) e a Prefeitura no portal Transferegov.br.  
_Avoid_: Contrato federal, Acordo, Termo genérico.

**ContratoExecucao**:  
O contrato administrativo municipal decorrente de processo licitatório firmado entre a Prefeitura e a empresa contratada/empreiteira.  
_Avoid_: Contrato do Transferegov, Convênio da obra, Acordo comercial.

**ClausulaSuspensiva**:  
A condição legal imposta ao convênio federal exigindo aprovação de projetos de engenharia ou licenças ambientais em prazo fatal sob pena de rescisão do repasse.  
_Avoid_: Pendência simples, Exigência burocrática, Trava.

---

### Execução e Liquidação Financeira

**Medicao**:  
A apuração periódica cumulativa dos serviços físicos executados pela contratada em determinado intervalo de tempo, atestada formalmente pelo fiscal.  
_Avoid_: Parcela, Fatura, Etapa solta.

**DocumentoHabil**:  
O documento fiscal formal (NFS-e, NF-e ou recibo legal) cadastrado no Transferegov contendo discriminação exata de valores brutos e retenções tributárias.  
_Avoid_: Nota fiscal comum, Boleto, Comprovante de despesa.

**RetencaoTributaria**:  
O montante financeiro deduzido do valor bruto da nota fiscal relativo a obrigações legais (INSS, ISS, IRRF) a ser recolhido pela Prefeitura.  
_Avoid_: Desconto, Glosa, Abatimento.

**OrdemPagamento**:  
O comando de liquidação financeira e pagamento bancário (OBTV) executado a partir da conta corrente vinculada do convênio para a conta da contratada.  
_Avoid_: TED, Pix, Transferência simples, Pagamento Caixa.
