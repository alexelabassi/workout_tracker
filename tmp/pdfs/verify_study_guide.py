from pathlib import Path
import sys

import pypdfium2 as pdfium
from PIL import Image, ImageDraw
from pypdf import PdfReader


ROOT = Path(r"C:\Users\Alexandru\Desktop\Licenta")
PDF = ROOT / "output" / "pdf" / "thesis_study_guide.pdf"
OUT = ROOT / "tmp" / "pdfs" / "study_guide_render"


def main() -> None:
    sys.stdout.reconfigure(encoding="utf-8")
    OUT.mkdir(parents=True, exist_ok=True)

    reader = PdfReader(str(PDF))
    print(f"pages={len(reader.pages)}")
    for idx in range(min(3, len(reader.pages))):
        text = (reader.pages[idx].extract_text() or "").replace("\n", " ")
        print(f"page_{idx + 1}_chars={len(text)}")
        print(text[:500])

    pdf = pdfium.PdfDocument(str(PDF))
    rendered = []
    for i in range(len(pdf)):
        page = pdf[i]
        bitmap = page.render(scale=1.25).to_pil()
        page_path = OUT / f"page_{i + 1:02d}.png"
        bitmap.save(page_path)
        rendered.append(page_path)

    thumbs = []
    for path in rendered:
        img = Image.open(path).convert("RGB")
        img.thumbnail((360, 510))
        canvas = Image.new("RGB", (380, 545), "white")
        canvas.paste(img, ((380 - img.width) // 2, 8))
        draw = ImageDraw.Draw(canvas)
        draw.text((12, 520), path.stem, fill=(20, 20, 20))
        thumbs.append(canvas)

    per_sheet = 6
    for start in range(0, len(thumbs), per_sheet):
        batch = thumbs[start : start + per_sheet]
        sheet = Image.new("RGB", (760, 545 * 3), "white")
        for j, thumb in enumerate(batch):
            x = (j % 2) * 380
            y = (j // 2) * 545
            sheet.paste(thumb, (x, y))
        sheet_path = OUT / f"contact_{start // per_sheet + 1}.png"
        sheet.save(sheet_path)
        print(sheet_path)


if __name__ == "__main__":
    main()
