#!/usr/bin/env python3
"""One source of truth for cross-language contracts and geographically identical demos."""
from pathlib import Path
import argparse
import json

ROOT=Path(__file__).resolve().parent.parent
parser=argparse.ArgumentParser(); parser.add_argument('--check',action='store_true'); args=parser.parse_args()
pairs=[
 ('spec/fixtures/contracts.json','swift/Tests/TrailCoreTests/Fixtures/contracts.json'),
 ('samples/journeys.json','android/playground/src/main/assets/journeys.json'),
 ('samples/journeys.json','ios/TrailPlayground/Resources/journeys.json'),
 ('samples/vehicles.json','android/playground/src/main/assets/vehicles.json'),
 ('samples/vehicles.json','ios/TrailPlayground/Resources/vehicles.json'),
]
stale=[]
for source,target in pairs:
    data=(ROOT/source).read_bytes(); json.loads(data)
    path=ROOT/target
    if args.check:
        if not path.exists() or path.read_bytes()!=data: stale.append(target)
    else:
        path.parent.mkdir(parents=True,exist_ok=True); path.write_bytes(data)
if stale: raise SystemExit('Stale fixtures; run scripts/sync-fixtures.py:\n'+'\n'.join(stale))
print('Shared contract and journey fixtures are in sync.')
