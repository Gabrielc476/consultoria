/**
 * Utilitários numéricos e monetários com padrão brasileiro (pt-BR).
 */

/**
 * Converte qualquer valor (number, string formatada com R$, pontos, vírgulas, etc.)
 * para um number primitivo válido em JavaScript.
 *
 * Exemplos:
 *   parseNumberBr('182.400,00') => 182400
 *   parseNumberBr('R$ 182.400,50') => 182400.5
 *   parseNumberBr('3,5') => 3.5
 *   parseNumberBr('3.5') => 3.5
 *   parseNumberBr(182400) => 182400
 *   parseNumberBr(null) => 0
 */
export function parseNumberBr(value: any): number {
  if (value === null || value === undefined || value === '') {
    return 0;
  }

  if (typeof value === 'number') {
    return isNaN(value) ? 0 : value;
  }

  let str = String(value).trim();
  // Remove todos os caracteres exceto dígitos, '.', ',', e '-'
  str = str.replace(/[^\d.,-]/g, '');
  if (!str || str === '-') {
    return 0;
  }

  const hasComma = str.includes(',');
  const hasDot = str.includes('.');

  if (hasComma && hasDot) {
    const lastComma = str.lastIndexOf(',');
    const lastDot = str.lastIndexOf('.');
    if (lastComma > lastDot) {
      // Padrão brasileiro: 182.400,50 -> remove pontos de milhar e troca vírgula por ponto
      str = str.replace(/\./g, '').replace(',', '.');
    } else {
      // Padrão internacional: 182,400.50 -> remove vírgulas de milhar
      str = str.replace(/,/g, '');
    }
  } else if (hasComma) {
    // Apenas vírgula: 182400,50 ou 3,5
    str = str.replace(',', '.');
  }

  const num = parseFloat(str);
  return isNaN(num) ? 0 : num;
}

/**
 * Formata um número ou string como moeda brasileira (Real - BRL).
 *
 * Exemplos:
 *   formatCurrencyBr(182400) => 'R$ 182.400,00'
 *   formatCurrencyBr(182400.5, false) => '182.400,50'
 */
export function formatCurrencyBr(value: any, includePrefix: boolean = true): string {
  if (value === null || value === undefined || value === '') {
    return includePrefix ? 'R$ 0,00' : '0,00';
  }

  const num = parseNumberBr(value);
  const formatted = num.toLocaleString('pt-BR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });

  return includePrefix ? `R$ ${formatted}` : formatted;
}

/**
 * Formata um número ou string decimal para o padrão brasileiro (com vírgula).
 *
 * Exemplos:
 *   formatDecimalBr(3.5) => '3,5'
 *   formatDecimalBr(24.50) => '24,5'
 */
export function formatDecimalBr(value: any, maxDecimals: number = 2): string {
  if (value === null || value === undefined || value === '') {
    return '0';
  }

  const num = parseNumberBr(value);
  return num.toLocaleString('pt-BR', {
    minimumFractionDigits: 0,
    maximumFractionDigits: maxDecimals
  });
}
