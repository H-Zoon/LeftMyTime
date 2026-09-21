"""Generate pre-Android-12 picker fallbacks, not screenshots or visual QA.

Android 15+ publishes the real RemoteViews; Android 12-14 uses previewLayout.
These compatibility images follow widget XML typography, dimensions and colors.
No example schedules or dates ship as data. Requires Pillow and a Korean sans font.
"""
from pathlib import Path
import argparse
import xml.etree.ElementTree as ET
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'app/src/main/res'
ANDROID = '{http://schemas.android.com/apk/res/android}'
SCALE = 3
TOKENS = ET.parse(RES / 'values/widget_tokens.xml').getroot()
DIMENSIONS = {e.attrib['name']: e.text for e in TOKENS if e.tag == 'dimen'}
STYLES = {e.attrib['name']: {i.attrib['name'].removeprefix('android:'): i.text for i in e} for e in TOKENS if e.tag == 'style'}
PARENTS = {e.attrib['name']: e.attrib.get('parent') for e in TOKENS if e.tag == 'style'}


def dimension(value):
    if value.startswith('@dimen/'):
        return dimension(DIMENSIONS[value.split('/')[-1]])
    return float(value.removesuffix('sp').removesuffix('dp'))


def style(name):
    parent = PARENTS[name] or (name.rsplit('.', 1)[0] if '.' in name else None)
    result = style(parent) if parent else {}
    result.update(STYLES[name])
    return result


def generate(font_path):
    width = dimension('@dimen/widget_preview_width')
    padding = dimension('@dimen/widget_padding')
    for locale in ('', '-ko'):
        strings = {e.attrib['name']: e.text or '' for e in ET.parse(RES / f'values{locale}/strings.xml').getroot() if e.tag == 'string'}
        for night in ('', '-night'):
            colors = {e.attrib['name']: (e.text or '').replace('#FF', '#', 1) for e in ET.parse(RES / f'values{night}/colors.xml').getroot()}
            folder = RES / f'drawable{locale}{night}-nodpi'
            folder.mkdir(exist_ok=True)
            for content in ('today', 'month', 'year', 'overview', 'schedule'):
                height = dimension('@dimen/widget_overview_preview_height' if content == 'overview' else '@dimen/widget_preview_height')
                image = Image.new('RGBA', (int(width * SCALE), int(height * SCALE)))
                draw = ImageDraw.Draw(image)
                draw.rounded_rectangle((0, 0, width * SCALE - 1, height * SCALE - 1), radius=24 * SCALE, fill=colors['activity_background'])
                layout = ET.parse(RES / f'layout/widget_picker_{content}.xml').getroot()
                texts = list(layout.iter('TextView'))

                def text(index, x, y, anchor='lt'):
                    node = texts[index]
                    attrs = style(node.attrib['style'].split('/')[-1])
                    attrs.update({k.removeprefix(ANDROID): v for k, v in node.attrib.items() if k.startswith(ANDROID)})
                    label = strings[attrs['text'].split('/')[-1]]
                    font = ImageFont.truetype(font_path, round(dimension(attrs['textSize']) * SCALE))
                    draw.text((x * SCALE, y * SCALE), label, font=font, fill=colors[attrs['textColor'].split('/')[-1]], anchor=anchor)
                    return draw.textlength(label, font=font) / SCALE

                if content == 'overview':
                    gap = dimension('@dimen/widget_gap')
                    small_gap = dimension('@dimen/widget_small_gap')
                    label_height = dimension('@dimen/widget_label_size')
                    value_height = dimension('@dimen/widget_summary_value_size')
                    label_y = (height - label_height - small_gap - value_height) / 2
                    inner = width - padding * 2 - gap * 2
                    widths = [inner * 1.5 / 3.5, inner / 3.5, inner / 3.5]
                    x = padding
                    for column in range(3):
                        text(column * 2, x, label_y)
                        text(1 + column * 2, x, label_y + label_height + small_gap)
                        x += widths[column] + gap
                else:
                    text(0, padding, 32)
                    value_width = text(1, padding, 100, anchor='ls')
                    text(2, padding + value_width + dimension('@dimen/widget_gap'), 100, anchor='ls')
                    inner = width - padding * 2
                    count = max(20, min(60, int(inner / 5)))
                    for index in range(count):
                        x = (padding + inner * (index + .5) / count) * SCALE
                        draw.line((x, 144 * SCALE, x, (120 if index % 5 == 0 else 132) * SCALE), fill=colors['widget_track'], width=2 * SCALE)
                    text(3, padding, 148)
                    text(4, width - padding, 148, anchor='rt')
                image.save(folder / f'widget_picker_{content}.png', optimize=True)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--font', required=True)
    generate(parser.parse_args().font)
