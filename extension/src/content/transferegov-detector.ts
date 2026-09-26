const HEADING = /incluir documento h[áa]bil/i;
const CONVENIO = /\b(\d{5,8}\/\d{4})\b/;

export interface PageLocation {
  hostname: string;
  href: string;
}

export interface ScreenDetection {
  matched: boolean;
  convenioNumero: string | null;
}

export function isPortalOrFixture(location: PageLocation): boolean {
  const host = location.hostname.toLowerCase();
  if (host === 'transferegov.sistema.gov.br' || host.endsWith('.transferegov.sistema.gov.br')) {
    return true;
  }
  if ((host === 'localhost' || host === '127.0.0.1') && /incluir-documento-habil/i.test(location.href)) {
    return true;
  }
  return false;
}

export function detectDocumentoHabilScreen(doc: Document, location: PageLocation): ScreenDetection {
  if (!isPortalOrFixture(location)) {
    return { matched: false, convenioNumero: null };
  }

  const headingNodes = doc.querySelectorAll('h1, h2, h3, h4, [role="heading"]');
  const headingMatch = Array.from(headingNodes).some((node) => HEADING.test(node.textContent ?? ''));
  const titleMatch = HEADING.test(doc.title ?? '');
  if (!headingMatch && !titleMatch) {
    return { matched: false, convenioNumero: null };
  }

  const visible = doc.body?.textContent ?? '';
  const convenio = visible.match(CONVENIO)?.[1] ?? null;
  return { matched: true, convenioNumero: convenio };
}
