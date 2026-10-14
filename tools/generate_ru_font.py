"""Offline Cyrillic bake; existing Pillow only, no runtime font dependency."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

root = Path(__file__).resolve().parents[1]
font = ImageFont.truetype(str(root / 'assets/source/ui/DejaVuSans.ttf'), 48)
ascent, descent = font.getmetrics()
atlas = Image.new('RGBA', (1024, 512))
draw = ImageDraw.Draw(atlas)
chars = sorted(set(range(32, 127)) | set(range(1040, 1104)) | {1025, 1105, 8211, 8212, 8230, 8594, 215})
rows = ['info face="NeedlePanel RU" size=48 bold=0 italic=0 charset="" unicode=1 stretchH=100 smooth=1 aa=1 padding=0,0,0,0 spacing=1,1',
        f'common lineHeight={ascent + descent} base={ascent} scaleW=1024 scaleH=512 pages=1 packed=0',
        'page id=0 file="needle-panel-ru.png"', f'chars count={len(chars)}']
x, y, row_height = 2, 2, 0
for code in chars:
    char = chr(code)
    left, top, right, bottom = font.getbbox(char, anchor='ls')
    width, height = right - left, bottom - top
    if x + width + 3 > atlas.width:
        x, y, row_height = 2, y + row_height + 3, 0
    assert y + height < atlas.height
    draw.text((x-left, y-top), char, font=font, anchor='ls', fill='white')
    rows.append(f'char id={code} x={x} y={y} width={width} height={height} xoffset={left} yoffset={ascent+top} xadvance={round(font.getlength(char))} page=0 chnl=15')
    x += width + 3
    row_height = max(row_height, height)
out = root / 'core/src/main/resources/ui'
atlas.save(out / 'needle-panel-ru.png')
(out / 'needle-panel-ru.fnt').write_text('\n'.join(rows) + '\n', encoding='utf-8')
print(f'Baked {len(chars)} Latin/Cyrillic glyphs; last row y={y}, height={row_height}')
