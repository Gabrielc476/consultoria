import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { RadarPrazosPageComponent } from './radar-prazos-page.component';
import { RadarPrazosApiService } from '../services/radar-prazos-api.service';
import { AuthService } from '../../../core/auth/auth.service';
import { ToastService } from '../../../core/ui/toast.service';
import { RadarPrazosResponse } from '../model/radar-prazos.model';
import { provideRouter } from '@angular/router';

describe('RadarPrazosPageComponent', () => {
  let component: RadarPrazosPageComponent;
  let fixture: ComponentFixture<RadarPrazosPageComponent>;
  let mockRadarApi: jasmine.SpyObj<RadarPrazosApiService>;
  let mockToast: jasmine.SpyObj<ToastService>;
  let mockAuth: Partial<AuthService>;

  const mockDados: RadarPrazosResponse = {
    resumo: {
      totalMonitorados: 3,
      totalCriticos: 1,
      totalAtencao: 1,
      totalRegulares: 1,
      dataReferencia: '2026-09-24'
    },
    agrupamentoPorMunicipio: [
      {
        municipio: 'Massaranduba',
        uf: 'PB',
        cnpjProponente: '08847784000144',
        nomeProponente: 'PREFEITURA DE MASSARANDUBA',
        totalConvenios: 2,
        totalCriticos: 1,
        totalAtencao: 1,
        totalRegulares: 0,
        maiorRisco: 'CRITICO'
      },
      {
        municipio: 'Campina Grande',
        uf: 'PB',
        cnpjProponente: '08847784000145',
        nomeProponente: 'PREFEITURA DE CAMPINA GRANDE',
        totalConvenios: 1,
        totalCriticos: 0,
        totalAtencao: 0,
        totalRegulares: 1,
        maiorRisco: 'REGULAR'
      }
    ],
    alertas: [
      {
        convenioId: 'c1',
        nrConvenio: '900001/2024',
        municipio: 'Massaranduba',
        uf: 'PB',
        cnpjProponente: '08847784000144',
        nomeProponente: 'PREFEITURA DE MASSARANDUBA',
        objeto: 'Pavimentação de ruas',
        nivelRisco: 'CRITICO',
        tipoPrazoMaisProximo: 'CLAUSULA_SUSPENSIVA',
        prazoMaisProximo: '2026-09-29',
        diasRestantes: 5,
        valorGlobal: 500000,
        valorRepasse: 450000
      },
      {
        convenioId: 'c2',
        nrConvenio: '900002/2024',
        municipio: 'Massaranduba',
        uf: 'PB',
        cnpjProponente: '08847784000144',
        nomeProponente: 'PREFEITURA DE MASSARANDUBA',
        objeto: 'Reforma de UBS',
        nivelRisco: 'ATENCAO',
        tipoPrazoMaisProximo: 'FIM_VIGENCIA',
        prazoMaisProximo: '2026-10-24',
        diasRestantes: 30,
        valorGlobal: 300000,
        valorRepasse: 280000
      },
      {
        convenioId: 'c3',
        nrConvenio: '900003/2024',
        municipio: 'Campina Grande',
        uf: 'PB',
        cnpjProponente: '08847784000145',
        nomeProponente: 'PREFEITURA DE CAMPINA GRANDE',
        objeto: 'Construção de Escola',
        nivelRisco: 'REGULAR',
        tipoPrazoMaisProximo: 'FIM_VIGENCIA',
        prazoMaisProximo: '2027-01-10',
        diasRestantes: 108,
        valorGlobal: 1000000,
        valorRepasse: 900000
      }
    ]
  };

  beforeEach(async () => {
    mockRadarApi = jasmine.createSpyObj('RadarPrazosApiService', ['getRadar', 'dispararAvaliacao']);
    mockToast = jasmine.createSpyObj('ToastService', ['sucesso', 'erro', 'aviso']);
    mockAuth = {
      usuario: signal<any>({
        analistaId: 'u1',
        nome: 'Gabriel Analista',
        email: 'gabriel@govflow.com.br',
        tenantId: 't1'
      }),
      logout: jasmine.createSpy('logout')
    };

    mockRadarApi.getRadar.and.returnValue(of(mockDados));

    await TestBed.configureTestingModule({
      imports: [RadarPrazosPageComponent],
      providers: [
        provideRouter([]),
        { provide: RadarPrazosApiService, useValue: mockRadarApi },
        { provide: ToastService, useValue: mockToast },
        { provide: AuthService, useValue: mockAuth }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(RadarPrazosPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('deve inicializar e carregar dados do radar', () => {
    expect(component).toBeTruthy();
    expect(mockRadarApi.getRadar).toHaveBeenCalled();
    expect(component.resumo().totalMonitorados).toBe(3);
    expect(component.resumo().totalCriticos).toBe(1);
    expect(component.resumo().totalAtencao).toBe(1);
    expect(component.resumo().totalRegulares).toBe(1);
  });

  it('deve filtrar convênios por nível de risco CRITICO', () => {
    component.setFiltroRisco('CRITICO');
    fixture.detectChanges();

    const alertas = component.alertasFiltrados();
    expect(alertas.length).toBe(1);
    expect(alertas[0].nrConvenio).toBe('900001/2024');
    expect(alertas[0].nivelRisco).toBe('CRITICO');
  });

  it('deve filtrar convênios por termo de busca', () => {
    component.termoBusca.set('Campina');
    fixture.detectChanges();

    const alertas = component.alertasFiltrados();
    expect(alertas.length).toBe(1);
    expect(alertas[0].municipio).toBe('Campina Grande');
  });

  it('deve alternar entre visão de convênios e visão de municípios', () => {
    expect(component.visaoAtiva()).toBe('CONVENIOS');

    component.alternarVisao('MUNICIPIOS');
    expect(component.visaoAtiva()).toBe('MUNICIPIOS');
    expect(component.municipiosFiltrados().length).toBe(2);
  });

  it('deve disparar avaliação de prazos e notificar via toast', () => {
    mockRadarApi.dispararAvaliacao.and.returnValue(of({ status: 'SUCESSO', totalAlertasCriticosEmitidos: 1 }));

    component.dispararAvaliacaoEAlertas();

    expect(mockRadarApi.dispararAvaliacao).toHaveBeenCalled();
    expect(mockToast.sucesso).toHaveBeenCalledWith(
      'Avaliação de Prazos Concluída',
      jasmine.stringContaining('1 alertas de prazos críticos despachados')
    );
  });
});
