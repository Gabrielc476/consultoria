import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ClausulaSuspensivaService } from './clausula-suspensiva.service';
import { API_ENDPOINTS } from '../../../core/api/api-endpoints';
import { DossieClausulaSuspensiva } from '../model/clausula-suspensiva.model';

describe('ClausulaSuspensivaService', () => {
  let service: ClausulaSuspensivaService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ClausulaSuspensivaService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(ClausulaSuspensivaService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('deve obter o dossiê da cláusula suspensiva via HTTP', () => {
    const mockConvenioId = 'conv-123';
    let resultado: DossieClausulaSuspensiva | undefined;

    service.obterDossiePorConvenioId(mockConvenioId).subscribe(data => {
      resultado = data;
    });

    const req = httpMock.expectOne(API_ENDPOINTS.CONVENIOS.CLAUSULA_SUSPENSIVA(mockConvenioId));
    expect(req.request.method).toBe('GET');

    req.flush({
      convenioId: mockConvenioId,
      numeroSiconv: '914250/2023',
      orgaoConcedente: 'FNDE',
      objeto: 'Creche',
      valorGlobal: 2000000,
      valorRepasse: 1800000,
      valorContrapartida: 200000,
      possuiClausulaSuspensiva: true,
      prorrogacaoSolicitada: false,
      diasRestantes: 60,
      criticidade: 'ATENCAO',
      criticidadeDescricao: 'Atenção',
      superada: false,
      condicionantes: []
    });

    expect(resultado).toBeDefined();
    expect(resultado?.numeroSiconv).toBe('914250/2023');
    expect(resultado?.criticidade).toBe('ATENCAO');
  });

  it('deve retornar mock fallback caso a chamada HTTP falhe (resiliência)', () => {
    const mockConvenioId = 'conv-fallback';
    let resultado: DossieClausulaSuspensiva | undefined;

    service.obterDossiePorConvenioId(mockConvenioId, '914250/2023').subscribe(data => {
      resultado = data;
    });

    const req = httpMock.expectOne(API_ENDPOINTS.CONVENIOS.CLAUSULA_SUSPENSIVA(mockConvenioId));
    req.error(new ProgressEvent('Network Error'));

    expect(resultado).toBeDefined();
    expect(resultado?.convenioId).toBe(mockConvenioId);
    expect(resultado?.condicionantes.length).toBe(3);
  });

  it('deve submeter condicionante para análise da Caixa', () => {
    const mockConvenioId = 'conv-123';
    let resultado: any;

    service.submeterParaAnalise(mockConvenioId, 'ENGENHARIA_PROJETOS_SINAPI').subscribe(data => {
      resultado = data;
    });

    const req = httpMock.expectOne(API_ENDPOINTS.CONVENIOS.SUBMETER_PILAR(mockConvenioId, 'ENGENHARIA_PROJETOS_SINAPI'));
    expect(req.request.method).toBe('POST');

    req.flush({
      id: 'cond-1',
      tipo: 'ENGENHARIA_PROJETOS_SINAPI',
      descricaoTipo: 'Engenharia',
      status: 'EM_ANALISE_CAIXA',
      statusDescricao: 'Em Análise pela Caixa'
    });

    expect(resultado).toBeDefined();
    expect(resultado.status).toBe('EM_ANALISE_CAIXA');
  });

  it('deve registrar diligência da Caixa', () => {
    const mockConvenioId = 'conv-123';
    let resultado: any;

    service.registrarDiligencia(mockConvenioId, 'LICENCIAMENTO_AMBIENTAL', {
      observacoes: 'Apresentar certidão SUDEMA',
      dataLimiteSaneamento: '2026-10-30'
    }).subscribe(data => {
      resultado = data;
    });

    const req = httpMock.expectOne(API_ENDPOINTS.CONVENIOS.DILIGENCIA_PILAR(mockConvenioId, 'LICENCIAMENTO_AMBIENTAL'));
    expect(req.request.method).toBe('POST');

    req.flush({
      id: 'cond-2',
      tipo: 'LICENCIAMENTO_AMBIENTAL',
      status: 'DILIGENCIA_EMITIDA',
      statusDescricao: 'Diligência Emitida',
      observacoesAnaliseCaixa: 'Apresentar certidão SUDEMA',
      dataLimiteSaneamento: '2026-10-30'
    });

    expect(resultado.status).toBe('DILIGENCIA_EMITIDA');
    expect(resultado.dataLimiteSaneamento).toBe('2026-10-30');
  });

  it('deve aprovar pilar condicionante com dados técnicos', () => {
    const mockConvenioId = 'conv-123';
    let resultado: any;

    service.aprovarCondicionante(mockConvenioId, 'TITULARIDADE_IMOVEL', {
      numeroDocumentoComprobatorio: 'Matrícula CRI 48912',
      orgaoEmissor: '1º CRI Patos'
    }).subscribe(data => {
      resultado = data;
    });

    const req = httpMock.expectOne(API_ENDPOINTS.CONVENIOS.APROVAR_PILAR(mockConvenioId, 'TITULARIDADE_IMOVEL'));
    expect(req.request.method).toBe('POST');

    req.flush({
      id: 'cond-3',
      tipo: 'TITULARIDADE_IMOVEL',
      status: 'APROVADO',
      statusDescricao: 'Aprovado',
      numeroDocumentoComprobatorio: 'Matrícula CRI 48912'
    });

    expect(resultado.status).toBe('APROVADO');
    expect(resultado.numeroDocumentoComprobatorio).toBe('Matrícula CRI 48912');
  });

  it('deve solicitar prorrogação de prazo fatal', () => {
    const mockConvenioId = 'conv-123';
    let resultado: any;

    service.solicitarProrrogacao(mockConvenioId, {
      novoPrazoProrrogado: '2027-02-15'
    }).subscribe(data => {
      resultado = data;
    });

    const req = httpMock.expectOne(API_ENDPOINTS.CONVENIOS.PRORROGAR(mockConvenioId));
    expect(req.request.method).toBe('POST');

    req.flush({
      convenioId: mockConvenioId,
      prorrogacaoSolicitada: true,
      novoPrazoProrrogado: '2027-02-15',
      prazoFatalEfetivo: '2027-02-15'
    });

    expect(resultado.prorrogacaoSolicitada).toBeTrue();
    expect(resultado.novoPrazoProrrogado).toBe('2027-02-15');
  });

  it('deve superar Cláusula Suspensiva quando todos os pilares forem aprovados', () => {
    const mockConvenioId = 'conv-123';
    let resultado: any;

    service.superarClausula(mockConvenioId, {
      s3KeyTermoRetirada: 's3/termo.pdf'
    }).subscribe(data => {
      resultado = data;
    });

    const req = httpMock.expectOne(API_ENDPOINTS.CONVENIOS.SUPERAR(mockConvenioId));
    expect(req.request.method).toBe('POST');

    req.flush({
      convenioId: mockConvenioId,
      superada: true,
      s3KeyTermoRetirada: 's3/termo.pdf'
    });

    expect(resultado.superada).toBeTrue();
    expect(resultado.s3KeyTermoRetirada).toBe('s3/termo.pdf');
  });
});
