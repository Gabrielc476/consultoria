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

export interface WhatsAppContact {
  id: string;
  nome: string;
  cargo: string;
  municipio: string;
  telefone: string;
  avatarCor: string;
  online: boolean;
  ultimoVisto: string;
  mensagensNaoLidas: number;
  convenioVinculadoId?: string;
  convenioVinculadoNumero?: string;
  convenioVinculadoObjeto?: string;
  ultimaMensagemTexto: string;
  ultimaMensagemHora: string;
  categoria: 'TODOS' | 'OBRAS' | 'FINANCAS' | 'JURIDICO';
}
