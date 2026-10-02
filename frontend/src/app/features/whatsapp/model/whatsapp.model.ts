export interface WhatsAppAnexo {
  id: string;
  nome: string;
  tipo: 'PDF' | 'IMAGEM' | 'AUDIO';
  tamanho: string;
  scoreOcr?: number;
  documentoId?: string;
  faseConvenio?: string;
}

export interface WhatsAppMessage {
  id: string;
  chatId: string;
  remetente: 'CONTATO' | 'USUARIO' | 'BOT_IA';
  texto: string;
  dataHora: string;
  status: 'ENVIADO' | 'ENTREGUE' | 'LIDO';
  anexo?: WhatsAppAnexo;
}

export interface ContatoConvenioItem {
  convenioId: string;
  prefeituraId: string;
  numeroSiconv?: string;
  objeto?: string;
  papelEspecifico?: string;
  principal: boolean;
}

export interface ChatResumo {
  remoteJid: string;
  phone: string;
  name: string;
  profilePicUrl?: string;
  lastMessage?: string;
  lastMessageTime?: string;
  isCadastrado: boolean;
}

export interface WhatsAppContact {
  id: string;
  nome: string;
  cargo: string;
  empresaOuOrgao?: string;
  municipio: string;
  telefone: string;
  avatarCor: string;
  online: boolean;
  ultimoVisto: string;
  mensagensNaoLidas: number;
  convenios?: ContatoConvenioItem[];
  convenioVinculadoId?: string;
  convenioVinculadoNumero?: string;
  convenioVinculadoObjeto?: string;
  ultimaMensagemTexto: string;
  ultimaMensagemHora: string;
  categoria: 'TODOS' | 'OBRAS' | 'FINANCAS' | 'JURIDICO';
}

export interface CadastrarContatoRequest {
  nome: string;
  phoneNumber: string;
  papel?: string;
  empresaOuOrgao?: string;
  conveniosIds?: string[];
  convenioPrincipalId?: string;
}

export interface MensagemChatDto {
  id: string;
  externalMessageId?: string;
  senderPhone?: string;
  senderName?: string;
  remetente?: 'CONTATO' | 'USUARIO' | 'BOT_IA';
  fromMe: boolean;
  messageType?: 'TEXT' | 'AUDIO' | 'DOCUMENT' | 'IMAGE';
  texto?: string;
  audioTranscription?: string;
  s3Key?: string;
  mediaUrl?: string;
  mediaMimeType?: string;
  fileName?: string;
  fileSizeBytes?: number;
  dataHora: string;
  status?: string;
}

export interface ContatoResponse {
  id: string;
  tenantId: string;
  phoneNumber: string;
  nome: string;
  papel?: string;
  empresaOuOrgao?: string;
  ativo: boolean;
  createdAt: string;
  convenios: ContatoConvenioItem[];
}

