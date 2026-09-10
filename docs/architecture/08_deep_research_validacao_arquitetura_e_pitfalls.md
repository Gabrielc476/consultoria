# Deep Research: Validação de Arquitetura, Pre-Mortem e Armadilhas de Implementação (Pitfalls)

**Projeto:** Plataforma GovFlow (Gestão e Automação do Transferegov para Consultorias)  
**Objetivo da Pesquisa:** Avaliar criticamente se a proposta arquitetural funciona na prática, identificar os modos de falha mais comuns em produção para cada componente e estabelecer as mitigações exatas.  
**Data:** Setembro de 2026  
**Veredito Geral:** **VIÁVEL COM RESSALVAS TÉCNICAS CRÍTICAS.** A arquitetura é robusta e moderna, mas possui **6 armadilhas operacionais clássicas** que precisam de blindagem específica para não quebrar em produção.

---

## 1. Sumário Executivo: A Proposta Funciona?

| Componente | Avaliação de Viabilidade | Risco Principal | Veredito |
| :--- | :---: | :--- | :---: |
| **Spring Cloud Gateway** | **9.0 / 10** | Bloqueio acidental do Event Loop do Netty (Thread Starvation). | **Funciona**, desde que 100% reativo e sem regras de negócio pesadas. |
| **Core Service (Hexagonal / DDD)** | **9.5 / 10** | Perda de contexto de `TenantId` do Hibernate 6 em threads assíncronas do RabbitMQ. | **Funciona perfeitamente**, requer propagação de tenant em headers AMQP. |
| **Transferegov Service (CSV ETL)** | **8.8 / 10** | Quebra de parser em strings multilinha com quebra de linha (`\n`) dentro de aspas. | **Funciona**, exige parser RFC 4180 em streaming e staging table com hash diff. |
| **WhatsApp Service (Evolution API)** | **8.2 / 10** | Quedas de sessão do Baileys e perda de conexões QR Code. | **Funciona para MVP**, requer health check automático e fallback para Meta API. |
| **AI Service (Python / Gemini 3.x)** | **9.2 / 10** | Alucinação de centavos em fotos borradas e estouro de prompt em Active Learning. | **Funciona com louvor**, exige auditoria matemática e poda de regras Few-Shot. |
| **Extensão Chrome (Manifest V3)** | **9.0 / 10** | Desserialização de eventos em inputs mascarados do governo e sono do Service Worker. | **Funciona**, blindado com Shadow DOM, eventos sintéticos e fallback de clipboard. |

---

## 2. Diagnóstico Aprofundado de Falhas e Pitfalls por Componente

---

### Componente 1: Spring Cloud Gateway (Clean Architecture)

#### O Problema mais Encontrado: O Bloqueio do Event Loop do Netty
* **Como a falha acontece:**  
  O Spring Cloud Gateway roda sobre o servidor **Netty** (reativo e não-bloqueante), que utiliza um número minúsculo de threads de I/O (geralmente `2 * núcleos de CPU`). Se um desenvolvedor colocar uma chamada bloqueante dentro de um filtro de autenticação ou caso de uso (ex: uma chamada JDBC tradicional, `Thread.sleep()`, ou um client HTTP síncrono `RestTemplate`), **a thread do Netty é travada**. Com apenas 20 a 50 requisições simultâneas, ocorre **Thread Starvation**: todo o gateway para de responder e o tempo de resposta sobe de 3ms para 30 segundos (Timeout geral).
* **A Armadilha do "Smart Gateway" (Anti-Pattern):**  
  Tentar implementar uma Clean Architecture "pesada" dentro do Gateway com entidades ricas de negócio e repositórios viola o princípio de gateway de borda. O Gateway deve cuidar apenas de segurança de tráfego, roteamento e correlação.

#### A Mitigação Arquitetural Obrigatória:
1. **Regra de Ouro Reativa:** Todas as operações no Gateway devem retornar `Mono<T>` ou `Flux<T>`. Nenhuma dependência bloqueante (JDBC, JPA clássico) pode existir no pom.xml do Gateway.
2. **Validação de Token In-Memory:** O Gateway **não deve consultar o banco de dados** a cada requisição. Ele deve validar a assinatura do JWT de forma puramente computacional usando a chave pública (RSA/HMAC) em memória e ler as claims (`tenant_id`, `roles`).
3. **Clean Architecture Enxuta:** A Clean Architecture no Gateway deve conter apenas objetos de política de rota (`RoutePolicy`), contexto de segurança (`TenantContext`) e portas abstratas (`TokenDecoderPort`, `RateLimiterPort`), delegando a persistência pesada ao `core-service`.

---

### Componente 2: Core Service (Hibernate 6 `@TenantId` e RabbitMQ)

#### O Problema mais Encontrado: Perda de Tenant em Threads Assíncronas
* **Como a falha acontece:**  
  O isolamento multi-tenant do Hibernate 6 (`@TenantId`) utiliza o `CurrentTenantIdentifierResolver`, que guarda o `tenant_id` da consultoria em uma variável `ThreadLocal` vinculada à thread HTTP da requisição.  
  Quando o `core-service` consome uma mensagem da fila do RabbitMQ (`@RabbitListener`) ou executa um método `@Async`, **o Spring despacha a execução para uma nova thread do pool de workers**. O `ThreadLocal` dessa nova thread está **completamente vazio (`null`)**!  
  *Consequência catastrófica:* O Hibernate lança `TenantIdentifierMismatchException` ou, pior ainda, executa a query SQL com `tenant_id IS NULL`, retornando zero registros ou corrompendo dados de auditoria.

#### O Segundo Problema: Sobrecarga de Memória com Upload de PDFs Gigantes
* Se o analista ou o webhook enviar um projeto executivo de engenharia de 80 MB passando pelo corpo da requisição HTTP do `core-service`, o container Java pode sofrer picos de memória e *OutOfMemoryError* (OOM).

#### A Mitigação Arquitetural Obrigatória:
1. **Propagação de Contexto no RabbitMQ:**
   Todo evento publicado no RabbitMQ **deve obrigatoriamente carregar o `tenant_id` nos Message Properties (Headers)**.
   ```java
   @RabbitListener(queues = "fila.documentos.processados")
   public void processarMensagem(Message message) {
       String tenantId = (String) message.getMessageProperties().getHeader("X-Tenant-Id");
       try {
           TenantContext.setCurrentTenant(UUID.fromString(tenantId));
           // Agora qualquer query JPA executará com o WHERE tenant_id correto!
           documentoService.salvarExtracao(...);
       } finally {
           TenantContext.clear(); // OBRIGATÓRIO para não contaminar a thread do pool
       }
   }
   ```
2. **Upload Direto via Pre-Signed URLs:**  
   Para arquivos grandes (> 10 MB), o backend nunca deve receber o payload binário. O frontend pede uma `Pre-Signed URL` para o `core-service` e faz o upload direto para o bucket do MinIO/Cloudflare R2. O backend só recebe a notificação com a `s3Key`.

---

### Componente 3: Transferegov Service (Cargas CSV do SICONV)

#### O Problema mais Encontrado: Quebras de Linha Dentro de Campos de Texto (`\n`)
* **Como a falha acontece:**  
  Nos arquivos CSV do SICONV (`siconv_proposta.csv`, `siconv_convenio.csv`, `siconv_justificativas.csv`), campos de texto como `OBJETO_PROPOSTA` ou `JUSTIFICATIVAS` frequentemente contêm quebras de linha reais (`\r\n`) e ponto-e-vírgula digitados pelos servidores públicos dentro das aspas.  
  Se o desenvolvedor usar um leitor ingênuo linha a linha (`BufferedReader.readLine().split(";")`), **o parser quebra na metade do registro**! A segunda metade do texto da justificativa é interpretada como uma nova linha de convênio, deslocando todas as colunas e fazendo o job falhar com erro de conversão de dados.

#### O Segundo Problema: Travamento do Banco por Upsert Excessivo (Snapshot vs. Delta)
* O governo disponibiliza o **snapshot completo** (centenas de milhares de linhas desde 2008). Fazer `INSERT ... ON CONFLICT DO UPDATE` linha a linha para 500 mil registros todo dia geraria contenção brutal de locks e dezenas de gigabytes de logs de replicação (WAL) no PostgreSQL.

#### A Mitigação Arquitetural Obrigatória:
1. **Parser Compatível com RFC 4180 em Modo Stream:**  
   Usar uma biblioteca de parsing de alta performance com suporte a quebras de linha entre aspas e delimitador customizado (ex: **Univocity Parsers** ou **Apache Commons CSV** no Java):
   ```java
   CsvParserSettings settings = new CsvParserSettings();
   settings.getFormat().setDelimiter(';');
   settings.getFormat().setQuote('"');
   settings.setHeaderExtractionEnabled(true);
   CsvParser parser = new CsvParser(settings);
   ```
2. **Filtro Geográfico Imediato (Streaming Filter):**  
   Durante a leitura do stream ZIP, se a coluna `UF_PROPONENTE != "PB"` e o CNPJ não estiver na tabela de prefeituras cadastradas, a linha é **descartada em memória imediatamente**, sem instanciar objetos nem tocar no banco. Isso reduz o volume de 500.000 linhas para menos de 4.000 linhas!
3. **Staging Table Temporária com Hash Diff:**  
   As linhas da Paraíba são carregadas em lote via `COPY` para uma tabela temporária não-logada (`staging_convenio`). O serviço só atualiza a tabela oficial se o hash `MD5` da linha for diferente do registro atual, executando a carga diária em **menos de 5 segundos**.

---

### Componente 4: WhatsApp Service (Evolution API / Baileys)

#### O Problema mais Encontrado: Queda de Sessão do WhatsApp (Desconexão Silenciosa)
* **Como a falha acontece:**  
  A Evolution API utiliza a biblioteca open-source **Baileys** para emular uma sessão de WhatsApp Web via WebSocket. Se o celular da consultoria ficar sem bateria, sem internet por mais de 14 dias, ou se os servidores do WhatsApp invalidarem a chave de criptografia de sessão, a conexão cai para o estado `close`.  
  *Sintoma em produção:* O secretário manda mensagem e nada acontece; o sistema não recebe o webhook e a consultoria acha que o software parou de funcionar.

#### O Segundo Problema: Mensagens Duplicadas
* O protocolo do WhatsApp reenvia mensagens se a confirmação de entrega (*ack*) demorar mais de alguns segundos, gerando risco de cadastrar o mesmo documento duas vezes.

#### A Mitigação Arquitetural Obrigatória:
1. **Heartbeat & Monitor de Conexão Ativo:**  
   O `whatsapp-service` roda um polling de saúde a cada 60 segundos no endpoint `/instance/connectionState` da Evolution API. Se o status mudar de `open` para `close` ou `connecting`, um alerta visual aparece no painel do Angular avisando o analista: *"Atenção: A instância do WhatsApp desconectou. Clique aqui para ler o QR Code novamente."*
2. **Tabela de Idempotência (`tb_mensagem_recebida`):**  
   Todo evento recebido tem um `message_id` único gerado pelo WhatsApp (WID). Antes de processar, o serviço faz um `INSERT ... ON CONFLICT DO NOTHING`. Se o ID já existir, o webhook é ignorado imediatamente com HTTP 200.
3. **Persistência das Sessões no Redis:**  
   Configurar a Evolution API para salvar os arquivos de sessão de autenticação no **Redis** (e não no disco efêmero do container Docker), garantindo que um restart do container não deslogue o número.

---

### Componente 5: AI Service (Python FastAPI & Gemini 3.x)

#### O Problema mais Encontrado: Fotos Ruins de Celular e Alucinação de Dígitos
* **Como a falha acontece:**  
  Diferente de empresas com scanners profissionais, no interior da Paraíba o fiscal tira foto da nota fiscal amassada no capô do carro, com sombras, reflexo de flash ou ângulo inclinado.  
  Modelos de visão podem confundir um `8` com um `3` ou ignorar casas decimais (ex: ler `R$ 80.000` em vez de `R$ 80.000,00`).

#### O Segundo Problema: Degradação de Prompt no Active Learning (Prompt Drift)
* Se cada correção feita pelo analista for concatenada indiscriminadamente no prompt da IA, o prompt vai crescer até estourar a janela de contexto ou confundir as instruções gerais do modelo com exceções conflitantes de prefeituras diferentes.

#### A Mitigação Arquitetural Obrigatória:
1. **Auditoria Algorítmica Rígida (Pre-Check Matemático):**  
   A IA **nunca** tem a palavra final sobre a matemática. O Python executa validação determinística:
   $$\text{Valor Bruto} - \sum (\text{Retenções}) == \text{Valor Líquido}$$
   Se a matemática da nota não bater com os campos extraídos, o campo recebe automaticamente um **score de confiança vermelho (< 0.60)** e o sistema força o analista a conferir o número.
2. **Poda e Indexação por Chave no Active Learning:**  
   As regras aprendidas nunca são globais. Elas são indexadas por `(tenant_id, cnpj_fornecedor)`. Quando uma nota da *Construtora Exemplo Ltda* chega, o sistema injeta no máximo as **3 correções mais recentes** exclusivas daquele fornecedor, com um limite de 300 caracteres de contexto.

---

### Componente 6: Extensão Chrome & Transferegov.br

#### O Problema mais Encontrado: Conflito de Máscaras e Eventos do DOM
* **Como a falha acontece:**  
  O portal do Transferegov utiliza máscaras de digitação em JavaScript para campos de dinheiro e CNPJ (ex: `R$ 0,00` e `00.000.000/0000-00`). Se a extensão simplesmente alterar o atributo `input.value = "85400.00"`, a máscara do portal não reconhece a alteração. Ao sair do campo ou tentar salvar, o formulário oficial apaga o valor ou acusa campo obrigatório vazio!

#### O Segundo Problema: O "Sono" do Service Worker (Manifest V3)
* No Manifest V3, o `background service worker` é descarregado da memória após 30 segundos de inatividade. Se a extensão depender de variáveis globais em memória para saber quem é o analista logado, o token é perdido.

#### A Mitigação Arquitetural Obrigatória:
1. **Injeção com Descritor Nativo e Eventos Sintéticos:**  
   Para contornar frameworks que interceptam o `setter` de inputs (como React, Angular e jQuery):
   ```typescript
   function setInputValueSafely(input: HTMLInputElement, value: string) {
       const nativeInputValueSetter = Object.getOwnPropertyDescriptor(
           window.HTMLInputElement.prototype, 'value'
       )?.set;
       nativeInputValueSetter?.call(input, value);
       
       // Dispara os 3 eventos que os frameworks escutam
       input.dispatchEvent(new Event('input', { bubbles: true }));
       input.dispatchEvent(new Event('change', { bubbles: true }));
       input.dispatchEvent(new Event('blur', { bubbles: true }));
   }
   ```
2. **Persistência no `chrome.storage.local`:**  
   Tokens JWT e dados da sessão ativa são gravados em `chrome.storage.local`, recuperados pelo Content Script a cada injeção, tornando a extensão totalmente tolerante ao ciclo de vida do Service Worker.
3. **Botão de Fallback (Clipboard):**  
   Se o governo alterar o ID de um campo em uma atualização de fim de semana, a extensão exibe o ícone de copiar ao lado do dado no painel flutuante, permitindo colar manualmente sem travar a rotina.

---

## 3. Matriz Consolidada de Riscos e Mitigações

```
+-------------------------------------------------------------------------------------------------------------+
| COMPONENTE       | MODO DE FALHA CRÍTICO        | IMPACTO | MITIGAÇÃO ARQUITETURAL DEFINIDA                 |
+------------------+------------------------------+---------+-------------------------------------------------+
| Gateway          | Bloqueio de Thread Netty     | Alto    | 100% Reativo, sem I/O de banco, JWT in-memory   |
| Core Service     | ThreadLocal vazio em RabbitMQ| Crítico | Propagar 'X-Tenant-Id' em headers AMQP          |
| Transferegov     | Quebra de CSV multilinha (\n)| Alto    | Parser RFC 4180 stream + Staging Hash Diff      |
| WhatsApp         | Sessão Baileys Desconectada  | Médio   | Polling de saúde a cada 60s + Alerta no Angular |
| AI Service       | Erro de centavos em fotos    | Alto    | Validação matemática determinística no Python   |
| Extensão Chrome  | Máscara JS ignorar injeção   | Médio   | Native prototype setter + Eventos sintéticos    |
+-------------------------------------------------------------------------------------------------------------+
```

---

## 4. Conclusão da Validação

A arquitetura proposta **funciona e é tecnicamente viável para um produto de escala comercial**, desde que as mitigações documentadas neste relatório sejam seguidas rigorosamente durante a implementação. 

O projeto não requer invenções de tecnologias não comprovadas: ele utiliza **padrões industriais consolidados** (Clean Gateway, Hexagonal Core, Streaming ETL, Event-Driven e Manifest V3), ajustados com precisão cirúrgica para a realidade burocrática dos sistemas governamentais brasileiros.
