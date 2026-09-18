"""Capture real debug screens on the disposable emulator; never writes app data."""
import argparse
import json
from pathlib import Path
import subprocess
import time
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent.parent
ADB = '/Users/jun/Library/Android/sdk/platform-tools/adb'
SERIAL = 'emulator-5556'
OUT = ROOT / 'design/qa-v2'

def adb(*args):
    return subprocess.run([ADB, '-s', SERIAL, *map(str, args)], check=True, capture_output=True).stdout

def capture(screen, palette='clay', theme='light', scale=1, small=False, period=0, locale='ko'):
    name = f'{screen}-{palette}-{theme}' + ('-320' if small else '') + (f'-font{scale}' if scale != 1 else '') + (f'-period{period}' if period else '') + ('-en' if locale == 'en' else '')
    adb('shell', 'am', 'force-stop', 'com.devidea.timeleft')
    activity = 'EnglishDesignGalleryActivity' if locale == 'en' else 'DesignGalleryActivity'
    adb('shell', 'am', 'start', '-W', '-n', f'com.devidea.timeleft/.design.{activity}', '--es', 'screen', screen, '--es', 'palette', palette, '--es', 'theme', theme, '--ef', 'fontScale', scale, '--ei', 'period', period)
    time.sleep(1.2)
    for attempt in range(5):
        adb('shell', 'uiautomator', 'dump', '/sdcard/timeleft-qa.xml')
        xml = adb('shell', 'cat', '/sdcard/timeleft-qa.xml')
        nodes = ET.fromstring(xml).iter('node')
        if any(n.get('package') == 'com.devidea.timeleft' and (n.get('text') or n.get('content-desc')) for n in nodes):
            (OUT / (name + '.xml')).write_bytes(xml)
            break
        time.sleep(1)
    else:
        raise RuntimeError(f'No rendered app content for {name}')
    image = adb('exec-out', 'screencap', '-p')
    (OUT / (name + '.png')).write_bytes(image)
    print(name, flush=True)
    return name

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('group', choices=['standard', 'palettes', 'small', 'widgets'])
    args = parser.parse_args()
    OUT.mkdir(exist_ok=True)
    adb('shell', 'wm', 'size', 'reset')
    adb('shell', 'wm', 'density', 'reset')
    names = []
    try:
        if args.group == 'standard':
            for screen, theme in [('home','light'), ('home','dark'), ('idle','light'), ('overlap','light'), ('empty','light'), ('dates','light'), ('editor-time','light'), ('editor','light'), ('settings','light'), ('settings','dark'), ('items','light'), ('grid','light')]:
                names.append(capture(screen, theme=theme))
        elif args.group == 'palettes':
            for palette in ['indigo','emerald','rose','amber','slate']:
                for theme in ['light','dark']:
                    names.append(capture('home', palette, theme))
        elif args.group == 'small':
            adb('shell', 'wm', 'size', '960x2142')
            adb('shell', 'wm', 'density', '480')
            for screen in ['long','empty','grid','editor-time','editor','settings','idle','picker-time','picker-date']:
                names.append(capture(screen, theme='dark', scale=1.5, small=True))
            names.append(capture('idle', scale=2, small=True, period=2))
            names.append(capture('editor-time', scale=1.5, small=True, locale='en'))
            names.append(capture('home', scale=1.5, small=True, locale='en'))
            names.append(capture('long', scale=1.5, small=True, locale='en'))
        else:
            for screen in ['widget-small-min','widget-small','widget-wide','widget-medium','widget-large']:
                for theme in ['light','dark']:
                    names.append(capture(screen, theme=theme))
            names.append(capture('widget-large', theme='dark', scale=1.5))
    finally:
        adb('shell', 'wm', 'size', 'reset')
        adb('shell', 'wm', 'density', 'reset')
    (OUT / (args.group + '.json')).write_text(json.dumps(names, ensure_ascii=False, indent=2))
