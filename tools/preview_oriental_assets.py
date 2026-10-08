"""Offline orthographic preview of the actual block JSON and pixel textures.

This is an asset contact sheet, not a Minecraft screenshot. Requires Pillow and
NumPy; run after generate_oriental_assets.py. No third-party art is used.
"""
from pathlib import Path
import json
import numpy as np
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/xiuxian'
OUTPUT = ROOT / 'build/oriental-preview/models.png'
N = 190
direction = np.array([-1.25, -1.05, 1.6])
direction /= np.linalg.norm(direction)
right = np.cross(direction, [0, 1, 0])
right /= np.linalg.norm(right)
up = np.cross(right, direction)
sx, sy = np.meshgrid(np.linspace(-15, 15, N), np.linspace(15, -15, N))
rays = np.array([8., 8., 8.]) - direction * 48 + sx[..., None] * right + sy[..., None] * up
textures = {}
for path in (ASSETS / 'textures/block').glob('oriental_*.png'):
    textures['xiuxian:block/' + path.stem] = np.asarray(Image.open(path).convert('RGB'))


def render(model):
    elements = model.get('elements', [{'from': [0, 0, 0], 'to': [16, 16, 16],
        'faces': {face: {'texture': '#all'} for face in ('north', 'south', 'east', 'west', 'up', 'down')}}])
    rgb = np.full((N, N, 3), (32, 39, 48), dtype=np.uint8)
    depth = np.full((N, N), np.inf)
    for part in elements:
        low, high = np.asarray(part['from']), np.asarray(part['to'])
        ta, tb = (low - rays) / direction, (high - rays) / direction
        enter = np.minimum(ta, tb)
        t = enter.max(axis=2)
        hits = (t <= np.maximum(ta, tb).min(axis=2)) & (t >= 0) & (t < depth)
        if not hits.any():
            continue
        axes = enter.argmax(axis=2)
        xyz = rays + t[..., None] * direction
        for axis in range(3):
            face = [('west', 'east'), ('down', 'up'), ('north', 'south')][axis][int(direction[axis] < 0)]
            mask = hits & (axes == axis)
            if face not in part['faces'] or not mask.any():
                continue
            reference = part['faces'][face]['texture'].lstrip('#')
            texture = textures[model['textures'][reference]]
            x, y, z = xyz[..., 0], xyz[..., 1], xyz[..., 2]
            uv = {'north': (16-x, 16-y), 'south': (x, 16-y),
                  'west': (z, 16-y), 'east': (16-z, 16-y),
                  'up': (x, z), 'down': (x, 16-z)}[face]
            u = np.clip((uv[0] * 2).astype(int), 0, 31)
            v = np.clip((uv[1] * 2).astype(int), 0, 31)
            shade = {'up': 1.1, 'north': .88, 'east': .70}.get(face, .8)
            color = np.clip(texture[v, u] * shade, 0, 255).astype(np.uint8)
            rgb[mask] = color[mask]
            depth[mask] = t[mask]
    return Image.fromarray(rgb)


labels = json.loads((ASSETS / 'lang/zh_cn.json').read_text(encoding='utf-8-sig'))
from oriental_expansion import DEFINITIONS
from jindan_assets import DEFINITIONS as JINDAN_DEFINITIONS
font_path = Path('C:/Windows/Fonts/msyh.ttc')
font = ImageFont.truetype(str(font_path), 15) if font_path.exists() else ImageFont.load_default()
title = ImageFont.truetype(str(font_path), 26) if font_path.exists() else font


def sheet_for(ids, name, filename):
    columns = 6
    sheet = Image.new('RGB', (columns * 208, 96 + ((len(ids)+columns-1)//columns) * 236), (24,30,38))
    draw = ImageDraw.Draw(sheet)
    draw.text((22,12), name + f' · {len(ids)} 种原创方块', font=title, fill=(233,213,167))
    draw.text((22,55), '实际模型和纹理的离线预览（非游戏截图；不模拟发光）', font=font, fill=(163,180,193))
    for i, block_id in enumerate(ids):
        x,y = i%columns * 208+9, 96+i//columns*236
        model = json.loads((ASSETS/'models/block'/f'{block_id}.json').read_text(encoding='utf-8'))
        sheet.paste(render(model), (x,y))
        draw.text((x,y+193), labels['block.xiuxian.'+block_id], font=font, fill=(223,212,189))
        draw.text((x,y+214), block_id.split('_',1)[1], fill=(146,162,177))
    output = OUTPUT.parent / filename
    output.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(output)
    print(output)


if __name__ == '__main__':
    sheet_for([p.stem for p in sorted((ASSETS/'models/block').glob('town_*.json'))], '既有东方建筑', 'models.png')
    ids = list(DEFINITIONS)
    jindan_ids = list(JINDAN_DEFINITIONS)
    sheet_for(jindan_ids, '\u91d1\u4e39\u5c45\u6240\u4e13\u5c5e\u8d44\u6e90', 'jindan-resources.png')
    sheet_for(ids[:18], '新增·建筑构件与纹饰', 'architecture.png')
    sheet_for(ids[18:40], '新增·灯具与修行陈设', 'cultivation.png')
    sheet_for(ids[40:], '新增·市井生活与园林', 'town-life.png')
