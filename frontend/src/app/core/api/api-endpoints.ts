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
  },
  TRANSFEREGOV: {
    BASE: `${API_BASE_URL}/transferegov`,
    CONVENIOS: `${API_BASE_URL}/transferegov/convenios`,
    RADAR_PRAZOS: `${API_BASE_URL}/transferegov/radar-prazos`,
    AVALIAR_PRAZOS: `${API_BASE_URL}/transferegov/radar-prazos/avaliar`,
  },
  CAUC: {
    BASE: `${API_BASE_URL}/cauc`,
    RESUMO: `${API_BASE_URL}/cauc/resumo`,
    POR_PREFEITURA: (id: string) => `${API_BASE_URL}/cauc/prefeituras/${id}`,
    CERTIDOES: (id: string) => `${API_BASE_URL}/cauc/prefeituras/${id}/certidoes`,
    AVALIAR: `${API_BASE_URL}/cauc/avaliar`,
  },
  CONVENIOS: {
    BASE: `${API_BASE_URL}/convenios`,
    POR_ID: (id: string) => `${API_BASE_URL}/convenios/${id}`,
    CLAUSULA_SUSPENSIVA: (id: string) => `${API_BASE_URL}/convenios/${id}/clausula-suspensiva`,
    SUBMETER_PILAR: (id: string, tipo: string) => `${API_BASE_URL}/convenios/${id}/clausula-suspensiva/condicionantes/${tipo}/submeter`,
    DILIGENCIA_PILAR: (id: string, tipo: string) => `${API_BASE_URL}/convenios/${id}/clausula-suspensiva/condicionantes/${tipo}/diligencia`,
    APROVAR_PILAR: (id: string, tipo: string) => `${API_BASE_URL}/convenios/${id}/clausula-suspensiva/condicionantes/${tipo}/aprovar`,
    UPLOAD_DOCUMENTO: (id: string, tipo: string) => `${API_BASE_URL}/convenios/${id}/clausula-suspensiva/condicionantes/${tipo}/documentos`,
    UPLOAD_LAUDO_PENDENCIAS: (id: string, tipo: string) => `${API_BASE_URL}/convenios/${id}/clausula-suspensiva/condicionantes/${tipo}/laudo-pendencias`,
    PRORROGAR: (id: string) => `${API_BASE_URL}/convenios/${id}/clausula-suspensiva/prorrogacao`,
    SUPERAR: (id: string) => `${API_BASE_URL}/convenios/${id}/clausula-suspensiva/superar`,
    UPLOAD_TERMO_RETIRADA: (id: string) => `${API_BASE_URL}/convenios/${id}/clausula-suspensiva/termo-retirada`,
  }
};

