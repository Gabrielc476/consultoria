import {
  Directive,
  ElementRef,
  forwardRef,
  HostListener,
  Input,
  Renderer2
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { formatCurrencyBr, parseNumberBr } from '../utils/number-utils';

@Directive({
  selector: 'input[appCurrencyMask]',
  standalone: true,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CurrencyMaskDirective),
      multi: true
    }
  ]
})
export class CurrencyMaskDirective implements ControlValueAccessor {
  @Input() showPrefix = true;

  private onChange: (val: number | null) => void = () => {};
  private onTouched: () => void = () => {};
  private rawValue: number | null = null;
  private isFocused = false;

  constructor(
    private el: ElementRef<HTMLInputElement>,
    private renderer: Renderer2
  ) {}

  // ControlValueAccessor: chamado pelo Angular quando o modelo muda
  writeValue(value: any): void {
    if (value === null || value === undefined || value === '') {
      this.rawValue = null;
      this.updateDisplay(this.isFocused ? '' : (this.showPrefix ? 'R$ 0,00' : '0,00'));
      return;
    }

    const num = parseNumberBr(value);
    this.rawValue = num;
    this.updateDisplay(formatCurrencyBr(num, this.showPrefix));
  }

  registerOnChange(fn: any): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  setDisabledState?(isDisabled: boolean): void {
    this.renderer.setProperty(this.el.nativeElement, 'disabled', isDisabled);
  }

  @HostListener('focus', ['$event'])
  onFocus(): void {
    this.isFocused = true;
    const input = this.el.nativeElement;
    // Seleciona todo o conteúdo ao focar para facilitar substituição rápida
    setTimeout(() => {
      input.select();
    }, 0);
  }

  @HostListener('input', ['$event'])
  onInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const val = input.value;

    if (!val || val.trim() === '' || val === 'R$' || val === 'R$ ') {
      this.rawValue = 0;
      this.onChange(0);
      return;
    }

    const num = parseNumberBr(val);
    this.rawValue = num;
    this.onChange(num);
  }

  @HostListener('blur')
  onBlur(): void {
    this.isFocused = false;
    this.onTouched();

    const input = this.el.nativeElement;
    const val = input.value;

    if (!val || val.trim() === '') {
      this.rawValue = 0;
      this.updateDisplay(this.showPrefix ? 'R$ 0,00' : '0,00');
      this.onChange(0);
      return;
    }

    const num = parseNumberBr(val);
    this.rawValue = num;
    this.updateDisplay(formatCurrencyBr(num, this.showPrefix));
    this.onChange(num);
  }

  @HostListener('keydown', ['$event'])
  onKeyDown(event: KeyboardEvent): void {
    // Permite teclas de controle: Backspace, Tab, Enter, Esc, Delete, setas, Ctrl+A, Ctrl+C, Ctrl+V, etc.
    if (
      [
        'Backspace',
        'Tab',
        'Enter',
        'Escape',
        'Delete',
        'ArrowLeft',
        'ArrowRight',
        'ArrowUp',
        'ArrowDown',
        'Home',
        'End'
      ].includes(event.key) ||
      (event.ctrlKey && ['a', 'c', 'v', 'x', 'z'].includes(event.key.toLowerCase())) ||
      (event.metaKey && ['a', 'c', 'v', 'x', 'z'].includes(event.key.toLowerCase()))
    ) {
      return;
    }

    // Permite dígitos e os separadores decimais vírgula e ponto
    if (/[0-9]/.test(event.key) || event.key === ',' || event.key === '.') {
      return;
    }

    // Bloqueia qualquer outro caractere não numérico
    event.preventDefault();
  }

  private updateDisplay(text: string): void {
    this.renderer.setProperty(this.el.nativeElement, 'value', text);
  }
}
