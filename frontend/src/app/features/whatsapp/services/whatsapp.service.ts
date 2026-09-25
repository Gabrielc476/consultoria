import { Injectable, computed, signal } from '@angular/core';
import { WhatsAppContact, WhatsAppMessage } from '../model/whatsapp.model';

const MOCK_CONTACTS: WhatsAppContact[] = [
  {
    id: 'chat-carlos-mendes',
    nome: 'Eng. Carlos Mendes',
    cargo: 'Fiscal de Obras - SMOP',
    municipio: 'Patos - PB',
    telefone: '+55 (83) 99824-1102',
    avatarCor: 'bg-emerald-600',
    online: true,
    ultimoVisto: 'Online agora',
    mensagensNaoLidas: 2,
    convenioVinculadoId: 'conv-914250',
    convenioVinculadoNumero: '914250/2023',
    convenioVinculadoObjeto: 'Construção de Creche Proinfância - Jatobá',
    ultimaMensagemTexto: 'Segue a NF-e 004821 e o Boletim BM-03 da Creche.',
    ultimaMensagemHora: '09:15',
    categoria: 'OBRAS'
  },
  {
    id: 'chat-marcos-vinicius',
    nome: 'Sec. Marcos Vinícius',
    cargo: 'Secretário de Finanças',
    municipio: 'Patos - PB',
    telefone: '+55 (83) 99105-3340',
    avatarCor: 'bg-gov-cobalt-600',
    online: false,
    ultimoVisto: 'Visto às 08:45',
    mensagensNaoLidas: 0,
    convenioVinculadoId: 'conv-914250',
    convenioVinculadoNumero: '914250/2023',
    convenioVinculadoObjeto: 'Construção de Creche Proinfância - Jatobá',
    ultimaMensagemTexto: 'O saldo da conta Op 006 (R$ 412k) já foi liberado?',
    ultimaMensagemHora: 'Ontem',
    categoria: 'FINANCAS'
  },
  {
    id: 'chat-juliana-torres',
    nome: 'Dra. Juliana Torres',
    cargo: 'Procuradora Municipal',
    municipio: 'Patos - PB',
    telefone: '+55 (83) 98712-9981',
    avatarCor: 'bg-purple-600',
    online: true,
    ultimoVisto: 'Online agora',
    mensagensNaoLidas: 0,
    convenioVinculadoId: 'conv-899201',
    convenioVinculadoNumero: '899201/2022',
    convenioVinculadoObjeto: 'Patrulha Mecanizada Agricultura Familiar',
    ultimaMensagemTexto: 'Ação judicial de blindagem (Súmula 230 TCU) protocolada.',
    ultimaMensagemHora: '18/09',
    categoria: 'JURIDICO'
  },
  {
    id: 'chat-roberto-alvorada',
    nome: 'Roberto Alencar',
    cargo: 'Diretor - Construtora Alvorada',
    municipio: 'Patos - PB',
    telefone: '+55 (83) 98845-7761',
    avatarCor: 'bg-amber-600',
    online: false,
    ultimoVisto: 'Visto há 3 horas',
    mensagensNaoLidas: 0,
    convenioVinculadoId: 'conv-914250',
    convenioVinculadoNumero: '914250/2023',
    convenioVinculadoObjeto: 'Construção de Creche Proinfância - Jatobá',
    ultimaMensagemTexto: 'Comprovante do DARF Previdenciário enviado.',
    ultimaMensagemHora: '17/09',
    categoria: 'OBRAS'
  }
];

const MOCK_MESSAGES_CARLOS: WhatsAppMessage[] = [
  {
    id: 'msg-01',
    chatId: 'chat-carlos-mendes',
    remetente: 'CONTATO',
    texto: 'Bom dia! Fizemos a vistoria da 3ª medição da Creche Jatobá ontem à tarde.',
    dataHora: 'Hoje, 09:12',
    status: 'LIDO'
  },
  {
    id: 'msg-02',
    chatId: 'chat-carlos-mendes',
    remetente: 'CONTATO',
    texto: 'A construtora concluiu a concretagem da laje e o assentamento dos tijolos. Segue a nota fiscal emitida e o boletim de medição assinado com a ART.',
    dataHora: 'Hoje, 09:14',
    status: 'LIDO',
    anexo: {
      id: 'anx-01',
      nome: 'NF-004821_Construtora_Alvorada.pdf',
      tipo: 'PDF',
      tamanho: '84.5 KB',
      scoreOcr: 0.985,
      documentoId: 'doc-nf-004821',
      faseConvenio: 'Fase 05 (Liquidação OBTV)'
    }
  },
  {
    id: 'msg-03',
    chatId: 'chat-carlos-mendes',
    remetente: 'CONTATO',
    texto: 'Aqui está também o Boletim de Medição BM-03 detalhado.',
    dataHora: 'Hoje, 09:15',
    status: 'LIDO',
    anexo: {
      id: 'anx-02',
      nome: 'Boletim_Medicao_BM-03_Creche.pdf',
      tipo: 'PDF',
      tamanho: '1.2 MB',
      scoreOcr: 0.942,
      documentoId: 'doc-bm-003',
      faseConvenio: 'Fase 04 (Execução Física RAE)'
    }
  },
  {
    id: 'msg-04',
    chatId: 'chat-carlos-mendes',
    remetente: 'BOT_IA',
    texto: '🤖 GovFlow OCR: Documentos recebidos com sucesso! NF-e 004821 processada via IA com 98.5% de confiança. Valor Bruto identificado: R$ 84.500,00. Encaminhado para a fila de conferência na Esteira.',
    dataHora: 'Hoje, 09:15',
    status: 'LIDO'
  },
  {
    id: 'msg-05',
    chatId: 'chat-carlos-mendes',
    remetente: 'USUARIO',
    texto: 'Excelente Carlos! Já estamos conferindo os dados fiscais e as certidões CND para liberar a ordem de pagamento no Transferegov.',
    dataHora: 'Hoje, 09:18',
    status: 'ENTREGUE'
  }
];

const MOCK_MESSAGES_MARCOS: WhatsAppMessage[] = [
  {
    id: 'msg-m-01',
    chatId: 'chat-marcos-vinicius',
    remetente: 'CONTATO',
    texto: 'Prezado consultor, como está a situação do desbloqueio da 2ª parcela da creche na Caixa?',
    dataHora: 'Ontem, 16:30',
    status: 'LIDO'
  },
  {
    id: 'msg-m-02',
    chatId: 'chat-marcos-vinicius',
    remetente: 'USUARIO',
    texto: 'Secretário, a Caixa já homologou o RAE de 68%. O saldo de R$ 412.300,50 está creditado na conta Op 006 e disponível para liquidação das medições via OBTV.',
    dataHora: 'Ontem, 16:35',
    status: 'LIDO'
  },
  {
    id: 'msg-m-03',
    chatId: 'chat-marcos-vinicius',
    remetente: 'CONTATO',
    texto: 'Perfeito, vou orientar a equipe a emitir a autorização bancária assim que a nota for conferida no sistema.',
    dataHora: 'Ontem, 16:42',
    status: 'LIDO'
  }
];

@Injectable({
  providedIn: 'root'
})
export class WhatsAppService {
  readonly contatos = signal<WhatsAppContact[]>(MOCK_CONTACTS);
  readonly contatoAtivoId = signal<string>('chat-carlos-mendes');

  private readonly mensagensPorChat = signal<Record<string, WhatsAppMessage[]>>({
    'chat-carlos-mendes': MOCK_MESSAGES_CARLOS,
    'chat-marcos-vinicius': MOCK_MESSAGES_MARCOS
  });

  readonly contatoAtivo = computed(() => {
    const id = this.contatoAtivoId();
    return this.contatos().find(c => c.id === id) || this.contatos()[0];
  });

  readonly mensagensDoChatAtivo = computed(() => {
    const id = this.contatoAtivoId();
    return this.mensagensPorChat()[id] || [];
  });

  selecionarContato(id: string): void {
    this.contatoAtivoId.set(id);
    // Zera mensagens não lidas
    this.contatos.update(lista =>
      lista.map(c => c.id === id ? { ...c, mensagensNaoLidas: 0 } : c)
    );
  }

  enviarMensagem(texto: string): void {
    if (!texto.trim()) return;

    const chatId = this.contatoAtivoId();
    const novaMensagem: WhatsAppMessage = {
      id: `msg-${Date.now()}`,
      chatId,
      remetente: 'USUARIO',
      texto,
      dataHora: 'Agora',
      status: 'ENVIADO'
    };

    this.mensagensPorChat.update(mapa => ({
      ...mapa,
      [chatId]: [...(mapa[chatId] || []), novaMensagem]
    }));

    // Atualiza preview no contato
    this.contatos.update(lista =>
      lista.map(c =>
        c.id === chatId
          ? { ...c, ultimaMensagemTexto: texto, ultimaMensagemHora: 'Agora' }
          : c
      )
    );
  }

  dispararMacroAlerta(tipo: 'PRAZO_15' | 'SOLICITAR_ART' | 'CONFIRMAR_OBTV'): void {
    const contato = this.contatoAtivo();
    let texto = '';

    if (tipo === 'PRAZO_15') {
      texto = `⚠️ Alerta de Prazo Transferegov: O convênio #${contato.convenioVinculadoNumero || '914250'} possui prazo fatal de liquidação em 18 dias. Favor regularizar pendências para evitar bloqueio no SIAFI.`;
    } else if (tipo === 'SOLICITAR_ART') {
      texto = `Prezado ${contato.nome}, solicitamos o envio urgente da ART de execução complementar e o Diário de Obra atualizado para anexação ao Transferegov.`;
    } else if (tipo === 'CONFIRMAR_OBTV') {
      texto = `Informamos que a Ordem Bancária de Transferência Voluntária (OBTV) no valor de R$ 84.500,00 foi enviada para comando no banco com sucesso.`;
    }

    this.enviarMensagem(texto);
  }
}
