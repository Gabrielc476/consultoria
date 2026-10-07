# Relatório de Sessão Técnica: Autenticação de Preview via Blob e Correção de Encoding UTF-8

**Data**: 03/10/2026  
**Status**: Concluído, Validado e Sincronizado no Git  
**Módulos Afetados**: `frontend` (Angular 18 Standalone), `PostgreSQL` (`core_schema.tb_triagem_inbox`), `API Gateway` (Filtros de Segurança)

---

## 1. Motivação e Diagnóstico Técnico

### 1.1 Falha na Renderização de Imagens (Ícone Quebrado / 401 Unauthorized)
- **Cenário**: Ao clicar em "Visualizar Foto/Imagem" na Caixa de Triagem (`/triagem`) ou abrir imagens no modal de pré-visualização do Ficheiro Digital (`DocumentoPreviewModalComponent`), o navegador apresentava o ícone de imagem corrompida.
- **Causa Raiz**:
  - Tags HTML nativas (`<img [src]>` e `<iframe>`) disparam requisições HTTP GET diretas pelo motor do navegador, sem anexar cabeçalhos customizados.
  - O cabeçalho `Authorization: Bearer <token>` injetado pelo `AuthInterceptor` do Angular existe exclusivamente para chamadas realizadas através do `HttpClient`.
  - O `AuthenticationGlobalFilter` do `govflow-api-gateway` interceptava a chamada sem cabeçalho e retornava `HTTP 401 Unauthorized` (`"Token Inválido ou Ausente"`).
  - O MinIO possuía o binário íntegro (57.532 bytes), mas o navegador era impedido de transferi-lo.

### 1.2 Caracteres Corrompidos com Pontos de Interrogação (`???`)
- **Cenário**: Na listagem da Caixa de Triagem, nomes de contatos e motivos de ambiguidade apareciam como `Lais Souza ??? Psic??loga` e `Confian??a de extra????o de IA abaixo do limiar ou documento n??o identificado (12%)`.
- **Causa Raiz**:
  - Um script de atualização de dados foi executado via pipeline do PowerShell 5.1 redirecionado para o `psql` (`Get-Content ... | docker exec ... psql`).
  - Por padrão no Windows PowerShell, o piping utiliza o encoding ANSI/Windows-1252, convertendo caracteres UTF-8 (`•`, `ç`, `ã`, `ó`) em interrogações literais (`?`) persistidas em disco no PostgreSQL.

---

## 2. Modificações e Soluções Implementadas

### 2.1 Frontend: Streaming Binário Autenticado via Blob (`HttpClient`)
1. **Serviços de API (`TriagemService` e `FicheiroDigitalService`)**:
   - Adicionado método `baixarArquivo(documentoId: string): Observable<Blob>` que consulta `/api/v1/documentos/{id}/arquivo` usando `responseType: 'blob'`.
   - O `AuthInterceptor` anexa automaticamente o token JWT na requisição, contornando a restrição de segurança no Gateway.

2. **Componente da Caixa de Triagem (`TriagemInboxPageComponent`)**:
   - Criados signals `previewBlobUrl`, `safePreviewBlobUrl`, `carregandoPreview` e `erroPreview`.
   - Método `abrirPreview(item)`:
     - Realiza guard de validação para `documentoId`.
     - Exibe spinner de carregamento visual no modal.
     - Ao receber o `Blob`, determina o MIME type correto a partir do nome do arquivo (`image/jpeg`, `image/png`, `application/pdf`, etc.).
     - Gera uma URL local em memória via `URL.createObjectURL(blob)`.
     - Permite visualização imediata da imagem e do PDF sem problemas de autenticação.
     - Suporta zoom por roda do mouse (`wheel`), zoom rápido em clique duplo (`dblclick`), rotação (90° horário/anti-horário) e arrasto suave com o mouse (*pan*).
   - Ciclo de Vida e Prevenção de Memory Leaks:
     - Invocação de `URL.revokeObjectURL()` ao fechar o modal e no `ngOnDestroy`.

3. **Modal de Detalhes e Auditoria (`DocumentoPreviewModalComponent`)**:
   - Substituída a atribuição direta da URL da API pelo download do `Blob` autenticado via `ficheiroService.baixarArquivo(documento.id)`.
   - Implementada interface `OnDestroy` com revogação da URL local em memória para liberar recursos do navegador.

### 2.2 Banco de Dados: Saneamento de Encoding UTF-8
- Executado script SQL com codificação UTF-8 direta no PostgreSQL através do contêiner Docker:
  ```sql
  UPDATE core_schema.tb_triagem_inbox
  SET sender_name = 'Lais Souza - Psicóloga',
      push_name = 'Lais Souza - Psicóloga',
      motivo_ambiguidade = 'Confiança de extração de IA abaixo do limiar ou documento não identificado (' || (confidence_score * 100)::int || '%)'
  WHERE sender_name LIKE '%?%' OR motivo_ambiguidade LIKE '%?%';
  ```
- Validado que zero registros restantes contêm caracteres corrompidos nas tabelas `tb_triagem_inbox`, `tb_documentos` e `whatsapp_schema.tb_contatos`.

---

## 3. Validação e Entrega

1. **Compilação Angular**:
   - `npm run build` executado com sucesso e zero erros de compilação TypeScript/Angular.
2. **Deploy Nginx**:
   - Novo bundle copiado para o contêiner `govflow-frontend` (`/usr/share/nginx/html/`) e recarregamento via `nginx -s reload`.
3. **Versionamento Git**:
   - Commit `2390d69`: `fix(frontend): Load image and document previews via authenticated blobs`.
   - Sincronizado com o repositório remoto (`origin/main`).
