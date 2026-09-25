import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ConveniosListPageComponent } from './convenios-list-page.component';

describe('ConveniosListPageComponent', () => {
  let component: ConveniosListPageComponent;
  let fixture: ComponentFixture<ConveniosListPageComponent>;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [ConveniosListPageComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ConveniosListPageComponent);
    component = fixture.componentInstance;
    component.municipioCtx.selecionarMunicipio('mun-patos-01');
    fixture.detectChanges();
  });

  it('deve criar a página de lista de convênios com os convênios do município', () => {
    expect(component).toBeTruthy();
    expect(component.conveniosDoMunicipio().length).toBeGreaterThan(0);
  });

  it('deve filtrar convênios por termo de busca', () => {
    component.termoBusca.set('Jatobá');
    fixture.detectChanges();
    const filtrados = component.conveniosFiltrados();
    expect(filtrados.length).toBe(1);
    expect(filtrados[0].objeto).toContain('Jatobá');
  });

  it('deve filtrar convênios por grupo de fases', () => {
    component.filtroFase.set('PLANEJAMENTO');
    fixture.detectChanges();
    const filtrados = component.conveniosFiltrados();
    expect(filtrados.every(c => c.faseAtualNumero <= 3)).toBeTrue();
  });
});
