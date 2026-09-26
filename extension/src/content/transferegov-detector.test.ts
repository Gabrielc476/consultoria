import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';
import { detectDocumentoHabilScreen } from './transferegov-detector';

function page(html: string, title = ''): Document {
  document.title = title;
  document.body.innerHTML = html;
  return document;
}

describe('detecção da tela Incluir Documento Hábil', () => {
  it('não reconhece host fora do portal e do fixture', () => {
    page('<h1>Incluir Documento Hábil</h1>');
    const detection = detectDocumentoHabilScreen(document, {
      hostname: 'exemplo.gov.br',
      href: 'https://exemplo.gov.br/incluir'
    });
    expect(detection.matched).toBe(false);
  });

  it('não reconhece o portal quando o heading não é a tela de documento hábil', () => {
    page('<h1>Painel de convênios</h1><p>Convênio 914250/2023</p>');
    const detection = detectDocumentoHabilScreen(document, {
      hostname: 'transferegov.sistema.gov.br',
      href: 'https://transferegov.sistema.gov.br/convenios'
    });
    expect(detection.matched).toBe(false);
  });

  it('reconhece o fixture local e lê o número do convênio', () => {
    const html = readFileSync(resolve(process.cwd(), 'fixtures/incluir-documento-habil.html'), 'utf8');
    const body = html.match(/<body>([\s\S]*?)<\/body>/i)?.[1] ?? '';
    page(body, 'Incluir Documento Hábil');
    const detection = detectDocumentoHabilScreen(document, {
      hostname: 'localhost',
      href: 'http://localhost:4173/fixtures/incluir-documento-habil.html'
    });
    expect(detection.matched).toBe(true);
    expect(detection.convenioNumero).toBe('914250/2023');
  });
});
