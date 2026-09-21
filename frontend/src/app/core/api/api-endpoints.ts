export const API_BASE_URL = 'http://localhost:8080/api/v1';

export const API_ENDPOINTS = {
  AUTH: {
    LOGIN: `${API_BASE_URL}/auth/login`,
  },
  DOCUMENTOS: {
    BASE: `${API_BASE_URL}/documentos`,
    POR_ID: (id: string) => `${API_BASE_URL}/documentos/${id}`,
    ARQUIVO: (id: string) => `${API_BASE_URL}/documentos/${id}/arquivo`,
    APROVAR: (id: string) => `${API_BASE_URL}/documentos/${id}/aprovar`,
    REJEITAR: (id: string) => `${API_BASE_URL}/documentos/${id}/rejeitar`,
    AUDITORIA: (id: string) => `${API_BASE_URL}/documentos/${id}/auditoria`,
  },
  PREFEITURAS: {
    BASE: `${API_BASE_URL}/prefeituras`,
  }
};
