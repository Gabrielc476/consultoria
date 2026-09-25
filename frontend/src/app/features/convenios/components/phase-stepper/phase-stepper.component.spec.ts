import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PhaseStepperComponent } from './phase-stepper.component';
import { FaseConvenio } from '../../model/convenio-fase.model';

describe('PhaseStepperComponent', () => {
  let component: PhaseStepperComponent;
  let fixture: ComponentFixture<PhaseStepperComponent>;

  const mockFases: FaseConvenio[] = [
    { numero: 0, codigo: 'Fase 00', nome: 'CAUC', subtitulo: 'LRF 25', status: 'CONCLUIDA', descricao: 'Desc 0' },
    { numero: 1, codigo: 'Fase 01', nome: 'Proposta', subtitulo: 'SICONV', status: 'CONCLUIDA', descricao: 'Desc 1' },
    { numero: 2, codigo: 'Fase 02', nome: 'Clausula', subtitulo: 'Caixa', status: 'EM_ANDAMENTO', descricao: 'Desc 2' }
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PhaseStepperComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(PhaseStepperComponent);
    component = fixture.componentInstance;
    component.fases = mockFases;
    fixture.detectChanges();
  });

  it('deve criar o componente stepper com as fases informadas', () => {
    expect(component).toBeTruthy();
    const compiled = fixture.nativeElement as HTMLElement;
    const botoes = compiled.querySelectorAll('button');
    expect(botoes.length).toBe(3);
  });

  it('deve emitir o evento faseSelecionada ao clicar na fase', () => {
    spyOn(component.faseSelecionada, 'emit');
    component.selecionar(mockFases[1]);
    expect(component.faseSelecionada.emit).toHaveBeenCalledWith(mockFases[1]);
  });
});
