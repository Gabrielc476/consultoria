import { TestBed } from '@angular/core/testing';
import { WhatsAppService } from './whatsapp.service';

describe('WhatsAppService', () => {
  let service: WhatsAppService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [WhatsAppService]
    });
    service = TestBed.inject(WhatsAppService);
  });

  it('deve ser instanciado com contatos e mensagens mockadas', () => {
    expect(service).toBeTruthy();
    expect(service.contatos().length).toBeGreaterThan(0);
    expect(service.contatoAtivo()).toBeTruthy();
    expect(service.mensagensDoChatAtivo().length).toBeGreaterThan(0);
  });

  it('deve selecionar outro contato e zerar mensagens não lidas', () => {
    service.selecionarContato('chat-carlos-mendes');
    expect(service.contatoAtivo().id).toBe('chat-carlos-mendes');
    expect(service.contatoAtivo().mensagensNaoLidas).toBe(0);
  });

  it('deve enviar uma nova mensagem e adicioná-la à thread', () => {
    const totalInicial = service.mensagensDoChatAtivo().length;
    service.enviarMensagem('Olá fiscal, medição aprovada.');
    const mensagens = service.mensagensDoChatAtivo();
    expect(mensagens.length).toBe(totalInicial + 1);
    expect(mensagens[mensagens.length - 1].texto).toBe('Olá fiscal, medição aprovada.');
    expect(mensagens[mensagens.length - 1].remetente).toBe('USUARIO');
  });

  it('deve disparar macro de alerta de prazo de 15 dias', () => {
    const totalInicial = service.mensagensDoChatAtivo().length;
    service.dispararMacroAlerta('PRAZO_15');
    const mensagens = service.mensagensDoChatAtivo();
    expect(mensagens.length).toBe(totalInicial + 1);
    expect(mensagens[mensagens.length - 1].texto).toContain('Alerta de Prazo Transferegov');
  });
});
