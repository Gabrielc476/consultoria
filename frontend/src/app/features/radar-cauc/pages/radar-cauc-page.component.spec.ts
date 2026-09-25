import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RadarCaucPageComponent } from './radar-cauc-page.component';
import { provideRouter } from '@angular/router';

describe('RadarCaucPageComponent', () => {
  let component: RadarCaucPageComponent;
  let fixture: ComponentFixture<RadarCaucPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RadarCaucPageComponent],
      providers: [provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(RadarCaucPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('deve carregar o radar CAUC com lista de municípios e matriz de risco', () => {
    expect(component).toBeTruthy();
    expect(component.municipios().length).toBeGreaterThan(0);
  });

  it('deve abrir o dossiê de certidões do município ao acionar a ação', () => {
    const mun = component.municipios()[0];
    component.abrirDossie(mun);
    expect(component.municipioSelecionado()?.id).toBe(mun.id);
    expect(component.dossieAberto()).toBeTrue();
  });
});
