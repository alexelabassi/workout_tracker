from pathlib import Path

from PIL import Image, ImageDraw


indir = Path("tmp/pdfs/security_guide_render")
imgs = sorted(indir.glob("page-*.png"))
thumbs = []
for path in imgs:
    image = Image.open(path).convert("RGB")
    image.thumbnail((260, 370))
    canvas = Image.new("RGB", (280, 400), "white")
    x = (280 - image.width) // 2
    canvas.paste(image, (x, 8))
    ImageDraw.Draw(canvas).text((140, 382), path.stem.split("-")[-1], anchor="mm", fill="black")
    thumbs.append(canvas)

cols = 3
rows = (len(thumbs) + cols - 1) // cols
out = Image.new("RGB", (cols * 280, rows * 400), (238, 238, 238))
for index, thumb in enumerate(thumbs):
    out.paste(thumb, ((index % cols) * 280, (index // cols) * 400))

out_path = Path("tmp/pdfs/security_guide_montage.png")
out.save(out_path)
print(out_path)
