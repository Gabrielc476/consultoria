import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { WhatsAppHubPageComponent } from './whatsapp-hub-page.component';

describe('WhatsAppHubPageComponent', () => {
  let component: WhatsAppHubPageComponent;
  let fixture: ComponentFixture<WhatsAppHubPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WhatsAppHubPageComponent],
      providers: [provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(WhatsAppHubPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('deve criar a central de mensageria WhatsApp com contatos e thread', () => {
    expect(component).toBeTruthy();
    expect(component.contatosFiltrados().length).toBeGreaterThan(0);
    expect(component.whatsService.contatoAtivo()).toBeTruthy();
  });

  it('deve enviar mensagem digitada ao acionar o envio', () => {
    component.textoNovaMensagem = 'Mensagem de teste';
    component.enviar();
    expect(component.textoNovaMensagem).toBe('');
    const mensagens = component.whatsService.mensagensDoChatAtivo();
    expect(mensagens[mensagens.length - 1].texto).toBe('Mensagem de teste');
  });

  it('deve filtrar contatos pelo termo de busca', () => {
    component.termoBusca.set('Carlos');
    fixture.detectChanges();
    const filtrados = component.contatosFiltrados();
    expect(filtrados.length).toBe(1);
    expect(filtrados[0].nome).toContain('Carlos');
  });
});
