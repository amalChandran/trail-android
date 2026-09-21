#!/usr/bin/env python3
"""Deterministic test data. Path expectations use a slow linear reference, not the SDK code."""
from pathlib import Path
import json
import math
import random
import argparse

ROOT = Path(__file__).resolve().parent.parent
paths = [
    ("empty", [], []), ("singleton", [[4,7]], []),
    ("coincident", [[2,3],[2,3],[2,3]], []),
    ("horizontal", [[0,0],[100,0]], []), ("vertical", [[0,0],[0,80]], []),
    ("diagonal", [[-20,-20],[80,80]], []), ("uneven-elbow", [[0,0],[3,0],[3,4]], []),
    ("reverse", [[100,20],[0,20],[0,-30]], []),
    ("repeated-vertices", [[0,0],[0,0],[3,0],[3,0],[3,4],[3,4]], []),
    ("closed", [[0,0],[30,0],[30,40],[0,40],[0,0]], []),
    ("world-seam", [[170,0],[180,0],[-180,0],[-170,0]], [2]),
    ("three-contours", [[0,0],[3,0],[100,100],[100,104],[-10,20],[-5,20]], [2,4]),
    ("zero-first-contour", [[0,0],[0,0],[20,0],[30,0]], [2]),
    ("zero-last-contour", [[0,0],[10,0],[100,0],[100,0]], [2]),
]

def edges(points, breaks):
    return [(a,b,math.dist(a,b)) for i,(a,b) in enumerate(zip(points,points[1:]),1) if i not in breaks and a != b]

def sample(points, breaks, fraction):
    es=edges(points,breaks); total=sum(e[2] for e in es)
    if not points: return None,0,total
    if not es: return points[0],0,total
    remaining=total*fraction
    for a,b,length in es:
        if remaining < length:
            t=remaining/length
            point=[a[0]+(b[0]-a[0])*t,a[1]+(b[1]-a[1])*t]
            tangent=math.atan2(b[1]-a[1],b[0]-a[0])
            break
        remaining-=length
    else:
        a,b,length=es[-1]; point=points[-1]; tangent=math.atan2(b[1]-a[1],b[0]-a[0])
    if fraction == 0: point=points[0]
    return point,tangent,total

cases={k:[] for k in ['pathSamples','pathSlices','routes','bounds','polylines','playback','projections']}
fractions=[0,.001,.01,.1,.2,.25,.4,.5,.6,.75,.9,.999,1]
windows=[(0,0),(0,1),(0,.25),(.25,.75),(.5,1),(.01,.99),(.75,.75),(1,1)]
for name,points,breaks in paths:
    for f in fractions:
        point,tangent,total=sample(points,breaks,f)
        cases['pathSamples'].append(dict(id=f'point/{name}/{f:g}',points=points,breaks=breaks,fraction=f,expected=point,tangent=tangent,length=total))
    total=sum(e[2] for e in edges(points,breaks))
    for a,b in windows:
        cases['pathSlices'].append(dict(id=f'slice/{name}/{a:g}-{b:g}',points=points,breaks=breaks,start=a,end=b,length=total*(b-a)))

pairs=[
 ('new-york-london',[40.6413,-73.7781],[51.4706,-.461941]),
 ('sydney-ferry',[-33.8597,151.21055],[-33.8006,151.284]),
 ('midtown-cab',[40.758,-73.9855],[40.7527,-73.9772]),
 ('equator',[0,0],[0,90]),('meridian',[-45,10],[45,10]),
 ('westward',[25,80],[30,-20]),('dateline-east',[10,170],[15,-170]),
 ('dateline-west',[-20,-179],[-10,179]),('coincident',[12.5,77.5],[12.5,77.5]),
 ('near-coincident',[0,0],[.00000000001,.00000000001]),
 ('near-north-pole',[84,-100],[84,80]),('north-pole',[90,0],[40,50]),
 ('south-pole',[-90,0],[-45,60]),('antipodal',[0,0],[0,180]),
 ('opposite-longitudes',[45,0],[45,180]),('same-dateline',[0,180],[0,-180]),
 ('short-dateline',[.001,179.999],[.002,-179.999]),
]
for name,a,b in pairs:
    for kind in ['direct','arc','greatCircle']:
        invalid=(kind=='arc' and max(abs(a[0]),abs(b[0]))>85.0511287798066) or (kind=='greatCircle' and name=='antipodal')
        cases['routes'].append(dict(id=f'route/{name}/{kind}',kind=kind,fromPoint=a,toPoint=b,steps=64,bend=.25,invalid=invalid))

for name,coords,center,latSpan,lonSpan in [
 ('empty',[],None,0,0),('one',[[10,20]],[10,20],0,0),
 ('ordinary',[[10,20],[30,40]],[20,30],20,20),
 ('east-dateline',[[10,179],[20,-179]],[15,180],10,2),
 ('west-dateline',[[-20,-170],[0,170]],[-10,180],20,20),
 ('same-dateline',[[0,180],[0,-180]],[0,180],0,0),
 ('prime-meridian',[[-10,-10],[10,10]],[0,0],20,20),
 ('north',[[80,-20],[84,20]],[82,0],4,40),
 ('south',[[-85,-20],[-80,20]],[-82.5,0],5,40),
 ('vertical',[[-45,50],[45,50]],[0,50],90,0),
 ('unordered',[[30,40],[10,20],[20,30]],[20,30],20,20),
 ('duplicate',[[1,2],[1,2],[1,2]],[1,2],0,0),
]: cases['bounds'].append(dict(id=f'bounds/{name}',coordinates=coords,center=center,latitudeSpan=latSpan,longitudeSpan=lonSpan))

def encode(points, precision):
    output=''; previous=[0,0]; scale=10**precision
    for point in points:
        for axis,n in enumerate(point):
            current=round(n*scale); delta=current-previous[axis]; previous[axis]=current
            value=(delta<<1) if delta>=0 else ~(delta<<1)
            while value>=32: output+=chr(((value&31)|32)+63); value>>=5
            output+=chr(value+63)
    return output

rng=random.Random(42026)
for precision in [5,6]:
    for i in range(18):
        points=[[round(rng.uniform(-80,80),precision),round(rng.uniform(-179,179),precision)] for _ in range(i+1)]
        cases['polylines'].append(dict(id=f'polyline/p{precision}/length-{i+1}',encoded=encode(points,precision),precision=precision,coordinates=points,invalid=False))
cases['polylines'].append(dict(id='polyline/google-reference',encoded='_p~iF~ps|U_ulLnnqC_mqNvxq`@',precision=5,coordinates=[[38.5,-120.2],[40.7,-120.95],[43.252,-126.453]],invalid=False))
for i,(value,precision) in enumerate([('?',5),('~',5),('~~~~~~~?',5),('!??',5),('é?',5),('😀?',5),('??_',5),('??\n?',5),('??',4),('??',7),(encode([[91,0]],5),5),(encode([[0,181]],5),5),('?????????',5),('~~~~~~~~~~~~',6)]):
    cases['polylines'].append(dict(id=f'polyline/invalid-{i}',encoded=value,precision=precision,coordinates=[],invalid=True))

for duration in [.001,1,2,60]:
    for repeat in [False,True]:
        scenarios=[
          ('initial',[],0,'playing'),
          ('quarter',[dict(op='advance',value=duration*.25)],.25,'playing'),
          ('boundary',[dict(op='advance',value=duration)],0 if repeat else 1,'playing' if repeat else 'finished'),
          ('paused',[dict(op='advance',value=duration*.25),dict(op='pause',value=0),dict(op='advance',value=duration*20)],.25,'paused'),
          ('seek-start',[dict(op='seek',value=0)],0,'paused'),
          ('seek-end',[dict(op='seek',value=1)],1,'paused'),
          ('seek-middle',[dict(op='seek',value=.75)],.75,'paused'),
          ('replay',[dict(op='advance',value=duration*4.5),dict(op='replay',value=0)],0,'playing'),
        ]
        for name,operations,progress,status in scenarios:
            cases['playback'].append(dict(id=f'playback/{duration}/{repeat}/{name}',duration=duration,repeats=repeat,operations=operations,progress=progress,status=status))

projectionRoutes=[('ordinary',[[10,20],[20,30],[15,40]]),('dateline',[[0,170],[0,-170]]),('empty',[])]
for name,points in projectionRoutes:
    for bearing in [0,30,90,180]:
        for density in [1,1.5,3]:
            for ready in [True,False]:
                cases['projections'].append(dict(id=f'projection/{name}/b{bearing}/d{density}/ready-{ready}',coordinates=points,bearing=bearing,density=density,ready=ready))

# Analytic vehicle-heading anchors, independent of the SDK's binary-search implementation.
cases['poses']=[]
def pose_case(name, points, fraction, expected, heading, reverse=False, breaks=None, contour=0, window=20):
    cases['poses'].append(dict(id='pose/'+name,points=points,breaks=breaks or [],fraction=fraction,
        expected=expected,heading=heading+(math.pi if reverse else 0),reverse=reverse,contour=contour,window=window))
for degrees in range(0,360,45):
    angle=math.radians(degrees)
    def rotate(p): return [30+p[0]*math.cos(angle)-p[1]*math.sin(angle),50+p[0]*math.sin(angle)+p[1]*math.cos(angle)]
    for reverse in [False,True]:
        for p in [0,.2,.5,.8,1]:
            pose_case(f'straight/{degrees}/{reverse}/{p}',[rotate([0,0]),rotate([100,0])],p,rotate([100*p,0]),angle,reverse)
    for turn in [-1,1]:
        for p,heading in [(.4,0),(.475,math.atan2(5,15)),(.5,math.pi/4),(.525,math.atan2(15,5)),(.6,math.pi/2)]:
            point=[200*p,0] if p<=.5 else [100,(200*p-100)*turn]
            pose_case(f'corner/{degrees}/{turn}/{p}',[rotate([0,0]),rotate([100,0]),rotate([100,100*turn])],p,rotate(point),angle+heading*turn)
for reverse in [False,True]:
    for p,point,angle,contour in [(.49,[98,0],0,0),(.5,[1000,0],math.pi/2,1),(.51,[1000,2],math.pi/2,1)]:
        pose_case(f'seam/{reverse}/{p}',[[0,0],[100,0],[1000,0],[1000,100]],p,point,angle,reverse,[2],contour,1000)
    for name,points,expected in [('empty',[],None),('single',[[7,8]],[7,8]),('duplicates',[[7,8],[7,8]],[7,8])]:
        pose_case(f'{name}/{reverse}',points,.5,expected,0,reverse)
pose_case('zero-first-contour',[[1,2],[1,2],[90,0],[100,0]],0,[1,2],0,breaks=[2])
pose_case('zero-last-contour',[[0,0],[10,0],[50,60],[50,60]],1,[50,60],0,breaks=[2],contour=1)
pose_case('exact-hairpin',[[0,0],[100,0],[0,0]],.5,[100,0],math.pi)
pose_case('zero-window',[[0,0],[100,0],[100,100]],.5,[100,0],math.pi/2,window=0)

output=ROOT/'spec/fixtures/contracts.json'
expected=json.dumps(dict(schema=1,**cases),indent=2)+'\n'
parser=argparse.ArgumentParser(); parser.add_argument('--check',action='store_true'); args=parser.parse_args()
if args.check:
    if not output.exists() or output.read_text()!=expected:
        raise SystemExit('Stale contract reference; run scripts/generate-contract-fixtures.py then scripts/sync-fixtures.py')
else:
    output.write_text(expected)
print({k:len(v) for k,v in cases.items()})
