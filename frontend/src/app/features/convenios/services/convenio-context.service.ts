import { Injectable, computed, inject, signal } from '@angular/core';
import { MunicipioContextService } from '../../../core/context/municipio-context.service';
import { ConvenioCockpit, FaseConvenio } from '../model/convenio-fase.model';

const FASES_TEMPLATE_PADRAO: (faseAtual: number) => FaseConvenio[] = (faseAtual: number) => [
  {
    numero: 0,
    codigo: 'Fase 00',
    nome: 'Regularidade Fiscal CAUC',
    subtitulo: 'CAUC / LRF 25',
    status: faseAtual > 0 ? 'CONCLUIDA' : (faseAtual === 0 ? 'EM_ANDAMENTO' : 'PENDENTE'),
    descricao: 'Verificação das 16 certidões obrigatórias do município no CAUC/SIAFI.'
  },
  {
    numero: 1,
    codigo: 'Fase 01',
    nome: 'Proposta & Plano de Trabalho',
    subtitulo: 'Plano Aprovado',
    status: faseAtual > 1 ? 'CONCLUIDA' : (faseAtual === 1 ? 'EM_ANDAMENTO' : 'PENDENTE'),
    descricao: 'Cadastramento de metas, etapas e cronograma físico-financeiro no SICONV.'
  },
  {
    numero: 2,
    codigo: 'Fase 02',
    nome: 'Cláusula Suspensiva',
    subtitulo: '3 Pilares Caixa',
    status: faseAtual > 2 ? 'CONCLUIDA' : (faseAtual === 2 ? 'EM_ANDAMENTO' : 'PENDENTE'),
    descricao: 'Superação dos 3 pilares: Projeto de Engenharia (SPA/LAE), Licença Ambiental e Titularidade.'
  },
  {
    numero: 3,
    codigo: 'Fase 03',
    nome: 'Licitação VRPL & AIO',
    subtitulo: 'AIO Emitida',
    status: faseAtual > 3 ? 'CONCLUIDA' : (faseAtual === 3 ? 'EM_ANDAMENTO' : 'PENDENTE'),
    descricao: 'Homologação do resultado licitatório pela Caixa (VRPL) e Autorização de Início de Objeto.'
  },
  {
    numero: 4,
    codigo: 'Fase 04',
    nome: 'Execução Física & Medições',
    subtitulo: 'RAE Caixa',
    status: faseAtual > 4 ? 'CONCLUIDA' : (faseAtual === 4 ? 'EM_ANDAMENTO' : 'PENDENTE'),
    descricao: 'Boletins de medição atestados pelo fiscal municipal e conciliados com relatório RAE da Caixa.'
  },
  {
    numero: 5,
    codigo: 'Fase 05',
    nome: 'Liquidação Financeira OBTV',
    subtitulo: 'Comando Duplo',
    status: faseAtual > 5 ? 'CONCLUIDA' : (faseAtual === 5 ? 'EM_ANDAMENTO' : 'PENDENTE'),
    descricao: 'Liquidação de documentos hábeis e autorização bancária de Ordens de Pagamento (OBTV).'
  },
  {
    numero: 6,
    codigo: 'Fase 06',
    nome: 'Termos Aditivos & Reajuste',
    subtitulo: 'Teto 25%',
    status: faseAtual > 6 ? 'CONCLUIDA' : (faseAtual === 6 ? 'EM_ANDAMENTO' : 'PENDENTE'),
    descricao: 'Alterações de prazo ou valor dentro do limite legal de 25% da Lei nº 14.133/2021.'
  },
  {
    numero: 7,
    codigo: 'Fase 07',
    nome: 'Prestação de Contas Parcial',
    subtitulo: 'Relatório Gestão',
    status: faseAtual > 7 ? 'CONCLUIDA' : (faseAtual === 7 ? 'EM_ANDAMENTO' : 'PENDENTE'),
    descricao: 'Comprovação periódica de aplicação dos recursos federais liberados por parcelas.'
  },
  {
    numero: 8,
    codigo: 'Fase 08',
    nome: 'Prestação de Contas Final',
    subtitulo: 'RCO & GRU R$ 0',
    status: faseAtual > 8 ? 'CONCLUIDA' : (faseAtual === 8 ? 'EM_ANDAMENTO' : 'PENDENTE'),
    descricao: 'Relatório de Cumprimento do Objeto (RCO), saldo bancário Op 006 zerado e devolução via GRU.'
  },
  {
    numero: 9,
    codigo: 'Fase 09',
    nome: 'Proteção Jurídica & SELIC',
    subtitulo: 'Súmula 230/TCU',
    status: faseAtual > 9 ? 'CONCLUIDA' : (faseAtual === 9 ? 'EM_ANDAMENTO' : 'PENDENTE'),
    descricao: 'Gestão de notificações de 45 dias com juros SELIC e blindagem do município por ação judicial.'
  }
];

export const MOCK_CONVENIOS_SISTEMA: ConvenioCockpit[] = [
  // --- PATOS (PB) ---
  {
    id: 'conv-914250',
    numeroSiconv: '914250/2023',
    ano: 2023,
    objeto: 'Construção de Creche Proinfância Tipo 2 - Bairro Jatobá',
    municipioId: 'mun-patos-01',
    municipioNome: 'Patos',
    municipioUf: 'PB',
    orgaoConcedente: 'FNDE / Ministério da Educação',
    valorTotal: 2050000.0,
    valorRepasse: 1850000.0,
    valorContrapartida: 200000.0,
    percentualExecucao: 68,
    saldoContaOp006: 412300.5,
    diasParaVencimento: 18,
    dataFimVigencia: '15/10/2026',
    faseAtualNumero: 5,
    statusGeral: 'CRITICO',
    fases: FASES_TEMPLATE_PADRAO(5)
  },
  {
    id: 'conv-932140',
    numeroSiconv: '932140/2023',
    ano: 2023,
    objeto: 'Pavimentação em Paralelepípedo e Drenagem - Bairro Monte Castelo',
    municipioId: 'mun-patos-01',
    municipioNome: 'Patos',
    municipioUf: 'PB',
    orgaoConcedente: 'Ministério das Cidades (MCID)',
    valorTotal: 1480000.0,
    valorRepasse: 1350000.0,
    valorContrapartida: 130000.0,
    percentualExecucao: 45,
    saldoContaOp006: 620000.0,
    diasParaVencimento: 42,
    dataFimVigencia: '30/11/2026',
    faseAtualNumero: 4,
    statusGeral: 'ALERTA',
    fases: FASES_TEMPLATE_PADRAO(4)
  },
  {
    id: 'conv-948712',
    numeroSiconv: '948712/2024',
    ano: 2024,
    objeto: 'Reforma e Ampliação da UPA 24h Campo Comprido',
    municipioId: 'mun-patos-01',
    municipioNome: 'Patos',
    municipioUf: 'PB',
    orgaoConcedente: 'Ministério da Saúde / FNS',
    valorTotal: 850000.0,
    valorRepasse: 800000.0,
    valorContrapartida: 50000.0,
    percentualExecucao: 12,
    saldoContaOp006: 0.0,
    diasParaVencimento: 120,
    dataFimVigencia: '28/02/2027',
    faseAtualNumero: 2,
    statusGeral: 'EM_DIA',
    fases: FASES_TEMPLATE_PADRAO(2)
  },
  {
    id: 'conv-899201',
    numeroSiconv: '899201/2022',
    ano: 2022,
    objeto: 'Aquisição de Patrulha Mecanizada para Agricultura Familiar',
    municipioId: 'mun-patos-01',
    municipioNome: 'Patos',
    municipioUf: 'PB',
    orgaoConcedente: 'MAPA / Agricultura',
    valorTotal: 620000.0,
    valorRepasse: 590000.0,
    valorContrapartida: 30000.0,
    percentualExecucao: 100,
    saldoContaOp006: 12450.2,
    diasParaVencimento: 8,
    dataFimVigencia: '05/10/2026',
    faseAtualNumero: 8,
    statusGeral: 'CRITICO',
    fases: FASES_TEMPLATE_PADRAO(8)
  },
  {
    id: 'conv-955310',
    numeroSiconv: '955310/2024',
    ano: 2024,
    objeto: 'Construção de Quadra Poliesportiva Coberta - Escola Municipal Frei Martinho',
    municipioId: 'mun-patos-01',
    municipioNome: 'Patos',
    municipioUf: 'PB',
    orgaoConcedente: 'Ministério do Esporte',
    valorTotal: 980000.0,
    valorRepasse: 900000.0,
    valorContrapartida: 80000.0,
    percentualExecucao: 0,
    saldoContaOp006: 0.0,
    diasParaVencimento: 210,
    dataFimVigencia: '30/06/2027',
    faseAtualNumero: 1,
    statusGeral: 'EM_DIA',
    fases: FASES_TEMPLATE_PADRAO(1)
  },

  // --- SOUSA (PB) ---
  {
    id: 'conv-921004',
    numeroSiconv: '921004/2023',
    ano: 2023,
    objeto: 'Ampliação do Sistema de Esgotamento Sanitário - Bairro Sorrilândia',
    municipioId: 'mun-sousa-02',
    municipioNome: 'Sousa',
    municipioUf: 'PB',
    orgaoConcedente: 'FUNASA / Ministério das Cidades',
    valorTotal: 3800000.0,
    valorRepasse: 3500000.0,
    valorContrapartida: 300000.0,
    percentualExecucao: 52,
    saldoContaOp006: 890000.0,
    diasParaVencimento: 65,
    dataFimVigencia: '20/12/2026',
    faseAtualNumero: 4,
    statusGeral: 'EM_DIA',
    fases: FASES_TEMPLATE_PADRAO(4)
  },
  {
    id: 'conv-935820',
    numeroSiconv: '935820/2023',
    ano: 2023,
    objeto: 'Modernização da Iluminação Pública com Eficiência LED',
    municipioId: 'mun-sousa-02',
    municipioNome: 'Sousa',
    municipioUf: 'PB',
    orgaoConcedente: 'MDR / Desenvolvimento Regional',
    valorTotal: 1150000.0,
    valorRepasse: 1050000.0,
    valorContrapartida: 100000.0,
    percentualExecucao: 20,
    saldoContaOp006: 0.0,
    diasParaVencimento: 14,
    dataFimVigencia: '12/10/2026',
    faseAtualNumero: 3,
    statusGeral: 'CRITICO',
    fases: FASES_TEMPLATE_PADRAO(3)
  },
  {
    id: 'conv-904120',
    numeroSiconv: '904120/2022',
    ano: 2022,
    objeto: 'Reforma Estrutural do Mercado Público Central',
    municipioId: 'mun-sousa-02',
    municipioNome: 'Sousa',
    municipioUf: 'PB',
    orgaoConcedente: 'Ministério do Turismo',
    valorTotal: 780000.0,
    valorRepasse: 720000.0,
    valorContrapartida: 60000.0,
    percentualExecucao: 85,
    saldoContaOp006: 115000.0,
    diasParaVencimento: 50,
    dataFimVigencia: '18/11/2026',
    faseAtualNumero: 7,
    statusGeral: 'EM_DIA',
    fases: FASES_TEMPLATE_PADRAO(7)
  },

  // --- CAJAZEIRAS (PB) ---
  {
    id: 'conv-942880',
    numeroSiconv: '942880/2023',
    ano: 2023,
    objeto: 'Drenagem Urbana e Contenção de Encostas - Canal das Capoeiras',
    municipioId: 'mun-cajazeiras-05',
    municipioNome: 'Cajazeiras',
    municipioUf: 'PB',
    orgaoConcedente: 'MDR / Defesa Civil',
    valorTotal: 2900000.0,
    valorRepasse: 2700000.0,
    valorContrapartida: 200000.0,
    percentualExecucao: 38,
    saldoContaOp006: 940000.0,
    diasParaVencimento: 90,
    dataFimVigencia: '15/01/2027',
    faseAtualNumero: 4,
    statusGeral: 'EM_DIA',
    fases: FASES_TEMPLATE_PADRAO(4)
  }
];

const STORAGE_KEY_CONVENIO_ATIVO = 'govflow_convenio_ativo_id';

@Injectable({
  providedIn: 'root'
})
export class ConvenioContextService {
  private readonly municipioCtx = inject(MunicipioContextService);

  readonly todosConvenios = signal<ConvenioCockpit[]>(MOCK_CONVENIOS_SISTEMA);
  readonly convenioAtivoId = signal<string>(this.obterIdSalvo());

  /**
   * Convênios pertencentes ao município atualmente selecionado no header
   */
  readonly conveniosDoMunicipio = computed(() => {
    const munAtivo = this.municipioCtx.municipioAtivo();
    if (!munAtivo) return this.todosConvenios();
    const filtrados = this.todosConvenios().filter(c => c.municipioId === munAtivo.id);
    return filtrados.length > 0 ? filtrados : this.todosConvenios();
  });

  /**
   * Convênio atualmente exibido no Cockpit
   */
  readonly convenioAtivo = computed(() => {
    const id = this.convenioAtivoId();
    const encontrados = this.todosConvenios().find(c => c.id === id);
    if (encontrados) return encontrados;

    // Se o selecionado não pertencer ao município atual, pega o primeiro do município atual
    const doMunicipio = this.conveniosDoMunicipio();
    return doMunicipio[0] || this.todosConvenios()[0];
  });

  selecionarConvenio(id: string): void {
    const existe = this.todosConvenios().find(c => c.id === id);
    if (existe) {
      this.convenioAtivoId.set(id);
      try {
        localStorage.setItem(STORAGE_KEY_CONVENIO_ATIVO, id);
      } catch {}

      // Se o convênio pertencer a outro município, atualiza o município ativo automaticamente
      if (existe.municipioId && existe.municipioId !== this.municipioCtx.municipioAtivoId()) {
        this.municipioCtx.selecionarMunicipio(existe.municipioId);
      }
    }
  }

  obterConvenioPorId(id: string): ConvenioCockpit | undefined {
    return this.todosConvenios().find(c => c.id === id);
  }

  private obterIdSalvo(): string {
    try {
      return localStorage.getItem(STORAGE_KEY_CONVENIO_ATIVO) || 'conv-914250';
    } catch {
      return 'conv-914250';
    }
  }
}
