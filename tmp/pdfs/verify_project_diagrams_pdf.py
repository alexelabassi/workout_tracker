from pathlib import Path
import sys

import pypdfium2 as pdfium
from PIL import Image, ImageDraw
from pypdf import PdfReader


ROOT = Path(r"C:\Users\Alexandru\Desktop\Licenta")
PDF = ROOT / "output" / "pdf" / "project_diagrams_study_guide.pdf"
OUT = ROOT / "tmp" / "pdfs" / "project_diagrams_render"


def main() -> None:
    sys.stdout.reconfigure(encoding="utf-8")
    OUT.mkdir(parents=True, exist_ok=True)
    reader = PdfReader(str(PDF))
    print(f"pages={len(reader.pages)}")
    for idx in range(min(2, len(reader.pages))):
        text = (reader.pages[idx].extract_text() or "").replace("\n", " ")
        print(f"page_{idx + 1}_chars={len(text)}")
        print(text[:500])

    pdf = pdfium.PdfDocument(str(PDF))
    rendered = []
    for i in range(len(pdf)):
        img = pdf[i].render(scale=1.55).to_pil()
        path = OUT / f"page_{i + 1:02d}.png"
        img.save(path)
        rendered.append(path)

    thumbs = []
    for path in rendered:
        img = Image.open(path).convert("RGB")
        img.thumbnail((520, 370))
        canvas = Image.new("RGB", (540, 410), "white")
        canvas.paste(img, ((540 - img.width) // 2, 8))
        draw = ImageDraw.Draw(canvas)
        draw.text((12, 385), path.stem, fill=(20, 20, 20))
        thumbs.append(canvas)

    per_sheet = 4
    for start in range(0, len(thumbs), per_sheet):
        batch = thumbs[start:start + per_sheet]
        sheet = Image.new("RGB", (1080, 820), "white")
        for j, thumb in enumerate(batch):
            sheet.paste(thumb, ((j % 2) * 540, (j // 2) * 410))
        sheet_path = OUT / f"contact_{start // per_sheet + 1}.png"
        sheet.save(sheet_path)
        print(sheet_path)


if __name__ == "__main__":
    main()
