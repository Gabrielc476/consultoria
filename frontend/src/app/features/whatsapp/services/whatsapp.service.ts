import { Injectable, computed, signal, inject, effect } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, catchError, of } from 'rxjs';
import {
  WhatsAppContact,
  WhatsAppMessage,
  WhatsAppAnexo,
  ChatResumo,
  CadastrarContatoRequest,
  MensagemChatDto,
  ContatoResponse,
  ContatoConvenioItem
} from '../model/whatsapp.model';
import { AuthService } from '../../../core/auth/auth.service';
import { API_ENDPOINTS } from '../../../core/api/api-endpoints';

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

const CORES_AVATAR = [
  'bg-emerald-600',
  'bg-gov-cobalt-600',
  'bg-purple-600',
  'bg-amber-600',
  'bg-rose-600',
  'bg-cyan-600',
  'bg-indigo-600'
];

@Injectable({
  providedIn: 'root'
})
export class WhatsAppService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService, { optional: true });

  readonly contatos = signal<WhatsAppContact[]>([]);
  readonly contatoAtivoId = signal<string | null>(null);
  readonly chatsRecentes = signal<ChatResumo[]>([]);
  readonly carregandoContatos = signal<boolean>(false);
  readonly carregandoChatsRecentes = signal<boolean>(false);
  readonly carregandoMensagens = signal<boolean>(false);
  readonly enviandoMensagem = signal<boolean>(false);
  readonly sincronizandoHistorico = signal<boolean>(false);

  private readonly mensagensPorChat = signal<Record<string, WhatsAppMessage[]>>({});

  constructor() {
    this.inicializarDados();
    if (this.authService) {
      effect(() => {
        const token = this.authService?.token();
        if (token) {
          this.inicializarDados();
        }
      });
    }
  }

  private isDemoOrUnauthenticated(): boolean {
    if (!this.authService) return true;
    return this.authService.isModoDemo() || !this.authService.isAuthenticated();
  }

  inicializarDados(): void {
    if (this.isDemoOrUnauthenticated()) {
      this.contatos.set(MOCK_CONTACTS);
      this.contatoAtivoId.set('chat-carlos-mendes');
      this.mensagensPorChat.set({
        'chat-carlos-mendes': MOCK_MESSAGES_CARLOS,
        'chat-marcos-vinicius': MOCK_MESSAGES_MARCOS
      });
    } else {
      this.carregarContatos();
    }
  }

  readonly contatoAtivo = computed<WhatsAppContact | null>(() => {
    const id = this.contatoAtivoId();
    const lista = this.contatos();
    if (lista.length === 0) return null;
    if (!id) return lista[0] || null;
    return lista.find(c => c.id === id) || lista[0] || null;
  });

  readonly mensagensDoChatAtivo = computed<WhatsAppMessage[]>(() => {
    const id = this.contatoAtivoId();
    if (!id) return [];
    return this.mensagensPorChat()[id] || [];
  });

  readonly totalNaoLidas = computed<number>(() => {
    return this.contatos().reduce((acc, c) => acc + (c.mensagensNaoLidas || 0), 0);
  });

  carregarContatos(): void {
    if (this.isDemoOrUnauthenticated()) return;

    this.carregandoContatos.set(true);
    this.http.get<ContatoResponse[]>(API_ENDPOINTS.CONTATOS.BASE).pipe(
      catchError(err => {
        console.error('Erro ao carregar contatos do WhatsApp:', err);
        return of([] as ContatoResponse[]);
      })
    ).subscribe(contatosResp => {
      this.carregandoContatos.set(false);
      const listaMapeada: WhatsAppContact[] = contatosResp.map((c, idx) => {
        const principalConv = c.convenios?.find(conv => conv.principal) || c.convenios?.[0];
        const papelNorm = (c.papel || '').toUpperCase();
        let categoria: 'TODOS' | 'OBRAS' | 'FINANCAS' | 'JURIDICO' = 'OBRAS';
        if (papelNorm.includes('FINAN') || papelNorm.includes('CONTAB')) {
          categoria = 'FINANCAS';
        } else if (papelNorm.includes('JURID') || papelNorm.includes('PROCURAD')) {
          categoria = 'JURIDICO';
        }

        const corAvatar = CORES_AVATAR[idx % CORES_AVATAR.length];

        return {
          id: c.id,
          nome: c.nome,
          cargo: c.papel || 'Contato Oficial',
          empresaOuOrgao: c.empresaOuOrgao || undefined,
          municipio: principalConv?.objeto ? (principalConv.numeroSiconv ? `Conv. #${principalConv.numeroSiconv}` : 'Convênio Vinculado') : 'Sem convênio',
          telefone: this.formatarTelefone(c.phoneNumber),
          avatarCor: corAvatar,
          online: c.ativo,
          ultimoVisto: c.ativo ? 'Conectado' : 'Inativo',
          mensagensNaoLidas: 0,
          convenios: c.convenios,
          convenioVinculadoId: principalConv?.convenioId,
          convenioVinculadoNumero: principalConv?.numeroSiconv,
          convenioVinculadoObjeto: principalConv?.objeto,
          ultimaMensagemTexto: c.convenios?.length ? `${c.convenios.length} convênio(s) vinculado(s)` : 'Sem mensagens recentes',
          ultimaMensagemHora: '',
          categoria
        };
      });

      this.contatos.set(listaMapeada);

      // Se nenhum contato ativo ou o ativo não está mais na lista, seleciona o primeiro
      const ativoAtual = this.contatoAtivoId();
      if ((!ativoAtual || !listaMapeada.some(c => c.id === ativoAtual)) && listaMapeada.length > 0) {
        this.selecionarContato(listaMapeada[0].id);
      } else if (ativoAtual) {
        this.carregarMensagensDoContato(ativoAtual);
      }
    });
  }

  selecionarContato(id: string): void {
    this.contatoAtivoId.set(id);
    this.contatos.update(lista =>
      lista.map(c => c.id === id ? { ...c, mensagensNaoLidas: 0 } : c)
    );
    this.carregarMensagensDoContato(id);
  }

  carregarMensagensDoContato(contatoId: string): void {
    if (this.isDemoOrUnauthenticated()) return;

    const contato = this.contatos().find(c => c.id === contatoId);
    if (!contato) return;

    const cleanPhone = contato.telefone.replace(/\D/g, '');
    if (!cleanPhone) return;

    this.carregandoMensagens.set(true);
    this.http.get<MensagemChatDto[]>(API_ENDPOINTS.WHATSAPP.MENSAGENS(cleanPhone)).pipe(
      catchError(err => {
        console.error(`Erro ao carregar mensagens para telefone ${cleanPhone}:`, err);
        return of([] as MensagemChatDto[]);
      })
    ).subscribe(msgsDto => {
      this.carregandoMensagens.set(false);
      const mensagens: WhatsAppMessage[] = msgsDto.map(m => this.converterDtoParaMensagem(m, contatoId));

      this.mensagensPorChat.update(mapa => ({
        ...mapa,
        [contatoId]: mensagens
      }));

      if (mensagens.length > 0) {
        const ultima = mensagens[mensagens.length - 1];
        this.contatos.update(lista =>
          lista.map(c => c.id === contatoId
            ? { ...c, ultimaMensagemTexto: ultima.texto, ultimaMensagemHora: ultima.dataHora }
            : c
          )
        );
      }
    });
  }

  carregarChatsRecentesEvolution(): Observable<ChatResumo[]> {
    this.carregandoChatsRecentes.set(true);
    return this.http.get<ChatResumo[]>(API_ENDPOINTS.WHATSAPP.CHATS_RECENTES).pipe(
      tap(res => {
        this.chatsRecentes.set(res || []);
        this.carregandoChatsRecentes.set(false);
      }),
      catchError(err => {
        console.error('Erro ao consultar chats recentes da Evolution API:', err);
        this.chatsRecentes.set([]);
        this.carregandoChatsRecentes.set(false);
        return of([] as ChatResumo[]);
      })
    );
  }

  cadastrarContatoEImportar(payload: CadastrarContatoRequest): Observable<ContatoResponse> {
    return this.http.post<ContatoResponse>(API_ENDPOINTS.CONTATOS.BASE, payload).pipe(
      tap(novoContato => {
        // Imediatamente sincroniza as últimas 10 mensagens da Evolution API
        const cleanPhone = payload.phoneNumber.replace(/\D/g, '');
        this.sincronizarHistorico(cleanPhone, novoContato.id).subscribe();
        // Recarrega contatos e seleciona o recém criado
        this.carregarContatos();
        setTimeout(() => this.selecionarContato(novoContato.id), 500);
      })
    );
  }

  sincronizarHistorico(phoneNumber?: string, contatoId?: string): Observable<MensagemChatDto[]> {
    const contato = this.contatoAtivo();
    const phone = phoneNumber || (contato ? contato.telefone.replace(/\D/g, '') : null);
    const idContato = contatoId || contato?.id;

    if (!phone) return of([]);

    this.sincronizandoHistorico.set(true);
    const params: any = {};
    if (idContato) params.contatoId = idContato;

    return this.http.post<MensagemChatDto[]>(API_ENDPOINTS.WHATSAPP.SINCRONIZAR_HISTORICO(phone), {}, { params }).pipe(
      tap(msgsDto => {
        this.sincronizandoHistorico.set(false);
        if (idContato && msgsDto && msgsDto.length > 0) {
          const mensagens = msgsDto.map(m => this.converterDtoParaMensagem(m, idContato));
          this.mensagensPorChat.update(mapa => ({
            ...mapa,
            [idContato]: mensagens
          }));
          const ultima = mensagens[mensagens.length - 1];
          this.contatos.update(lista =>
            lista.map(c => c.id === idContato
              ? { ...c, ultimaMensagemTexto: ultima.texto, ultimaMensagemHora: ultima.dataHora }
              : c
            )
          );
        }
      }),
      catchError(err => {
        console.error('Erro ao sincronizar histórico retroativo:', err);
        this.sincronizandoHistorico.set(false);
        return of([] as MensagemChatDto[]);
      })
    );
  }

  enviarMensagem(texto: string): void {
    if (!texto.trim()) return;

    const contato = this.contatoAtivo();
    if (!contato) return;

    const chatId = contato.id;

    if (this.isDemoOrUnauthenticated()) {
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

      this.contatos.update(lista =>
        lista.map(c =>
          c.id === chatId
            ? { ...c, ultimaMensagemTexto: texto, ultimaMensagemHora: 'Agora' }
            : c
        )
      );
      return;
    }

    // Modo Real via Evolution API
    const cleanPhone = contato.telefone.replace(/\D/g, '');
    this.enviandoMensagem.set(true);

    this.http.post<MensagemChatDto>(
      API_ENDPOINTS.WHATSAPP.ENVIAR(cleanPhone),
      { texto },
      { params: { contatoId: contato.id } }
    ).pipe(
      catchError(err => {
        console.error('Erro ao enviar mensagem pelo WhatsApp:', err);
        this.enviandoMensagem.set(false);
        return of(null);
      })
    ).subscribe(resp => {
      this.enviandoMensagem.set(false);
      if (resp) {
        const msg = this.converterDtoParaMensagem(resp, chatId);
        this.mensagensPorChat.update(mapa => ({
          ...mapa,
          [chatId]: [...(mapa[chatId] || []), msg]
        }));
        this.contatos.update(lista =>
          lista.map(c =>
            c.id === chatId
              ? { ...c, ultimaMensagemTexto: texto, ultimaMensagemHora: msg.dataHora }
              : c
          )
        );
      }
    });
  }

  dispararMacroAlerta(tipo: 'PRAZO_15' | 'SOLICITAR_ART' | 'CONFIRMAR_OBTV'): void {
    const contato = this.contatoAtivo();
    if (!contato) return;

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

  private converterDtoParaMensagem(dto: MensagemChatDto, chatId: string): WhatsAppMessage {
    let remetente: 'CONTATO' | 'USUARIO' | 'BOT_IA' = 'CONTATO';
    if (dto.fromMe || dto.remetente === 'USUARIO') {
      remetente = 'USUARIO';
    } else if (dto.remetente === 'BOT_IA') {
      remetente = 'BOT_IA';
    }

    let textoExibicao = dto.texto || '';
    if (!textoExibicao && dto.audioTranscription) {
      textoExibicao = `🎙️ [Áudio Transcrito]: "${dto.audioTranscription}"`;
    } else if (!textoExibicao && dto.fileName) {
      textoExibicao = `📎 Arquivo anexo: ${dto.fileName}`;
    }

    let anexo: WhatsAppAnexo | undefined = undefined;
    if (dto.messageType === 'DOCUMENT' || dto.messageType === 'AUDIO' || dto.messageType === 'IMAGE' || dto.s3Key) {
      const tipo: 'PDF' | 'IMAGEM' | 'AUDIO' = dto.messageType === 'AUDIO'
        ? 'AUDIO'
        : (dto.messageType === 'IMAGE' ? 'IMAGEM' : 'PDF');

      const tamanhoStr = dto.fileSizeBytes
        ? (dto.fileSizeBytes > 1048576
          ? `${(dto.fileSizeBytes / 1048576).toFixed(1)} MB`
          : `${Math.round(dto.fileSizeBytes / 1024)} KB`)
        : 'Anexo';

      anexo = {
        id: dto.id || Math.random().toString(),
        nome: dto.fileName || (tipo === 'AUDIO' ? 'Audio_WhatsApp.ogg' : 'Documento.pdf'),
        tipo,
        tamanho: tamanhoStr,
        documentoId: dto.s3Key,
        scoreOcr: dto.audioTranscription ? 0.95 : undefined
      };
    }

    return {
      id: dto.id || dto.externalMessageId || `msg-${Date.now()}-${Math.random()}`,
      chatId,
      remetente,
      texto: textoExibicao,
      dataHora: this.formatarDataHora(dto.dataHora),
      status: (dto.status as any) || (remetente === 'USUARIO' ? 'ENTREGUE' : 'LIDO'),
      anexo
    };
  }

  private formatarDataHora(dataIso?: string): string {
    if (!dataIso) return 'Agora';
    try {
      const d = new Date(dataIso);
      const hoje = new Date();
      const mesmoDia = d.getDate() === hoje.getDate() &&
        d.getMonth() === hoje.getMonth() &&
        d.getFullYear() === hoje.getFullYear();

      const horas = d.getHours().toString().padStart(2, '0');
      const minutos = d.getMinutes().toString().padStart(2, '0');

      if (mesmoDia) {
        return `Hoje, ${horas}:${minutos}`;
      }

      const ontem = new Date(hoje);
      ontem.setDate(ontem.getDate() - 1);
      const eraOntem = d.getDate() === ontem.getDate() &&
        d.getMonth() === ontem.getMonth() &&
        d.getFullYear() === ontem.getFullYear();

      if (eraOntem) {
        return `Ontem, ${horas}:${minutos}`;
      }

      const dia = d.getDate().toString().padStart(2, '0');
      const mes = (d.getMonth() + 1).toString().padStart(2, '0');
      return `${dia}/${mes} ${horas}:${minutos}`;
    } catch {
      return 'Recente';
    }
  }

  private formatarTelefone(telefone?: string): string {
    if (!telefone) return '';
    const limpo = telefone.replace(/\D/g, '');
    if (limpo.length === 13 && limpo.startsWith('55')) {
      // 55 83 99999 8888
      return `+55 (${limpo.slice(2, 4)}) ${limpo.slice(4, 9)}-${limpo.slice(9)}`;
    }
    if (limpo.length === 12 && limpo.startsWith('55')) {
      return `+55 (${limpo.slice(2, 4)}) ${limpo.slice(4, 8)}-${limpo.slice(8)}`;
    }
    return `+${limpo}`;
  }
}
