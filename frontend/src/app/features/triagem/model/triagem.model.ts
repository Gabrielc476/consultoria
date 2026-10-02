export interface TriagemItem {
  id: string;
  tenantId: string;
  agenteResponsavelId?: string;
  mensagemInboundId?: string;
  documentoId?: string;
  convenioSugeridoId?: string;
  convenioSugeridoNumeroSiconv?: string;
  convenioSugeridoObjeto?: string;
  prefeituraSugeridaId?: string;
  faseSugerida?: string;
  confidenceScore: number;
  motivoAmbiguidade?: string;
  phoneNumber?: string;
  senderName?: string;
  pushName?: string;
  remetenteNovo: boolean;
  conteudoResumo?: string;
  status: 'PENDENTE' | 'RESOLVIDO' | 'IGNORADO';
  resolvidoEm?: string;
  createdAt: string;
  documentoNomeOriginal?: string;
  documentoContentType?: string;
  documentoTamanhoBytes?: number;
}

export interface CadastrarContatoETriarPayload {
  nome: string;
  phoneNumber: string;
  papel?: string;
  empresaOuOrgao?: string;
  conveniosIds: string[];
  convenioPrincipalId?: string;
  faseCicloVida?: string;
  arquivarDocumento: boolean;
}

export interface CadastrarContatoPayload {
  nome: string;
  phoneNumber: string;
  papel?: string;
  empresaOuOrgao?: string;
  conveniosIds: string[];
  convenioPrincipalId?: string;
}

export interface ContatoConvenioDto {
  convenioId: string;
  prefeituraId: string;
  numeroSiconv?: string;
  objeto?: string;
  papelEspecifico?: string;
  principal: boolean;
}

export interface ContatoDto {
  id: string;
  tenantId: string;
  phoneNumber: string;
  nome: string;
  papel?: string;
  empresaOuOrgao?: string;
  ativo: boolean;
  createdAt: string;
  convenios: ContatoConvenioDto[];
}
