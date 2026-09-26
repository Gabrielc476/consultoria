import {
  API_BASE_URL,
  AuthStatus,
  DocumentoApi,
  ExtensionMessage,
  FetchDocumentosResult,
  LoginResult
} from '../shared/contrato';
import { selecionarProntos } from '../content/payload-mapper';

const TOKEN_KEY = 'govflow_extension_token';
const EMAIL_KEY = 'govflow_extension_email';
const NOME_KEY = 'govflow_extension_nome';

interface PageBody {
  content?: DocumentoApi[];
  items?: DocumentoApi[];
}

async function readSession(): Promise<AuthStatus> {
  const stored = await chrome.storage.local.get([TOKEN_KEY, EMAIL_KEY, NOME_KEY]);
  const token = typeof stored[TOKEN_KEY] === 'string' ? stored[TOKEN_KEY] : '';
  return {
    autenticado: token.length > 0,
    email: typeof stored[EMAIL_KEY] === 'string' ? stored[EMAIL_KEY] : null,
    nome: typeof stored[NOME_KEY] === 'string' ? stored[NOME_KEY] : null
  };
}

async function login(email: string, senha: string): Promise<LoginResult> {
  const response = await fetch(`${API_BASE_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, senha })
  });

  if (!response.ok) {
    return {
      ok: false,
      autenticado: false,
      email: null,
      nome: null,
      erro: `Login recusado (${response.status}).`
    };
  }

  const body = await response.json() as { token?: string; email?: string; nome?: string };
  if (!body.token) {
    return { ok: false, autenticado: false, email: null, nome: null, erro: 'Resposta sem token.' };
  }

  await chrome.storage.local.set({
    [TOKEN_KEY]: body.token,
    [EMAIL_KEY]: body.email ?? email,
    [NOME_KEY]: body.nome ?? ''
  });

  return {
    ok: true,
    autenticado: true,
    email: body.email ?? email,
    nome: body.nome ?? null
  };
}

async function fetchDocumentos(): Promise<FetchDocumentosResult> {
  const stored = await chrome.storage.local.get(TOKEN_KEY);
  const token = typeof stored[TOKEN_KEY] === 'string' ? stored[TOKEN_KEY] : '';
  if (!token) {
    return { ok: false, autenticado: false, documentos: [], erro: 'Sem sessão.' };
  }

  const url = `${API_BASE_URL}/documentos?status=PRONTO_PARA_TRANSFEREGOV&page=0&size=50`;
  const response = await fetch(url, {
    headers: { Authorization: `Bearer ${token}` }
  });

  if (response.status === 401) {
    await chrome.storage.local.remove([TOKEN_KEY, EMAIL_KEY, NOME_KEY]);
    return { ok: false, autenticado: false, documentos: [], erro: 'Sessão expirada.' };
  }

  if (!response.ok) {
    return { ok: false, autenticado: true, documentos: [], erro: `Falha ao listar documentos (${response.status}).` };
  }

  const body = await response.json() as PageBody;
  const items = body.content ?? body.items ?? [];
  return {
    ok: true,
    autenticado: true,
    documentos: selecionarProntos(items)
  };
}

chrome.runtime.onMessage.addListener((message: ExtensionMessage, _sender, sendResponse) => {
  const run = async () => {
    if (message.type === 'LOGIN') return login(message.email, message.senha);
    if (message.type === 'LOGOUT') {
      await chrome.storage.local.remove([TOKEN_KEY, EMAIL_KEY, NOME_KEY]);
      return { autenticado: false, email: null, nome: null };
    }
    if (message.type === 'AUTH_STATUS') return readSession();
    return fetchDocumentos();
  };

  void run()
    .then((result) => sendResponse(result))
    .catch((error: unknown) => {
      const texto = error instanceof Error ? error.message : 'Falha inesperada.';
      sendResponse({ ok: false, autenticado: false, documentos: [], erro: texto, email: null, nome: null });
    });

  return true;
});
