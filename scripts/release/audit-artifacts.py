#!/usr/bin/env python3
"""Report measured archive sizes and packaged native alignment; never infer device download size."""
from pathlib import Path
import hashlib, json, struct, zipfile
ROOT = Path(__file__).resolve().parents[2]
def archive(path):
    with zipfile.ZipFile(path) as z:
        archive_bytes=path.read_bytes()
        libs=[]
        for info in z.infolist():
            if not info.filename.endswith('.so'): continue
            data=z.read(info)
            aligns=[]
            if data[:5] == b'\x7fELF\x02' and data[5] == 1:
                phoff=struct.unpack_from('<Q',data,32)[0]
                entsize,count=struct.unpack_from('<HH',data,54)
                for i in range(count):
                    offset=phoff+i*entsize
                    if struct.unpack_from('<I',data,offset)[0] == 1:
                        aligns.append(struct.unpack_from('<Q',data,offset+48)[0])
            name_length,extra_length=struct.unpack_from('<HH',archive_bytes,info.header_offset+26)
            data_offset=info.header_offset+30+name_length+extra_length
            stored_apk=path.suffix=='.apk' and info.compress_type==zipfile.ZIP_STORED
            libs.append({'file':info.filename,'load_alignments':aligns,
                         'elf_16kb':(bool(aligns) and all(a>=16384 for a in aligns)) if data[4]==2 else None,
                         'zip_data_offset':data_offset,
                         'apk_zip_16kb':data_offset%16384==0 if stored_apk else None,
                         'note':('64-bit load alignment' if data[4]==2 else '32-bit ABI; 16 KB runtime check is not applicable') +
                                ('; APK entry is stored uncompressed' if stored_apk else '; bundle/compressed entry: final APK alignment must be checked separately')})
        return {'path':str(path.relative_to(ROOT)),'bytes':path.stat().st_size,
                'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),
                'dex_compressed_bytes':sum(i.compress_size for i in z.infolist() if i.filename.endswith('.dex')),
                'native_libraries':libs,
                'jar_signature_present':any(i.filename.startswith('META-INF/') and i.filename.endswith(('.RSA','.EC','.DSA')) for i in z.infolist())}
report={'artifacts':[], 'note':'Local archive bytes, not installed sizes. Signature presence is not certificate identity verification.'}
sample_apk=ROOT/'android/playground/build/outputs/apk/release/playground-release.apk'
if sample_apk.is_file(): report['artifacts'].append(archive(sample_apk))
report['libraries']=[{'path':str(p.relative_to(ROOT)),'bytes':p.stat().st_size} for p in sorted((ROOT/'android/build/release-repository').glob('**/*')) if p.suffix in ('.aar','.jar') and '-sources' not in p.name and '-javadoc' not in p.name]
probes={}
for flavor in ('viewBase','viewTrail','mapBase','mapTrail'):
    matches=list((ROOT/f'android/size-probe/build/outputs/apk/{flavor}/release').glob('*.apk'))
    if len(matches)==1: probes[flavor]=archive(matches[0])
report['matched_hosts']=probes
report['incremental_apk_bytes']={label:probes[trail]['bytes']-probes[base]['bytes'] for label,base,trail in [('view','viewBase','viewTrail'),('google_maps','mapBase','mapTrail')] if base in probes and trail in probes}
out=ROOT/'artifacts/release/audit.json'; out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps({'incremental_apk_bytes':report['incremental_apk_bytes'],'libraries':report['libraries'],'report':str(out)},indent=2))
