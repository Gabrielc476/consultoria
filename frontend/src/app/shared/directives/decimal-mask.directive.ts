import {
  Directive,
  ElementRef,
  forwardRef,
  HostListener,
  Input,
  Renderer2
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { formatDecimalBr, parseNumberBr } from '../utils/number-utils';

@Directive({
  selector: 'input[appDecimalMask]',
  standalone: true,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DecimalMaskDirective),
      multi: true
    }
  ]
})
export class DecimalMaskDirective implements ControlValueAccessor {
  @Input() maxDecimals = 2;
  @Input() suffix = '';

  private onChange: (val: number | null) => void = () => {};
  private onTouched: () => void = () => {};
  private rawValue: number | null = null;
  private isFocused = false;

  constructor(
    private el: ElementRef<HTMLInputElement>,
    private renderer: Renderer2
  ) {}

  writeValue(value: any): void {
    if (value === null || value === undefined || value === '') {
      this.rawValue = null;
      this.updateDisplay(this.isFocused ? '' : '0');
      return;
    }

    const num = parseNumberBr(value);
    this.rawValue = num;
    const formatted = formatDecimalBr(num, this.maxDecimals);
    this.updateDisplay(this.suffix ? `${formatted} ${this.suffix}` : formatted);
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
    // Remove o sufixo temporariamente para edição se houver
    if (this.suffix && input.value.includes(this.suffix)) {
      const semSufixo = input.value.replace(this.suffix, '').trim();
      this.updateDisplay(semSufixo);
    }
    setTimeout(() => {
      input.select();
    }, 0);
  }

  @HostListener('input', ['$event'])
  onInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const val = input.value;

    if (!val || val.trim() === '') {
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
      const formattedZero = '0';
      this.updateDisplay(this.suffix ? `${formattedZero} ${this.suffix}` : formattedZero);
      this.onChange(0);
      return;
    }

    const num = parseNumberBr(val);
    this.rawValue = num;
    const formatted = formatDecimalBr(num, this.maxDecimals);
    this.updateDisplay(this.suffix ? `${formatted} ${this.suffix}` : formatted);
    this.onChange(num);
  }

  @HostListener('keydown', ['$event'])
  onKeyDown(event: KeyboardEvent): void {
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

    // Permite dígitos e vírgula/ponto
    if (/[0-9]/.test(event.key) || event.key === ',' || event.key === '.') {
      return;
    }

    event.preventDefault();
  }

  private updateDisplay(text: string): void {
    this.renderer.setProperty(this.el.nativeElement, 'value', text);
  }
}
