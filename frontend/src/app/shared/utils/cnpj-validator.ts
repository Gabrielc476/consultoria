const WEIGHTS_FIRST_DIGIT = [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
const WEIGHTS_SECOND_DIGIT = [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];

export function limparCnpj(cnpj: string | null | undefined): string {
  return (cnpj || '').replace(/\D/g, '');
}

export function validarCnpj(cnpj: string | null | undefined): boolean {
  const clean = limparCnpj(cnpj);
  if (clean.length !== 14) return false;
  if (/^(\d)\1{13}$/.test(clean)) return false;

  const firstDigit = calculateDigit(clean.substring(0, 12), WEIGHTS_FIRST_DIGIT);
  const secondDigit = calculateDigit(clean.substring(0, 12) + firstDigit, WEIGHTS_SECOND_DIGIT);

  return Number(clean[12]) === firstDigit && Number(clean[13]) === secondDigit;
}

export function calcularDvCnpj(base12: string): { d1: number; d2: number } {
  const clean = base12.replace(/\D/g, '').padEnd(12, '0').substring(0, 12);
  const d1 = calculateDigit(clean, WEIGHTS_FIRST_DIGIT);
  const d2 = calculateDigit(clean + d1, WEIGHTS_SECOND_DIGIT);
  return { d1, d2 };
}

export function sugerirCnpjCorreto(cnpj: string | null | undefined): string | null {
  const clean = limparCnpj(cnpj);
  if (clean.length < 12) return null;
  const base = clean.substring(0, 12);
  const { d1, d2 } = calcularDvCnpj(base);
  const validClean = `${base}${d1}${d2}`;
  return `${validClean.substring(0, 2)}.${validClean.substring(2, 5)}.${validClean.substring(5, 8)}/${validClean.substring(8, 12)}-${validClean.substring(12, 14)}`;
}

function calculateDigit(base: string, weights: number[]): number {
  let sum = 0;
  for (let i = 0; i < weights.length; i++) {
    sum += Number(base[i]) * weights[i];
  }
  const remainder = sum % 11;
  return remainder < 2 ? 0 : 11 - remainder;
}
