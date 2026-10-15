#!/usr/bin/env python3
"""Generates all textures, models, lang and data JSON for KonoRPG. Run from the project root."""
import json, os, random, struct, zlib

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources')
A = os.path.join(ROOT, 'assets', 'konorpg')
D = os.path.join(ROOT, 'data', 'konorpg')


def w(path, content):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    mode = 'wb' if isinstance(content, bytes) else 'w'
    with open(path, mode, **({} if mode == 'wb' else {'encoding': 'utf-8'})) as f:
        f.write(content)


def js(path, obj):
    w(path, json.dumps(obj, ensure_ascii=False, indent=1))


def png(path, W, H, px):
    raw = b''
    for y in range(H):
        raw += b'\x00' + b''.join(bytes(px[y][x]) for x in range(W))
    def chunk(t, d):
        c = struct.pack('>I', len(d)) + t + d
        return c + struct.pack('>I', zlib.crc32(t + d) & 0xffffffff)
    data = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', W, H, 8, 6, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b'')
    w(path, data)


def canvas(W, H, c=(0, 0, 0, 0)):
    return [[c for _ in range(W)] for _ in range(H)]


def rect(img, x, y, ww, hh, c):
    for j in range(y, y + hh):
        for i in range(x, x + ww):
            if 0 <= j < len(img) and 0 <= i < len(img[0]):
                img[j][i] = c


def rgb(h):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


def shade(c, f):
    if isinstance(c, str):
        c = rgb(c)
    return (max(0, min(255, int(c[0] * f))), max(0, min(255, int(c[1] * f))), max(0, min(255, int(c[2] * f))), 255)

# ---------------------------------------------------------------- skins
FACES = {
    'head': [(8, 0, 8, 8), (16, 0, 8, 8), (0, 8, 8, 8), (8, 8, 8, 8), (16, 8, 8, 8), (24, 8, 8, 8)],
    'body': [(20, 16, 8, 4), (28, 16, 8, 4), (16, 20, 4, 12), (20, 20, 8, 12), (28, 20, 4, 12), (32, 20, 8, 12)],
    'rleg': [(4, 16, 4, 4), (8, 16, 4, 4), (0, 20, 4, 12), (4, 20, 4, 12), (8, 20, 4, 12), (12, 20, 4, 12)],
    'rarm': [(44, 16, 4, 4), (48, 16, 4, 4), (40, 20, 4, 12), (44, 20, 4, 12), (48, 20, 4, 12), (52, 20, 4, 12)],
    'lleg': [(20, 48, 4, 4), (24, 48, 4, 4), (16, 52, 4, 12), (20, 52, 4, 12), (24, 52, 4, 12), (28, 52, 4, 12)],
    'larm': [(36, 48, 4, 4), (40, 48, 4, 4), (32, 52, 4, 12), (36, 52, 4, 12), (40, 52, 4, 12), (44, 52, 4, 12)],
}


def make_skin(name, skin, hair, top, bottom, boots, eye='#222222', long_hair=False, extra=None, scales=False, bones=False):
    img = canvas(64, 64)
    skin, hair, top, bottom, boots = map(rgb, (skin, hair, top, bottom, boots))
    for part in ('head', 'body', 'rleg', 'rarm', 'lleg', 'larm'):
        base = {'head': skin, 'body': top, 'rleg': bottom, 'lleg': bottom, 'rarm': top, 'larm': top}[part]
        for k, (u, v, ww, hh) in enumerate(FACES[part]):
            rect(img, u, v, ww, hh, shade(base, 1.0 if k != 1 else 0.7))
    # hair
    for k, (u, v, ww, hh) in enumerate(FACES['head']):
        if k in (0, 4, 2, 5):
            rect(img, u, v, ww, hh, hair)
    u, v, _, _ = FACES['head'][3]
    rect(img, u, v, 8, 2, hair)  # bangs
    rect(img, u, v + 2, 1, 2, hair); rect(img, u + 7, v + 2, 1, 2, hair)
    # face
    rect(img, u + 1, v + 4, 2, 1, (255, 255, 255, 255)); rect(img, u + 5, v + 4, 2, 1, (255, 255, 255, 255))
    ec = rgb(eye)
    img[v + 4][u + 2] = ec; img[v + 4][u + 5] = ec
    rect(img, u + 3, v + 6, 2, 1, shade(skin, 0.6))
    if long_hair:
        bu, bv, bw, bh = FACES['body'][5]
        rect(img, bu, bv, bw, 8, hair)
        for (u2, v2, w2, h2) in (FACES['rarm'][5], FACES['larm'][5]):
            rect(img, u2, v2, w2, 3, hair)
    # hands (skin) + boots
    for part in ('rarm', 'larm'):
        for (u2, v2, w2, h2) in FACES[part][2:]:
            rect(img, u2, v2 + 9, w2, 3, skin)
    for part in ('rleg', 'lleg'):
        for (u2, v2, w2, h2) in FACES[part][2:]:
            rect(img, u2, v2 + 9, w2, 3, rgb(boots) if isinstance(boots, str) else boots)
    # belt
    for (u2, v2, w2, h2) in FACES['body'][2:]:
        rect(img, u2, v2 + 9, w2, 1, shade(bottom, 0.6))
    if extra == 'apron':
        u2, v2, _, _ = FACES['body'][3]
        rect(img, u2 + 1, v2 + 3, 6, 9, rgb('#e8e0cc'))
    if extra == 'armor':
        for (u2, v2, w2, h2) in FACES['body'][2:]:
            rect(img, u2, v2, w2, 5, rgb('#9aa3ad'))
        rect(img, FACES['head'][0][0], FACES['head'][0][1], 8, 8, rgb('#7d8791'))
        for k in (2, 3, 4, 5):
            u2, v2, w2, h2 = FACES['head'][k]
            rect(img, u2, v2, w2, 3, rgb('#7d8791'))
    if extra == 'robe':
        for part in ('rleg', 'lleg'):
            for (u2, v2, w2, h2) in FACES[part]:
                rect(img, u2, v2, w2, h2, top)
    if scales:
        for part in ('head', 'body', 'rarm', 'larm', 'rleg', 'lleg'):
            for (u2, v2, w2, h2) in FACES[part]:
                for j in range(v2, v2 + h2, 2):
                    for i in range(u2 + (j // 2) % 2, u2 + w2, 2):
                        img[j][i] = shade(img[j][i], 0.82)
    if bones:
        for (u2, v2, w2, h2) in FACES['body'][2:]:
            for j in range(v2 + 1, v2 + 9, 2):
                rect(img, u2, j, w2, 1, rgb('#6e6a58'))
        for part in ('rarm', 'larm', 'rleg', 'lleg'):
            for (u2, v2, w2, h2) in FACES[part][2:]:
                rect(img, u2 + 1, v2, 1, h2, rgb('#6e6a58'))
        rect(img, u + 1, v + 4, 2, 2, (20, 20, 20, 255)); rect(img, u + 5, v + 4, 2, 2, (20, 20, 20, 255))
    png(os.path.join(A, 'textures', 'entity', name + '.png'), 64, 64, img)


SKINS = {
    'luna': dict(skin='#f1c9a5', hair='#7a3fa0', top='#f2f2f2', bottom='#4a2b6b', boots='#3a2a20', eye='#5522aa', long_hair=True),
    'shop_weapon': dict(skin='#e0b088', hair='#2b1a10', top='#a02020', bottom='#4a3a2a', boots='#2a1a10', extra='apron'),
    'shop_armor': dict(skin='#d9a579', hair='#555555', top='#5d6d7d', bottom='#3a3a44', boots='#222222', extra='armor'),
    'shop_magic': dict(skin='#f0d0b0', hair='#e8e8f0', top='#5b2d8e', bottom='#5b2d8e', boots='#2a1a40', eye='#7733cc', extra='robe', long_hair=True),
    'shop_item': dict(skin='#e8c09a', hair='#8a5a2a', top='#3f8f3f', bottom='#5a4a2a', boots='#2a1a10', extra='apron'),
    'blacksmith': dict(skin='#c08860', hair='#222222', top='#5a3a22', bottom='#3a2a1a', boots='#1a1a1a', extra='apron'),
    'alchemist': dict(skin='#f0d8c0', hair='#2a6a6a', top='#f0f0f0', bottom='#2a4a4a', boots='#3a2a20', eye='#116666'),
    'innkeeper': dict(skin='#f0c8a0', hair='#c05a20', top='#d98a2a', bottom='#6a3a1a', boots='#3a2a10', extra='apron', long_hair=True),
    'tavernkeeper': dict(skin='#d8a880', hair='#4a2a10', top='#8a2a2a', bottom='#3a2a2a', boots='#222222', extra='apron'),
    'guard': dict(skin='#e0b088', hair='#333333', top='#7a8794', bottom='#3a4048', boots='#222222', extra='armor'),
    'aqua': dict(skin='#f6d8c0', hair='#4fc3f7', top='#8fd6ff', bottom='#e8f6ff', boots='#4f8fcf', eye='#2a66cc', long_hair=True),
    'goblin': dict(skin='#5a9a3a', hair='#2b4a1a', top='#6a4a2a', bottom='#4a3820', boots='#2a1a10', eye='#ffe040'),
    'kobold': dict(skin='#b5683a', hair='#6a3a1a', top='#7a5a3a', bottom='#4a3a2a', boots='#2a1a10', eye='#ffcc00'),
    'orc': dict(skin='#3f7a2f', hair='#111111', top='#2a2a2a', bottom='#4a3a2a', boots='#111111', eye='#ff3030', extra='armor'),
    'lizardman': dict(skin='#2a9d8f', hair='#1d6f65', top='#264653', bottom='#264653', boots='#1a2a30', eye='#ffee00', scales=True),
    'skeleton_warrior': dict(skin='#e8e4d0', hair='#d8d4c0', top='#d8d4c0', bottom='#d8d4c0', boots='#c8c4b0', eye='#222222', bones=True),
    'giant_toad': dict(skin='#6b8e23', hair='#4f6b18', top='#7a9a30', bottom='#5f7f20', boots='#4f6b18', eye='#ffd700', scales=True),
}
VILL = [('#f1c9a5', '#6a3a1a', '#3a6ea5', '#4a3a2a'), ('#e0b088', '#222222', '#a55a3a', '#3a3a4a'), ('#f0d0b0', '#d9a040', '#7a3a7a', '#4a2a2a'),
        ('#c9966b', '#2a1a10', '#3a8a5a', '#3a3a2a'), ('#e8c09a', '#8a2a2a', '#c9a030', '#4a3a3a'), ('#f6d8c0', '#f0e060', '#e89aa8', '#6a4a4a'),
        ('#d8a880', '#111111', '#5a5a8a', '#2a2a3a'), ('#f1c9a5', '#aaaaaa', '#6a8a3a', '#4a4a2a')]
for i, (s, h, t, b) in enumerate(VILL):
    SKINS['villager_' + 'abcdefgh'[i]] = dict(skin=s, hair=h, top=t, bottom=b, boots='#2a1a10', long_hair=(i % 3 == 2))
KEEP = {'aqua', 'wiz'}
for n, kw in SKINS.items():
    if n in KEEP and os.path.exists(os.path.join(A, 'textures', 'entity', n + '.png')):
        continue
    make_skin(n, **kw)

# ---------------------------------------------------------------- items
def icon(name, fn):
    img = canvas(16, 16)
    fn(img)
    png(os.path.join(A, 'textures', 'item', name + '.png'), 16, 16, img)


def px(img, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        img[y][x] = c if len(c) == 4 else rgb(c) if isinstance(c, str) else c


def sword(blade, hilt, gem=None, wide=False):
    def f(img):
        for i in range(3, 13):
            px(img, i, 15 - i, blade); px(img, i + 1, 15 - i, shade(blade, 0.8))
            if wide: px(img, i, 14 - i, shade(blade, 1.1) if False else blade)
        for i in range(2, 5):
            px(img, i, 15 - i - 0, hilt)
        px(img, 3, 11, hilt); px(img, 4, 12, hilt); px(img, 2, 12, hilt); px(img, 4, 10, hilt)
        px(img, 1, 14, hilt); px(img, 2, 13, hilt)
        if gem: px(img, 3, 12, gem)
    return f


def axe(head, wood):
    def f(img):
        for i in range(3, 13): px(img, i, 15 - i, wood)
        for (x, y) in [(9, 2), (10, 2), (11, 3), (10, 3), (9, 3), (8, 4), (11, 4), (10, 4), (9, 4), (12, 4)]: px(img, x, y, head)
    return f


def staff(wood, gem):
    def f(img):
        for i in range(3, 14): px(img, i, 15 - i, wood)
        for (x, y) in [(12, 3), (13, 2), (12, 2), (13, 3), (11, 2), (12, 1), (13, 1), (14, 2), (14, 3), (13, 4)]: px(img, x, y, gem)
        px(img, 12, 3, (255, 255, 255, 255))
    return f


def bow(wood):
    def f(img):
        for i in range(2, 14):
            x = 3 + int(3 * (1 - abs(i - 8) / 6.0))
            px(img, x, i, wood)
        for i in range(2, 14): px(img, 12, i, '#dddddd')
    return f


def ring(gem, band='#d4af37'):
    def f(img):
        for (x, y) in [(6, 8), (5, 9), (5, 10), (5, 11), (6, 12), (7, 13), (8, 13), (9, 12), (10, 11), (10, 10), (10, 9), (9, 8), (7, 8), (8, 8)]: px(img, x, y, band)
        for (x, y) in [(7, 6), (8, 6), (7, 7), (8, 7), (6, 7), (9, 7)]: px(img, x, y, gem)
    return f


def necklace(gem, chain='#d4af37'):
    def f(img):
        for i in range(2, 14):
            px(img, i, 2 + int(((i - 8) ** 2) / 5.0), chain)
        for (x, y) in [(7, 9), (8, 9), (7, 10), (8, 10), (7, 11), (8, 11), (6, 10), (9, 10)]: px(img, x, y, gem)
    return f


def potion(color):
    def f(img):
        for (x, y) in [(7, 2), (8, 2), (7, 3), (8, 3), (7, 4), (8, 4)]: px(img, x, y, '#bbbbbb')
        for y in range(5, 14):
            half = 2 + min(3, y - 5) if y < 9 else 5 - max(0, y - 11)
            for x in range(8 - half, 8 + half): px(img, x, y, color)
        px(img, 6, 8, (255, 255, 255, 255))
    return f


def blob(c1, c2=None):
    def f(img):
        c2_ = c2 or shade(rgb(c1), 0.7)
        for y in range(4, 13):
            for x in range(4, 13):
                d = (x - 8) ** 2 + (y - 8) ** 2
                if d < 20: px(img, x, y, c1 if d < 11 else c2_)
        px(img, 6, 6, (255, 255, 255, 255))
    return f


def scroll(c='#e8d8a8'):
    def f(img):
        for y in range(3, 13):
            for x in range(4, 12): px(img, x, y, c)
        for x in range(3, 13): px(img, x, 3, '#a07040'); px(img, x, 12, '#a07040')
        for y in (5, 7, 9): 
            for x in range(5, 11): px(img, x, y, '#7a5a30')
    return f


def card():
    def f(img):
        for y in range(3, 13):
            for x in range(1, 15): px(img, x, y, '#c9a227' if (x in (1, 14) or y in (3, 12)) else '#f0e0a0')
        for x in range(3, 7):
            for y in range(5, 9): px(img, x, y, '#6a4a2a')
        for x in range(8, 13): px(img, x, 6, '#7a5a30'); px(img, x, 8, '#7a5a30'); px(img, x, 10, '#7a5a30')
    return f


def leg(c):
    def f(img):
        for (x, y) in [(5, 10), (6, 9), (7, 8), (8, 7), (9, 6), (10, 5)]: px(img, x, y, c); px(img, x + 1, y, c)
        for (x, y) in [(11, 4), (12, 4), (12, 5), (11, 3)]: px(img, x, y, '#f0e6d2')
    return f


def cabbage_icon():
    def f(img):
        for y in range(3, 14):
            for x in range(3, 14):
                d = (x - 8) ** 2 + (y - 8) ** 2
                if d < 26: px(img, x, y, '#5fb04a' if d > 12 else '#a8e07a')
        for i in range(5, 12): px(img, i, 8, '#3e8a30')
    return f


def bottle():
    def f(img):
        for (x, y) in [(7, 2), (8, 2), (7, 3), (8, 3), (7, 4), (8, 4)]: px(img, x, y, '#a8c8a8')
        for y in range(5, 14):
            for x in range(5, 11): px(img, x, y, '#d8ecd8' if y < 8 else '#e8e8f8')
        px(img, 6, 6, '#ffffff')
    return f


ICONS = {
    'goblin_ear': blob('#5a9a3a'), 'kobold_fang': leg('#e0d8b0'), 'orc_tusk': leg('#efe8d0'), 'lizard_scale': blob('#2a9d8f', '#1d6f65'),
    'bone_dust': blob('#e8e4d0', '#b8b4a0'), 'magic_crystal': blob('#7a5af0', '#4a2ac0'), 'toad_leg': leg('#6b8e23'), 'fried_toad_leg': leg('#c98a3a'),
    'cabbage': cabbage_icon(), 'health_potion': potion('#d83030'), 'greater_health_potion': potion('#ff4a6a'), 'mana_potion': potion('#3060e0'),
    'greater_mana_potion': potion('#40a0ff'), 'return_scroll': scroll(), 'adventurer_card': card(), 'sake_bottle': bottle(),
    'adventurer_sword': sword('#b8b8c0', '#6a4a2a'), 'goblin_hatchet': axe('#8a8a90', '#6a4a2a'), 'knight_longsword': sword('#d8d8e0', '#8a6a2a'),
    'crusader_blade': sword('#9fd0ff', '#d4af37', '#ff4040'), 'gram_sword': sword('#e8c8ff', '#d4af37', '#ff3030', True), 'hunter_bow': bow('#8a5a2a'),
    'apprentice_staff': staff('#7a5a30', '#4a90e0'), 'archmage_staff': staff('#4a2a6a', '#e04aff'),
    'iron_ring': ring('#aaaaaa', '#888888'), 'mana_ring': ring('#3a70ff'), 'lucky_ring': ring('#40d060'), 'swift_ring': ring('#f0e040'),
    'guardian_ring': ring('#e04040'), 'flame_necklace': necklace('#ff7020'), 'eris_amulet': necklace('#90f0ff'),
}
for n, f in ICONS.items():
    icon(n, f)

# block textures
img = canvas(16, 16, rgb('#8a6a3a'))
for y in range(16):
    for x in range(16):
        img[y][x] = shade(rgb('#8a6a3a'), 0.9 + 0.1 * ((x * 3 + y * 5) % 3) / 2)
rect(img, 0, 0, 16, 1, rgb('#4a3216')); rect(img, 0, 15, 16, 1, rgb('#4a3216')); rect(img, 0, 0, 1, 16, rgb('#4a3216')); rect(img, 15, 0, 1, 16, rgb('#4a3216'))
for (x, y, ww, hh) in [(2, 2, 5, 6), (9, 3, 5, 5), (3, 9, 4, 5), (9, 10, 5, 4)]:
    rect(img, x, y, ww, hh, rgb('#f0e6c0'))
    rect(img, x + 1, y + 1, ww - 2, 1, rgb('#7a5a30'))
png(os.path.join(A, 'textures', 'block', 'quest_board_front.png'), 16, 16, img)
img = canvas(16, 16, rgb('#6a4a24'))
for y in range(16):
    for x in range(16):
        img[y][x] = shade(rgb('#6a4a24'), 0.9 + 0.1 * ((x + y * 2) % 3) / 2)
png(os.path.join(A, 'textures', 'block', 'quest_board_side.png'), 16, 16, img)

# ---------------------------------------------------------------- models
ITEMS_HANDHELD = ['adventurer_sword', 'goblin_hatchet', 'knight_longsword', 'crusader_blade', 'gram_sword', 'hunter_bow', 'apprentice_staff', 'archmage_staff']
for n in ICONS:
    js(os.path.join(A, 'models', 'item', n + '.json'), {'parent': 'minecraft:item/handheld' if n in ITEMS_HANDHELD else 'minecraft:item/generated', 'textures': {'layer0': 'konorpg:item/' + n}})
EGGS = ['goblin', 'kobold', 'orc', 'lizardman', 'skeleton_warrior', 'giant_toad', 'town_npc']
for e in EGGS:
    js(os.path.join(A, 'models', 'item', e + '_spawn_egg.json'), {'parent': 'minecraft:item/template_spawn_egg'})
js(os.path.join(A, 'models', 'block', 'quest_board.json'), {'parent': 'minecraft:block/orientable', 'textures': {'top': 'konorpg:block/quest_board_side', 'front': 'konorpg:block/quest_board_front', 'side': 'konorpg:block/quest_board_side'}})
js(os.path.join(A, 'models', 'item', 'quest_board.json'), {'parent': 'konorpg:block/quest_board'})
js(os.path.join(A, 'blockstates', 'quest_board.json'), {'variants': {
    'facing=north': {'model': 'konorpg:block/quest_board'},
    'facing=east': {'model': 'konorpg:block/quest_board', 'y': 90},
    'facing=south': {'model': 'konorpg:block/quest_board', 'y': 180},
    'facing=west': {'model': 'konorpg:block/quest_board', 'y': 270}}})

# ---------------------------------------------------------------- lang
NAMES = {
    'goblin_ear': ('Goblin Ear', 'Orelha de Goblin'), 'kobold_fang': ('Kobold Fang', 'Presa de Kobold'), 'orc_tusk': ('Orc Tusk', 'Presa de Orc'),
    'lizard_scale': ('Lizardman Scale', 'Escama de Homem-Lagarto'), 'bone_dust': ('Bone Dust', 'Pó de Osso'), 'magic_crystal': ('Magic Crystal', 'Cristal Mágico'),
    'toad_leg': ('Giant Toad Leg', 'Perna de Sapo Gigante'), 'fried_toad_leg': ('Fried Toad Leg', 'Perna de Sapo Frita'), 'cabbage': ('Flying Cabbage', 'Repolho Voador'),
    'health_potion': ('Health Potion', 'Poção de Vida'), 'greater_health_potion': ('Greater Health Potion', 'Poção de Vida Maior'),
    'mana_potion': ('Mana Potion', 'Poção de Mana'), 'greater_mana_potion': ('Greater Mana Potion', 'Poção de Mana Maior'),
    'return_scroll': ('Return Scroll', 'Pergaminho de Retorno'), 'adventurer_card': ('Adventurer Card', 'Cartão de Aventureiro'), 'sake_bottle': ('Axis Sake', 'Sake da Ordem Axis'),
    'adventurer_sword': ('Adventurer Sword', 'Espada de Aventureiro'), 'goblin_hatchet': ('Goblin Hatchet', 'Machadinha de Goblin'), 'knight_longsword': ('Knight Longsword', 'Espada Longa do Cavaleiro'),
    'crusader_blade': ('Crusader Blade', 'Lâmina do Cruzado'), 'gram_sword': ("Demon Sword 'Gram'", "Espada Demoníaca 'Gram'"), 'hunter_bow': ('Hunter Bow', 'Arco do Caçador'),
    'apprentice_staff': ('Apprentice Staff', 'Cajado de Aprendiz'), 'archmage_staff': ('Archmage Staff', 'Cajado do Arquimago'),
    'iron_ring': ('Iron Ring', 'Anel de Ferro'), 'mana_ring': ('Ring of Mana', 'Anel de Mana'), 'lucky_ring': ('Lucky Ring', 'Anel da Sorte'), 'swift_ring': ('Swift Ring', 'Anel Veloz'),
    'guardian_ring': ('Guardian Ring', 'Anel do Guardião'), 'flame_necklace': ('Flame Necklace', 'Colar de Chamas'), 'eris_amulet': ("Eris's Amulet", 'Amuleto de Eris'),
    'quest_board': ('Quest Board', 'Mural de Quests'),
}
ENT = {'goblin': ('Goblin', 'Goblin'), 'kobold': ('Kobold', 'Kobold'), 'orc': ('Orc', 'Orc'), 'lizardman': ('Lizardman', 'Homem-Lagarto'),
       'skeleton_warrior': ('Skeleton Warrior', 'Guerreiro Esqueleto'), 'giant_toad': ('Giant Toad', 'Sapo Gigante'), 'town_npc': ('Townsperson', 'Morador'), 'companion': ('Companion', 'Companheiro')}
KEYS = {'key.konorpg.hub': ('Open Adventurer Hub', 'Abrir Painel do Aventureiro'), 'key.konorpg.hud': ('Toggle Progress HUD', 'Alternar HUD de Progresso'),
        'key.konorpg.skill1': ('Skill 1', 'Habilidade 1'), 'key.konorpg.skill2': ('Skill 2', 'Habilidade 2'), 'key.konorpg.skill3': ('Skill 3', 'Habilidade 3'),
        'key.konorpg.skill4': ('Skill 4', 'Habilidade 4'), 'key.categories.konorpg': ('KonoRPG', 'KonoRPG'), 'itemGroup.konorpg': ('KonoRPG', 'KonoRPG')}
for idx, lang in enumerate(('en_us', 'pt_br')):
    d = {}
    for k, v in NAMES.items():
        d[('block.konorpg.' if k == 'quest_board' else 'item.konorpg.') + k] = v[idx]
    for k, v in ENT.items():
        d['entity.konorpg.' + k] = v[idx]
    for e in EGGS:
        d['item.konorpg.' + e + '_spawn_egg'] = (ENT[e][idx] + (' Spawn Egg' if idx == 0 else ' (Ovo de Invocação)'))
    for k, v in KEYS.items():
        d[k] = v[idx]
    js(os.path.join(A, 'lang', lang + '.json'), d)

# ---------------------------------------------------------------- data: loot, recipes, spawns
def drop_pool(item, chance, lo=1, hi=1):
    entry = {'type': 'minecraft:item', 'name': item, 'conditions': [{'condition': 'minecraft:random_chance', 'chance': chance}]}
    if hi > 1:
        entry['functions'] = [{'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}}]
    return {'rolls': 1, 'entries': [entry]}


LOOT = {
    'goblin': [('konorpg:goblin_ear', 0.75, 1, 2), ('konorpg:magic_crystal', 0.05, 1, 1), ('minecraft:iron_nugget', 0.4, 1, 3)],
    'kobold': [('konorpg:kobold_fang', 0.75, 1, 2), ('minecraft:stick', 0.5, 1, 2)],
    'orc': [('konorpg:orc_tusk', 0.65, 1, 2), ('minecraft:iron_ingot', 0.3, 1, 2), ('konorpg:health_potion', 0.1, 1, 1)],
    'lizardman': [('konorpg:lizard_scale', 0.75, 1, 3), ('konorpg:magic_crystal', 0.06, 1, 1)],
    'skeleton_warrior': [('konorpg:bone_dust', 0.8, 1, 2), ('minecraft:bone', 0.6, 1, 2), ('konorpg:mana_potion', 0.08, 1, 1)],
    'giant_toad': [('konorpg:toad_leg', 1.0, 1, 2), ('konorpg:magic_crystal', 0.04, 1, 1)],
}
for e, drops in LOOT.items():
    js(os.path.join(D, 'loot_table', 'entities', e + '.json'), {'type': 'minecraft:entity', 'pools': [drop_pool(*x) for x in drops]})
js(os.path.join(D, 'loot_table', 'blocks', 'quest_board.json'), {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'konorpg:quest_board'}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
js(os.path.join(D, 'recipe', 'fried_toad_leg.json'), {'type': 'minecraft:smelting', 'ingredient': {'item': 'konorpg:toad_leg'}, 'result': {'id': 'konorpg:fried_toad_leg'}, 'experience': 0.2, 'cookingtime': 200})
js(os.path.join(D, 'recipe', 'fried_toad_leg_smoking.json'), {'type': 'minecraft:smoking', 'ingredient': {'item': 'konorpg:toad_leg'}, 'result': {'id': 'konorpg:fried_toad_leg'}, 'experience': 0.2, 'cookingtime': 100})
js(os.path.join(D, 'neoforge', 'biome_modifier', 'spawns.json'), {'type': 'neoforge:add_spawns', 'biomes': '#minecraft:is_overworld', 'spawners': [
    {'type': 'konorpg:goblin', 'weight': 5, 'minCount': 2, 'maxCount': 3}, {'type': 'konorpg:kobold', 'weight': 4, 'minCount': 2, 'maxCount': 3},
    {'type': 'konorpg:giant_toad', 'weight': 6, 'minCount': 1, 'maxCount': 3}, {'type': 'konorpg:orc', 'weight': 1, 'minCount': 1, 'maxCount': 2},
    {'type': 'konorpg:lizardman', 'weight': 2, 'minCount': 1, 'maxCount': 2}, {'type': 'konorpg:skeleton_warrior', 'weight': 2, 'minCount': 1, 'maxCount': 2}]})

# ---------------------------------------------------------------- data: quests
def quest(id, **kw):
    q = {'name': id, 'description': '', 'type': 'kill', 'amount': 1, 'xp': 30, 'money': 50, 'difficulty': 1, 'minRank': 'F', 'category': 'board', 'repeatable': False}
    q.update(kw)
    js(os.path.join(D, 'konorpg_quests', id + '.json'), q)


quest('main_01_guild', name='Capítulo 1: Bem-vindo à Guilda', description='Você acordou em um mundo de fantasia. Vá até a Guilda dos Aventureiros (prédio ao norte da praça), fale com a recepcionista Luna e registre-se.', location='Guilda de Axel', type='event', target='register_guild', xp=50, money=100, category='main', unlocks=['main_02_class'], humor='Dica: uma taxa de inscrição existe. Sim, até no outro mundo tem burocracia.', reputation={'guild': 2})
quest('main_02_class', name='Capítulo 1: Escolha sua Classe', description='Abra o painel (tecla K) > GUILDA e escolha sua classe. Depois aprenda uma habilidade na aba SKILLS.', location='Guilda de Axel', type='event', target='choose_class', xp=60, money=150, category='main', unlocks=['main_03_toads'], reputation={'guild': 2})
quest('main_03_toads', name='Capítulo 1: Sapos Gigantes', description='Os sapos gigantes estão engolindo aventureiros nas planícies. Derrote 3 deles fora da cidade.', location='Planícies fora de Axel', type='kill', target='konorpg:giant_toad', amount=3, xp=120, money=300, category='main', unlocks=['main_04_goblins'], humor='Aviso: sapos gostam de engolir coisas. Principalmente deusas.', reputation={'guild': 3, 'city': 2})
quest('main_04_goblins', name='Capítulo 1: Praga de Goblins', description='Goblins estão roubando as colheitas. Derrote 6 goblins.', location='Arredores de Axel', type='kill', target='konorpg:goblin', amount=6, xp=160, money=400, category='main', unlocks=['main_05_gate'], difficulty=2, reputation={'guild': 3, 'city': 3})
quest('main_05_gate', name='Capítulo 1: Os Portões', description='Visite o portão sul de Axel, a saída da cidade, e converse com os guardas sobre os rumores.', location='Portão sul de Axel', type='explore', target='gate', xp=80, money=200, category='main', unlocks=['main_06_skeletons'], reputation={'city': 3})
quest('main_06_skeletons', name='Capítulo 1: Guerreiros Esqueleto', description='Esqueletos armados foram vistos à noite. Derrote 5 Guerreiros Esqueleto.', location='Fora da cidade (à noite)', type='kill', target='konorpg:skeleton_warrior', amount=5, xp=220, money=600, category='main', unlocks=['main_07_rank'], difficulty=3, reputation={'guild': 4})
quest('main_07_rank', name='Capítulo 1: Aventureiro de Verdade', description='Faça quests no mural até alcançar o Rank D na Guilda. (Capítulo 2: dungeons e chefes - EM BREVE)', location='Guilda de Axel', type='event', target='rank:D', xp=400, money=1500, category='main', difficulty=3, minLevel=5, reputation={'guild': 10, 'city': 5})

quest('b_toads', name='Caça aos Sapos Gigantes', description='Derrote 3 sapos gigantes.', location='Planícies', target='konorpg:giant_toad', amount=3, xp=90, money=180, repeatable=True, reputation={'guild': 2}, humor='Recompensa: dinheiro e uma lição sobre sapos.')
quest('b_goblins', name='Controle de Goblins', description='Derrote 6 goblins.', location='Arredores', target='konorpg:goblin', amount=6, xp=110, money=220, repeatable=True, reputation={'guild': 2})
quest('b_kobolds', name='Kobolds Chatos', description='Derrote 6 kobolds.', location='Arredores', target='konorpg:kobold', amount=6, xp=120, money=240, repeatable=True, minRank='E', difficulty=2, reputation={'guild': 2})
quest('b_lizardmen', name='Homens-Lagarto', description='Derrote 5 homens-lagarto.', location='Pântanos e rios', target='konorpg:lizardman', amount=5, xp=200, money=380, repeatable=True, minRank='E', difficulty=2, reputation={'guild': 3})
quest('b_skeletons', name='Limpeza de Esqueletos', description='Derrote 6 guerreiros esqueleto.', location='Campos à noite', target='konorpg:skeleton_warrior', amount=6, xp=220, money=420, repeatable=True, minRank='E', difficulty=3, reputation={'guild': 3})
quest('b_orcs', name='Ameaça Orc', description='Derrote 4 orcs.', location='Florestas', target='konorpg:orc', amount=4, xp=300, money=600, repeatable=True, minRank='D', difficulty=4, reputation={'guild': 4, 'city': 2}, humor='Orcs fedem. A recompensa compensa. Mais ou menos.')
quest('b_goblin_ears', name='Orelhas de Goblin', description='A Guilda paga por orelhas de goblin. Entregue 8.', location='Qualquer lugar', type='collect', target='konorpg:goblin_ear', amount=8, xp=80, money=200, repeatable=True, reputation={'guild': 1, 'merchants': 1}, humor='Sim, é nojento. Sim, paga bem.')
quest('b_toad_legs', name='Cardápio de Sapo', description='A taverna quer pernas de sapo gigante. Entregue 5.', location='Qualquer lugar', type='collect', target='konorpg:toad_leg', amount=5, xp=70, money=150, repeatable=True, reputation={'merchants': 2}, humor='Dizem que parece frango. Ninguém confirmou.')
quest('b_crystals', name='Cristais Mágicos', description='Entregue 2 cristais mágicos para o Mago da cidade.', location='Qualquer lugar', type='collect', target='konorpg:magic_crystal', amount=2, xp=250, money=700, repeatable=True, minRank='E', difficulty=3, reputation={'guild': 2, 'merchants': 3})
quest('b_bones', name='Pó de Osso', description='Entregue 6 porções de pó de osso para a alquimista.', location='Qualquer lugar', type='collect', target='konorpg:bone_dust', amount=6, xp=100, money=260, repeatable=True, reputation={'merchants': 2})
quest('c_aqua_sake', name='Aqua e o Sake Sumido', description='Aqua jura que não bebeu o sake. Traga 2 garrafas de sake da loja para provar a inocência dela.', location='Taverna de Axel', type='collect', target='konorpg:sake_bottle', amount=2, xp=100, money=100, category='special', repeatable=False, require={'companion': 'aqua'}, humor='Recompensa: gratidão (duvidosa) da deusa.', reputation={'axis': 5})

# ---------------------------------------------------------------- data: dialogues
def node(text, choices):
    return {'text': text, 'choices': choices}


def ch(text, next='', effects=None, **kw):
    c = {'text': text, 'next': next}
    if effects: c['effects'] = effects
    c.update(kw)
    return c


def act(a):
    return {'type': 'action', 'action': a}


js(os.path.join(D, 'konorpg_dialogues', 'luna.json'), {'speaker': 'Luna', 'start': 'back', 'starts': [{'require': {'registered': False}, 'node': 'new'}], 'nodes': {
    'new': node('Bem-vindo à Guilda dos Aventureiros! Para se registrar, a taxa de inscrição é de {fee} Eris. Você tem {money} Eris.', [
        ch('Quero me registrar.', 'done', [act('register')], next_fail='poor'), ch('Só estou olhando.')]),
    'done': node('Pronto! Aqui está o seu Cartão de Aventureiro. Abra o painel (tecla K) > GUILDA para escolher a classe e confira o mural de quests!', [ch('Obrigado!')]),
    'poor': node('Hmm... a taxa é {fee} Eris e você tem só {money}. Aventureiros sem dinheiro? Comum por aqui. Volte quando tiver.', [ch('Entendi.')]),
    'back': node(['Olá, {player}! Rank {rank}, nível {level}. Como posso ajudar?', 'Bem-vindo de volta! Algum monstro precisando de uma lição?'], [
        ch('Abrir o painel da Guilda (classe, cartão)', '', [act('open_guild')]), ch('Ver o mural de quests', '', [act('open_board')]),
        ch('Dicas de aventureiro', 'tips'), ch('Tchau.')]),
    'tips': node('Dicas: monstros dão XP e Eris. Itens deles valem dinheiro nas lojas. Quests sobem seu rank. E nunca, NUNCA deixe a deusa beber antes de uma luta.', [ch('Voltar', 'back')])}})

js(os.path.join(D, 'konorpg_dialogues', 'shopkeeper.json'), {'speaker': 'Lojista', 'start': 'greet', 'nodes': {
    'greet': node(['Bem-vindo! Veja minhas mercadorias.', 'Olá, aventureiro! Eris na mão, itens no inventário.'], [
        ch('Ver a loja', '', [act('open_shop')]), ch('Alguma novidade?', 'news'), ch('Tchau.')]),
    'news': node(['Dizem que sapos gigantes andam engolindo aventureiros nas planícies. Principalmente os que ficam gritando.', 'Cuidado com os goblins à noite. E com deusas bêbadas, a qualquer hora.', 'Reputação com os comerciantes dá desconto, sabia? Vendendo itens de monstro você ganha a nossa confiança.'], [ch('Voltar', 'greet')])}})

js(os.path.join(D, 'konorpg_dialogues', 'blacksmith.json'), {'speaker': 'Ferreiro', 'start': 'greet', 'nodes': {
    'greet': node('Bom trabalho exige bom aço. O que precisa?', [ch('Ver a loja', '', [act('open_shop')]), ch('Reparar/Reforçar equipamentos (EM BREVE)', 'soon'), ch('Tchau.')]),
    'soon': node('Estou montando a bancada. Em breve eu reparo e reforço suas armas. Volte depois!', [ch('Voltar', 'greet')])}})

js(os.path.join(D, 'konorpg_dialogues', 'alchemist.json'), {'speaker': 'Alquimista', 'start': 'greet', 'nodes': {
    'greet': node('Poções! Efeitos colaterais não inclusos.', [ch('Ver a loja', '', [act('open_shop')]), ch('Criar poções especiais (EM BREVE)', 'soon'), ch('Tchau.')]),
    'soon': node('Meu laboratório ainda está em obras. Em breve, receitas com ingredientes de monstros!', [ch('Voltar', 'greet')])}})

js(os.path.join(D, 'konorpg_dialogues', 'innkeeper.json'), {'speaker': 'Dona da Pousada', 'start': 'greet', 'nodes': {
    'greet': node('Bem-vindo à pousada! Uma noite de descanso por 50 Eris cura tudo e salva seu ponto de retorno.', [
        ch('Descansar (50 Eris)', 'rested', [act('rest')], next_fail='greet'), ch('Só salvar meu ponto de retorno (grátis)', 'saved', [act('set_respawn')]), ch('Tchau.')]),
    'rested': node('Dormiu bem? Seus ferimentos e sua mana foram restaurados.', [ch('Obrigado!')]),
    'saved': node('Pronto, se você cair em batalha, acorda aqui. Se a deusa não ajudar antes.', [ch('Valeu.')])}})

js(os.path.join(D, 'konorpg_dialogues', 'tavern.json'), {'speaker': 'Taverneiro', 'start': 'greet', 'nodes': {
    'greet': node(['O que vai ser? Pernas de sapo fritas é o prato do dia!', 'Boa noite! Aqui se bebe, se come e se fofoca.'], [
        ch('Ver o cardápio', '', [act('open_shop')]), ch('Ouvir rumores', 'rumor'), ch('Tchau.')]),
    'rumor': node(['Ouvi dizer que existe uma caverna de goblins além dos portões... e que alguém muito poderoso a vigia. (EM BREVE)', 'Uma deusa de cabelo azul esteve aqui outro dia e me deixou a conta. Mas isso é papo de bêbado.', 'Dizem que um general demoníaco está reunindo exércitos. Espero que ele seja ruim em logística.'], [ch('Voltar', 'greet')])}})

js(os.path.join(D, 'konorpg_dialogues', 'guard.json'), {'speaker': 'Guarda', 'start': 'greet', 'nodes': {
    'greet': node(['Mantenha a ordem na cidade, aventureiro.', 'Axel é segura. Quase sempre. Não bata nos moradores.', 'Os monstros lá fora estão ficando mais ousados. Cuide-se.'], [ch('Rumores?', 'rumor'), ch('Tchau.')]),
    'rumor': node('Ultimamente, mortos-vivos têm surgido à noite. Um padre faria bem... ou uma deusa. Se ela estiver sóbria.', [ch('Entendi.')])}})

js(os.path.join(D, 'konorpg_dialogues', 'villager.json'), {'speaker': 'Morador', 'start': 'greet', 'nodes': {
    'greet': node(['Bom dia! Você é novo por aqui? Cuidado com os sapos.', 'Eu queria ser aventureiro, mas tenho medo de sapos. Muito medo.', 'Sabia que repolhos voam por aqui? Eu não acreditei. Agora acredito.', 'Dizem que a Guilda paga bem por orelhas de goblin. Eu prefiro não perguntar para que.', 'Minha vaca fugiu. Disseram que foi um goblin. Mas não tenho vaca.'], [
        ch('Tchau.'), ch('Alguma dica?', 'tip')]),
    'tip': node('Dica: venda os itens de monstros nas lojas. E mantenha sempre uma poção de vida no bolso.', [ch('Valeu!')])}})

js(os.path.join(D, 'konorpg_dialogues', 'aqua.json'), {'speaker': 'Aqua', 'start': 'talk', 'nodes': {
    'talk': node(['Sim? Fale com a deusa. Rápido, estou ocupada. (Não estou.)', 'Oi! Veio me pagar uma bebida? Não? Que droga.'], [
        ch('Aqua, me cure!', '', [act('companion_heal'), {'type': 'friendship', 'id': 'aqua', 'amount': 1}]),
        ch('Como você está?', 'how'),
        ch('Me fale da Ordem Axis.', 'axis', require={'friendship': {'id': 'aqua', 'min': 2}}),
        ch('Fique aqui / Siga-me', '', [act('companion_stay')]),
        ch('Preciso que você vá embora.', 'refuse'),
        ch('Tchau.')]),
    'how': node(['Eu? Maravilhosa, claro! Só estou com sede... e com fome... e com saudade do céu.', 'Estou ótima! Mas se aparecer um sapo gigante, eu me escondo atrás de você. Combinado?'], [ch('Voltar', 'talk')]),
    'axis': node('A Ordem Axis me adora! É a maior religião do mundo... ou será que é a pior reputação do mundo? Ninguém sabe. Mas eles me dão presentes, às vezes.', [ch('Voltar', 'talk', [{'type': 'rep', 'faction': 'axis', 'amount': 1}])]),
    'refuse': node('Como é?! Eu sou uma DEUSA! Não vou a lugar nenhum! Você me trouxe para este mundo, agora aguenta!', [ch('Desculpe...', 'talk', [{'type': 'friendship', 'id': 'aqua', 'amount': -1}])])}})

# ---------------------------------------------------------------- data: shops + economy
def shop(id, name, items):
    js(os.path.join(D, 'konorpg_shops', id + '.json'), {'name': name, 'sell': True, 'buy': [{'item': i, 'price': p} for i, p in items]})


shop('weapons', 'Armas do Bork', [('konorpg:adventurer_sword', 250), ('konorpg:goblin_hatchet', 400), ('konorpg:knight_longsword', 1200), ('konorpg:crusader_blade', 4000), ('konorpg:hunter_bow', 900), ('minecraft:arrow', 4)])
shop('armor', 'Armaduras da Greta', [('minecraft:leather_helmet', 60), ('minecraft:leather_chestplate', 100), ('minecraft:leather_leggings', 90), ('minecraft:leather_boots', 50),
                                       ('minecraft:chainmail_chestplate', 500), ('minecraft:iron_helmet', 300), ('minecraft:iron_chestplate', 600), ('minecraft:iron_leggings', 500), ('minecraft:iron_boots', 280), ('minecraft:shield', 220)])
shop('magic', 'Magia do Wiz', [('konorpg:apprentice_staff', 500), ('konorpg:archmage_staff', 6000), ('konorpg:mana_potion', 90), ('konorpg:greater_mana_potion', 280), ('konorpg:mana_ring', 1500), ('konorpg:flame_necklace', 2500), ('konorpg:lucky_ring', 2200)])
shop('items', 'Itens do Tomás', [('konorpg:health_potion', 80), ('konorpg:greater_health_potion', 250), ('konorpg:return_scroll', 150), ('minecraft:bread', 10), ('minecraft:torch', 3), ('konorpg:iron_ring', 700), ('konorpg:swift_ring', 1500), ('konorpg:guardian_ring', 1500)])
shop('smith', 'Forja do Grom', [('minecraft:iron_ingot', 40), ('minecraft:coal', 6), ('minecraft:iron_sword', 220), ('minecraft:iron_pickaxe', 220), ('minecraft:iron_axe', 220)])
shop('alchemy', 'Alquimia da Lyra', [('konorpg:health_potion', 75), ('konorpg:mana_potion', 85), ('konorpg:greater_health_potion', 235), ('konorpg:greater_mana_potion', 265), ('konorpg:magic_crystal', 200)])
shop('tavern', 'Taverna do Joe', [('konorpg:fried_toad_leg', 30), ('minecraft:cooked_beef', 20), ('minecraft:bread', 8), ('konorpg:sake_bottle', 120)])
js(os.path.join(D, 'konorpg_economy', 'sell_prices.json'), {
    'konorpg:goblin_ear': 12, 'konorpg:kobold_fang': 10, 'konorpg:orc_tusk': 35, 'konorpg:lizard_scale': 25, 'konorpg:bone_dust': 8, 'konorpg:magic_crystal': 80,
    'konorpg:toad_leg': 8, 'konorpg:fried_toad_leg': 18, 'konorpg:cabbage': 40, 'konorpg:sake_bottle': 40,
    'minecraft:iron_ingot': 15, 'minecraft:gold_ingot': 30, 'minecraft:diamond': 120, 'minecraft:emerald': 50, 'minecraft:bone': 4, 'minecraft:string': 3, 'minecraft:gunpowder': 8,
    'minecraft:rotten_flesh': 1, 'minecraft:iron_nugget': 2, 'minecraft:coal': 3})



# ================================================================= round 2 additions
# ---- toad texture (64x32) and workstation block
img = canvas(64, 32, rgb('#5f8f2a'))
for y in range(32):
    for x in range(64):
        img[y][x] = shade(rgb('#5f8f2a'), 0.85 + 0.15 * (((x * 7 + y * 11) % 5) / 4.0))
for (x, y, ww, hh) in [(2, 2, 3, 3), (12, 4, 4, 3), (20, 1, 3, 2), (8, 7, 4, 3), (24, 6, 3, 3)]:
    rect(img, x, y, ww, hh, rgb('#3f6a1a'))
rect(img, 0, 8, 32, 4, rgb('#d8d19a'))  # belly strip (bottom/sides area)
rect(img, 0, 20, 8, 4, rgb('#e8c828'))
for (x, y) in [(1, 21), (2, 21), (5, 21), (6, 21)]:
    img[y][x] = (10, 10, 10, 255)
rect(img, 0, 12, 20, 7, rgb('#6aa030'))
rect(img, 2, 17, 6, 1, rgb('#2a4a12'))
png(os.path.join(A, 'textures', 'entity', 'toad_body.png'), 64, 32, img)

img = canvas(16, 16, rgb('#7a5a30'))
for y in range(16):
    for x in range(16):
        img[y][x] = shade(rgb('#7a5a30'), 0.88 + 0.12 * (((x * 5 + y * 3) % 4) / 3.0))
rect(img, 0, 0, 16, 2, rgb('#3a3a40')); rect(img, 0, 14, 16, 2, rgb('#3a3a40'))
rect(img, 0, 0, 2, 16, rgb('#3a3a40')); rect(img, 14, 0, 2, 16, rgb('#3a3a40'))
rect(img, 6, 6, 4, 4, rgb('#d4af37'))
png(os.path.join(A, 'textures', 'block', 'workstation.png'), 16, 16, img)
js(os.path.join(A, 'models', 'block', 'workstation.json'), {'parent': 'minecraft:block/cube_all', 'textures': {'all': 'konorpg:block/workstation'}})
js(os.path.join(A, 'models', 'item', 'workstation.json'), {'parent': 'konorpg:block/workstation'})
js(os.path.join(A, 'blockstates', 'workstation.json'), {'variants': {'': {'model': 'konorpg:block/workstation'}}})
for lang, nm in (('en_us', 'Workstation'), ('pt_br', 'Bloco de Trabalho')):
    pth = os.path.join(A, 'lang', lang + '.json')
    dd = json.load(open(pth, encoding='utf-8'))
    dd['block.konorpg.workstation'] = nm
    dd['entity.konorpg.giant_toad'] = 'Giant Toad' if lang == 'en_us' else 'Sapo Gigante'
    js(pth, dd)

# ---- favor quests (given and handed in by townsfolk, not by the guild)
def favor(id, name, desc, item, amount, money, xp=25, humor=''):
    quest(id, name=name, description=desc, location='Cidade de Axel', type='collect', target=item, amount=amount, xp=xp, money=money,
          category='favor', repeatable=True, cooldownMinutes=30, humor=humor, reputation={'city': 1})


favor('fav_flowers', 'Flores para a Floricultura', 'Traga 3 papoulas (flores vermelhas) para o morador.', 'minecraft:poppy', 3, 60, humor='Ele jura que é para "decoração".')
favor('fav_sticks', 'Gravetos para a Lareira', 'Traga 8 gravetos.', 'minecraft:stick', 8, 40)
favor('fav_sake', 'Um Sake da Ordem Axis', 'Traga 1 garrafa de Sake da Ordem Axis (taverna).', 'konorpg:sake_bottle', 1, 150, humor='Dizem que é "para fins religiosos".')
favor('fav_wheat', 'Trigo para o Padeiro', 'Traga 6 trigos.', 'minecraft:wheat', 6, 70)
favor('fav_apples', 'Maçãs para a Torta', 'Traga 3 maçãs.', 'minecraft:apple', 3, 80)
favor('fav_cobble', 'Pedras para o Muro', 'Traga 16 pedregulhos.', 'minecraft:cobblestone', 16, 90)


def favor_dialogue(id, speaker, quest_id, ask, waiting, done, greet):
    js(os.path.join(D, 'konorpg_dialogues', id + '.json'), {'speaker': speaker, 'start': 'greet', 'starts': [
        {'require': {'quest_ready': quest_id}, 'node': 'deliver'}, {'require': {'quest_active': quest_id}, 'node': 'waiting'}], 'nodes': {
        'greet': node(greet, [ch('Posso ajudar com alguma coisa?', 'offer'), ch('Conversar', 'chat'), ch('Tchau.')]),
        'offer': node(ask, [ch('Claro, eu busco!', '', [{'type': 'start_quest', 'id': quest_id}, {'type': 'msg', 'text': 'Favor aceito! (Entregue falando com a pessoa de novo.)'}]), ch('Agora não.')]),
        'waiting': node(waiting, [ch('Já volto.')]),
        'deliver': node('Você trouxe! Muito obrigado, aventureiro.', [ch('Entregar', 'thanks', [{'type': 'claim_quest', 'id': quest_id}])]),
        'thanks': node(done, [ch('De nada!')]),
        'chat': node(['Axel é pacata... exceto quando uma certa deusa aparece.', 'Os monstros lá fora não param de aparecer. Ainda bem que temos a Guilda.', 'Dizem que a Guilda tem um mural novo toda hora.', 'Ouvi falar de outras cidades além das colinas. Um dia eu vou lá!'], [ch('Voltar', 'greet')])}})


favor_dialogue('villager_flowers', 'Moradora', 'fav_flowers', 'Quero decorar a casa... Você pode me trazer 3 papoulas?', 'Ainda preciso daquelas 3 papoulas!', 'Ficaram lindas! Aqui está sua recompensa.', ['Bom dia! O sol está lindo hoje.', 'Olá, viajante! Gosta de flores?'])
favor_dialogue('villager_sticks', 'Morador', 'fav_sticks', 'A lareira está apagando! Pode me trazer 8 gravetos?', 'Os 8 gravetos... preciso deles antes do frio.', 'Agora a casa vai ficar quentinha. Valeu!', ['Eita, que frio!', 'Opa, aventureiro!'])
favor_dialogue('villager_sake', 'Morador', 'fav_sake', 'Psiu... conseguiria um Sake da Ordem Axis para mim? Está na taverna. É para uma... cerimônia.', 'O sake! Já estou com sede... quer dizer, com fé.', 'Glub glub... digo, abençoado seja!', ['Psiu, você aí.', 'Ei, forasteiro...'])
favor_dialogue('villager_wheat', 'Padeiro', 'fav_wheat', 'Meu estoque de trigo acabou. Traga 6 trigos e eu lhe pago bem!', 'Ainda aguardo o trigo. Sem trigo, sem pão.', 'Pão fresquinho amanhã, graças a você!', ['Cheirinho de pão, né?', 'Bom dia, bom dia!'])
favor_dialogue('villager_apples', 'Moradora', 'fav_apples', 'Vou fazer uma torta, mas faltam 3 maçãs. Pode buscar?', 'As maçãs... as macieiras ficam fora dos muros!', 'Que torta vai ficar! Quer um pedaço? Não? Então toma isto.', ['Oi! Você parece faminto.', 'Olá! Já provou a torta da taverna?'])
favor_dialogue('villager_gossip', 'Morador', 'fav_cobble', 'O muro do meu quintal desabou. Consegue 16 pedregulhos?', 'Ainda preciso dos 16 pedregulhos!', 'Meu muro vai ficar firme de novo!', ['Já ouviu? Dizem que um goblin roubou uma galinha.', 'Cuidado com as planícies: sapos gigantes engolem gente.', 'Eu vi uma deusa chorando perto da fonte...'])

# ---- Luna (receptionist) with reward collection
js(os.path.join(D, 'konorpg_dialogues', 'luna.json'), {'speaker': 'Luna', 'start': 'back', 'starts': [{'require': {'registered': False}, 'node': 'new'}], 'nodes': {
    'new': node('Bem-vindo à Guilda dos Aventureiros! Para se registrar, a taxa de inscrição é de {fee} Eris. Você tem {money} Eris.', [
        ch('Quero me registrar.', 'done', [act('register')], next_fail='poor'), ch('Só estou olhando.')]),
    'done': node('Pronto! Aqui está o seu Cartão de Aventureiro. Abra o painel (tecla K) > GUILDA para escolher a classe e vá ao mural de quests!', [ch('Obrigado!')]),
    'poor': node('Hmm... a taxa é {fee} Eris e você tem só {money}. Aventureiros sem dinheiro? Comum por aqui. Volte quando tiver.', [ch('Entendi.')]),
    'back': node(['Olá, {player}! Rank {rank}, nível {level}. Como posso ajudar?', 'Bem-vindo de volta! Algum monstro precisando de uma lição?', 'Que bom ver você inteiro! A Guilda agradece.'], [
        ch('Entregar quests concluídas (receber recompensas)', 'paid', [act('claim_board')], next_fail='nothing'),
        ch('Abrir o painel da Guilda (classe, cartão)', '', [act('open_guild')]),
        ch('Como funcionam as quests?', 'howto'), ch('Dicas de aventureiro', 'tips'), ch('Tchau.')]),
    'paid': node(['Excelente trabalho! A Guilda agradece e a recompensa já está com você.', 'Missão cumprida! Seu rank vai agradecer.'], [ch('Obrigado!'), ch('Mais alguma coisa?', 'back')]),
    'nothing': node('Hmm, você não tem nenhuma quest concluída no momento. Aceite quests no mural e volte quando terminar!', [ch('Entendi.', 'back')]),
    'howto': node('Quests ficam no MURAL (praça e aqui dentro). Você aceita lá, cumpre o objetivo e vem falar comigo para receber XP, Eris e reputação. O mural muda com o tempo e algumas quests têm recarga.', [ch('Entendi!', 'back')]),
    'tips': node('Dicas: monstros dão XP e Eris. Itens deles valem dinheiro nas lojas. Alimente seus companheiros de tempos em tempos. E nunca, NUNCA deixe a deusa beber antes de uma luta.', [ch('Voltar', 'back')])}})

# ---- Aqua (companion) with feeding and cooldown heal
js(os.path.join(D, 'konorpg_dialogues', 'aqua.json'), {'speaker': 'Aqua', 'start': 'talk', 'nodes': {
    'talk': node(['Sim? Fale com a deusa. Rápido, estou ocupada. (Não estou.)', 'Oi! Veio me pagar uma bebida? Não? Que droga.', 'Pode falar, humilde aventureiro.'], [
        ch('Aqua, me cure!', 'healed', [act('companion_heal'), {'type': 'friendship', 'id': 'aqua', 'amount': 1}], next_fail='talk'),
        ch('Toma, comida! (alimentar com o item da mão)', 'fed', [act('companion_feed')], next_fail='talk'),
        ch('Como você está?', 'how'),
        ch('Me fale da Ordem Axis.', 'axis', require={'friendship': {'id': 'aqua', 'min': 2}}),
        ch('Conte uma história de deusa.', 'story', require={'friendship': {'id': 'aqua', 'min': 3}}),
        ch('Fique aqui / Siga-me', '', [act('companion_stay')]),
        ch('Preciso que você vá embora.', 'refuse'),
        ch('Tchau.')]),
    'healed': node('Heal! Pode agradecer depois. (Eu só consigo fazer isso de tempos em tempos, tá? Sou uma deusa, não uma fonte.)', [ch('Valeu, Aqua.', 'talk')]),
    'fed': node(['Hmm! Isso estava ótimo! Você realmente sabe cuidar de uma deusa.', 'Nham nham... se tiver sake, eu aceito também!'], [ch('De nada.', 'talk')]),
    'how': node(['Eu? Maravilhosa, claro! Só estou com sede... e com fome... e com saudade do céu.', 'Estou ótima! Mas se aparecer um sapo gigante, eu me escondo atrás de você. Combinado?'], [ch('Voltar', 'talk')]),
    'axis': node('A Ordem Axis me adora! É a maior religião do mundo... ou será que é a pior reputação do mundo? Ninguém sabe. Mas eles me dão presentes, às vezes.', [ch('Voltar', 'talk', [{'type': 'rep', 'faction': 'axis', 'amount': 1}])]),
    'story': node('Uma vez eu purifiquei um lago inteiro. Ficou tão limpo que os peixes foram embora. Dizem que é o meu poder... eu digo que foi uma vitória.', [ch('Voltar', 'talk', [{'type': 'friendship', 'id': 'aqua', 'amount': 1}])]),
    'refuse': node('Como é?! Eu sou uma DEUSA! Não vou a lugar nenhum! Você me trouxe para este mundo, agora aguenta!', [ch('Desculpe...', 'talk', [{'type': 'friendship', 'id': 'aqua', 'amount': -1}])])}})

# ---- more guard / shopkeeper / tavern lines
js(os.path.join(D, 'konorpg_dialogues', 'guard.json'), {'speaker': 'Guarda', 'start': 'greet', 'nodes': {
    'greet': node(['Mantenha a ordem na cidade, aventureiro.', 'Axel é segura. Quase sempre. Não bata nos moradores.', 'Os monstros lá fora estão ficando mais ousados. Cuide-se.', 'Se ouvir gritos de deusa, o problema provavelmente é um sapo.'], [ch('Rumores?', 'rumor'), ch('Onde fica a Guilda?', 'guild'), ch('Tchau.')]),
    'rumor': node(['Ultimamente, mortos-vivos têm surgido à noite. Um padre faria bem... ou uma deusa. Se ela estiver sóbria.', 'Falam de uma caverna de goblins além dos portões. (EM BREVE)']  , [ch('Entendi.')]),
    'guild': node('A Guilda é o prédio grande ao norte da praça. O mural de quests fica na praça e dentro dela.', [ch('Obrigado!')])}})
js(os.path.join(D, 'konorpg_dialogues', 'shopkeeper.json'), {'speaker': 'Lojista', 'start': 'greet', 'nodes': {
    'greet': node(['Bem-vindo! Veja minhas mercadorias.', 'Olá, aventureiro! Eris na mão, itens no inventário.', 'Hoje o estoque mudou! Dê uma olhada.'], [
        ch('Ver a loja', '', [act('open_shop')]), ch('Alguma novidade?', 'news'), ch('Tchau.')]),
    'news': node(['Dizem que sapos gigantes andam engolindo aventureiros nas planícies. Principalmente os que ficam gritando.', 'Cuidado com os goblins à noite. E com deusas bêbadas, a qualquer hora.', 'Reputação com os comerciantes dá desconto, sabia? Vendendo itens de monstro você ganha a nossa confiança.', 'Meu estoque muda todo dia. Quem chega cedo leva os melhores itens.'], [ch('Voltar', 'greet')])}})

# ---- shops with rotating stock ("rotate": changes every in-game day; other items are always available)
def shop2(id, name, items):
    js(os.path.join(D, 'konorpg_shops', id + '.json'), {'name': name, 'sell': True, 'buy': [
        ({'item': i, 'price': p, 'rotate': True} if r else {'item': i, 'price': p}) for (i, p, r) in items]})


shop2('weapons', 'Armas do Bork', [('konorpg:adventurer_sword', 250, False), ('minecraft:arrow', 4, False), ('konorpg:goblin_hatchet', 400, True), ('konorpg:knight_longsword', 1200, True),
                                   ('konorpg:crusader_blade', 4000, True), ('konorpg:hunter_bow', 900, True), ('minecraft:iron_sword', 200, True), ('minecraft:bow', 150, True)])
shop2('armor', 'Armaduras da Greta', [('minecraft:leather_chestplate', 100, False), ('minecraft:shield', 220, False), ('minecraft:leather_helmet', 60, True), ('minecraft:leather_leggings', 90, True),
                                      ('minecraft:leather_boots', 50, True), ('minecraft:chainmail_chestplate', 500, True), ('minecraft:iron_helmet', 300, True), ('minecraft:iron_chestplate', 600, True),
                                      ('minecraft:iron_leggings', 500, True), ('minecraft:iron_boots', 280, True)])
shop2('magic', 'Magia do Wiz', [('konorpg:mana_potion', 90, False), ('konorpg:apprentice_staff', 500, False), ('konorpg:greater_mana_potion', 280, True), ('konorpg:archmage_staff', 6000, True),
                                ('konorpg:mana_ring', 1500, True), ('konorpg:flame_necklace', 2500, True), ('konorpg:lucky_ring', 2200, True), ('konorpg:magic_crystal', 260, True)])
shop2('items', 'Itens do Tomás', [('konorpg:health_potion', 80, False), ('konorpg:return_scroll', 150, False), ('minecraft:bread', 10, False), ('minecraft:torch', 3, False),
                                  ('konorpg:greater_health_potion', 250, True), ('konorpg:iron_ring', 700, True), ('konorpg:swift_ring', 1500, True), ('konorpg:guardian_ring', 1500, True),
                                  ('minecraft:apple', 8, True), ('minecraft:cooked_chicken', 18, True)])
shop2('tavern', 'Taverna do Joe', [('konorpg:fried_toad_leg', 30, False), ('konorpg:sake_bottle', 120, False), ('minecraft:bread', 8, False), ('minecraft:cooked_beef', 20, True), ('minecraft:cooked_porkchop', 18, True), ('minecraft:apple', 7, True)])
with open(os.path.join(D, 'konorpg_economy', 'sell_prices.json'), encoding='utf-8') as f:
    prices = json.load(f)
prices.update({'minecraft:poppy': 4, 'minecraft:stick': 1, 'minecraft:wheat': 3, 'minecraft:apple': 5, 'minecraft:cobblestone': 1})
js(os.path.join(D, 'konorpg_economy', 'sell_prices.json'), prices)


# ================================================================= round 3
js(os.path.join(D, 'konorpg_dialogues', 'innkeeper.json'), {'speaker': 'Dona da Pousada', 'start': 'greet', 'nodes': {
    'greet': node(['Bem-vindo à pousada! Descansar custa 50 Eris. Também alugo quartos privados para dormir (150 Eris).', 'Cansado da viagem? Temos camas macias.'], [
        ch('Descansar (50 Eris): cura e salva o ponto de retorno', 'rested', [act('rest')], next_fail='greet'),
        ch('Alugar um quarto privado (150 Eris)', '', [act('rent_room')]),
        ch('Só salvar meu ponto de retorno (grátis)', 'saved', [act('set_respawn')]), ch('Tchau.')]),
    'rested': node('Dormiu bem? Seus ferimentos e sua mana foram restaurados.', [ch('Obrigado!')]),
    'saved': node('Pronto, se você cair em batalha e ninguém te reviver, acorda aqui.', [ch('Valeu.')])}})
js(os.path.join(D, 'konorpg_dialogues', 'tavern.json'), {'speaker': 'Taverneiro', 'start': 'greet', 'nodes': {
    'greet': node(['O que vai ser? Pernas de sapo fritas é o prato do dia! E eu compro carnes em geral.', 'Boa noite! Aqui se bebe, se come, se fofoca... e eu pago bem por carne fresca.'], [
        ch('Ver o cardápio / vender carnes', '', [act('open_shop')]), ch('Ouvir rumores', 'rumor'), ch('Tchau.')]),
    'rumor': node(['Ouvi dizer que existe uma caverna de goblins além dos portões... (EM BREVE)', 'Uma deusa de cabelo azul esteve aqui outro dia e me deixou a conta. Mas isso é papo de bêbado.', 'Dizem que um general demoníaco está reunindo exércitos. Espero que ele seja ruim em logística.'], [ch('Voltar', 'greet')])}})
MEATS = ['minecraft:beef', 'minecraft:cooked_beef', 'minecraft:porkchop', 'minecraft:cooked_porkchop', 'minecraft:mutton', 'minecraft:cooked_mutton',
         'minecraft:chicken', 'minecraft:cooked_chicken', 'minecraft:rabbit', 'minecraft:cooked_rabbit', 'minecraft:cod', 'minecraft:cooked_cod',
         'minecraft:salmon', 'minecraft:cooked_salmon', 'konorpg:toad_leg', 'konorpg:fried_toad_leg']
tav = json.load(open(os.path.join(D, 'konorpg_shops', 'tavern.json'), encoding='utf-8'))
tav['sellFilter'] = MEATS
js(os.path.join(D, 'konorpg_shops', 'tavern.json'), tav)
prices = json.load(open(os.path.join(D, 'konorpg_economy', 'sell_prices.json'), encoding='utf-8'))
prices.update({'minecraft:beef': 6, 'minecraft:cooked_beef': 14, 'minecraft:porkchop': 6, 'minecraft:cooked_porkchop': 14, 'minecraft:mutton': 5, 'minecraft:cooked_mutton': 12,
               'minecraft:chicken': 4, 'minecraft:cooked_chicken': 10, 'minecraft:rabbit': 5, 'minecraft:cooked_rabbit': 11, 'minecraft:cod': 4, 'minecraft:cooked_cod': 9,
               'minecraft:salmon': 5, 'minecraft:cooked_salmon': 10})
js(os.path.join(D, 'konorpg_economy', 'sell_prices.json'), prices)


# ================================================================= round 4: new monsters, armor, items
for n, kw in {
    'golem': dict(skin='#8a8f8a', hair='#6a7a5a', top='#7a7f7a', bottom='#6a6f6a', boots='#555a55', eye='#ffcc33'),
    'demon': dict(skin='#b03030', hair='#2a0a0a', top='#2a1a2a', bottom='#3a1a2a', boots='#111111', eye='#ffee00'),
    'demon_general': dict(skin='#5a2a6a', hair='#110011', top='#1a1a2a', bottom='#2a1a2a', boots='#0a0a0a', eye='#ff3300', extra='armor'),
}.items():
    make_skin(n, **kw)


def toad_tex(path, base, belly, eye):
    img = canvas(64, 32, rgb(base))
    for y in range(32):
        for x in range(64):
            img[y][x] = shade(rgb(base), 0.85 + 0.15 * (((x * 7 + y * 11) % 5) / 4.0))
    for (x, y, ww, hh) in [(2, 2, 3, 3), (12, 4, 4, 3), (20, 1, 3, 2), (8, 7, 4, 3), (24, 6, 3, 3)]:
        rect(img, x, y, ww, hh, shade(rgb(base), 0.7))
    rect(img, 0, 8, 32, 4, rgb(belly))
    rect(img, 0, 20, 8, 4, rgb(eye))
    for (x, y) in [(1, 21), (2, 21), (5, 21), (6, 21)]:
        img[y][x] = (10, 10, 10, 255)
    rect(img, 0, 12, 20, 7, shade(rgb(base), 1.1))
    rect(img, 2, 17, 6, 1, shade(rgb(base), 0.45))
    png(path, 64, 32, img)


toad_tex(os.path.join(A, 'textures', 'entity', 'toad_queen.png'), '#7d8f2a', '#e8c828', '#ff8800')

# cabbage 32x24
img = canvas(32, 24, rgb('#5fb04a'))
for y in range(24):
    for x in range(32):
        img[y][x] = shade(rgb('#5fb04a'), 0.8 + 0.25 * (((x * 5 + y * 3) % 6) / 5.0))
for x in range(0, 32, 4):
    rect(img, x, 0, 1, 16, rgb('#3e8a30'))
rect(img, 8, 8, 8, 8, rgb('#a8e07a'))
img[11][10] = (10, 10, 10, 255); img[11][13] = (10, 10, 10, 255)
rect(img, 11, 13, 2, 1, (60, 30, 20, 255))
rect(img, 0, 16, 20, 5, rgb('#7fd060'))
png(os.path.join(A, 'textures', 'entity', 'cabbage.png'), 32, 24, img)

# mimic 64x64 (closed = plain chest, open = teeth and red mouth)
def mimic_tex(path, opened):
    img = canvas(64, 64, rgb('#8a6a3a'))
    for y in range(64):
        for x in range(64):
            img[y][x] = shade(rgb('#8a6a3a'), 0.85 + 0.2 * (((x * 3 + y * 7) % 5) / 4.0))
    for x in range(0, 56, 14):
        rect(img, x, 0, 1, 19, rgb('#4a3216')); rect(img, x, 19, 1, 24, rgb('#4a3216'))
    rect(img, 0, 33, 56, 2, rgb('#3a3a40'))      # metal band on the base
    rect(img, 14, 14, 14, 1, rgb('#3a3a40'))
    rect(img, 20, 16, 2, 3, rgb('#d4af37'))      # latch
    if opened:
        rect(img, 14, 19, 14, 14, rgb('#a01818'))  # red mouth (base top face)
        rect(img, 14, 0, 14, 14, rgb('#701010'))   # lid underside
        for k in range(0, 14, 3):
            rect(img, 14 + k, 14, 2, 3, rgb('#f4f4f4'))   # lid front teeth
            rect(img, 14 + k, 33, 2, 3, rgb('#f4f4f4'))   # base front teeth
        rect(img, 18, 36, 6, 3, rgb('#d04060'))          # tongue
    png(path, 64, 64, img)


mimic_tex(os.path.join(A, 'textures', 'entity', 'mimic_closed.png'), False)
mimic_tex(os.path.join(A, 'textures', 'entity', 'mimic_open.png'), True)

# ---- armor layer textures (64x32) and item icons
ARMOR = {  # set: (base, trim)
    'adventurer': ('#8a5a2a', '#c9a060'), 'mage': ('#5b2d8e', '#b58cf0'),
    'knight': ('#9aa3ad', '#d8dde2'), 'crusader': ('#e8e8f0', '#d4af37'),
}
for setname, (base, trim) in ARMOR.items():
    for layer in (1, 2):
        img = canvas(64, 32, rgb(base))
        for y in range(32):
            for x in range(64):
                img[y][x] = shade(rgb(base), 0.82 + 0.22 * (((x * 5 + y * 9) % 6) / 5.0))
        for y in range(0, 32, 8):
            rect(img, 0, y, 64, 1, rgb(trim))
        for x in range(0, 64, 16):
            rect(img, x, 0, 1, 32, shade(rgb(trim), 0.8))
        png(os.path.join(A, 'textures', 'models', 'armor', setname + '_layer_%d.png' % layer), 64, 32, img)


def armor_icon(kind, base, trim):
    def f(img):
        b = rgb(base); t = rgb(trim)
        if kind == 'helmet':
            for y in range(4, 11):
                for x in range(3, 13):
                    if (x - 8) ** 2 / 25.0 + (y - 11) ** 2 / 49.0 < 1: px(img, x, y, b)
            rect(img, 3, 11, 10, 2, t); rect(img, 6, 8, 4, 2, (20, 20, 20, 255))
        elif kind == 'chestplate':
            rect(img, 3, 3, 10, 11, b); rect(img, 1, 3, 3, 5, b); rect(img, 12, 3, 3, 5, b)
            rect(img, 6, 3, 4, 2, (20, 20, 20, 255)); rect(img, 3, 11, 10, 2, t); rect(img, 7, 5, 2, 6, t)
        elif kind == 'leggings':
            rect(img, 3, 3, 10, 4, b); rect(img, 3, 7, 4, 7, b); rect(img, 9, 7, 4, 7, b); rect(img, 3, 3, 10, 1, t)
        else:
            rect(img, 3, 6, 4, 6, b); rect(img, 9, 6, 4, 6, b); rect(img, 2, 11, 5, 3, t); rect(img, 9, 11, 5, 3, t)
    return f


ARMOR_NAMES = {'helmet': 'helmet', 'chestplate': 'chestplate', 'leggings': 'leggings', 'boots': 'boots'}
for setname, (base, trim) in ARMOR.items():
    for kind in ARMOR_NAMES:
        icon(setname + '_' + kind, armor_icon(kind, base, trim))
        js(os.path.join(A, 'models', 'item', setname + '_' + kind + '.json'), {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'konorpg:item/' + setname + '_' + kind}})

NEW_ICONS = {
    'golem_core': blob('#7a8a7a', '#4a6a4a'), 'demon_horn': leg('#a02020'), 'wolf_fang': leg('#d8d8d8'), 'mimic_tooth': leg('#f4f4f4'),
    'spider_gland': blob('#4aa04a', '#2a6a2a'), 'demon_sigil': blob('#b030d0', '#601070'),
    'toad_queen_ring': ring('#7fd030', '#e8c828'), 'demon_blade': sword('#6a1a3a', '#222222', '#ff4400', True),
}
for n, f in NEW_ICONS.items():
    icon(n, f)
    js(os.path.join(A, 'models', 'item', n + '.json'), {'parent': 'minecraft:item/handheld' if n == 'demon_blade' else 'minecraft:item/generated', 'textures': {'layer0': 'konorpg:item/' + n}})
NEW_EGGS = ['golem', 'demon', 'giant_spider', 'dire_wolf', 'flying_cabbage', 'mimic', 'toad_queen', 'demon_general']
for e in NEW_EGGS:
    js(os.path.join(A, 'models', 'item', e + '_spawn_egg.json'), {'parent': 'minecraft:item/template_spawn_egg'})

# ---- lang (names) for new content
NEW_NAMES_EN_PT = {
    'item': {
        'golem_core': ('Golem Core', 'Núcleo de Golem'), 'demon_horn': ('Demon Horn', 'Chifre de Demônio'), 'wolf_fang': ('Dire Wolf Fang', 'Presa de Lobo Terrível'),
        'mimic_tooth': ('Mimic Tooth', 'Dente de Mímico'), 'spider_gland': ('Spider Gland', 'Glândula de Aranha'), 'demon_sigil': ('Demon Sigil', 'Sigilo Demoníaco'),
        'toad_queen_ring': ("Toad Queen's Ring", 'Anel da Rainha dos Sapos'), 'demon_blade': ('Demon Blade', 'Lâmina Demoníaca'),
        'adventurer_helmet': ("Adventurer's Cap", 'Boné de Aventureiro'), 'adventurer_chestplate': ("Adventurer's Tunic", 'Túnica de Aventureiro'),
        'adventurer_leggings': ("Adventurer's Trousers", 'Calças de Aventureiro'), 'adventurer_boots': ("Adventurer's Boots", 'Botas de Aventureiro'),
        'mage_helmet': ("Mage's Hood", 'Capuz de Mago'), 'mage_chestplate': ("Mage's Robe", 'Manto de Mago'), 'mage_leggings': ("Mage's Leggings", 'Calças de Mago'), 'mage_boots': ("Mage's Boots", 'Botas de Mago'),
        'knight_helmet': ("Knight's Helmet", 'Elmo de Cavaleiro'), 'knight_chestplate': ("Knight's Cuirass", 'Peitoral de Cavaleiro'), 'knight_leggings': ("Knight's Greaves", 'Grevas de Cavaleiro'), 'knight_boots': ("Knight's Boots", 'Botas de Cavaleiro'),
        'crusader_helmet': ("Crusader's Helm", 'Elmo de Cruzado'), 'crusader_chestplate': ("Crusader's Plate", 'Armadura de Cruzado'), 'crusader_leggings': ("Crusader's Greaves", 'Grevas de Cruzado'), 'crusader_boots': ("Crusader's Boots", 'Botas de Cruzado'),
    },
    'entity': {
        'golem': ('Golem', 'Golem'), 'demon': ('Demon', 'Demônio'), 'giant_spider': ('Giant Spider', 'Aranha Gigante'), 'dire_wolf': ('Dire Wolf', 'Lobo Terrível'),
        'flying_cabbage': ('Flying Cabbage', 'Repolho Voador'), 'mimic': ('Mimic', 'Mímico'), 'toad_queen': ('Giant Toad Queen', 'Rainha dos Sapos Gigantes'), 'demon_general': ('Demon General', 'General Demoníaco'),
    },
}
NPC_NAMES = {
    'luna': ('Luna', 'Luna'), 'bork': ('Bork (Weapons)', 'Bork (Armas)'), 'greta': ('Greta (Armor)', 'Greta (Armaduras)'), 'wiz': ('Wiz (Magic)', 'Wiz (Magia)'),
    'tomas': ('Tomás (Items)', 'Tomás (Itens)'), 'grom': ('Grom (Blacksmith)', 'Grom (Ferreiro)'), 'lyra': ('Lyra (Alchemist)', 'Lyra (Alquimista)'),
    'marta': ('Marta (Inn)', 'Marta (Pousada)'), 'joe': ('Joe (Tavern)', 'Joe (Taverna)'),
    'guard_1': ('Guard', 'Guarda'), 'guard_2': ('Guard', 'Guarda'), 'guard_3': ('Square Guard', 'Guarda da Praça'), 'guard_4': ('Square Guard', 'Guarda da Praça'), 'guard_5': ('East Gate Guard', 'Guarda do Portão Leste'),
    'wandering_merchant': ('Wandering Merchant', 'Mercador Ambulante'), 'wounded_adventurer': ('Wounded Adventurer', 'Aventureiro Ferido'),
}
for idx, lang in enumerate(('en_us', 'pt_br')):
    pth = os.path.join(A, 'lang', lang + '.json')
    dd = json.load(open(pth, encoding='utf-8'))
    for k, v in NEW_NAMES_EN_PT['item'].items():
        dd['item.konorpg.' + k] = v[idx]
    for k, v in NEW_NAMES_EN_PT['entity'].items():
        dd['entity.konorpg.' + k] = v[idx]
    for e in NEW_EGGS:
        nm = NEW_NAMES_EN_PT['entity'][e][idx]
        dd['item.konorpg.' + e + '_spawn_egg'] = nm + (' Spawn Egg' if idx == 0 else ' (Ovo de Invocação)')
    for k, v in NPC_NAMES.items():
        dd['npc.konorpg.' + k] = v[idx]
    js(pth, dd)

# ---- loot tables
NEW_LOOT = {
    'golem': [('konorpg:golem_core', 0.45, 1, 1), ('minecraft:iron_ingot', 0.6, 1, 3), ('konorpg:magic_crystal', 0.1, 1, 1)],
    'demon': [('konorpg:demon_horn', 0.6, 1, 2), ('konorpg:magic_crystal', 0.15, 1, 1), ('konorpg:greater_health_potion', 0.1, 1, 1)],
    'giant_spider': [('konorpg:spider_gland', 0.7, 1, 2), ('minecraft:string', 0.9, 1, 3)],
    'dire_wolf': [('konorpg:wolf_fang', 0.7, 1, 2), ('minecraft:leather', 0.5, 1, 2)],
    'flying_cabbage': [('konorpg:cabbage', 1.0, 1, 2)],
    'mimic': [('konorpg:mimic_tooth', 0.8, 1, 2), ('minecraft:gold_nugget', 0.9, 3, 9), ('konorpg:lucky_ring', 0.08, 1, 1), ('konorpg:mana_potion', 0.4, 1, 2)],
    'toad_queen': [('konorpg:toad_queen_ring', 1.0, 1, 1), ('konorpg:magic_crystal', 1.0, 2, 4), ('konorpg:greater_health_potion', 1.0, 2, 3), ('konorpg:toad_leg', 1.0, 4, 8)],
    'demon_general': [('konorpg:demon_sigil', 1.0, 1, 1), ('konorpg:demon_blade', 0.35, 1, 1), ('konorpg:magic_crystal', 1.0, 3, 6), ('konorpg:demon_horn', 1.0, 2, 4)],
}
for e, drops in NEW_LOOT.items():
    js(os.path.join(D, 'loot_table', 'entities', e + '.json'), {'type': 'minecraft:entity', 'pools': [drop_pool(*x) for x in drops]})
js(os.path.join(D, 'neoforge', 'biome_modifier', 'spawns.json'), {'type': 'neoforge:add_spawns', 'biomes': '#minecraft:is_overworld', 'spawners': [
    {'type': 'konorpg:goblin', 'weight': 5, 'minCount': 2, 'maxCount': 3}, {'type': 'konorpg:kobold', 'weight': 4, 'minCount': 2, 'maxCount': 3},
    {'type': 'konorpg:giant_toad', 'weight': 6, 'minCount': 1, 'maxCount': 2}, {'type': 'konorpg:orc', 'weight': 1, 'minCount': 1, 'maxCount': 2},
    {'type': 'konorpg:lizardman', 'weight': 2, 'minCount': 1, 'maxCount': 2}, {'type': 'konorpg:skeleton_warrior', 'weight': 2, 'minCount': 1, 'maxCount': 2},
    {'type': 'konorpg:giant_spider', 'weight': 3, 'minCount': 1, 'maxCount': 1}, {'type': 'konorpg:dire_wolf', 'weight': 2, 'minCount': 2, 'maxCount': 3},
    {'type': 'konorpg:flying_cabbage', 'weight': 6, 'minCount': 2, 'maxCount': 4}, {'type': 'konorpg:golem', 'weight': 1, 'minCount': 1, 'maxCount': 1},
    {'type': 'konorpg:demon', 'weight': 1, 'minCount': 1, 'maxCount': 1}]})
print('assets generated')
