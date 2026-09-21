import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BoundingBoxOverlayComponent } from './bounding-box-overlay.component';

describe('BoundingBoxOverlayComponent', () => {
  let component: BoundingBoxOverlayComponent;
  let fixture: ComponentFixture<BoundingBoxOverlayComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BoundingBoxOverlayComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(BoundingBoxOverlayComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('deve ser instanciado com sucesso', () => {
    expect(component).toBeTruthy();
  });

  it('deve converter coordenadas normalizadas em porcentagens CSS', () => {
    component.boundingBoxes = {
      valorBruto: { ymin: 0.15, xmin: 0.20, ymax: 0.25, xmax: 0.50 }
    };
    component.scores = { valorBruto: 0.98 };
    fixture.detectChanges();

    const items = component.boxItems();
    expect(items.length).toBe(1);
    expect(items[0].top).toBe('15%');
    expect(items[0].left).toBe('20%');
    expect(items[0].width).toBe('30%');
    expect(items[0].height).toBe('10%');
    expect(items[0].score).toBe(0.98);
  });

  it('deve aplicar estilo de spotlight no campo ativo', () => {
    component.boundingBoxes = {
      valorBruto: { ymin: 0.1, xmin: 0.1, ymax: 0.2, xmax: 0.3 }
    };
    component.campoAtivo = 'valorBruto';
    fixture.detectChanges();

    const item = component.boxItems()[0];
    expect(item.isAtivo).toBeTrue();
    const classes = component.getBoxClasses(item);
    expect(classes).toContain('animate-spotlight');
    expect(classes).toContain('border-blue-500');
  });

  it('deve emitir evento ao clicar na caixa', () => {
    spyOn(component.caixaClicada, 'emit');
    component.onBoxClick('cnpjCredor');
    expect(component.caixaClicada.emit).toHaveBeenCalledWith('cnpjCredor');
  });
});
