from pathlib import Path
from PIL import Image, ImageDraw
import json
import sys
root=Path(__file__).resolve().parent/'qa-v2'
group=sys.argv[1]
names=json.loads((root/(group+'.json')).read_text())
canvas=Image.new('RGB',(270*4,595*((len(names)+3)//4)),'#dedede')
for i,name in enumerate(names):
    im=Image.open(root/(name+'.png')).convert('RGB')
    im.thumbnail((250,550))
    x=(i%4)*270;y=(i//4)*595
    canvas.paste(im,(x+(270-im.width)//2,y+32))
    ImageDraw.Draw(canvas).text((x+4,y+4),name,fill='black')
canvas.save(root/(group+'-contact.png'))
