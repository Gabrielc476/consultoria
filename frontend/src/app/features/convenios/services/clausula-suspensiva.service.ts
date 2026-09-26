import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { API_ENDPOINTS } from '../../../core/api/api-endpoints';
import {
  AprovarCondicionantePayload,
  AtualizarCondicionantePayload,
  CondicionanteItem,
  DossieClausulaSuspensiva,
  RegistrarDiligenciaPayload,
  SolicitarProrrogacaoPayload,
  SuperarClausulaPayload,
  TipoCondicionante
} from '../model/clausula-suspensiva.model';

export const MOCK_DOSSIE_PADRAO: (convenioId: string, siconv?: string) => DossieClausulaSuspensiva = (
  convenioId: string,
  siconv = '914250/2023'
) => ({
  convenioId,
  numeroSiconv: siconv,
  numeroProcesso: '00124/2023',
  orgaoConcedente: 'FNDE / Ministério da Educação',
  objeto: 'Construção de Creche Proinfância Tipo 2 - Bairro Jatobá',
  valorGlobal: 2050000.0,
  valorRepasse: 1850000.0,
  valorContrapartida: 200000.0,
  possuiClausulaSuspensiva: true,
  prazoOriginal: '2026-11-20',
  prorrogacaoSolicitada: false,
  prazoFatalEfetivo: '2026-11-20',
  diasRestantes: 55,
  criticidade: 'ATENCAO',
  criticidadeDescricao: 'Atenção: Prazo entre 31 e 90 dias',
  superada: false,
  condicionantes: [
    {
      id: 'cond-eng-01',
      tipo: 'ENGENHARIA_PROJETOS_SINAPI',
      descricaoTipo: 'Engenharia, Projetos & Orçamento SINAPI',
      status: 'EM_ANALISE_CAIXA',
      statusDescricao: 'Em Análise pela Caixa',
      numeroDocumentoComprobatorio: 'SPA-914250/2026',
      observacoesAnaliseCaixa: 'Auditoria técnica de engenharia da Caixa GIGOV em andamento.',
      s3KeyDocumento: 'clausula-suspensiva/patos/projeto_basico.pdf',
      valorOrcamentoAprovadoCaixa: 2050000.0,
      percentualBdiAprovado: 22.12,
      numeroArtRrt: 'ART-PB-884210',
      orgaoEmissor: 'Caixa GIGOV Campina Grande'
    },
    {
      id: 'cond-amb-02',
      tipo: 'LICENCIAMENTO_AMBIENTAL',
      descricaoTipo: 'Licenciamento Ambiental',
      status: 'DILIGENCIA_EMITIDA',
      statusDescricao: 'Diligência Emitida',
      numeroDocumentoComprobatorio: 'LI-2024/098',
      dataValidade: '2027-06-30',
      observacoesAnaliseCaixa: 'Necessário apresentar anexo da Outorga de Recursos Hídricos da SUDEMA.',
      s3KeyLaudoPendencias: 'clausula-suspensiva/patos/laudo_pendencias_amb.pdf',
      dataLimiteSaneamento: '2026-10-15',
      orgaoEmissor: 'SUDEMA / Órgão Ambiental PB'
    },
    {
      id: 'cond-tit-03',
      tipo: 'TITULARIDADE_IMOVEL',
      descricaoTipo: 'Comprovação de Titularidade do Imóvel',
      status: 'APROVADO',
      statusDescricao: 'Aprovado',
      numeroDocumentoComprobatorio: 'Matrícula CRI 48.912',
      dataAprovacao: '2026-08-10',
      dataValidade: '2026-11-10',
      observacoesAnaliseCaixa: 'Certidão de Inteiro Teor de Matrícula com ônus reais aceita pela Caixa.',
      s3KeyDocumento: 'clausula-suspensiva/patos/matricula_cri_48912.pdf',
      orgaoEmissor: '1º Cartório de Registro de Imóveis de Patos'
    }
  ]
});

@Injectable({
  providedIn: 'root'
})
export class ClausulaSuspensivaService {
  private readonly http = inject(HttpClient);

  obterDossiePorConvenioId(convenioId: string, numeroSiconv?: string): Observable<DossieClausulaSuspensiva> {
    return this.http.get<DossieClausulaSuspensiva>(API_ENDPOINTS.CONVENIOS.CLAUSULA_SUSPENSIVA(convenioId)).pipe(
      catchError(() => {
        return of(MOCK_DOSSIE_PADRAO(convenioId, numeroSiconv));
      })
    );
  }

  obterDossiePorNumeroSiconv(numeroSiconv: string): Observable<DossieClausulaSuspensiva> {
    return this.http.get<DossieClausulaSuspensiva>(`${API_ENDPOINTS.CONVENIOS.BASE}/siconv/${numeroSiconv}/clausula-suspensiva`).pipe(
      catchError(() => {
        return of(MOCK_DOSSIE_PADRAO(numeroSiconv, numeroSiconv));
      })
    );
  }

  submeterParaAnalise(convenioId: string, tipo: TipoCondicionante): Observable<CondicionanteItem> {
    return this.http.post<CondicionanteItem>(API_ENDPOINTS.CONVENIOS.SUBMETER_PILAR(convenioId, tipo), {}).pipe(
      catchError(() => {
        const item: CondicionanteItem = {
          id: `cond-${Date.now()}`,
          tipo,
          descricaoTipo: this.obterDescricaoTipo(tipo),
          status: 'EM_ANALISE_CAIXA',
          statusDescricao: 'Em Análise pela Caixa'
        };
        return of(item);
      })
    );
  }

  registrarDiligencia(convenioId: string, tipo: TipoCondicionante, payload: RegistrarDiligenciaPayload): Observable<CondicionanteItem> {
    return this.http.post<CondicionanteItem>(API_ENDPOINTS.CONVENIOS.DILIGENCIA_PILAR(convenioId, tipo), payload).pipe(
      catchError(() => {
        const item: CondicionanteItem = {
          id: `cond-${Date.now()}`,
          tipo,
          descricaoTipo: this.obterDescricaoTipo(tipo),
          status: 'DILIGENCIA_EMITIDA',
          statusDescricao: 'Diligência Emitida',
          observacoesAnaliseCaixa: payload.observacoes,
          dataLimiteSaneamento: payload.dataLimiteSaneamento,
          s3KeyLaudoPendencias: payload.s3KeyLaudoPendencias
        };
        return of(item);
      })
    );
  }

  aprovarCondicionante(convenioId: string, tipo: TipoCondicionante, payload: AprovarCondicionantePayload): Observable<CondicionanteItem> {
    return this.http.post<CondicionanteItem>(API_ENDPOINTS.CONVENIOS.APROVAR_PILAR(convenioId, tipo), payload).pipe(
      catchError(() => {
        const item: CondicionanteItem = {
          id: `cond-${Date.now()}`,
          tipo,
          descricaoTipo: this.obterDescricaoTipo(tipo),
          status: 'APROVADO',
          statusDescricao: 'Aprovado',
          numeroDocumentoComprobatorio: payload.numeroDocumentoComprobatorio,
          dataAprovacao: payload.dataAprovacao || new Date().toISOString().split('T')[0],
          dataValidade: payload.dataValidade,
          valorOrcamentoAprovadoCaixa: payload.valorOrcamentoAprovado,
          percentualBdiAprovado: payload.percentualBdiAprovado,
          numeroArtRrt: payload.numeroArtRrt,
          orgaoEmissor: payload.orgaoEmissor,
          s3KeyDocumento: payload.s3KeyDocumento
        };
        return of(item);
      })
    );
  }

  atualizarCondicionante(convenioId: string, tipo: TipoCondicionante, payload: AtualizarCondicionantePayload): Observable<CondicionanteItem> {
    return this.http.put<CondicionanteItem>(`${API_ENDPOINTS.CONVENIOS.CLAUSULA_SUSPENSIVA(convenioId)}/condicionantes/${tipo}`, payload).pipe(
      catchError(() => {
        const item: CondicionanteItem = {
          id: `cond-${Date.now()}`,
          tipo,
          descricaoTipo: this.obterDescricaoTipo(tipo),
          status: 'PENDENTE',
          statusDescricao: 'Pendente de Submissão',
          numeroDocumentoComprobatorio: payload.numeroDocumentoComprobatorio,
          dataValidade: payload.dataValidade,
          orgaoEmissor: payload.orgaoEmissor,
          valorOrcamentoAprovadoCaixa: payload.valorOrcamentoAprovado,
          percentualBdiAprovado: payload.percentualBdiAprovado,
          numeroArtRrt: payload.numeroArtRrt,
          observacoesAnaliseCaixa: payload.observacoes
        };
        return of(item);
      })
    );
  }

  solicitarProrrogacao(convenioId: string, payload: SolicitarProrrogacaoPayload): Observable<DossieClausulaSuspensiva> {
    return this.http.post<DossieClausulaSuspensiva>(API_ENDPOINTS.CONVENIOS.PRORROGAR(convenioId), payload).pipe(
      catchError(() => {
        const mock = MOCK_DOSSIE_PADRAO(convenioId);
        mock.prorrogacaoSolicitada = true;
        mock.novoPrazoProrrogado = payload.novoPrazoProrrogado;
        mock.prazoFatalEfetivo = payload.novoPrazoProrrogado;
        mock.diasRestantes = 120;
        mock.criticidade = 'REGULAR';
        mock.criticidadeDescricao = 'Regular: Mais de 90 dias';
        return of(mock);
      })
    );
  }

  superarClausula(convenioId: string, payload: SuperarClausulaPayload): Observable<DossieClausulaSuspensiva> {
    return this.http.post<DossieClausulaSuspensiva>(API_ENDPOINTS.CONVENIOS.SUPERAR(convenioId), payload).pipe(
      catchError(() => {
        const mock = MOCK_DOSSIE_PADRAO(convenioId);
        mock.superada = true;
        mock.s3KeyTermoRetirada = payload.s3KeyTermoRetirada;
        mock.condicionantes.forEach(c => (c.status = 'APROVADO'));
        return of(mock);
      })
    );
  }

  uploadDocumento(convenioId: string, tipo: TipoCondicionante, file: File): Observable<CondicionanteItem> {
    const formData = new FormData();
    formData.append('arquivo', file);

    return this.http.post<CondicionanteItem>(API_ENDPOINTS.CONVENIOS.UPLOAD_DOCUMENTO(convenioId, tipo), formData).pipe(
      catchError(() => {
        const item: CondicionanteItem = {
          id: `cond-${Date.now()}`,
          tipo,
          descricaoTipo: this.obterDescricaoTipo(tipo),
          status: 'EM_ANALISE_CAIXA',
          statusDescricao: 'Em Análise pela Caixa',
          s3KeyDocumento: `clausula-suspensiva/${convenioId}/${tipo}/${file.name}`
        };
        return of(item);
      })
    );
  }

  uploadTermoRetirada(convenioId: string, file: File): Observable<DossieClausulaSuspensiva> {
    const formData = new FormData();
    formData.append('arquivo', file);

    return this.http.post<DossieClausulaSuspensiva>(API_ENDPOINTS.CONVENIOS.UPLOAD_TERMO_RETIRADA(convenioId), formData).pipe(
      catchError(() => {
        const mock = MOCK_DOSSIE_PADRAO(convenioId);
        mock.superada = true;
        mock.s3KeyTermoRetirada = `clausula-suspensiva/${convenioId}/termo-retirada/${file.name}`;
        mock.condicionantes.forEach(c => (c.status = 'APROVADO'));
        return of(mock);
      })
    );
  }

  private obterDescricaoTipo(tipo: TipoCondicionante): string {
    switch (tipo) {
      case 'ENGENHARIA_PROJETOS_SINAPI':
        return 'Engenharia, Projetos & Orçamento SINAPI';
      case 'LICENCIAMENTO_AMBIENTAL':
        return 'Licenciamento Ambiental';
      case 'TITULARIDADE_IMOVEL':
        return 'Comprovação de Titularidade do Imóvel';
    }
  }
}
