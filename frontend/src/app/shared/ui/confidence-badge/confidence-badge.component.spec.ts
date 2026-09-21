import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ConfidenceBadgeComponent } from './confidence-badge.component';

describe('ConfidenceBadgeComponent', () => {
  let component: ConfidenceBadgeComponent;
  let fixture: ComponentFixture<ConfidenceBadgeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConfidenceBadgeComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(ConfidenceBadgeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('deve exibir percentual formatado corretamente', () => {
    component.score = 0.985;
    fixture.detectChanges();
    expect(component.percentualFormatado()).toBe('99%');
  });

  it('deve aplicar estilo verde esmeralda para score >= 0.90', () => {
    component.score = 0.95;
    fixture.detectChanges();
    expect(component.classes()).toContain('emerald');
  });

  it('deve aplicar estilo âmbar para score entre 0.70 e 0.89', () => {
    component.score = 0.78;
    fixture.detectChanges();
    expect(component.classes()).toContain('amber');
  });

  it('deve aplicar estilo rose/vermelho para score < 0.70', () => {
    component.score = 0.55;
    fixture.detectChanges();
    expect(component.classes()).toContain('rose');
  });
});
