# Relatório de Sessão Técnica: Triagem de Documentos com Baixa Certeza e Visualizador Interativo de Imagens

**Data**: 02/10/2026  
**Status**: Concluído e Implantado em Produção Local (Docker)  
**Módulos Afetados**: `core-service` (Backend Hexagonal), `frontend` (Angular 18 Standalone), `PostgreSQL` (`core_schema.tb_documentos` e `core_schema.tb_triagem_inbox`)

---

## 1. Motivação e Regra de Negócio

1. **Roteamento de Documentos com Baixa Confiança da IA**:
   - Documentos recebidos via WhatsApp ou upload cujo modelo de visão computacional/IA obteve confiança geral inferior a 70% (`< 0.70`), ou cujo tipo documental não pôde ser classificado com certeza, não devem poluir a Esteira de Documentos (`/documentos`) em status `EM_CONFERENCIA`.
   - Esses itens agora são transicionados automaticamente para `status = 'EM_TRIAGEM'` e inseridos na **Caixa de Triagem Omnicanal** (`/triagem` / `core_schema.tb_triagem_inbox`) como `PENDENTE`, aguardando a decisão humana do operador para associação correta de convênio e contato.

2. **Visualizador Especializado de Imagens (WhatsApp Inbound)**:
   - Fotos de notas fiscais, cupons, recibos e medições enviadas via WhatsApp frequentemente chegam em formatos de imagem (`JPEG`, `PNG`, `WEBP`) e muitas vezes rotacionadas de lado ou de cabeça para baixo devido à orientação da câmera do celular.
   - O visualizador anterior esperava primariamente PDFs e quebrava ao tentar renderizar binários de imagem no visualizador PDF (`ngx-extended-pdf-viewer`).
   - Implementou-se um visualizador dedicado com controle de rotação 90° (horário e anti-horário), zoom de 20% a 400%, navegação por arraste (Pan/Drag com mouse), filtros de legibilidade e realce de contraste para leitura de papéis apagados, e suporte a visualização direta dentro dos cards da Caixa de Triagem.

---

## 2. Modificações Realizadas

### 2.1 Backend (`services/core-service`)
- **Status Documento**:
  - Adicionado enum `EM_TRIAGEM` em `StatusDocumento.java`.
- **Regras de Negócio no Domínio**:
  - `Documento.java`: Método `marcarEmTriagem(motivo)` e `enviarParaConferencia()`.
  - Atualização do método `registrarExtracaoIA` para transicionar documentos com confiança `< 0.70` ou tipo nulo diretamente para `EM_TRIAGEM`.
- **Serviços de Aplicação**:
  - `ProcessarDocumentoExtraidoService.java`: Injeção de `SpringDataTriagemInboxRepository`. Criação automática do item na caixa de triagem se o status for `EM_TRIAGEM`.
  - `DocumentoClassificadoListener.java`: Roteamento automático para triagem caso o score não seja de alta confiança.
  - `TriagemService.java`: Despacho do documento para `EM_CONFERENCIA` assim que a triagem é resolvida pelo usuário.
  - `ObterArquivoDocumentoService.java`: Detecção dinâmica do Content-Type (`image/jpeg`, `image/png`, `image/webp`, `text/html`, `application/pdf`) a partir da extensão do arquivo ou metadados, evitando o retorno forçado de `application/pdf` para imagens.
  - `DocumentoRestMapper.java`: Normalização do `effectiveContentType` nas respostas da API REST.
- **Controlador REST**:
  - `DocumentoController.java`: Adicionado endpoint `PUT /api/v1/documentos/{id}/enviar-triagem?motivo=...` para permitir devolução manual da Esteira para a Triagem.

### 2.2 Banco de Dados (`PostgreSQL 16`)
- Saneamento dos registros existentes com confiança `< 0.70`:
  - 6 documentos atualizados para `status = 'EM_TRIAGEM'` e com seus Content-Types corrigidos (`image/jpeg` e `xlsx`).
  - Inserção de 6 pendências na tabela `core_schema.tb_triagem_inbox` com status `PENDENTE`.
  - A Esteira de Documentos agora contém estritamente documentos de alta confiança prontos para conferência.

### 2.3 Frontend (`Angular 18 Standalone`)
- **Visualizador Side-by-Side (`MediaWorkspaceComponent`)**:
  - Detecção rigorosa de formato: `isImage` priorizado antes de `isPdf`.
  - Controles de rotação: botões `↺ 90°` e `↻ 90°`, com indicador de ângulo.
  - Controles de zoom: botões `+`, `-`, porcentagem, `Ajustar` (Fit) e `1:1`. Suporte a rolagem pelo mouse (wheel) e 2x clique para zoom rápido.
  - Arraste interativo (Pan/Drag): permite arrastar a imagem pelo mouse quando ampliada com cursor adaptativo (`grab` e `grabbing`).
  - Filtros visuais de legibilidade:
    - *Normal*: sem filtro.
    - *☕ Conforto*: papel suave para redução de contraste.
    - *⚡ Alto Contraste*: saturação e contraste elevados para recibos desbotados ou fotos escuras.
    - *🔲 P&B / Nitidez*: escala de cinza de alta precisão.
    - *🌓 Invertido*: inversão tonal para auditoria noturna.
  - Sincronização de Bounding Boxes da IA: caixas delimitadoras redimensionadas e rotacionadas junto com a imagem.
  - Botão de download direto do arquivo original.
- **Modal do Ficheiro Digital (`DocumentoPreviewModalComponent`)**:
  - Suporte completo a imagens com zoom, rotação 90° e arraste interativo.
  - Correção na prioridade de renderização para impedir que imagens sejam abertas em tags `iframe` de PDF.
- **Caixa de Triagem (`TriagemInboxPageComponent`)**:
  - Adicionado botão `👁️ Visualizar Foto/Documento` em cada card de pendência.
  - Modal integrado com suporte a zoom, rotação e pan para conferência imediata do anexo de WhatsApp antes de tomar a decisão de arquivamento ou vínculo de contato.
- **Esteira de Documentos (`DocumentosListPageComponent`)**:
  - Filtragem de documentos com `status !== 'EM_TRIAGEM'` para manter a esteira limpa.
  - Link de acesso direto para a Caixa de Triagem no cabeçalho com badge.
  - Botão `📥` ("Encaminhar para Caixa de Triagem") em cada linha da tabela com confirmação rápida.
- **Badges de Status (`StatusPillComponent`)**:
  - Adicionado suporte estilizado ao status `EM_TRIAGEM` (cor púrpura/violeta suave com indicador pulsante).
- **Serviços de API (`RevisaoApiService` e `api-endpoints.ts`)**:
  - Adicionado endpoint e método `enviarParaTriagem(id, motivo)`.

---

## 3. Validação e Testes
- Compilação do Backend (`mvn package -DskipTests`): Build bem-sucedido.
- Suíte de Testes Unitários de Regressão: 13 testes executados com 0 falhas.
- Compilação do Frontend (`npm run build`): Bundle gerado sem erros de tipagem em 21.7s.
- Deploy nos Contêineres Docker:
  - `govflow-core-service`: Recriado e respondendo normalmente nas portas 8080/8081.
  - `govflow-frontend`: Artefatos copiados para `/usr/share/nginx/html/` e Nginx recarregado.
- Teste de Integração de API:
  - `GET /api/v1/triagem/pendentes`: Retornando os 6 documentos pendentes com scores entre 0% e 47.2%.
  - `GET /api/v1/documentos/{id}/arquivo`: Retornando `Content-Type: image/jpeg` e os bytes reais do arquivo (130 KB).
