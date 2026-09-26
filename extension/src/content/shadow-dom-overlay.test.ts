import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { DocumentoPronto } from '../shared/contrato';
import { mountOverlay } from './shadow-dom-overlay';

const documentoRecente: DocumentoPronto = {
  id: 'doc-recente',
  updatedAt: '2026-09-20T12:00:00Z',
  convenioId: null,
  campos: [
    { id: 'numeroDocumento', label: 'Número da NF', value: '0001542', selector: '#dh-nr-documento' },
    { id: 'dataEmissao', label: 'Data', value: '20/08/2026', selector: '#dh-dt-emissao' },
    { id: 'valorBruto', label: 'Valor', value: '85.400,00', selector: '#dh-vl-documento' },
    { id: 'cnpjCredor', label: 'CNPJ', value: '08123456000190', selector: '#dh-cd-credor' },
    { id: 'retencao-INSS-0', label: 'Retenção INSS', value: '9.394,00', selector: '#dh-retencao-inss' },
    { id: 'retencao-ISS-1', label: 'Retenção ISS', value: '4.270,00', selector: '#dh-retencao-iss' },
    { id: 'retencao-IRRF-2', label: 'Retenção IRRF', value: '1.281,00', selector: '#dh-retencao-irrf' }
  ]
};

function carregarFixture(): void {
  const html = readFileSync(resolve(process.cwd(), 'fixtures/incluir-documento-habil.html'), 'utf8');
  const body = html.match(/<body>([\s\S]*?)<\/body>/i)?.[1] ?? '';
  document.body.innerHTML = body;
}

function escutarEventos(): Record<string, string[]> {
  const vistos: Record<string, string[]> = {};
  document.querySelectorAll('input').forEach((input) => {
    vistos[input.id] = [];
    input.addEventListener('input', () => vistos[input.id].push('input'));
    input.addEventListener('change', () => vistos[input.id].push('change'));
  });
  return vistos;
}

afterEach(() => {
  document.getElementById('govflow-copilot-host')?.remove();
  vi.unstubAllGlobals();
});

describe('painel e preenchimento', () => {
  it('isola o painel no Shadow DOM fechado', () => {
    carregarFixture();
    const { host, shadowRoot } = mountOverlay(document, {
      convenioNumero: '914250/2023',
      autenticado: true,
      documentos: [documentoRecente]
    });

    expect(host.id).toBe('govflow-copilot-host');
    expect(document.querySelector('.govflow-panel')).toBeNull();
    expect(document.querySelector('.govflow-btn-primary')).toBeNull();
    expect(shadowRoot.querySelector('.govflow-panel')).not.toBeNull();
    expect(host.shadowRoot).toBeNull();
  });

  it('preenche todos os campos oficiais e dispara input e change', () => {
    carregarFixture();
    const vistos = escutarEventos();
    const { shadowRoot } = mountOverlay(document, {
      convenioNumero: '914250/2023',
      autenticado: true,
      documentos: [documentoRecente]
    });

    shadowRoot.querySelector<HTMLButtonElement>('#btn-preencher')?.click();

    expect((document.querySelector('#dh-nr-documento') as HTMLInputElement).value).toBe('0001542');
    expect((document.querySelector('#dh-dt-emissao') as HTMLInputElement).value).toBe('20/08/2026');
    expect((document.querySelector('#dh-vl-documento') as HTMLInputElement).value).toBe('85.400,00');
    expect((document.querySelector('#dh-cd-credor') as HTMLInputElement).value).toBe('08123456000190');
    expect((document.querySelector('#dh-retencao-inss') as HTMLInputElement).value).toBe('9.394,00');
    expect((document.querySelector('#dh-retencao-iss') as HTMLInputElement).value).toBe('4.270,00');
    expect((document.querySelector('#dh-retencao-irrf') as HTMLInputElement).value).toBe('1.281,00');

    for (const id of ['dh-nr-documento', 'dh-dt-emissao', 'dh-vl-documento', 'dh-cd-credor', 'dh-retencao-inss', 'dh-retencao-iss', 'dh-retencao-irrf']) {
      expect(vistos[id]).toEqual(['input', 'change']);
    }
  });

  it('copia só o valor do campo clicado', async () => {
    carregarFixture();
    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, 'clipboard', {
      configurable: true,
      value: { writeText }
    });

    const { shadowRoot } = mountOverlay(document, {
      convenioNumero: '914250/2023',
      autenticado: true,
      documentos: [documentoRecente]
    });

    const botoes = shadowRoot.querySelectorAll<HTMLButtonElement>('.govflow-copy');
    expect(botoes.length).toBe(documentoRecente.campos.length);

    Array.from(botoes).forEach((botao) => botao.click());

    await vi.waitFor(() => {
      expect(writeText).toHaveBeenCalledTimes(documentoRecente.campos.length);
    });

    documentoRecente.campos.forEach((campo, index) => {
      expect(writeText.mock.calls[index][0]).toBe(campo.value);
    });
  });
});
