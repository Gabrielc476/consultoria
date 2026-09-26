import { CampoFormulario } from '../shared/contrato';

export interface PreenchimentoCampo extends CampoFormulario {
  preenchido: boolean;
}

export function setInputValue(root: ParentNode, selector: string, value: string): boolean {
  const input = root.querySelector(selector);
  if (!(input instanceof HTMLInputElement)) return false;

  const descriptor = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value');
  if (descriptor?.set) {
    descriptor.set.call(input, value);
  } else {
    input.value = value;
  }

  const view = input.ownerDocument.defaultView ?? window;
  input.dispatchEvent(new view.Event('input', { bubbles: true }));
  input.dispatchEvent(new view.Event('change', { bubbles: true }));
  return true;
}

export function fillOfficialForm(root: ParentNode, campos: CampoFormulario[]): PreenchimentoCampo[] {
  return campos.map((campo) => ({
    ...campo,
    preenchido: setInputValue(root, campo.selector, campo.value)
  }));
}
