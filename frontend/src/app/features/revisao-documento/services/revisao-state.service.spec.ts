import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { RevisaoStateService } from './revisao-state.service';
import { Documento } from '../model/documento.model';

describe('RevisaoStateService', () => {
  let service: RevisaoStateService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        RevisaoStateService,
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    });
    service = TestBed.inject(RevisaoStateService);
  });

  it('deve inicializar com valores padrão corretos', () => {
    expect(service.documentoAtual()).toBeNull();
    expect(service.carregando()).toBeFalse();
    expect(service.salvando()).toBeFalse();
    expect(service.totalPendentes()).toBe(0);
  });

  it('deve recalcular valorTotalDeducoes e valorLiquidoCalculado via Signals ao alterar retenções', () => {
    service.atualizarCampo('valorBruto', 10000);
    service.adicionarRetencao('INSS');
    service.atualizarRetencao(0, {
      tipoTributo: 'INSS',
      valorRetido: 1100,
      aliquotaPercentual: 11
    });

    expect(service.valorTotalDeducoes()).toBe(1100);
    expect(service.valorLiquidoCalculado()).toBe(8900);
  });

  it('deve identificar divergência matemática quando líquido informado diferir do calculado', () => {
    service.atualizarCampo('valorBruto', 10000);
    service.adicionarRetencao('ISS');
    service.atualizarRetencao(0, {
      tipoTributo: 'ISS',
      valorRetido: 500
    });

    // Líquido esperado = 9500, mas informado = 9000 (diferença de R$ 500)
    service.atualizarCampo('valorLiquido', 9000);

    expect(service.possuiDivergencia()).toBeTrue();
    expect(service.diferencaLiquido()).toBe(500);
  });

  it('deve sincronizar valor líquido com 1 clique usando sincronizarValorLiquido()', () => {
    service.atualizarCampo('valorBruto', 5000);
    service.adicionarRetencao('INSS');
    service.atualizarRetencao(0, {
      tipoTributo: 'INSS',
      valorRetido: 550
    });
    service.atualizarCampo('valorLiquido', 4000); // divergente

    expect(service.possuiDivergencia()).toBeTrue();

    service.sincronizarValorLiquido();

    expect(service.formulario().valorLiquido).toBe(4450);
    expect(service.possuiDivergencia()).toBeFalse();
  });

  it('deve alternar a visibilidade do drawer de fila', () => {
    expect(service.drawerFilaAberto()).toBeFalse();
    service.toggleDrawer();
    expect(service.drawerFilaAberto()).toBeTrue();
    service.toggleDrawer();
    expect(service.drawerFilaAberto()).toBeFalse();
  });
});
