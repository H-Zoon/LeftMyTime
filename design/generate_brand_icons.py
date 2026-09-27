"""Render compatibility/store icons from the shared vector mark and background.

Requires resvg-py and Pillow; not part of the app build. Adaptive icons use XML directly.
The mark geometry stays in ic_launcher_playstore_foreground_vector.xml.
Gradient stops come from the adaptive icon's background drawable/color resources.
"""
from io import BytesIO
from pathlib import Path
import xml.etree.ElementTree as ET

from PIL import Image
from resvg_py import svg_to_bytes

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'app/src/main/res'
ANDROID = '{http://schemas.android.com/apk/res/android}'


def mark():
    root = ET.parse(RES / 'drawable/ic_launcher_playstore_foreground_vector.xml').getroot()
    attributes = {'pathData': 'd', 'fillColor': 'fill', 'strokeColor': 'stroke',
                  'strokeWidth': 'stroke-width', 'strokeLineCap': 'stroke-linecap',
                  'strokeLineJoin': 'stroke-linejoin'}
    paths = []
    for path in root.findall('path'):
        values = {}
        for source, target in attributes.items():
            value = path.get(ANDROID + source)
            if value is not None:
                values[target] = 'none' if value == '@android:color/transparent' else value
        paths.append(ET.tostring(ET.Element('path', values), encoding='unicode'))
    return ''.join(paths)


def background_gradient():
    colors = {e.get('name'): e.text for e in ET.parse(RES / 'values/ic_launcher_background.xml').getroot()}
    gradient = ET.parse(RES / 'drawable/ic_launcher_background.xml').getroot().find('gradient')
    if gradient.get(ANDROID + 'type') != 'linear' or gradient.get(ANDROID + 'angle') != '315':
        raise ValueError('Update the SVG direction when changing the Android icon gradient.')
    stops = []
    for attribute, offset in [('startColor', '0'), ('centerColor', '.5'), ('endColor', '1')]:
        reference = gradient.get(ANDROID + attribute)
        color = colors[reference.removeprefix('@color/')]
        stops.append(f'<stop offset="{offset}" stop-color="{color}"/>')
    return '<defs><linearGradient id="brand-background" x1="0" y1="0" x2="1" y2="1">' + ''.join(stops) + '</linearGradient></defs>'


def render(background, size, transform=None):
    symbol = mark()
    if transform:
        symbol = f'<g transform="{transform}">{symbol}</g>'
    gradient = background_gradient() if background else ''
    svg = f'<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 512 512">{gradient}{background}{symbol}</svg>'
    return svg_to_bytes(svg_string=svg, width=size, height=size)


def generate():
    fill = 'url(#brand-background)'
    store = render(f'<rect width="512" height="512" fill="{fill}"/>', 512)
    (ROOT / 'app/src/main/ic_launcher-playstore.png').write_bytes(store)
    (ROOT / 'store-assets/store-listing/app-icon-512.png').write_bytes(store)
    for density, size in [('mdpi', 48), ('hdpi', 72), ('xhdpi', 96), ('xxhdpi', 144), ('xxxhdpi', 192)]:
        folder = RES / f'mipmap-{density}'
        for name, background in [
            ('ic_launcher', f'<rect x="21.333" y="21.333" width="469.334" height="469.334" rx="96" fill="{fill}"/>'),
            ('ic_launcher_round', f'<circle cx="256" cy="256" r="256" fill="{fill}"/>'),
        ]:
            Image.open(BytesIO(render(background, size))).save(folder / f'{name}.webp', lossless=True)
        # Keep the older raster foreground consistent with the adaptive XML's safe area.
        foreground_size = size * 108 // 48
        (folder / 'ic_launcher_foreground.png').write_bytes(
            render('', foreground_size, 'translate(94.72 94.72) scale(0.63)')
        )


if __name__ == '__main__':
    generate()
