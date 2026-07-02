from pathlib import Path

from PIL import Image, ImageDraw


indir = Path("tmp/pdfs/database_guide_render")
imgs = sorted(indir.glob("page-*.png"))
thumbs = []
for path in imgs:
    image = Image.open(path).convert("RGB")
    image.thumbnail((180, 255))
    canvas = Image.new("RGB", (196, 285), "white")
    x = (196 - image.width) // 2
    canvas.paste(image, (x, 8))
    ImageDraw.Draw(canvas).text((98, 270), path.stem.split("-")[-1], anchor="mm", fill="black")
    thumbs.append(canvas)

cols = 6
rows = (len(thumbs) + cols - 1) // cols
out = Image.new("RGB", (cols * 196, rows * 285), (238, 238, 238))
for index, thumb in enumerate(thumbs):
    out.paste(thumb, ((index % cols) * 196, (index // cols) * 285))

out_path = Path("tmp/pdfs/database_guide_montage.png")
out.save(out_path)
print(out_path)
