# -*- coding: utf-8 -*-
import sys
import io
import time
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(BASE_DIR / "src"))

import pypdfium2 as pdfium
from google import genai
from google.genai import types
from PIL import Image
from infrastructure.config.settings import get_settings

def call_with_exponential_backoff(client, model, contents, max_retries=4, base_delay=4.0):
    for attempt in range(max_retries + 1):
        try:
            print(f"  [*] [Tentativa {attempt + 1}/{max_retries + 1}] Enviando requisicao para {model}...")
            response = client.models.generate_content(
                model=model,
                contents=contents,
            )
            return response
        except Exception as exc:
            err_str = str(exc)
            is_503 = "503" in err_str or "UNAVAILABLE" in err_str
            if is_503 and attempt < max_retries:
                delay = base_delay * (2 ** attempt)
                print(f"  [!] Detectado 503 UNAVAILABLE. Aplicando Exponential Backoff: aguardando {delay:.1f}s...")
                time.sleep(delay)
            else:
                raise exc

def run():
    print("=" * 70)
    print("  Teste Gemma 4 Vision com Retry e Exponential Backoff para 503")
    print("=" * 70)

    settings = get_settings()
    client = genai.Client(api_key=settings.gemini_api_key)

    pdf_path = BASE_DIR / "tests" / "fixtures" / "nf_smoke.pdf"
    pdf = pdfium.PdfDocument(pdf_path.read_bytes())
    page = pdf[0]
    
    # Renderiza em 300 DPI (scale 4.166)
    pil_image = page.render(scale=3.0).to_pil()
    buf = io.BytesIO()
    pil_image.save(buf, format="JPEG", quality=95)
    jpeg_bytes = buf.getvalue()
    print(f"[*] Imagem JPEG gerada a partir do PDF ({len(jpeg_bytes)} bytes).")

    part = types.Part.from_bytes(data=jpeg_bytes, mime_type="image/jpeg")
    prompt = "Descreva os campos fiscais desta nota fiscal: numero do documento, data de emissao, cnpj do prestador e valor total."

    models = ["gemma-4-31b-it", "gemma-4-26b-a4b-it"]

    for model_name in models:
        print(f"\n---> Testando modelo: {model_name}")
        try:
            response = call_with_exponential_backoff(
                client=client,
                model=model_name,
                contents=[part, prompt],
                max_retries=3,
                base_delay=5.0,
            )
            print(f"\n[+] SUCESSO com {model_name}!")
            print("-" * 50)
            print(response.text)
            print("-" * 50)
        except Exception as exc:
            print(f"\n[-] FALHA FINAL com {model_name}: {type(exc).__name__}: {exc}")

if __name__ == "__main__":
    run()
