#!/usr/bin/env python3
"""Original top-down vehicle vectors. Shared, dependency-free artwork for the native demos.

Path opcodes: 0 move, 1 line, 2 cubic, 3 quadratic, 4 close. Colors are ARGB.
All noses point up. Each asset has an explicit route anchor, independent of its shadow.
"""
from pathlib import Path
import argparse
import json

ROOT = Path(__file__).resolve().parent.parent
parser = argparse.ArgumentParser(); parser.add_argument('--check', action='store_true'); args = parser.parse_args()

def polygon(*points):
    return [[0, *points[0]], *[[1, *p] for p in points[1:]], [4]]

def rounded(x, y, w, h, r):
    return [[0,x+r,y],[1,x+w-r,y],[3,x+w,y,x+w,y+r],[1,x+w,y+h-r],
            [3,x+w,y+h,x+w-r,y+h],[1,x+r,y+h],[3,x,y+h,x,y+h-r],
            [1,x,y+r],[3,x,y,x+r,y],[4]]

def layer(path, fill, stroke=None, width=1):
    result = dict(path=path, fill=fill)
    if stroke: result.update(stroke=stroke, width=width)
    return result

ink = '#FF213443'; white = '#FFF3F7FA'; glass = '#FF203F50'; highlight = '#FF92B8CC'
car_body = [[0,32,7],[2,44,7,49,11,50,22],[1,51,74],[2,51,85,46,89,32,89],
            [2,18,89,13,85,13,74],[1,14,22],[2,15,11,20,7,32,7],[4]]
car = [
    layer(rounded(12,11,44,82,15),'#2506121C'),
    *[layer(rounded(x,y,7,15,2),ink) for x in (9,48) for y in (22,62)],
    layer(rounded(7,35,50,6,3),ink),
    layer(car_body,white,ink,1.8),
    layer(polygon((17,22),(20,13),(27,10),(24,30),(18,34)),'#FFFFFFFF'),
    layer(polygon((45,16),(48,25),(49,76),(44,83),(43,33)),'#FFB5C8D2'),
    layer(rounded(22,14,20,13,4),'#FFFFCC66'),
    layer(polygon((21,30),(43,30),(46,43),(18,43)),glass,ink),
    layer(polygon((23,32),(41,32),(43,35),(22,38)),highlight),
    layer(rounded(20,44,24,22,4),white,ink,1.1),
    layer(polygon((16,44),(18,46),(18,64),(16,67)),glass),
    layer(polygon((48,44),(46,46),(46,64),(48,67)),glass),
    layer(polygon((20,68),(44,68),(46,77),(18,77)),glass,ink),
    layer(polygon((23,69),(42,69),(43,71),(22,73)),highlight),
    layer(rounded(24,48,16,7,2),'#FFFFBE42',ink,.8),
    *[layer(rounded(x,49.5,2,4,.4),ink) for x in (27,31,35)],
    layer(rounded(16,17,5,6,2),'#FFFFF8D6'), layer(rounded(43,17,5,6,2),'#FFFFF8D6'),
    layer(rounded(17,81,7,3,1),'#FFE97065'), layer(rounded(40,81,7,3,1),'#FFE97065'),
    layer(rounded(26,84,12,2,1),'#FFCFDCE2'),
]

wings = polygon((43,33),(5,58),(4,64),(43,54),(53,54),(92,64),(91,58),(53,33))
tail = polygon((44,69),(29,82),(29,86),(47,81),(49,81),(67,86),(67,82),(52,69))
fuselage = [[0,48,5],[2,53,7,56,17,56,27],[1,54,65],[1,50,89],
            [3,48,94,46,89],[1,42,65],[1,40,27],[2,40,17,43,7,48,5],[4]]
plane = [
    layer([[v[0],*[n+(2 if i%2==0 else 3) for i,n in enumerate(v[1:])]] for v in wings],'#290A2332'),
    layer(rounded(43,11,16,84,8),'#290A2332'),
    *[layer(rounded(x,44,8,17,4),'#FFD8E5EC',ink,1.2) for x in (25,63)],
    *[layer(rounded(x,44,6,4,2),glass) for x in (26,64)],
    layer(wings,white,ink,1.2),
    layer(polygon((8,60),(43,42),(43,50),(7,63)),'#FFD2E3EB'),
    layer(polygon((88,60),(53,42),(53,50),(89,63)),'#FFB2CBD9'),
    layer(tail,white,ink,1),
    layer(fuselage,white,ink,1.3),
    layer([[0,50,9],[2,54,20,53,38,52,61],[1,49,86],[1,48,63],[1,48,17],[4]],'#FFC5DCE7'),
    layer(polygon((43,18),(47,15),(49,15),(53,18),(52,23),(49,20),(47,20),(44,23)),glass),
    *[layer(rounded(x,y,1.8,3,.8),glass) for x in (42.5,51.7) for y in (29,36,43,50,57)],
    layer(polygon((47,68),(49,68),(50,87),(48,91),(46,87)),'#FF37AEB8'),
    layer(rounded(4,60,4,3,1),'#FFFF7868'), layer(rounded(88,60,4,3,1),'#FF52CCA9'),
]

# Long tapered bow, broad passenger deck, flat stern; original generic ferry silhouette.
hull = [[0,36,5],[2,48,17,60,28,60,47],[1,60,87],[3,60,99,51,103],
        [1,21,103],[3,12,99,12,87],[1,12,47],[2,12,28,24,17,36,5],[4]]
ship = [
    layer(polygon((17,100),(9,119),(23,110),(36,114),(49,110),(63,119),(55,100)),'#70FFFFFF'),
    layer(rounded(15,27,48,79,18),'#29092330'),
    layer(hull,'#FF214A5B','#FF102E3B',1.8),
    layer([[0,36,10],[2,45,22,56,32,56,48],[1,56,87],[3,56,96,49,98],
           [1,23,98],[3,16,96,16,87],[1,16,48],[2,16,32,27,22,36,10],[4]],white),
    layer(polygon((36,15),(48,33),(24,33)),'#FFD9E9EB'),
    layer(rounded(22,36,28,48,5),'#FFC1D5DB',ink,1),
    layer(rounded(25,39,22,38,4),'#FF54B9AE'),
    layer(polygon((25,42),(47,42),(50,50),(22,50)),glass),
    layer(polygon((27,43),(44,43),(46,46),(26,47)),highlight),
    layer(rounded(27,55,18,20,3),white),
    *[layer(rounded(x,55,2,5,.5),glass) for x in (23,47)],
    *[layer(rounded(x,65,2,5,.5),glass) for x in (23,47)],
    layer(rounded(32,59,8,8,2),'#FF274D5B'),
    layer(rounded(27,86,18,7,2),'#FF58BAAE'),
    layer(rounded(19,51,2,37,1),'#FFFFFFFF'), layer(rounded(51,51,2,37,1),'#FFFFFFFF'),
    layer(rounded(34.5,22,3,11,1),'#FF789BA9'),
]

data = {'schema':1, 'vehicles':[
    dict(id='cab',width=64,height=96,anchor=[32,48],size=46,headingWindow=14,duration=18,layers=car),
    dict(id='flight',width=96,height=96,anchor=[48,48],size=52,headingWindow=24,duration=16,layers=plane),
    dict(id='ferry',width=72,height=120,anchor=[36,56],size=56,headingWindow=30,duration=20,layers=ship),
]}
content=json.dumps(data,separators=(',',':'))+'\n'
target=ROOT/'samples/vehicles.json'
if args.check:
    if not target.exists() or target.read_text()!=content: raise SystemExit('Stale vehicle artwork; run scripts/generate-vehicle-artwork.py')
else: target.write_text(content)
print(f'Vehicle artwork: 3 original vectors, {len(content.encode())} bytes before compression.')
