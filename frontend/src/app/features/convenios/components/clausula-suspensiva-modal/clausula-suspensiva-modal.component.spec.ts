import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { ClausulaSuspensivaModalComponent } from './clausula-suspensiva-modal.component';
import { ClausulaSuspensivaService } from '../../services/clausula-suspensiva.service';
import { ConvenioCockpit } from '../../model/convenio-fase.model';
import { DossieClausulaSuspensiva } from '../../model/clausula-suspensiva.model';

describe('ClausulaSuspensivaModalComponent', () => {
  let component: ClausulaSuspensivaModalComponent;
  let fixture: ComponentFixture<ClausulaSuspensivaModalComponent>;
  let service: jasmine.SpyObj<ClausulaSuspensivaService>;

  const mockConvenio: ConvenioCockpit = {
    id: 'conv-test-123',
    numeroSiconv: '914250/2023',
    ano: 2023,
    objeto: 'Construção de Creche Municipal e Escola de Educação Infantil',
    municipioNome: 'Patos',
    municipioUf: 'PB',
    orgaoConcedente: 'MEC / FNDE',
    valorTotal: 2500000,
    valorRepasse: 2250000,
    valorContrapartida: 250000,
    percentualExecucao: 0,
    saldoContaOp006: 0,
    diasParaVencimento: 180,
    dataFimVigencia: '2025-12-31',
    faseAtualNumero: 2,
    statusGeral: 'EM_DIA',
    fases: []
  };

  const mockDossie: DossieClausulaSuspensiva = {
    convenioId: 'conv-test-123',
    numeroSiconv: '914250/2023',
    orgaoConcedente: 'MEC / FNDE',
    objeto: 'Construção de Creche Municipal',
    valorGlobal: 2500000,
    valorRepasse: 2250000,
    valorContrapartida: 250000,
    possuiClausulaSuspensiva: true,
    prazoOriginal: '2024-06-12',
    prazoFatalEfetivo: '2024-06-12',
    prorrogacaoSolicitada: false,
    diasRestantes: 45,
    criticidade: 'ATENCAO',
    criticidadeDescricao: 'Atenção (31 a 60 dias)',
    superada: false,
    condicionantes: [
      {
        id: 'cond-eng',
        tipo: 'ENGENHARIA_PROJETOS_SINAPI',
        descricaoTipo: 'Engenharia e Projetos SINAPI',
        status: 'EM_ANALISE_CAIXA',
        statusDescricao: 'Em Análise pela Caixa GIGOV'
      },
      {
        id: 'cond-amb',
        tipo: 'LICENCIAMENTO_AMBIENTAL',
        descricaoTipo: 'Licenciamento Ambiental',
        status: 'PENDENTE',
        statusDescricao: 'Pendente de Documentação'
      },
      {
        id: 'cond-tit',
        tipo: 'TITULARIDADE_IMOVEL',
        descricaoTipo: 'Titularidade do Imóvel',
        status: 'APROVADO',
        statusDescricao: 'Aprovado pelo Agente Financeiro'
      }
    ]
  };

  beforeEach(async () => {
    const serviceSpy = jasmine.createSpyObj('ClausulaSuspensivaService', [
      'obterDossiePorConvenioId',
      'submeterParaAnalise',
      'aprovarCondicionante',
      'registrarDiligencia',
      'solicitarProrrogacao',
      'superarClausula',
      'uploadDocumento',
      'uploadTermoRetirada'
    ]);

    serviceSpy.obterDossiePorConvenioId.and.returnValue(of(mockDossie));

    await TestBed.configureTestingModule({
      imports: [ClausulaSuspensivaModalComponent],
      providers: [
        { provide: ClausulaSuspensivaService, useValue: serviceSpy },
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    service = TestBed.inject(ClausulaSuspensivaService) as jasmine.SpyObj<ClausulaSuspensivaService>;
    fixture = TestBed.createComponent(ClausulaSuspensivaModalComponent);
    component = fixture.componentInstance;
    component.convenio = mockConvenio;
    fixture.detectChanges();
  });

  it('deve criar o componente e carregar o dossiê com os 3 pilares', () => {
    expect(component).toBeTruthy();
    expect(service.obterDossiePorConvenioId).toHaveBeenCalledWith('conv-test-123', '914250/2023');
    expect(component.dossie()).toEqual(mockDossie);
    expect(component.pilarEngenharia()).toBeDefined();
    expect(component.pilarAmbiental()).toBeDefined();
    expect(component.pilarTitularidade()).toBeDefined();
    expect(component.pilaresAprovadosCount()).toBe(1);
  });

  it('deve emitir o evento fechar ao acionar o fechamento', () => {
    spyOn(component.fechar, 'emit');
    const botaoFechar = fixture.nativeElement.querySelector('button[type="button"]');
    botaoFechar.click();
    expect(component.fechar.emit).toHaveBeenCalled();
  });

  it('deve aprovar pilar rapidamente com parecer técnico', () => {
    const pilarAprovado = {
      ...mockDossie.condicionantes[1],
      status: 'APROVADO' as const,
      statusDescricao: 'Aprovado'
    };
    service.aprovarCondicionante.and.returnValue(of(pilarAprovado));

    component.aprovarPilarRapido('LICENCIAMENTO_AMBIENTAL');
    expect(service.aprovarCondicionante).toHaveBeenCalledWith(
      'conv-test-123',
      'LICENCIAMENTO_AMBIENTAL',
      jasmine.objectContaining({ numeroDocumentoComprobatorio: jasmine.any(String) })
    );
    expect(component.mensagemSucesso()).toContain('aprovado com sucesso');
  });

  it('deve abrir modal de diligência e salvar apontamento', () => {
    component.abrirModalDiligencia('ENGENHARIA_PROJETOS_SINAPI');
    expect(component.modalDiligenciaAberto()).toBeTrue();
    expect(component.pilarDiligencia()).toBe('ENGENHARIA_PROJETOS_SINAPI');

    component.diligenciaObservacoes = 'Ajustar BDI desonerado para 22.5%';
    component.diligenciaDataLimite = '2024-05-30';

    const pilarComDiligencia = {
      ...mockDossie.condicionantes[0],
      status: 'DILIGENCIA_EMITIDA' as const,
      statusDescricao: 'Diligência Emitida'
    };
    service.registrarDiligencia.and.returnValue(of(pilarComDiligencia));

    component.salvarDiligencia();

    expect(service.registrarDiligencia).toHaveBeenCalledWith(
      'conv-test-123',
      'ENGENHARIA_PROJETOS_SINAPI',
      {
        observacoes: 'Ajustar BDI desonerado para 22.5%',
        dataLimiteSaneamento: '2024-05-30'
      }
    );
    expect(component.modalDiligenciaAberto()).toBeFalse();
  });

  it('deve protocolar prorrogação de prazo excepcional', () => {
    component.novaDataProrrogacao = '2024-09-30';
    const dossieProrrogado = {
      ...mockDossie,
      prorrogacaoSolicitada: true,
      prazoFatalEfetivo: '2024-09-30'
    };
    service.solicitarProrrogacao.and.returnValue(of(dossieProrrogado));

    component.salvarProrrogacao();

    expect(service.solicitarProrrogacao).toHaveBeenCalledWith('conv-test-123', {
      novoPrazoProrrogado: '2024-09-30'
    });
    expect(component.dossie()?.prorrogacaoSolicitada).toBeTrue();
  });

  it('deve acionar superação da cláusula suspensiva e emitir evento', () => {
    spyOn(component.atualizado, 'emit');
    const dossieSuperado = {
      ...mockDossie,
      superada: true
    };
    service.superarClausula.and.returnValue(of(dossieSuperado));

    component.superarClausulaDireto();

    expect(service.superarClausula).toHaveBeenCalled();
    expect(component.dossie()?.superada).toBeTrue();
    expect(component.atualizado.emit).toHaveBeenCalledWith(dossieSuperado);
  });
});
