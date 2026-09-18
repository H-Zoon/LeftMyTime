"""Generate legacy-launcher thumbnails from picker XML; not an app screenshot or visual QA.

Android 12+ uses the localized previewLayout. Earlier launchers use these localized,
light/dark thumbnails. Values stay as em dashes so no sample schedules ship as data.
Run with Pillow and a sans font containing Korean glyphs (--font).
"""
from pathlib import Path
import argparse
import xml.etree.ElementTree as ET
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'app/src/main/res'
ANDROID = '{http://schemas.android.com/apk/res/android}'
SCALE = 3


def generate(font_path):
    for locale in ('', '-ko'):
        strings = {e.attrib['name']: e.text or '' for e in ET.parse(RES / f'values{locale}/strings.xml').getroot() if e.tag == 'string'}
        for night in ('', '-night'):
            colors = {e.attrib['name']: (e.text or '').replace('#FF', '#', 1) for e in ET.parse(RES / f'values{night}/colors.xml').getroot()}
            folder = RES / f'drawable{locale}{night}-nodpi'
            folder.mkdir(exist_ok=True)
            for content in ('today', 'month', 'year', 'overview', 'schedule'):
                width, height = (320, 170) if content in ('overview', 'schedule') else (180, 140)
                image = Image.new('RGBA', (width * SCALE, height * SCALE))
                draw = ImageDraw.Draw(image)
                draw.rounded_rectangle((0, 0, width * SCALE - 1, height * SCALE - 1), radius=20 * SCALE, fill=colors['activity_background'])
                layout = ET.parse(RES / f'layout/widget_picker_{content}.xml').getroot()
                texts = list(layout.iter('TextView'))

                def text(index, x, y):
                    node = texts[index]
                    label = strings[node.attrib[ANDROID + 'text'].split('/')[-1]]
                    size = int(node.attrib[ANDROID + 'textSize'].removesuffix('sp'))
                    color = colors[node.attrib[ANDROID + 'textColor'].split('/')[-1]]
                    font = ImageFont.truetype(font_path, size * SCALE)
                    draw.text((x * SCALE, y * SCALE), label, font=font, fill=color, anchor='lt')

                if content == 'overview':
                    text(0, 16, 42)
                    for column in range(3):
                        x = 20 + column * ((width - 32) / 3)
                        text(1 + column * 2, x, 72)
                        text(2 + column * 2, x, 92)
                else:
                    text(0, 16, 16)
                    text(1, 16, 48)
                    text(2, 16, 94)
                image.save(folder / f'widget_picker_{content}.png', optimize=True)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--font', required=True)
    generate(parser.parse_args().font)
