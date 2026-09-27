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
        statusDescricao: 'Em Análise pela Caixa GIGOV',
        s3KeyDocumento: 's3/lae.pdf'
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
      'deferirProrrogacao',
      'superarClausula',
      'uploadDocumento',
      'uploadTermoRetirada',
      'downloadDocumento',
      'downloadTermoRetirada'
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

  it('deve abrir modal de aprovação técnica e aprovar com sucesso', () => {
    const pilarAprovado = {
      ...mockDossie.condicionantes[1],
      status: 'APROVADO' as const,
      statusDescricao: 'Aprovado'
    };
    service.aprovarCondicionante.and.returnValue(of(pilarAprovado));

    component.abrirModalAprovacao('LICENCIAMENTO_AMBIENTAL');
    expect(component.modalAprovacaoAberto()).toBeTrue();
    expect(component.pilarAprovacao()).toBe('LICENCIAMENTO_AMBIENTAL');

    component.aprovacaoNumeroDocumento = 'LI-2026/001';
    component.salvarAprovacaoTecnica();

    expect(service.aprovarCondicionante).toHaveBeenCalledWith(
      'conv-test-123',
      'LICENCIAMENTO_AMBIENTAL',
      jasmine.objectContaining({ numeroDocumentoComprobatorio: 'LI-2026/001' })
    );
    expect(component.modalAprovacaoAberto()).toBeFalse();
    expect(component.mensagemSucesso()).toContain('aprovado com sucesso');
  });

  it('deve barrar aprovação de engenharia com BDI acima de 30% (Acórdão TCU nº 2.622/2013)', () => {
    component.abrirModalAprovacao('ENGENHARIA_PROJETOS_SINAPI');
    component.aprovacaoNumeroDocumento = 'SPA-914250/2026';
    component.aprovacaoPercentualBdi = 31.5;

    component.salvarAprovacaoTecnica();

    expect(service.aprovarCondicionante).not.toHaveBeenCalled();
    expect(component.mensagemErro()).toContain('teto do TCU de 30%');
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

  it('deve protocolar solicitação de prorrogação', () => {
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

  it('deve deferir prorrogação de prazo fatal com sucesso', () => {
    const dossieComSolicitacao = {
      ...mockDossie,
      prorrogacaoSolicitada: true,
      novoPrazoProrrogado: '2024-12-31'
    };
    component.dossie.set(dossieComSolicitacao);

    const dossieDeferido = {
      ...dossieComSolicitacao,
      prorrogacaoSolicitada: false,
      prazoFatalEfetivo: '2024-12-31'
    };
    service.deferirProrrogacao.and.returnValue(of(dossieDeferido));

    component.deferirProrrogacao();

    expect(service.deferirProrrogacao).toHaveBeenCalledWith('conv-test-123', {
      novoPrazoProrrogado: '2024-12-31'
    });
    expect(component.dossie()?.prorrogacaoSolicitada).toBeFalse();
  });

  it('deve solicitar download de documento do MinIO', () => {
    const mockBlob = new Blob(['%PDF'], { type: 'application/pdf' });
    service.downloadDocumento.and.returnValue(of(mockBlob));
    spyOn(window.URL, 'createObjectURL').and.returnValue('blob:http://localhost/mock');
    spyOn(window.URL, 'revokeObjectURL');

    component.baixarDocumento('ENGENHARIA_PROJETOS_SINAPI');

    expect(service.downloadDocumento).toHaveBeenCalledWith('conv-test-123', 'ENGENHARIA_PROJETOS_SINAPI');
    expect(window.URL.createObjectURL).toHaveBeenCalledWith(mockBlob);
  });
});
