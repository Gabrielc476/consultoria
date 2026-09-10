# Especificação Arquitetural: Frontend Web (Angular 19/20) e Extensão Chrome (Manifest V3)

**Componentes:** `frontend` (SPA Web) e `extension` (Extensão de Navegador)  
**Tecnologias:** Angular 19/20 (Standalone Components + Signals) / `ngx-extended-pdf-viewer` / TypeScript / Manifest V3  
**Padrão Arquitetural:** **Feature-Sliced Design (FSD)** no Angular e **Shadow DOM Content Script com DOM Injector Strategy** na Extensão  

---

## 1. Frontend Angular: Feature-Sliced Design (FSD) com Signals

O frontend é projetado para máxima reatividade, carregamento instantâneo e manutenibilidade por módulos isolados:

```
frontend/
├── angular.json
├── package.json
└── src/
    └── app/
        ├── core/                                     # NÚCLEO TRANSVERSAL (SINGLETONS)
        │   ├── auth/                                 # AuthService (Signals), AuthGuard
        │   ├── interceptors/
        │   │   ├── auth.interceptor.ts               # Injeta Authorization: Bearer <token>
        │   │   └── error.interceptor.ts              # Trata RFC 7807 (Problem Details)
        │   └── state/
        │       └── tenant.state.ts                   # Signal com a Consultoria e Usuário logados
        │
        ├── shared/                                   # COMPONENTES E UTILITÁRIOS REUTILIZÁVEIS
        │   ├── components/
        │   │   ├── status-badge/                     # Semáforo verde, amarelo, vermelho
        │   │   ├── confidence-indicator/             # Indicador de score de confiança da IA
        │   │   └── pdf-viewer-wrapper/               # Encapsulamento do ngx-extended-pdf-viewer
        │   └── pipes/
        │       └── currency-brl.pipe.ts
        │
        └── features/                                 # MÓDULOS DE NEGÓCIO ISOLADOS
            ├── auth/                                 # Login e seleção de tenant
            │   └── pages/login/
            │
            ├── revisao-documento/                    # TELA CRÍTICA: CONFERÊNCIA LADO A LADO
            │   ├── components/
            │   │   ├── pdf-canvas-viewer/            # Visualizador com bounding boxes
            │   │   ├── extraction-form/              # Formulário de conferência auditada
            │   │   └── audit-trail-dialog/           # Histórico de quem aprovou/alterou
            │   ├── services/
            │   │   └── revisao.service.ts
            │   └── pages/revisao-detalhe/
            │
            ├── convenios-dashboard/                  # GESTÃO DE CONVÊNIOS E TIMELINE
            │   ├── components/
            │   │   ├── convenio-card/
            │   │   ├── deadline-alerts-banner/       # Alertas de cláusula suspensiva
            │   │   └── checklist-tree/
            │   └── pages/convenios-list/
            │
            └── whatsapp-inbox/                       # CENTRAL DE MENSAGENS POR MUNICÍPIO
                ├── components/
                │   ├── chat-thread/
                │   ├── audio-player-transcription/   # Player de voz com texto transcrito
                │   └── suggested-reply-bar/          # Resposta pronta da IA com 1 clique
                └── pages/inbox/
```

---

## 2. A Tela de Conferência Lado a Lado (Revisão Documental)

A tela `revisao-detalhe` é o principal instrumento de produtividade do analista:

```
+----------------------------------------------------------------------------------------------------+
|                                    TELA DE CONFERÊNCIA LADO A LADO                                 |
|                                                                                                    |
|  [ VOLTAR PARA CONVÊNIOS ]          Município: Massaranduba-PB | Convênio: 912345 (Creche Tipo 1) |
+---------------------------------------------------+------------------------------------------------+
|           VISUALIZADOR PDF (50% Tela)             |           CAMPOS EXTRAÍDOS (50% Tela)          |
|                                                   |                                                |
|  [ngx-extended-pdf-viewer]                        |  [Card: Documento Hábil]                       |
|  ┌─────────────────────────────────────────────┐  |  Tipo de Documento:                            |
|  │ NOTA FISCAL DE SERVIÇOS ELETRÔNICA          │  |  [ NOTA FISCAL               ▼ ] 🟢 (100%)     |
|  │ Número: [0001542] (Caixa Verde)             │  |                                                |
|  │ Data:   [20/08/2026]                        │  |  Número da NF:                                 |
|  │                                             │  |  [ 0001542                  ] 🟢 (99%)         |
|  │ PRESTADOR: CONSTRUTORA EXEMPLO LTDA         │  |                                                |
|  │ CNPJ: [08.123.456/0001-90] (Caixa Verde)    │  |  CNPJ do Credor:                               |
|  │                                             │  |  [ 08.123.456/0001-90       ] 🟢 (100%)        |
|  │ VALOR TOTAL: [R$ 85.400,00] (Caixa Verde)   │  |                                                |
|  │ RETENÇÃO INSS: [R$ 9.394,00] (Caixa Amarela)│  |  Valor Bruto:                                  |
|  └─────────────────────────────────────────────┘  |  [ R$ 85.400,00             ] 🟢 (98%)         |
|                                                   |                                                |
|                                                   |  Retenção INSS (11%):                          |
|                                                   |  [ R$ 9.394,00              ] 🟡 (82%)         |
|                                                   |                                                |
|                                                   |  [x] Marcar regra para memorizar fornecedor    |
|                                                   |                                                |
|                                                   |  [ REJEITAR DOCUMENTO ]  [ APROVAR DADOS (OK) ]|
+---------------------------------------------------+------------------------------------------------+
```

---

## 3. Extensão do Chrome: Shadow DOM & DOM Injector Strategy

Para interagir com o portal oficial do governo (`transferegov.sistema.gov.br`) de forma blindada contra quebras de CSS e mudanças de layout:

```
extension/
├── manifest.json                             # Manifest V3 com host_permissions
├── tsconfig.json
└── src/
    ├── background/
    │   └── service-worker.ts                 # Autenticação e bridge com o API Gateway
    │
    ├── content/
    │   ├── transferegov-detector.ts          # Detecta quando a tela é "Incluir Documento Hábil"
    │   ├── shadow-dom-overlay.ts             # Injeta o painel flutuante encapsulado
    │   ├── dom-injector.ts                   # Injeta valores nos inputs e dispara eventos
    │   └── clipboard-fallback.ts             # Copia dados com 1 clique se o DOM mudar
    │
    └── popup/
        ├── popup.html                        # Menu da extensão no navegador
        └── popup.ts                          # Status de conexão com o GovFlow Hub
```

### 3.1 Isolamento de Estilos com Shadow DOM (`shadow-dom-overlay.ts`):
Para que o CSS do Transferegov.br não desconfigure os botões da nossa extensão (e vice-versa), o painel flutuante é injetado dentro de uma `ShadowRoot` fechada:

```typescript
export class ShadowDomOverlay {
    public static mount(payload: DocumentoHabilPayload): void {
        const hostElement = document.createElement('div');
        hostElement.id = 'govflow-copilot-host';
        document.body.appendChild(hostElement);

        // Cria a raiz Shadow isolada
        const shadowRoot = hostElement.attachShadow({ mode: 'closed' });

        // Injeta CSS próprio que nunca vazará para a página do governo
        const style = document.createElement('style');
        style.textContent = `
            .govflow-panel {
                position: fixed;
                bottom: 20px;
                right: 20px;
                width: 380px;
                background: #1e293b;
                color: #ffffff;
                border-radius: 12px;
                box-shadow: 0 10px 25px rgba(0,0,0,0.5);
                z-index: 999999;
                font-family: system-ui, sans-serif;
                padding: 16px;
            }
            .govflow-btn-primary {
                background: #10b981;
                color: white;
                border: none;
                padding: 10px 16px;
                border-radius: 6px;
                cursor: pointer;
                font-weight: bold;
                width: 100%;
            }
        `;
        shadowRoot.appendChild(style);

        const panel = document.createElement('div');
        panel.className = 'govflow-panel';
        panel.innerHTML = `
            <h3>GovFlow Copiloto Transferegov</h3>
            <p><strong>Obra:</strong> ${payload.municipio} - Medição Pronta</p>
            <p><strong>NF:</strong> ${payload.camposFormulario.nr_documento_habil}</p>
            <p><strong>Valor:</strong> R$ ${payload.camposFormulario.vl_documento_habil}</p>
            <button id="btn-preencher" class="govflow-btn-primary">⚡ Preencher Formulário Oficial</button>
            <div id="fallback-buttons" style="margin-top: 10px;"></div>
        `;
        shadowRoot.appendChild(panel);

        shadowRoot.getElementById('btn-preencher')?.addEventListener('click', () => {
            DomInjector.fillForm(payload.camposFormulario);
        });
    }
}
```

### 3.2 O Disparador de Eventos Sintéticos (`dom-injector.ts`):
Muitos sistemas de governo usam JavaScript para validar campos. Apenas fazer `input.value = "123"` não aciona os scripts da página. O `DomInjector` dispara os eventos `input` e `change` sinteticamente:

```typescript
export class DomInjector {
    public static setInputValue(selector: string, value: string): boolean {
        const input = document.querySelector(selector) as HTMLInputElement;
        if (!input) return false;

        input.value = value;
        input.dispatchEvent(new Event('input', { bubbles: true }));
        input.dispatchEvent(new Event('change', { bubbles: true }));
        return true;
    }
}
```
Isso garante que o formulário oficial do Transferegov reconheça o preenchimento imediatamente, habilitando o botão de salvar do governo sem nenhum travamento.
