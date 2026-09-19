#!/usr/bin/env python3
"""Assemble a Portal bundle from Gradle staging. Fail closed if signatures or metadata are absent."""
from pathlib import Path
import argparse, hashlib, zipfile, xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[2]
p=argparse.ArgumentParser();p.add_argument('--unsigned-preview',action='store_true');args=p.parse_args()
repo=ROOT/'android/build/release-repository'
poms=list(repo.glob('**/*.pom')); assert len(poms)==5, 'Stage exactly the five public modules into a clean release-repository first'
ns={'m':'http://maven.apache.org/POM/4.0.0'}
files=[]
for pom in poms:
    root=ET.parse(pom).getroot()
    for field in ('name','description','url','licenses/license/name','developers/developer/id','scm/connection'):
        assert root.find('m:'+field.replace('/','/m:'),ns) is not None, f'Missing {field} in {pom}'
    base=pom.with_suffix('')
    for suffix in ('-sources.jar','-javadoc.jar'): assert Path(str(base)+suffix).is_file(), f'Missing {suffix}'
    for artifact in pom.parent.iterdir():
        if artifact.suffix not in ('.pom','.jar','.aar','.module'): continue
        if not args.unsigned_preview: assert Path(str(artifact)+'.asc').is_file(), f'Missing signature: {artifact.name}. Set TRAIL_SIGNING_KEY and TRAIL_SIGNING_PASSWORD before staging.'
        files.append(artifact)
        signature=Path(str(artifact)+'.asc')
        if signature.exists(): files.append(signature)
output=ROOT/'artifacts/release'/('central-UNSIGNED-PREVIEW.zip' if args.unsigned_preview else 'central-bundle.zip')
output.parent.mkdir(parents=True,exist_ok=True)
with zipfile.ZipFile(output,'w',zipfile.ZIP_DEFLATED) as z:
    for path in sorted(set(files)):
        data=path.read_bytes();name=str(path.relative_to(repo));z.writestr(name,data)
        if path.suffix!='.asc':
            for algorithm in ('md5','sha1','sha256','sha512'): z.writestr(name+'.'+algorithm,hashlib.new(algorithm,data).hexdigest())
print(f'{output} — '+('PREVIEW ONLY: unsigned, not publishable' if args.unsigned_preview else 'ready for Central Portal validation; not uploaded'))
