"""Check packaged asset chains rather than only counting block registrations."""
import json
from pathlib import Path
from zipfile import ZipFile

root = Path(__file__).resolve().parents[1]
jar_path = root / 'build/libs/xiuxian-1.0.0-SNAPSHOT.jar'
prefix = 'assets/xiuxian/'
with ZipFile(jar_path) as jar:
    names = set(jar.namelist())
    ids = sorted(Path(p).stem for p in names if p.startswith(prefix + 'blockstates/')
                 and Path(p).stem.startswith(('town_', 'xian_', 'daotai_', 'jindan_')))
    assert sum(p.startswith('xian_') for p in ids) == 56
    assert sum(p.startswith('daotai_') for p in ids) == 132
    assert sum(p.startswith('jindan_') for p in ids) == 136
    for block_id in ids:
        model = json.loads(jar.read(prefix + f'models/block/{block_id}.json'))
        state = json.loads(jar.read(prefix + f'blockstates/{block_id}.json'))
        item = json.loads(jar.read(prefix + f'models/item/{block_id}.json'))
        assert item['parent'] == f'xiuxian:block/{block_id}'
        assert all(v['model'] == f'xiuxian:block/{block_id}' for v in state['variants'].values())
        assert f'data/xiuxian/loot_tables/blocks/{block_id}.json' in names
        for texture in model['textures'].values():
            assert texture.startswith('xiuxian:block/')
            assert prefix + 'textures/' + texture.split(':', 1)[1] + '.png' in names
        if 'elements' in model:
            assert 'particle' in model['textures']
            assert set(state['variants']) == {'facing=north', 'facing=east', 'facing=south', 'facing=west'}
            for element in model['elements']:
                assert all(0 <= a < b <= 16 for a, b in zip(element['from'], element['to']))
                assert all(face['texture'][1:] in model['textures'] for face in element['faces'].values())
    for lang in ('zh_cn', 'en_us'):
        labels = json.loads(jar.read(prefix + f'lang/{lang}.json'))
        assert all(f'block.xiuxian.{block_id}' in labels for block_id in ids)
    assert prefix + 'models/item/daotai_residence_inspection_token.json' in names
    assert prefix + 'textures/item/daotai_residence_inspection_token.png' in names
    print(f'PASS: {len(ids)} packaged blocks, including 132 Dao-Tai and '
          f'{sum(p.startswith("jindan_") for p in ids)} Jin-Dan blocks; '
          'blockstate/model/item/texture/particle/loot/language chains valid.')
