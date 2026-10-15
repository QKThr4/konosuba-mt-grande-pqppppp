#!/usr/bin/env python3
"""Lists English literals in Java sources that look user-facing and have no Portuguese entry."""
import re, glob, json, os, sys
root = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
sys.path.insert(0, os.path.join(root, 'tools'))
import importlib
jp = importlib.import_module('i18n.java_pairs').PAIRS
skip_files = ('Config.java', 'Scheduler.java', 'KonoRPG.java')
missing = {}
for f in sorted(glob.glob(os.path.join(root, 'src/main/java/**/*.java'), recursive=True)):
    if f.endswith(skip_files): continue
    src = open(f, encoding='utf-8').read()
    for i, line in enumerate(src.split('\n'), 1):
        if line.strip().startswith(('//', '*', '/*', 'import', 'package')): continue
        for m in re.finditer(r'"((?:[^"\\]|\\.)*)"', line):
            l = m.group(1).replace('\\"', '"').replace("\\'", "'")
            if not re.search(r'[A-Za-z]{2,}', l): continue
            if l.startswith(('minecraft:', 'konorpg', 'textures', 'net.', 'key.', 'item.', 'entity.', 'block.', 'npc.', 'rpg_', 'sync_', 'open_', 'main', 'intro', 'hub', 'board', 'dialogue', 'shop', 'close', 'card')): continue
            if ' ' not in l and not l[0].isupper(): continue
            if re.fullmatch(r'[a-z_]+', l): continue
            if l in jp: continue
            missing.setdefault(l, []).append(os.path.basename(f) + ':' + str(i))
for k, v in missing.items():
    print(repr(k), '<-', v[0])
print(len(missing), 'candidates', file=sys.stderr)
