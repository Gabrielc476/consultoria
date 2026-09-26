import { FetchDocumentosResult } from '../shared/contrato';
import { detectDocumentoHabilScreen } from './transferegov-detector';
import { mountOverlay } from './shadow-dom-overlay';

function boot(): void {
  const detection = detectDocumentoHabilScreen(document, window.location);
  if (!detection.matched) return;

  void chrome.runtime.sendMessage({ type: 'FETCH_DOCUMENTOS' }).then((resposta: FetchDocumentosResult | undefined) => {
    const autenticado = Boolean(resposta?.autenticado);
    const ok = Boolean(resposta?.ok);
    mountOverlay(document, {
      convenioNumero: detection.convenioNumero,
      autenticado,
      documentos: ok ? (resposta?.documentos ?? []) : [],
      erro: autenticado && !ok ? (resposta?.erro ?? 'Falha ao buscar os documentos.') : null
    });
  });
}

chrome.storage.onChanged.addListener((changes, area) => {
  if (area === 'local' && changes.govflow_extension_token) {
    boot();
  }
});

boot();
