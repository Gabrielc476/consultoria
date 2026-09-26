import { CampoFormulario, DocumentoPronto } from '../shared/contrato';
import { copyFieldValue } from './clipboard-fallback';
import { fillOfficialForm, PreenchimentoCampo } from './dom-injector';

const PANEL_CSS = `
  .govflow-panel {
    position: fixed;
    bottom: 20px;
    right: 20px;
    width: 380px;
    max-height: 80vh;
    overflow: auto;
    background: #1e293b;
    color: #ffffff;
    border-radius: 12px;
    z-index: 2147483647;
    font-family: system-ui, sans-serif;
    padding: 16px;
    box-sizing: border-box;
  }
  .govflow-panel h3 {
    margin: 0 0 8px;
    font-size: 16px;
  }
  .govflow-panel p {
    margin: 0 0 8px;
    font-size: 13px;
    line-height: 1.4;
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
  .govflow-row {
    display: flex;
    justify-content: space-between;
    gap: 8px;
    align-items: center;
    margin-top: 8px;
    font-size: 12px;
  }
  .govflow-copy {
    background: transparent;
    color: #e2e8f0;
    border: 1px solid #64748b;
    border-radius: 6px;
    padding: 4px 8px;
    cursor: pointer;
  }
  .govflow-select {
    width: 100%;
    margin-bottom: 8px;
    padding: 6px;
  }
  .govflow-muted { color: #cbd5e1; }
  .govflow-erro { color: #fca5a5; }
`;

export interface OverlayState {
  convenioNumero: string | null;
  autenticado: boolean;
  documentos: DocumentoPronto[];
  erro?: string | null;
}

export interface OverlayHandle {
  host: HTMLElement;
  shadowRoot: ShadowRoot;
  ultimoPreenchimento: () => PreenchimentoCampo[];
}

export function mountOverlay(page: Document, state: OverlayState): OverlayHandle {
  page.getElementById('govflow-copilot-host')?.remove();

  const host = page.createElement('div');
  host.id = 'govflow-copilot-host';
  page.body.appendChild(host);

  const shadowRoot = host.attachShadow({ mode: 'closed' });
  const style = page.createElement('style');
  style.textContent = PANEL_CSS;
  shadowRoot.appendChild(style);

  const panel = page.createElement('div');
  panel.className = 'govflow-panel';
  shadowRoot.appendChild(panel);

  let preenchimento: PreenchimentoCampo[] = [];
  let selecionado = state.documentos[0] ?? null;

  const render = () => {
    panel.replaceChildren();
    const titulo = page.createElement('h3');
    titulo.textContent = 'GovFlow Copiloto Transferegov';
    panel.appendChild(titulo);

    const convenio = page.createElement('p');
    convenio.textContent = state.convenioNumero
      ? `Convênio na tela: ${state.convenioNumero}`
      : 'Convênio na tela: não identificado';
    panel.appendChild(convenio);

    if (!state.autenticado) {
      const aviso = page.createElement('p');
      aviso.className = 'govflow-muted';
      aviso.textContent = 'Clique no ícone do GovFlow na barra do Chrome e entre com e-mail e senha. O login do site Angular não vale nesta extensão.';
      panel.appendChild(aviso);
      return;
    }

    if (state.erro) {
      const aviso = page.createElement('p');
      aviso.className = 'govflow-erro';
      aviso.textContent = state.erro;
      panel.appendChild(aviso);
      return;
    }

    if (state.documentos.length === 0) {
      const vazio = page.createElement('p');
      vazio.className = 'govflow-muted';
      vazio.textContent = 'Nenhum documento PRONTO_PARA_TRANSFEREGOV com revisão auditada para este tenant.';
      panel.appendChild(vazio);
      return;
    }

    if (state.documentos.length > 1) {
      const select = page.createElement('select');
      select.className = 'govflow-select';
      select.setAttribute('aria-label', 'Documento auditado');
      state.documentos.forEach((documento, index) => {
        const option = page.createElement('option');
        option.value = String(index);
        const nf = documento.campos.find((campo) => campo.id === 'numeroDocumento')?.value || documento.id;
        option.textContent = `NF ${nf}`;
        select.appendChild(option);
      });
      select.addEventListener('change', () => {
        selecionado = state.documentos[Number(select.value)] ?? state.documentos[0];
        render();
        const novo = panel.querySelector('select');
        if (novo instanceof HTMLSelectElement) novo.value = select.value;
      });
      panel.appendChild(select);
    }

    const nota = page.createElement('p');
    nota.className = 'govflow-muted';
    nota.textContent = selecionado
      ? `Documento ${selecionado.id.slice(0, 8)} — valores da revisão do analista.`
      : '';
    panel.appendChild(nota);

    const botao = page.createElement('button');
    botao.id = 'btn-preencher';
    botao.type = 'button';
    botao.className = 'govflow-btn-primary';
    botao.textContent = 'Preencher Formulário Oficial';
    botao.addEventListener('click', () => {
      if (!selecionado) return;
      preenchimento = fillOfficialForm(page, selecionado.campos);
      const falhas = preenchimento.filter((campo) => !campo.preenchido);
      const status = panel.querySelector('[data-fill-status]');
      if (status) {
        status.textContent = falhas.length === 0
          ? 'Campos preenchidos.'
          : `Não encontrados: ${falhas.map((campo) => campo.label).join(', ')}. Use copiar.`;
      }
    });
    panel.appendChild(botao);

    const status = page.createElement('p');
    status.dataset.fillStatus = 'true';
    status.className = 'govflow-muted';
    panel.appendChild(status);

    const lista = page.createElement('div');
    lista.id = 'fallback-buttons';
    (selecionado?.campos ?? []).forEach((campo) => appendCopyRow(page, lista, campo));
    panel.appendChild(lista);
  };

  render();

  return {
    host,
    shadowRoot,
    ultimoPreenchimento: () => preenchimento
  };
}

function appendCopyRow(page: Document, lista: HTMLElement, campo: CampoFormulario): void {
  const row = page.createElement('div');
  row.className = 'govflow-row';

  const texto = page.createElement('span');
  texto.textContent = `${campo.label}: ${campo.value || '—'}`;
  row.appendChild(texto);

  const copiar = page.createElement('button');
  copiar.type = 'button';
  copiar.className = 'govflow-copy';
  copiar.dataset.copyId = campo.id;
  copiar.dataset.copyValue = campo.value;
  copiar.textContent = 'Copiar';
  copiar.addEventListener('click', () => {
    void copyFieldValue(campo.value).then(() => {
      copiar.textContent = 'Copiado';
    }).catch(() => {
      copiar.textContent = 'Falhou';
    });
  });
  row.appendChild(copiar);
  lista.appendChild(row);
}
