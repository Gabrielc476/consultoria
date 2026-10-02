# Relatório de Sessão Técnica: Refatoração do Seletor de Arquétipos (Dropdown Unificado), Seleção Automática pela IA e Purga da Base

**Data**: 01/10/2026  
**Contexto**: Telas de Auditoria Side-by-Side (`ExtractionFormComponent` e `RevisaoStateService`) e Banco de Dados PostgreSQL (`core_schema.tb_documentos`).

---

## 1. Objetivos da Sessão
1. **Purga Total de Documentos da Base de Dados**: Remover todos os documentos da base de dados local (`core_schema.tb_documentos` com cascata para auditorias, dados fiscais e triagem) para reiniciar a esteira com a base zerada.
2. **Refatoração Visual do Layout do Auditor**:
   - Eliminar a barra horizontal de botões carrossel de cenários que ocupava espaço vertical desnecessário.
   - Substituir por um **dropdown compacto, intuitivo e unificado** no cabeçalho do formulário.
3. **Classificação Automática pela IA (Zero-Touch UX)**:
   - A tela agora é aberta **já configurada e pronta pela IA**, baseada na dedução automática do arquivo/metadados/categoria.
   - O analista humano só precisa alterar o dropdown caso a classificação da IA esteja incorreta.
   - Adicionado badge dinâmico:
     - `🤖 Sugerido pela IA`: quando o formulário ativo corresponde ao modelo sugerido automaticamente pelo pipeline.
     - `✏️ Ajustado Manualmente`: quando o analista modifica o tipo, acompanhado do botão `↺ Restaurar IA` para reverter instantaneamente.

---

## 2. Alterações Implementadas

### 2.1 Banco de Dados PostgreSQL
- Executado comando de limpeza:
  ```sql
  TRUNCATE TABLE core_schema.tb_documentos CASCADE;
  ```
- Cascata aplicada para:
  - `core_schema.tb_auditorias_revisao`
  - `core_schema.tb_documentos_habeis_dados`
  - `core_schema.tb_documentos_auditoria`
  - `core_schema.tb_triagem_inbox`
- Verificação executada:
  - `SELECT count(*) FROM core_schema.tb_documentos;` => **0**
  - `SELECT count(*) FROM core_schema.tb_triagem_inbox;` => **0**

### 2.2 Frontend - Estado Reativo (`revisao-state.service.ts`)
- **`arquetipoOriginalIA`**: Signal computado que deduz com precisão o modelo arquitetural a partir de `extracaoSugerida.tipoDocumento`, `categoriaDocumento`, nome do arquivo e termos-chave.
- **`arquetipoAtivo`**: Signal computado prioritário: adota `arquetipoManual()` se o usuário selecionou algo manualmente; caso contrário, adota `arquetipoOriginalIA()`.
- **`foiModificadoManualmente`**: Signal computado booleano que detecta discrepância entre o tipo ativo e o tipo originalmente inferido pela IA.
- **`valorSelecaoDropdown`**: Signal computado que mantém o `<select>` sincronizado tanto para arquétipos formais quanto para cenários de simulação rápida (`CENARIO:id`).
- **`alterarSelecaoTipo(valor: string)`**: Manipulador unificado que direciona para `selecionarCenario()` ou `definirArquetipo()`.
- **`restaurarClassificacaoIA()`**: Método que reseta qualquer sobreposição manual, voltando imediatamente para o tipo deduzido pela IA com feedback via toast.

### 2.3 Frontend - Componente de Formulário (`extraction-form.component.ts`)
- Substituída a barra de botões carrossel de 9 itens por uma barra de topo elegante e limpa:
  - Exibe o nome do arquivo (`📄 [nomeArquivoOriginal]`).
  - Pílula de fase do ciclo de vida e status do documento.
  - Título com ícone do tipo de documento.
  - Badge interativo: `🤖 Sugerido pela IA` ou `✏️ Ajustado Manualmente` + link `↺ Restaurar IA`.
  - Dropdown com dois `<optgroup>`s:
    1. `📋 Tipos Oficiais de Convênio (IA / Arquétipos)`:
       - 📐 Medição de Obras & Diário (Fase 04)
       - 📄 Nota Fiscal / Hábil (Fase 05)
       - 🌿 Licença Ambiental & Projetos (Fase 02)
       - ⚖️ Contrato & Licitação (Fase 03)
       - 🏛️ Regularidade & CAUC (Fase 00)
       - 🔄 Termos Aditivos & Prazos (Fase 06)
       - 📋 Prestação de Contas Final & RCO (Fase 07)
       - ⚠️ Notificação SELIC & TCE (Fase 09)
       - 🌐 Documento Agnóstico / Livre
    2. `⚡ Simulação com Dados Preenchidos (Testes)`:
       - Permite carregar dados mockados com 1 clique diretamente pelo seletor, sem poluição na tela.
  - Alternância compacta entre os modos **👁️ Leitura** e **✏️ Editar**.

### 2.4 Resolução de Build no Docker
- Desativado o inlining remoto de fontes do Google Fonts no `frontend/angular.json` (`"optimization": { "fonts": { "inline": false } }`), prevenindo falhas de timeout de rede durante builds em containers Docker.

---

## 3. Validação e Testes
- Compilação do Angular com `ng build --configuration production`: **Sucesso (0 erros de tipo/template)**.
- Recriação dos containers via Docker Compose: **`govflow-frontend` saudável e ativo na porta 4200**.
- Consulta SQL: **0 documentos no banco**.
