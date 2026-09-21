#!/usr/bin/env python3
"""Fail if documentation references absent GIFs, or a still image masquerades as an animation."""
import json, re, subprocess, shutil
from pathlib import Path
root=Path(__file__).resolve().parents[2]
ffprobe=shutil.which('ffprobe') or '/opt/homebrew/bin/ffprobe'
for name in re.findall(r'(?:src="|\]\()(docs/media/[^" )]+\.gif)',(root/'README.md').read_text()):
    path=root/name;assert path.is_file(),f'Missing {name}'
    result=json.loads(subprocess.check_output([ffprobe,'-v','error','-count_frames','-show_entries','stream=nb_read_frames','-of','json',str(path)]))
    assert int(result['streams'][0]['nb_read_frames'])>1,f'Not animated: {name}'
print('README animation files exist and contain multiple frames.')
