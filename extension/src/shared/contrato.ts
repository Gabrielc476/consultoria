export const API_BASE_URL = 'http://localhost:8080/api/v1';

export const FORM_SELECTORS = {
  numeroDocumento: '#dh-nr-documento',
  dataEmissao: '#dh-dt-emissao',
  valorBruto: '#dh-vl-documento',
  cnpjCredor: '#dh-cd-credor',
  retencao: (tipo: string) => `#dh-retencao-${tipo.trim().toLowerCase()}`
} as const;

export interface CampoFormulario {
  id: string;
  label: string;
  value: string;
  selector: string;
}

export interface DocumentoPronto {
  id: string;
  updatedAt: string;
  convenioId: string | null;
  campos: CampoFormulario[];
}

export interface RetencaoApi {
  tipo?: string | null;
  valor?: number | string | null;
}

export interface DadosRevisaoApi {
  numeroDocumento?: string | null;
  dataEmissao?: string | null;
  valorBruto?: number | string | null;
  cnpjCredor?: string | null;
  retencoes?: RetencaoApi[] | null;
}

export interface DocumentoApi {
  id?: string;
  convenioId?: string | null;
  updatedAt?: string | null;
  createdAt?: string | null;
  status?: string | null;
  dadosRevisao?: DadosRevisaoApi | null;
}

export type ExtensionMessage =
  | { type: 'LOGIN'; email: string; senha: string }
  | { type: 'LOGOUT' }
  | { type: 'AUTH_STATUS' }
  | { type: 'FETCH_DOCUMENTOS' };

export interface AuthStatus {
  autenticado: boolean;
  email: string | null;
  nome: string | null;
}

export interface LoginResult extends AuthStatus {
  ok: boolean;
  erro?: string;
}

export interface FetchDocumentosResult {
  ok: boolean;
  autenticado: boolean;
  documentos: DocumentoPronto[];
  erro?: string;
}
