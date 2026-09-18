"""Manual real-screen QA for the calendar summary on the disposable emulator.
Run one of: standard, small, palettes, real. Calendar details are opened
directly; no last-period preference is read or changed.
Uses actual HomeScreen and its sheet; screenshots are not regression tests.
"""
import sys,time,re
from pathlib import Path
import xml.etree.ElementTree as E
ROOT=Path(__file__).resolve().parent.parent
sys.path.insert(0,str(ROOT/'design'))
import capture_v2 as c
c.OUT=ROOT/'design/qa-flow'; c.OUT.mkdir(exist_ok=True)

def dump(name=None):
 c.adb('shell','uiautomator','dump','/sdcard/timeleft-flow.xml')
 xml=c.adb('shell','cat','/sdcard/timeleft-flow.xml')
 if name: (c.OUT/(name+'.xml')).write_bytes(xml)
 return E.fromstring(xml)
def snap(name):
 r=dump(name);(c.OUT/(name+'.png')).write_bytes(c.adb('exec-out','screencap','-p'));print(name,flush=True);return r
def tap(label,prefix=False):
 r=dump()
 nodes=[n for n in r.iter('node') if any((n.get(k,'').startswith(label) if prefix else n.get(k)==label) for k in ('text','content-desc'))]
 assert nodes, 'Missing '+label
 a,b,x,y=map(int,re.findall(r'\d+',nodes[0].get('bounds')))
 c.adb('shell','input','tap',(a+x)//2,(b+y)//2);time.sleep(1)
def has(r,label):return any(label in n.get('text','') or label in n.get('content-desc','') for n in r.iter('node'))
def detail(period,name,locale='ko',back=False):
 tap(period,True);r=snap(name)
 close='Close period details' if locale=='en' else '기간 상세 닫기'
 assert has(r,close)
 if back:c.adb('shell','input','keyevent','4');time.sleep(1)
 else:tap(close)
 assert not has(dump(),close)

group=sys.argv[1]
c.adb('shell','wm','size','reset');c.adb('shell','wm','density','reset')
try:
 if group=='standard':
  c.capture('home')
  original={n.get('content-desc'):n.get('bounds') for n in dump().iter('node') if n.get('clickable')=='true' and n.get('content-desc')}
  for period,key in [('오늘,','today'),('이번 달,','month'),('올해,','year')]: detail(period,'detail-'+key,back=key=='month')
  restored={n.get('content-desc'):n.get('bounds') for n in dump().iter('node') if n.get('clickable')=='true' and n.get('content-desc')}
  assert original==restored,'Home position changed'
  for screen in ['idle','empty','dates','overlap','period-long','period-boundary','progress-hidden']:
   c.capture(screen)
   if screen=='progress-hidden':
    tap('올해,',True);r=snap('detail-hidden');assert not has(r,'퍼센트 남음');tap('기간 상세 닫기')
  c.capture('home',theme='dark');detail('올해,','detail-year-dark')
 elif group=='small':
  c.adb('shell','wm','size','960x2142');c.adb('shell','wm','density','480')
  for screen,scale,locale in [('home',1,'ko'),('long',1.5,'ko'),('empty',1.5,'ko'),('idle',1.5,'ko'),('period-long',2,'ko'),('long',1.5,'en'),('period-long',2,'en')]:
   name=c.capture(screen,theme='dark',small=True,scale=scale,locale=locale)
   if screen=='period-long':
    detail('Today,' if locale=='en' else '오늘,',name+'-detail-today',locale)
    detail('This year,' if locale=='en' else '올해,',name+'-detail-year',locale)
 elif group=='palettes':
  for palette in ['indigo','emerald','rose','amber','slate']:
   for theme in ['light','dark']:c.capture('home',palette=palette,theme=theme)
 elif group=='real':
  def launch():
   c.adb('shell','am','force-stop','com.devidea.timeleft')
   c.adb('shell','am','start','-W','-n','com.devidea.timeleft/.activity.MainActivity');time.sleep(2)
  launch();r=snap('real-home')
  english=has(r,'This year,')
  year='This year,' if english else '올해,'
  close='Close period details' if english else '기간 상세 닫기'
  tap('This month,' if english else '이번 달,',True);r=snap('real-month');assert has(r,'2026-09-30');tap(close)
  launch();assert not has(dump(),close)
  tap(year,True);r=snap('real-year');assert has(r,'2026-12-31');tap(close)
  launch();assert not has(dump(),close)
  tap('All schedules' if english else '전체 일정');r=snap('real-items');assert has(r,'My items' if english else '내 일정');c.adb('shell','input','keyevent','4');time.sleep(1)
  tap('Open settings' if english else '설정 열기');snap('real-settings');c.adb('shell','input','keyevent','4');time.sleep(1)
 print('PASS '+group,flush=True)
finally:
 c.adb('shell','wm','size','reset');c.adb('shell','wm','density','reset')
