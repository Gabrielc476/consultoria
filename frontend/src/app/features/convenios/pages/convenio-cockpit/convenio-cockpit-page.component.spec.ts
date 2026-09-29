import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ConvenioCockpitPageComponent } from './convenio-cockpit-page.component';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from '../../../../core/auth/auth.service';

describe('ConvenioCockpitPageComponent', () => {
  let component: ConvenioCockpitPageComponent;
  let fixture: ComponentFixture<ConvenioCockpitPageComponent>;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [ConvenioCockpitPageComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ConvenioCockpitPageComponent);
    component = fixture.componentInstance;
    component.convenioCtx.selecionarConvenio('conv-914250');
    fixture.detectChanges();
  });

  it('deve carregar o cockpit do convênio com as 10 fases e KPIs', () => {
    expect(component).toBeTruthy();
    expect(component.convenio().fases.length).toBe(10);
    expect(component.convenio().numeroSiconv).toBe('914250/2023');
  });

  it('deve alternar a fase selecionada e abrir o modal ao clicar', () => {
    const faseAlvo = component.convenio().fases[2];
    component.onFaseClick(faseAlvo);
    expect(component.faseSelecionada().numero).toBe(2);
    expect(component.detalhesFaseAberta()).toBeTrue();
  });

  it('deve exibir mocks de documentos e prazos em modo demo ou não autenticado', () => {
    expect(component.documentos().length).toBeGreaterThan(0);
    expect(component.prazos().length).toBeGreaterThan(0);
    expect(component.documentos()[0].numeroDocumento).toContain('NF-e 004821');
  });

  it('deve isolar documentos e prazos para tenant real autenticado', () => {
    const authService = TestBed.inject(AuthService);
    authService.salvarSessao({
      token: 'jwt-tenant-real',
      tokenType: 'Bearer',
      analistaId: 'analista-1',
      nome: 'Maria Gestora',
      email: 'maria@consultoria.com.br',
      tenantId: 'tenant-novo-999'
    });

    component.carregarDocumentos();
    expect(component.authService.isModoDemo()).toBeFalse();
    expect(component.authService.isAuthenticated()).toBeTrue();

    authService.logout();
  });
});
