# -*- coding: utf-8 -*-
import sys
from pathlib import Path
BASE_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(BASE_DIR / "src"))

import pypdfium2 as pdfium
from google import genai
from google.genai import types
from domain.schemas.documento_habil_schema import DocumentoHabilExtraction
from domain.rules.financial_rules import validate_documento_fiscal, average_field_confidence
from infrastructure.config.settings import get_settings

settings = get_settings()
client = genai.Client(api_key=settings.gemini_api_key)

pdf_path = BASE_DIR / "tests" / "fixtures" / "nf_smoke.pdf"
pdf = pdfium.PdfDocument(pdf_path.read_bytes())
page = pdf[0]
pil_img = page.render(scale=2.0).to_pil()

prompt = """
Voce e um especialista em OCR e extracao fiscal de Documentos Habeis / Notas Fiscais brasileiras.
Analise a imagem da nota fiscal e extraia os dados estritamente no seguinte formato JSON:
{
  "tipo_documento": {"valor": "NOTA_FISCAL_SERVICOS", "confianca": 0.99, "coordenadas": null},
  "numero_documento": {"valor": "string", "confianca": 0.99, "coordenadas": null},
  "data_emissao": {"valor": "YYYY-MM-DD", "confianca": 0.99, "coordenadas": null},
  "valor_bruto": {"valor": "0.00", "confianca": 0.99, "coordenadas": null},
  "valor_liquido": {"valor": "0.00", "confianca": 0.99, "coordenadas": null},
  "cnpj_credor": {"valor": "apenas digitos sem pontos", "confianca": 0.99, "coordenadas": null},
  "razao_social_credor": {"valor": "string", "confianca": 0.99, "coordenadas": null},
  "numero_empenho": null,
  "descricao_servico": {"valor": "string", "confianca": 0.99, "coordenadas": null},
  "chave_acesso_nfe": null,
  "retencoes": [
    {
      "tipo": "INSS",
      "aliquota": 11.0,
      "valor": 110.00,
      "confianca": 0.99,
      "coordenadas": null
    }
  ],
  "alertas_inconsistencia": []
}
Valores numericos e datas devem estar normalizados. CNPJ apenas com digitos.
tipo em retencoes deve ser um de: INSS, ISS, IRRF, PIS, COFINS, CSLL. aliquota e valor em formato numerico float.
"""

models_to_test = ["gemma-4-31b-it", "gemma-4-26b-a4b-it"]
response = None
chosen_model = None

for model_name in models_to_test:
    print(f"[*] Testando OCR com modelo Gemma: {model_name}...")
    try:
        response = client.models.generate_content(
            model=model_name,
            contents=[pil_img, prompt],
            config=types.GenerateContentConfig(
                response_mime_type="application/json",
                temperature=0.0,
            ),
        )
        if response and response.text:
            chosen_model = model_name
            print(f"[+] Sucesso na resposta do {model_name}!")
            break
    except Exception as exc:
        print(f"[-] Modelo {model_name} retornou erro: {exc}")
        print("[*] Tentando proximo modelo da familia Gemma 4...")

if not response or not response.text:
    print("[ERRO] Nenhum modelo Gemma conseguiu responder.")
    sys.exit(1)

print("\n[*] Resposta JSON gerada pelo Gemma 4:")
print(response.text)

extraction = DocumentoHabilExtraction.model_validate_json(response.text)
print("\n" + "=" * 65)
print(f"  EXTRACAO COM {chosen_model.upper()} REALIZADA COM SUCESSO!")
print("=" * 65)
print(f"Tipo:          {extraction.tipo_documento.valor} (confianca: {extraction.tipo_documento.confianca})")
print(f"Numero:        {extraction.numero_documento.valor} (confianca: {extraction.numero_documento.confianca})")
print(f"Data Emissao:  {extraction.data_emissao.valor} (confianca: {extraction.data_emissao.confianca})")
print(f"CNPJ Credor:   {extraction.cnpj_credor.valor} (confianca: {extraction.cnpj_credor.confianca})")
print(f"Razao Social:  {extraction.razao_social_credor.valor}")
print(f"Valor Bruto:   R$ {extraction.valor_bruto.valor}")
print(f"Valor Liquido: R$ {extraction.valor_liquido.valor}")
print(f"Retencoes ({len(extraction.retencoes)}):")
for r in extraction.retencoes:
    print(f"  - {r.tipo}: R$ {r.valor:.2f} (Aliquota: {r.aliquota}%)")

validacao = validate_documento_fiscal(extraction)
confidence = average_field_confidence(extraction)
print("-" * 65)
print(f"Consistencia Matematica: {'VALIDA' if validacao.consistente else 'DIVERGENTE'}")
print(f"Diferenca Calculada:     R$ {validacao.diferenca}")
print(f"Confianca Media Geral:   {confidence:.2%}")
print("=" * 65)
