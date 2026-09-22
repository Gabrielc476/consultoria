import { validarCnpj, sugerirCnpjCorreto, limparCnpj, calcularDvCnpj } from './cnpj-validator';

describe('cnpj-validator', () => {
  it('deve validar corretamente CNPJs válidos', () => {
    expect(validarCnpj('12.345.678/0001-95')).toBeTrue();
    expect(validarCnpj('08.778.326/0001-56')).toBeTrue();
    expect(validarCnpj('13.519.354/0001-99')).toBeTrue();
    expect(validarCnpj('12345678000195')).toBeTrue();
  });

  it('deve rejeitar CNPJs com dígitos verificadores incorretos', () => {
    expect(validarCnpj('12.345.678/0001-90')).toBeFalse();
    expect(validarCnpj('12.345.678/0001-92')).toBeFalse();
    expect(validarCnpj('08.778.326/0001-00')).toBeFalse();
  });

  it('deve rejeitar CNPJs com dígitos repetidos ou tamanho inválido', () => {
    expect(validarCnpj('00.000.000/0000-00')).toBeFalse();
    expect(validarCnpj('11.111.111/1111-11')).toBeFalse();
    expect(validarCnpj('123')).toBeFalse();
    expect(validarCnpj('')).toBeFalse();
    expect(validarCnpj(null)).toBeFalse();
  });

  it('deve calcular os dígitos corretos e sugerir CNPJ válido', () => {
    expect(sugerirCnpjCorreto('12.345.678/0001-90')).toBe('12.345.678/0001-95');
    expect(sugerirCnpjCorreto('12345678000100')).toBe('12.345.678/0001-95');
    expect(sugerirCnpjCorreto('08778326000100')).toBe('08.778.326/0001-56');
  });
});
