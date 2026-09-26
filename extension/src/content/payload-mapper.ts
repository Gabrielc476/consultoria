import {
  CampoFormulario,
  DadosRevisaoApi,
  DocumentoApi,
  DocumentoPronto,
  FORM_SELECTORS,
  RetencaoApi
} from '../shared/contrato';

const ROTULOS: Record<string, string> = {
  numeroDocumento: 'Número da NF',
  dataEmissao: 'Data',
  valorBruto: 'Valor',
  cnpjCredor: 'CNPJ'
};

export function formatarDataBr(iso: string | null | undefined): string {
  if (!iso) return '';
  const match = /^(\d{4})-(\d{2})-(\d{2})/.exec(iso);
  if (!match) return iso;
  return `${match[3]}/${match[2]}/${match[1]}`;
}

export function formatarValorBr(valor: number | string | null | undefined): string {
  if (valor === null || valor === undefined || valor === '') return '';
  const numero = typeof valor === 'number' ? valor : Number(String(valor).replace(',', '.'));
  if (!Number.isFinite(numero)) return String(valor);
  return new Intl.NumberFormat('pt-BR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(numero);
}

function campo(id: string, value: string, selector: string): CampoFormulario {
  return { id, label: ROTULOS[id] ?? id, value, selector };
}

function campoRetencao(retencao: RetencaoApi, index: number): CampoFormulario | null {
  const tipo = (retencao.tipo ?? '').trim();
  if (!tipo) return null;
  return {
    id: `retencao-${tipo}-${index}`,
    label: `Retenção ${tipo}`,
    value: formatarValorBr(retencao.valor),
    selector: FORM_SELECTORS.retencao(tipo)
  };
}

export function camposDeRevisao(revisao: DadosRevisaoApi): CampoFormulario[] {
  const campos: CampoFormulario[] = [
    campo('numeroDocumento', revisao.numeroDocumento ?? '', FORM_SELECTORS.numeroDocumento),
    campo('dataEmissao', formatarDataBr(revisao.dataEmissao), FORM_SELECTORS.dataEmissao),
    campo('valorBruto', formatarValorBr(revisao.valorBruto), FORM_SELECTORS.valorBruto),
    campo('cnpjCredor', revisao.cnpjCredor ?? '', FORM_SELECTORS.cnpjCredor)
  ];

  (revisao.retencoes ?? []).forEach((retencao, index) => {
    const mapeada = campoRetencao(retencao, index);
    if (mapeada) campos.push(mapeada);
  });

  return campos;
}

export function mapDocumentoPronto(raw: DocumentoApi): DocumentoPronto | null {
  if (!raw.id || !raw.dadosRevisao) return null;
  if (raw.status && raw.status !== 'PRONTO_PARA_TRANSFEREGOV') return null;
  return {
    id: raw.id,
    updatedAt: raw.updatedAt || raw.createdAt || '',
    convenioId: raw.convenioId ?? null,
    campos: camposDeRevisao(raw.dadosRevisao)
  };
}

export function ordenarMaisRecentes(documentos: DocumentoPronto[]): DocumentoPronto[] {
  return [...documentos].sort((a, b) => {
    const ta = Date.parse(a.updatedAt);
    const tb = Date.parse(b.updatedAt);
    const aTime = Number.isNaN(ta) ? 0 : ta;
    const bTime = Number.isNaN(tb) ? 0 : tb;
    return bTime - aTime;
  });
}

export function selecionarProntos(items: DocumentoApi[]): DocumentoPronto[] {
  const mapeados = items
    .map(mapDocumentoPronto)
    .filter((item): item is DocumentoPronto => item !== null);
  return ordenarMaisRecentes(mapeados);
}
