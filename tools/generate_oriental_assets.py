"""Original pixel art and cuboid models for the town; no third-party assets.

Run from any directory with Python + Pillow. Geometry is shared with the Java
collision definitions, so the visible legs, rails and open lattice match hitboxes.
"""
from pathlib import Path
import json
import random
from PIL import Image, ImageDraw
from oriental_expansion import DEFINITIONS as EXPANSION, generate_textures
from jindan_assets import DEFINITIONS as JINDAN_DEFINITIONS, generate_textures as generate_jindan_textures

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/xiuxian'
DATA = ROOT / 'src/main/resources/data'
PREVIEW = ROOT / 'build/oriental-preview'

def save_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

def clear_jindan_generated_files():
    """Remove stale Jin-Dan outputs before writing the current definition set."""
    for directory, pattern in (
        (ASSETS / 'blockstates', 'jindan_*.json'),
        (ASSETS / 'models/block', 'jindan_*.json'),
        (ASSETS / 'models/item', 'jindan_*.json'),
        (DATA / 'xiuxian/loot_tables/blocks', 'jindan_*.json'),
        (ASSETS / 'textures/block', 'oriental_jindan_*.png'),
    ):
        for path in directory.glob(pattern):
            path.unlink()

clear_jindan_generated_files()

palette = {
    'jade': (211, 226, 214), 'plaster': (226, 221, 199),
    'black_brick': (49, 61, 67), 'paving': (62, 76, 83),
    'vermilion': (145, 39, 36), 'rosewood': (65, 33, 34),
    'roof_red': (130, 45, 38), 'roof_blue': (40, 91, 105),
    'gold': (196, 152, 66), 'porcelain': (226, 231, 221),
    'lantern': (179, 42, 34), 'glow': (255, 218, 130),
    'scrolls': (202, 186, 143), 'carved_jade': (178, 202, 186),
    'cloud': (187, 152, 78), 'leaves': (64, 108, 69),
}
for index, (name, color) in enumerate(palette.items()):
    rng = random.Random(1701 + index)
    im = Image.new('RGB', (32, 32))
    for y in range(32):
        for x in range(32):
            n = rng.randrange(-7, 8)
            im.putpixel((x, y), tuple(max(0, min(255, c + n)) for c in color))
    d = ImageDraw.Draw(im)
    if name in ('black_brick', 'paving', 'plaster'):
        dark = tuple(max(0, c - 22) for c in color)
        for y in range(0, 32, 8 if name != 'paving' else 16):
            d.line((0, y, 31, y), fill=dark)
            for x in range((y // 8 % 2) * 8, 32, 16):
                d.line((x, y, x, y + 7), fill=dark)
    elif name.startswith('roof'):
        for x in range(0, 32, 8):
            d.rectangle((x, 0, x+1, 31), fill=tuple(max(0, c-25) for c in color))
            d.line((x+3, 0, x+3, 31), fill=tuple(min(255, c+30) for c in color))
        for y in (0, 15, 31):
            d.line((0, y, 31, y), fill=tuple(max(0, c-18) for c in color))
    elif name in ('rosewood', 'vermilion'):
        for x in range(2, 32, 5):
            d.line((x, 0, x+1, 15, x, 31), fill=tuple(max(0,c-13) for c in color))
        if name == 'rosewood':
            d.arc((7, 6, 15, 25), 65, 290, fill=(92, 48, 40))
    elif name in ('jade', 'carved_jade'):
        for y in (5, 20):
            d.line((0,y,8,y+2,18,y-1,31,y+1), fill=(185,207,192))
        if name == 'carved_jade':
            d.rectangle((2,2,29,29), outline=(132,166,146))
            d.arc((7,7,23,23), 10, 300, fill=(227,238,214), width=2)
            d.line((12,15,20,15,20,20,16,20), fill=(132,166,146), width=2)
    elif name == 'porcelain':
        for y in (3, 26):
            d.line((0,y,31,y), fill=(47,87,135), width=2)
        for x in (1, 17):
            d.arc((x,8,x+13,23), 0, 300, fill=(45,86,133), width=2)
            d.line((x+5,19,x+10,13), fill=(53,105,145))
    elif name == 'lantern':
        d.rectangle((0,0,31,3), fill=(215,164,65))
        d.rectangle((0,28,31,31), fill=(215,164,65))
        for x in (3, 11, 19, 27):
            d.line((x,4,x,27), fill=(231,104,60))
        d.line((7,14,13,9,20,9,24,13,19,16,11,16,7,20,14,23,23,21), fill=(249,199,98), width=2)
    elif name == 'scrolls':
        d.rectangle((0,0,31,31), fill=(47,28,31))
        for x in range(2,32,5):
            d.rectangle((x,3,x+3,27), fill=(219,203,161))
            d.line((x,13,x+3,13), fill=(140,56,43), width=2)
            d.line((x+1,6,x+1,10), fill=(128,107,70))
    elif name == 'cloud':
        d.rectangle((0,0,31,31), fill=(109,33,33))
        d.rectangle((1,1,30,30), outline=(211,170,79), width=2)
        for x in (3,17):
            d.arc((x,7,x+12,20), 35, 320, fill=(236,191,91), width=2)
        d.line((6,22,25,22), fill=(236,191,91), width=2)
    path = ASSETS / 'textures/block' / f'oriental_{name}.png'
    path.parent.mkdir(parents=True, exist_ok=True)
    im.save(path)

def b(coords, texture):
    return (list(coords), texture)

def legs(height=10, texture='rosewood'):
    return [b((x,0,z,x+2,height,z+2), texture) for x in (2,12) for z in (2,12)]

roof = lambda texture: [b((0,0,0,16,5,16),texture)] + [
    b((x,5,0,x+2,8,16),texture) for x in (1,5,9,13)]

# All parts lie in one block, including the sculpted details, to avoid invisible
# overlap with adjacent blocks and oversized selection boxes.
definitions = {
    'town_black_bricks': ('青黛石砖', 'Black Bluestone Bricks', 'stone', 0, 'black_brick'),
    'town_paving': ('青石铺地', 'Bluestone Paving', 'stone', 0, 'paving'),
    'town_white_jade': ('云纹白玉', 'Cloud Veined White Jade', 'stone', 0, 'jade'),
    'town_white_plaster': ('素白粉墙', 'White Lime Plaster', 'stone', 0, 'plaster'),
    'town_rosewood': ('紫檀木板', 'Rosewood Planks', 'wood', 0, 'rosewood'),
    'town_vermilion_pillar': ('朱漆柱材', 'Vermilion Lacquer Timber', 'wood', 0, 'vermilion'),
    'town_red_roof_tiles': ('朱红琉璃瓦', 'Red Glazed Roof Tiles', 'stone', 0, roof('roof_red')),
    'town_blue_roof_tiles': ('黛青琉璃瓦', 'Blue Glazed Roof Tiles', 'stone', 0, roof('roof_blue')),
    'town_roof_ridge': ('鎏金云纹屋脊', 'Gilded Cloud Roof Ridge', 'stone', 0, [
        b((0,0,4,16,4,12),'roof_blue'), b((0,4,6,16,9,10),'gold'), b((0,9,7,16,12,9),'gold')]),
    'town_eave_bracket': ('彩绘斗拱', 'Painted Dougong Bracket', 'wood', 0, [
        b((5,0,5,11,5,11),'vermilion'), b((2,5,4,14,8,12),'roof_blue'),
        b((0,8,6,16,11,10),'gold'), b((3,11,0,6,14,16),'vermilion'),
        b((10,11,0,13,14,16),'vermilion'), b((0,14,0,16,16,16),'roof_blue')]),
    'town_lattice_window': ('回纹花窗', 'Rosewood Lattice Window', 'wood', 0, [
        b((0,0,6,2,16,10),'rosewood'), b((14,0,6,16,16,10),'rosewood'),
        b((2,0,6,14,2,10),'gold'), b((2,14,6,14,16,10),'gold'),
    ] + [b((x,2,7,x+1,14,9),'rosewood') for x in (5,10)]
      + [b((2,y,7,14,y+1,9),'rosewood') for y in (5,10)]),
    'town_jade_railing': ('雕花白玉栏', 'Carved Jade Balustrade', 'stone', 0, [
        b((0,0,6,16,2,10),'jade'), b((0,12,5,16,15,11),'carved_jade'),
    ] + [b((x,2,7,x+2,12,9),'jade') for x in (1,5,9,13)]),
    'town_red_lantern': ('流苏红灯笼', 'Red Lantern with Tassel', 'wood', 15, [
        b((7,14,7,9,16,9),'gold'), b((4,12,4,12,14,12),'gold'),
        b((3,5,3,13,12,13),'lantern'), b((4,3,4,12,5,12),'gold'),
        b((7,0,7,9,3,9),'vermilion')]),
    'town_stone_lamp': ('云纹石灯', 'Cloud Carved Stone Lamp', 'stone', 15, [
        b((2,0,2,14,2,14),'carved_jade'), b((5,2,5,11,5,11),'jade'),
        b((3,5,3,13,7,13),'carved_jade'), b((5,7,5,11,12,11),'glow'),
        b((2,12,2,14,14,14),'roof_blue'), b((4,14,4,12,16,12),'roof_blue')]
        + [b((x,7,z,x+1,12,z+1),'jade') for x in (3,12) for z in (3,12)]),
    'town_low_desk': ('紫檀书案', 'Rosewood Writing Desk', 'wood', 0,
        legs(8) + [b((0,8,1,16,10,15),'rosewood'), b((1,6,1,15,8,3),'cloud'),
                   b((3,10,5,10,10.5,12),'scrolls'), b((12,10,6,14,12,8),'porcelain')]),
    'town_bench': ('紫檀长凳', 'Rosewood Bench', 'wood', 0, [
        b((1,0,5,4,6,11),'rosewood'), b((12,0,5,15,6,11),'rosewood'),
        b((0,6,3,16,8,13),'rosewood'), b((2,3,7,14,5,9),'gold')]),
    'town_cabinet': ('云纹货柜', 'Cloud Carved Shop Cabinet', 'wood', 0, [
        b((1,1,2,15,13,14),'rosewood'), b((0,13,1,16,15,15),'rosewood'),
        b((2,3,1,7,12,2),'cloud'), b((9,3,1,14,12,2),'cloud'),
        b((6,7,0,7,9,1),'gold'), b((9,7,0,10,9,1),'gold')]),
    'town_scroll_shelf': ('经卷书格', 'Scripture Scroll Shelf', 'wood', 0, [
        b((0,0,2,2,16,15),'rosewood'), b((14,0,2,16,16,15),'rosewood'),
        b((2,0,13,14,16,15),'rosewood')]
        + [b((2,y,2,14,y+1,13),'rosewood') for y in (0,7,15)]
        + [b((2,y,4,14,y+5,12),'scrolls') for y in (1,9)]),
    'town_folding_screen': ('云霞屏风', 'Cloud and Sunset Folding Screen', 'wood', 0, [
        b((1,0,4,3,16,12),'rosewood'), b((13,0,4,15,16,12),'rosewood'),
        b((3,3,7,13,14,9),'cloud'), b((3,14,6,13,16,10),'gold')]),
    'town_throne': ('紫檀礼座', 'Ceremonial Rosewood Chair', 'wood', 0,
        legs(6) + [b((1,6,2,15,8,14),'vermilion'), b((2,8,12,14,15,15),'cloud'),
                   b((0,10,2,2,12,14),'gold'), b((14,10,2,16,12,14),'gold')]),
    'town_porcelain_planter': ('青花灵植盆', 'Blue and White Spirit Planter', 'stone', 0, [
        b((5,0,5,11,2,11),'porcelain'), b((3,2,3,13,7,13),'porcelain'),
        b((2,7,2,14,9,14),'porcelain'), b((4,9,4,12,10,12),'rosewood'),
        b((7,10,7,9,14,9),'leaves'), b((4,12,6,12,14,10),'leaves'),
        b((6,14,5,10,16,11),'leaves')]),
    'town_cloud_plaque': ('鎏金云霞匾', 'Gilded Cloud Plaque', 'wood', 0, [
        b((0,2,6,16,14,9),'rosewood'), b((1,3,5,15,13,6),'cloud')]),
    'town_ridge_finial': ('卷云鸱吻', 'Cloud Curl Roof Finial', 'stone', 0, [
        b((3,0,3,13,3,13),'roof_red'), b((5,3,5,11,7,11),'gold'),
        b((6,7,4,10,11,10),'gold'), b((5,11,2,11,14,7),'gold'),
        b((4,14,1,12,16,4),'roof_red')]),
    'town_stone_lion': ('镇门玉狮', 'Jade Guardian Lion', 'stone', 0, [
        b((2,0,2,14,2,14),'carved_jade'), b((4,2,6,12,9,13),'jade'),
        b((3,2,2,6,7,6),'jade'), b((10,2,2,13,7,6),'jade'),
        b((3,8,3,13,14,12),'carved_jade'), b((5,9,1,11,12,4),'jade'),
        b((3,14,4,6,16,7),'jade'), b((10,14,4,13,16,7),'jade'),
        b((5,12,2,6,13,3),'black_brick'), b((10,12,2,11,13,3),'black_brick')]),
}

definitions.update(EXPANSION)
definitions.update(JINDAN_DEFINITIONS)
generate_textures(ASSETS)
generate_jindan_textures(ASSETS)

java = ['package xiuxian.block;', '',
    'import java.util.LinkedHashMap;', 'import java.util.Map;',
    'import net.minecraft.world.item.BlockItem;', 'import net.minecraft.world.item.CreativeModeTabs;',
    'import net.minecraft.world.item.Item;', 'import net.minecraft.world.level.block.Block;',
    'import net.minecraft.world.level.block.SoundType;',
    'import net.minecraft.world.level.block.state.BlockState;',
    'import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;',
    'import net.minecraftforge.eventbus.api.IEventBus;',
    'import net.minecraftforge.registries.DeferredRegister;',
    'import net.minecraftforge.registries.ForgeRegistries;',
    'import net.minecraftforge.registries.RegistryObject;', '',
    '/** Original oriental materials. Generated by tools/generate_oriental_assets.py. */',
    'public final class OrientalBlocks {',
    '    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "xiuxian");',
    '    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "xiuxian");',
    '    private static final Map<String, RegistryObject<Block>> MATERIALS = new LinkedHashMap<>();',
    '    static {']
tags = {'stone': [], 'wood': []}
for name, (zh, en, material, light, geometry) in definitions.items():
    textures = sorted({t for _, t in geometry} if isinstance(geometry, list) else {geometry})
    model = {'textures': {t: f'xiuxian:block/oriental_{t}' for t in textures},
             'ambientocclusion': True, 'gui_light': 'side',
             'parent': 'minecraft:block/block'}
    if isinstance(geometry, str):
        model = {'parent':'minecraft:block/cube_all', 'textures':{'all':f'xiuxian:block/oriental_{geometry}'}}
        variant = {'':{'model':f'xiuxian:block/{name}'}}
        factory = 'new Block(properties)'
    else:
        model['textures']['particle'] = f'xiuxian:block/oriental_{geometry[0][1]}'
        model['elements'] = []
        model['render_type'] = 'minecraft:cutout'
        for coords, tex in geometry:
            model['elements'].append({'from':coords[:3], 'to':coords[3:],
                'faces':{face:{'texture':f'#{tex}'} for face in ('north','south','east','west','up','down')}})
        variant = {f'facing={d}':dict(model=f'xiuxian:block/{name}', y=a)
                   for d,a in [('north',0),('east',90),('south',180),('west',270)]}
        boxes = ', '.join('{' + ', '.join(str(v) for v in coords) + '}' for coords,_ in geometry)
        factory = f'new OrientalDecorationBlock(properties, new double[][]{{{boxes}}})'
    save_json(ASSETS/'models/block'/f'{name}.json', model)
    save_json(ASSETS/'blockstates'/f'{name}.json', {'variants':variant})
    save_json(ASSETS/'models/item'/f'{name}.json', {'parent':f'xiuxian:block/{name}'})
    save_json(DATA/'xiuxian/loot_tables/blocks'/f'{name}.json', {
        'type':'minecraft:block', 'pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'xiuxian:{name}'}],
          'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
    sound = 'WOOD' if material == 'wood' else 'STONE'
    java.extend([f'        add("{name}", Block.Properties.of().strength(2.0F).sound(SoundType.{sound})'
                 f'.lightLevel(state -> {light}), properties -> {factory});'])
    tags[material].append(f'xiuxian:{name}')

java.extend(['    }', '', '    private OrientalBlocks() {}',
    '    private static void add(String id, Block.Properties properties, java.util.function.Function<Block.Properties, Block> factory) {',
    '        RegistryObject<Block> block = BLOCKS.register(id, () -> factory.apply(properties));',
    '        MATERIALS.put(id, block);',
    '        ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));',
    '    }',
    '    public static BlockState state(String id) { return MATERIALS.get(id).get().defaultBlockState(); }',
    '    public static void register(IEventBus bus) {',
    '        BLOCKS.register(bus); ITEMS.register(bus);',
    '        bus.addListener(OrientalBlocks::creativeContents);',
    '    }',
    '    private static void creativeContents(BuildCreativeModeTabContentsEvent event) {',
    '        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)',
    '            MATERIALS.values().forEach(block -> event.accept(block.get().asItem()));',
    '    }', '}'])
(ROOT/'src/main/java/xiuxian/block/OrientalBlocks.java').write_text('\n'.join(java)+'\n', encoding='utf-8')
for tool, material in [('pickaxe','stone'),('axe','wood')]:
    path = DATA/'minecraft/tags/blocks/mineable'/f'{tool}.json'
    old = json.loads(path.read_text(encoding='utf-8')) if path.exists() else {'replace':False,'values':[]}
    old['values'] = [value for value in old['values'] if not value.startswith('xiuxian:jindan_')]
    old['values'] = list(dict.fromkeys(old['values'] + tags[material]))
    save_json(path, old)
for lang, field in [('zh_cn',0),('en_us',1)]:
    path = ASSETS/'lang'/f'{lang}.json'
    content = json.loads(path.read_text(encoding='utf-8-sig'))
    content = {key: value for key, value in content.items()
               if not key.startswith('block.xiuxian.jindan_')}
    content.update({f'block.xiuxian.{name}':values[field] for name,values in definitions.items()})
    save_json(path, content)

# Contact sheet of original textures for quick art inspection.
PREVIEW.mkdir(parents=True, exist_ok=True)
sheet = Image.new('RGB', (512, ((len(palette)+3)//4)*154), (24,29,35))
d = ImageDraw.Draw(sheet)
for i,name in enumerate(palette):
    x,y = (i%4)*128, (i//4)*154
    texture = Image.open(ASSETS/'textures/block'/f'oriental_{name}.png')
    sheet.paste(texture.resize((112,112),Image.Resampling.NEAREST), (x+8,y+8))
    d.text((x+8,y+124),name,fill=(230,222,204))
sheet.save(PREVIEW/'textures.png')
print(f'Generated {len(definitions)} original blocks, models, items, collision definitions and loot tables.')
