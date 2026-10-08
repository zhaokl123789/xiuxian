"""144 original Luoxia sect resources, with geometry shared by models and Java collision shapes."""
from collections import Counter
import hashlib
import json
from pathlib import Path
import random
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/xiuxian"
DATA = ROOT / "src/main/resources/data"
DEFINITIONS = {}
PALETTE = {
    "jade": (204, 221, 207), "stone": (68, 79, 80), "red": (150, 40, 38),
    "wood": (78, 42, 49), "roof": (36, 87, 91), "gold": (206, 160, 67),
    "bronze": (99, 126, 111), "paper": (228, 222, 202), "blue": (43, 96, 148),
    "glow": (248, 215, 135), "leaf": (54, 117, 75), "silk": (174, 75, 102),
}


def box(x1, y1, z1, x2, y2, z2, texture):
    return ([x1, y1, z1, x2, y2, z2], texture)


def legs(height=6, texture="wood"):
    return [box(x, 0, z, x+2, height, z+2, texture) for x in (2,12) for z in (2,12)]


def pedestal(texture="jade"):
    return [box(1,0,1,15,2,15,texture), box(3,2,3,13,4,13,"gold")]


def frame(texture="red"):
    return [box(0,0,6,2,16,10,texture),box(14,0,6,16,16,10,texture),
            box(2,0,6,14,2,10,"gold"),box(2,14,6,14,16,10,"gold")]


def geometry(kind, variant, texture):
    v = variant % 4
    if kind == "cube": return texture
    if kind == "pillar":
        return pedestal() + [box(4,4,4,12,14,12,texture),box(2,14,2,14,16,14,"gold")]
    if kind == "roof":
        return [box(0,0,0,16,4,16,texture)] + [box(x,4,0,x+2,7+v,16,"gold" if variant%3==0 else "roof") for x in (1,5,9,13)]
    if kind == "ridge":
        return [box(0,0,4,16,3,12,texture),box(0,3,6,16,8,10,"gold"),box(3+v,8,6,13,12+v,10,texture)]
    if kind == "bracket":
        return [box(5,0,5,11,5,11,texture),box(2,5,4,14,8,12,"gold"),
                box(0,8,6,16,11,10,texture),box(3,11,0,6,14,16,texture),
                box(10,11,0,13,14,16,texture),box(0,14,0,16,16,16,"wood")]
    if kind in ("window", "door"):
        parts = frame()
        if kind == "door": parts += [box(2,2,7,14,14,9,texture),box(10,7,5,12,9,7,"bronze")]
        else:
            parts += [box(x,2,7,x+0.7,14,9,texture) for x in range(4,13,3)]
            parts += [box(2,y,7,14,y+0.7,9,"gold") for y in range(4+v,14,4)]
        return parts
    if kind == "rail":
        return [box(0,0,6,16,2,10,texture),box(0,12,5,16,15,11,"gold")] + [box(x,2,7,x+1.5,12,9,texture) for x in range(1,15,3)]
    if kind == "stairs":
        return [box(0,0,0,16,4,16,texture),box(0,4,8,16,8,16,texture),box(0,8,12,16,12,16,"jade")]
    if kind == "plaque":
        return [box(0,2,6,16,14,9,"wood"),box(1,3,5,15,13,6,texture),box(0,13,4,16,15,10,"gold")]
    if kind == "seat":
        return legs() + [box(1,6,2,15,8,14,texture),box(2,8,13,14,14+v*0.4,15,"wood"),
                         box(1,10,3,3,12,13,"gold"),box(13,10,3,15,12,13,"gold")]
    if kind == "bench":
        return [box(1,0,4,4,6,12,"wood"),box(12,0,4,15,6,12,"wood"),box(0,6,2,16,8,14,texture)]
    if kind == "cushion":
        return [box(2,0,1,14,3,15,texture),box(1,0,3,15,3,13,texture),box(4,3,4,12,4,12,"gold")]
    if kind == "desk":
        return legs(7) + [box(0,7,1,16,9,15,"wood"),box(3,9,4,11,9.6,12,texture),box(12,9,6,14,12,8,"bronze")]
    if kind == "shelf":
        return [box(0,0,2,2,16,15,"wood"),box(14,0,2,16,16,15,"wood"),box(2,0,13,14,16,15,"wood")] + [box(2,y,2,14,y+1,13,"gold") for y in (0,7,15)] + [box(x,y,4,x+2, y+5,12,texture) for x in (3,7,11) for y in (1,9)]
    if kind == "screen":
        return [box(1,0,5,3,16,11,"wood"),box(13,0,5,15,16,11,"wood"),box(3,3,7,13,14,9,texture),box(2,14,6,14,16,10,"gold")]
    if kind == "banner":
        return pedestal() + [box(7,4,7,9,16,9,"bronze"),box(2,14,6,14,16,9,"gold"),box(3,5,6,13,14,7,texture)]
    if kind == "basin":
        return pedestal() + [box(2,4,2,4,8,14,texture),box(12,4,2,14,8,14,texture),
                            box(4,4,2,12,8,4,texture),box(4,4,12,12,8,14,texture),box(4,4,4,12,5,12,"blue")]
    if kind == "plant":
        return geometry("basin",variant,texture) + [box(7,5,7,9,13,9,"wood"),box(3,10,4,10,12,11,"leaf"),box(7,13,5,14,15,12,"leaf")]
    if kind == "statue":
        return pedestal() + [box(4,4,6,12,10,13,texture),box(3,4,2,6,8,6,texture),box(10,4,2,13,8,6,texture),
                            box(4,10,3,12,15,11,texture),box(5,10,1,11,13,4,"gold"),box(3,14,4,5,16,6,texture),box(11,14,4,13,16,6,texture)]
    if kind == "crane":
        return pedestal() + [box(5,4,8,6,9,9,"gold"),box(10,4,8,11,9,9,"gold"),
                box(4,9,6,12,12,13,texture),box(3,9,8,5,11,14,texture),box(11,9,8,13,11,14,texture),
                box(6,11,5,9,15,7,texture),box(6,14,3,10,16,6,texture),box(7,14,0,9,15,3,"gold"),
                box(6,10,13,10,11,16,"stone")]
    if kind == "dragon":
        return pedestal() + [box(3,4,7,13,8,12,texture),box(3,8,8,6,12,12,texture),
                box(4,11,4,11,14,9,texture),box(5,11,1,10,13,5,"gold"),
                box(3,13,6,5,16,8,"gold"),box(10,13,6,12,16,8,"gold"),
                box(11,6,11,14,10,14,texture),box(12,9,12,14,13,14,texture)]
    if kind == "stele":
        return pedestal() + [box(3,4,5,13,14,11,"jade"),box(5,14,6,11,16,10,"gold"),box(4,6,4,12,13,5,texture)]
    if kind == "array":
        return pedestal("stone") + [box(4,4,4,12,5,12,texture),box(6,5,6,10,9,10,"bronze"),box(7,9,7,9,13,9,"glow")]
    if kind == "incense":
        return [box(3,0,3,5,5,5,"bronze"),box(11,0,3,13,5,5,"bronze"),box(7,0,11,9,5,13,"bronze"),
                box(3,5,3,13,10,13,texture),box(2,10,2,14,12,14,"gold"),box(7,12,7,9,16,9,"bronze")]
    if kind == "lamp":
        return pedestal() + [box(6,4,6,10,9+v,10,"bronze"),box(3,9+v,3,13,14,13,texture),
                            box(1,14,1,15,16,15,"roof"),box(2,9,2,3,14,3,"gold"),box(13,9,13,14,14,14,"gold")]
    if kind == "hanging":
        return [box(7,13,7,9,16,9,"bronze"),box(3,11,3,13,13,13,"gold"),box(2,4,2,14,11,14,texture),
                box(4,2,4,12,4,12,"gold"),box(7,0,7,9,2,9,"silk")]
    if kind == "wall_lamp":
        return [box(4,1,13,12,15,16,"bronze"),box(7,5,6,9,7,14,"gold"),box(3,7,3,13,9,12,"gold"),
                box(5,9,5,11,13,10,texture),box(3,13,3,13,15,12,"roof")]
    if kind == "qin":
        return legs(6) + [box(0,6,1,16,8,15,"wood"),box(1,8,4,15,10,12,texture)] + [box(2,10,5+i*0.8,14,10.2,5.2+i*0.8,"gold") for i in range(7)]
    if kind == "flute":
        return legs(5) + [box(1,5,2,15,7,14,"wood"),box(2,7,6,14,8.5,8,texture)] + [box(x,8.5,6.5,x+0.6,8.7,7.2,"stone") for x in (4,6,8,10,12)]
    if kind == "pipa":
        return [box(2,0,3,14,2,13,"wood"),box(6,2,10,10,13,12,"wood"),
                box(4,2,6,12,7,9,texture),box(3,3,6,13,6,9,texture),
                box(6,7,6,10,10,9,texture),box(7,10,6,9,15,8,"wood"),
                box(6,14,6,10,16,8,"gold")] + [box(6+i,3,5.8,6.2+i,14,6,"gold") for i in range(4)]
    if kind == "sheng":
        return [box(1,0,2,15,2,14,"wood"),box(5,2,5,11,5,11,texture),
                box(7,3,1,9,4,6,"bronze")] + [box(x,5,z,x+1,top,z+1,"leaf")
                for x,z,top in ((5,6,11),(7,6,14),(9,6,16),(6,9,13),(8,9,15),(10,9,10))]
    if kind == "chimes":
        return [box(1,0,3,3,16,13,"wood"),box(13,0,3,15,16,13,"wood"),box(0,14,5,16,16,11,"gold")] + [box(x,5+i,7,x+2,13,9,texture) for i,x in enumerate((4,7,10))]
    if kind == "bell":
        return frame("wood") + [box(7,11,7,9,14,9,"gold"),box(5,7,5,11,11,11,texture),box(3,3,3,13,7,13,texture)]
    if kind == "drum":
        return legs(4) + [box(3,4,3,13,6,13,"gold"),box(2,6,2,14,12,14,texture),box(1,12,1,15,14,15,"paper"),box(3,14,6,13,14.7,7,"wood")]
    if kind == "gong":
        return frame("wood") + [box(7,11,7,9,14,9,"gold"),box(3,4,7,13,12,9,texture),box(6,6,6,10,10,7,"gold")]
    raise ValueError(kind)


def group(category, rows):
    for row in rows.strip().splitlines():
        name, zh, kind, texture = row.strip().split("|")
        key = "sect_" + name
        index = len(DEFINITIONS)
        geo = geometry(kind,index,texture)
        # Attach the emblem to a front surface; a detached plate would appear to float.
        if isinstance(geo,list):
            fronts = [part for part in geo if part[0][2] >= 0.2]
            if fronts:
                surface, _ = max(fronts, key=lambda part: (part[1] == texture,
                                 (part[0][3]-part[0][0])*(part[0][4]-part[0][1])))
                x1,y1,z1,x2,y2,z2 = surface
                dx,dy = (x2-x1)*0.15,(y2-y1)*0.15
                geo.append(box(x1+dx,y1+dy,z1-0.1,x2-dx,y2-dy,z1+0.05,"motif"))
            else:
                surface, _ = max((part for part in geo if part[0][4] <= 15.9),
                                 key=lambda part: part[0][4])
                x1,y1,z1,x2,y2,z2 = surface
                dx,dz = (x2-x1)*0.15,(z2-z1)*0.15
                geo.append(box(x1+dx,y2-0.05,z1+dz,x2-dx,y2+0.1,z2-dz,"motif"))
        DEFINITIONS[key] = dict(zh=zh,en=name.replace("_"," ").title(),category=category,
                                kind=kind,texture=texture,geometry=geo)


group("materials", """
sunset_marble|赤霞云纹石|cube|red
white_jade|宗门白玉|cube|jade
dark_foundation|玄青台基石|cube|stone
cloud_paving|流云铺地|cube|jade
ceremony_floor|朝仪纹砖|cube|gold
vermilion_timber|朱漆殿柱木|cube|red
rosewood_board|紫檀殿板|cube|wood
cedar_beam|灵杉梁木|cube|wood
bronze_panel|青铜护板|cube|bronze
gilded_panel|鎏金云铜板|cube|gold
blue_roof_base|青黛瓦基|cube|roof
red_roof_base|赤霞瓦基|cube|red
plaster_wall|云白粉墙|cube|paper
carved_stone|回纹雕石|cube|stone
lotus_floor|莲华殿砖|cube|jade
star_floor|观星纹砖|cube|blue
discipline_floor|执律纹砖|cube|stone
lecture_floor|讲法纹砖|cube|paper
watercourt_tile|水院青砖|cube|blue
herb_border|药园围石|cube|leaf
fireproof_brick|丹房耐火砖|cube|red
forge_ironstone|炼器玄铁石|cube|stone
scroll_wall|藏经壁砖|cube|paper
cloud_mosaic|落霞彩拼砖|cube|bronze
""")
group("architecture", """
hall_pillar|主殿朱柱|pillar|red
lecture_pillar|讲法玉柱|pillar|jade
bronze_pillar|回廊铜柱|pillar|bronze
pillar_cap|莲云柱头|ridge|gold
pillar_base|重台柱础|pillar|stone
blue_roof_tiles|青黛筒瓦|roof|roof
red_roof_tiles|赤霞琉璃瓦|roof|red
gilded_roof_tiles|鎏金脊瓦|roof|gold
main_ridge|主殿云脊|ridge|roof
side_ridge|配殿卷脊|ridge|red
dragon_finial|云龙脊饰|ridge|gold
phoenix_finial|赤凤脊饰|ridge|bronze
hall_dougong|主殿彩绘斗拱|bracket|red
corridor_dougong|廊下青铜斗拱|bracket|bronze
double_eave|重檐角梁|bracket|roof
cloud_window|流云花窗|window|jade
lotus_window|莲华花窗|window|gold
star_window|星河花窗|window|blue
bamboo_window|修竹花窗|window|leaf
hall_door|主殿雕门扇|door|red
library_door|藏经雕门扇|door|wood
watercourt_door|水院雕门扇|door|blue
jade_railing|白玉回廊栏|rail|jade
bronze_railing|青铜云桥栏|rail|bronze
terrace_step|宗门重台石阶|stairs|jade
bridge_step|渡云桥阶|stairs|stone
main_plaque|落霞宗殿匾|plaque|gold
lecture_plaque|讲法堂匾|plaque|paper
""")
group("ritual", """
grand_incense_burner|朝仪大香炉|incense|bronze
lotus_incense_burner|莲台香炉|incense|jade
dragon_incense_burner|云龙香炉|incense|gold
oath_stele|入门誓碑|stele|red
discipline_stele|宗门戒律碑|stele|stone
scripture_stele|经文石碑|stele|paper
lineage_stele|传承谱碑|stele|gold
sunset_banner|落霞宗旗|banner|red
lecture_banner|讲法幡|banner|paper
guardian_banner|护山阵旗|banner|blue
ceremony_banner|朝仪长幡|banner|gold
guardian_lion|镇门玉狮|statue|jade
guardian_crane|护庭灵鹤像|crane|paper
dragon_guardian|镇桥云龙像|dragon|bronze
formation_core|护山阵心|array|blue
cloud_array|流云阵台|array|jade
five_elements_array|五行阵台|array|leaf
star_array|观星阵盘|array|gold
offering_table|朝仪供案|desk|gold
ancestral_tablet|宗门祖师牌|stele|wood
""")
group("lights", """
red_lantern|宗门红宫灯|hanging|red
gold_lantern|朝仪金宫灯|hanging|gold
blue_lantern|观星青宫灯|hanging|blue
lotus_lantern|莲华悬灯|hanging|jade
library_lantern|藏经纸灯|hanging|paper
watercourt_lantern|水院流苏灯|hanging|bronze
hall_lamp|主殿云灯柱|lamp|gold
courtyard_lamp|庭院石灯|lamp|jade
bridge_lamp|渡云桥灯|lamp|blue
herb_lamp|药园灵灯|lamp|leaf
ceremony_lamp|朝仪铜灯|lamp|red
star_lamp|星辉阵灯|lamp|blue
disciple_lamp|弟子院灯|lamp|paper
tea_lamp|茶院暖灯|lamp|silk
hall_sconce|殿内铜壁灯|wall_lamp|gold
library_sconce|经楼壁灯|wall_lamp|paper
forge_sconce|炼器壁灯|wall_lamp|red
corridor_sconce|廊下云灯|wall_lamp|jade
lotus_sconce|莲华壁灯|wall_lamp|silk
water_sconce|临瀑水灯|wall_lamp|blue
""")
group("furniture", """
elder_throne|长老礼座|seat|gold
master_chair|掌门云座|seat|red
lecture_chair|讲法扶手椅|seat|jade
disciple_chair|弟子木椅|seat|wood
corridor_bench|回廊长凳|bench|wood
watercourt_bench|水院石凳|bench|jade
meditation_cushion|宗门蒲团|cushion|silk
lotus_cushion|莲华坐垫|cushion|jade
lecture_desk|讲法书案|desk|paper
scribe_desk|抄经书案|desk|gold
tea_table|宗门茶案|desk|blue
pill_desk|丹房配药案|desk|leaf
forge_desk|炼器图案桌|desk|stone
registration_desk|执事登记案|desk|red
scroll_shelf|藏经卷架|shelf|paper
manual_shelf|功法玉简架|shelf|jade
herb_cabinet|宗门药材柜|shelf|leaf
pill_cabinet|丹药陈列柜|shelf|gold
tool_shelf|炼器工具架|shelf|stone
robe_shelf|法衣陈列架|shelf|silk
cloud_screen|流云屏风|screen|jade
sunset_screen|赤霞屏风|screen|red
star_screen|观星屏风|screen|blue
bamboo_screen|修竹屏风|screen|leaf
brush_stand|执事笔架|desk|bronze
seal_stand|宗门印台|array|red
tea_service|青瓷茶具台|desk|blue
map_stand|洞天图架|desk|paper
""")
group("music", """
qin_table|落霞古琴案|qin|wood
jade_qin|白玉七弦琴|qin|jade
bronze_se|青铜瑟台|qin|bronze
moon_pipa|月华琵琶台|pipa|silk
bamboo_flute|修竹笛架|flute|leaf
jade_xiao|白玉箫架|flute|jade
cloud_sheng|流云笙台|sheng|gold
ritual_chimes|朝仪编钟|chimes|bronze
jade_chimes|灵玉编磬|chimes|jade
morning_bell|宗门晨钟|bell|gold
evening_drum|宗门暮鼓|drum|red
bronze_gong|护山铜锣|gong|bronze
""")
group("garden", """
herb_planter|宗门药圃盆|plant|jade
red_maple_bonsai|赤枫盆景|plant|red
spirit_pine_bonsai|灵松盆景|plant|stone
bamboo_planter|修竹花池|plant|blue
lotus_basin|莲华水池|basin|jade
spring_basin|灵泉水盆|basin|bronze
wash_basin|弟子净手盆|basin|paper
water_clock|水院漏刻|array|blue
garden_stele|药园识草碑|stele|leaf
waterside_marker|临瀑观景碑|stele|blue
training_target|演武木桩|pillar|wood
cloud_waystone|渡云路标|stele|gold
""")

VOICES = {"qin_table":0,"jade_qin":5,"bronze_se":0,"moon_pipa":0,"bamboo_flute":1,
          "jade_xiao":1,"cloud_sheng":1,"ritual_chimes":2,"jade_chimes":2,
          "morning_bell":4,"evening_drum":3,"bronze_gong":4}


def texture_image(key, tone, pattern):
    seed = int(hashlib.sha256((key+tone).encode()).hexdigest()[:16],16)
    rng = random.Random(seed)
    base = PALETTE[tone]
    image = Image.new("RGB",(32,32))
    for y in range(32):
        for x in range(32):
            n = rng.randrange(-7,8)
            image.putpixel((x,y),tuple(max(0,min(255,c+n)) for c in base))
    d = ImageDraw.Draw(image)
    gold, dark = PALETTE["gold"], tuple(max(0,c-32) for c in base)
    if tone in ("wood","red"):
        for x in range(2,32,5): d.line((x,0,x+1,14,x,31),fill=dark)
    elif tone in ("stone","roof"):
        for y in range(0,32,8):
            d.line((0,y,31,y),fill=dark)
            for x in range((y//8%2)*8,32,16): d.line((x,y,x,y+7),fill=dark)
    elif tone in ("jade","blue"):
        for y in (5,22): d.line((0,y,8,y+2,18,y-2,31,y+1),fill=dark)
    if pattern in ("cube","motif"):
        d.rectangle((1,1,30,30),outline=gold)
        offset = rng.randrange(3,7)
        d.arc((offset,6,28-offset,24),15,310,fill=gold,width=2)
        d.line((8,18,15,18,15,12,22,12),fill=gold,width=2)
        # Named emblems vary with the resource: stars, leaves, scriptures and wave bands.
        if any(word in key for word in ("star","array")):
            for x,y in ((7,7),(23,8),(16,24)): d.line((x-2,y,x+2,y),fill=gold); d.line((x,y-2,x,y+2),fill=gold)
        elif any(word in key for word in ("herb","bamboo","leaf","planter")):
            for x in (8,16,24): d.line((x,7,x,26,x+4,20,x,20,x-4,14),fill=PALETTE["leaf"],width=2)
        elif any(word in key for word in ("scroll","lecture","library","script")):
            for x in (9,16,23): d.line((x,7,x,23,x-3,19,x+2,15),fill=dark,width=2)
    else:
        if tone in ("gold","bronze","silk","paper"):
            for y in (3,28): d.line((0,y,31,y),fill=gold,width=2)
        if pattern in ("lamp","hanging","wall_lamp") and tone not in ("wood","bronze","roof","gold"):
            for x in (4,12,20,28): d.line((x,0,x,31),fill=PALETTE["glow"],width=2)
    return image


def validate():
    assert len(DEFINITIONS)==144,len(DEFINITIONS)
    for key,entry in DEFINITIONS.items():
        assert "\ufffd" not in entry["zh"]
        geo = entry["geometry"]
        if isinstance(geo,list):
            for coords,tone in geo:
                assert all(0<=a<b<=16 for a,b in zip(coords[:3],coords[3:])),(key,coords)
                assert tone in PALETTE or tone=="motif"
    assert len({v["zh"] for v in DEFINITIONS.values()})==144
    return dict(Counter(v["category"] for v in DEFINITIONS.values()))


def save_json(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")


def generate():
    print("Definitions:",validate())
    java = ['package xiuxian.block;', '',
        'import java.util.LinkedHashMap;', 'import java.util.Map;',
        'import net.minecraft.world.item.BlockItem;', 'import net.minecraft.world.item.Item;',
        'import net.minecraft.world.item.CreativeModeTabs;',
        'import net.minecraft.world.level.block.Block;', 'import net.minecraft.world.level.block.SoundType;',
        'import net.minecraft.world.level.block.state.BlockState;',
        'import net.minecraft.world.level.block.entity.BlockEntityType;',
        'import net.minecraft.world.entity.EntityType;', 'import net.minecraft.world.entity.MobCategory;',
        'import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;',
        'import net.minecraftforge.eventbus.api.IEventBus;', 'import net.minecraftforge.registries.DeferredRegister;',
        'import net.minecraftforge.registries.ForgeRegistries;', 'import net.minecraftforge.registries.RegistryObject;',
        'import xiuxian.entity.SectSeatEntity;', '',
        '/** Generated by tools/sect_assets.py. Dedicated resources for the large inner-realm sect. */',
        'public final class SectBlocks {',
        '    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "xiuxian");',
        '    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "xiuxian");',
        '    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "xiuxian");',
        '    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "xiuxian");',
        '    private static final Map<String, RegistryObject<Block>> RESOURCES = new LinkedHashMap<>();',
        '    static {']
    tags = {"axe":[],"pickaxe":[]}
    textures = ASSETS/"textures/block"
    textures.mkdir(parents=True,exist_ok=True)
    for key,entry in DEFINITIONS.items():
        geo,kind,category = entry["geometry"],entry["kind"],entry["category"]
        wood = kind in ("seat","bench","cushion","desk","shelf","screen","banner","qin","flute","pipa","sheng","chimes","drum") or entry["texture"] in ("wood","red","silk","paper")
        tags["axe" if wood else "pickaxe"].append("xiuxian:"+key)
        sound = "WOOD" if wood else "STONE"
        state_name = "lit" if category=="lights" else "playing" if category=="music" else None
        tones = [entry["texture"]] if isinstance(geo,str) else sorted({tone for _,tone in geo})
        model_textures = {}
        for tone in tones:
            filename = f"oriental_{key}_{tone}"
            texture_image(key,entry["texture"] if tone=="motif" else tone,"motif" if tone=="motif" else kind).save(textures/(filename+".png"))
            model_textures[tone] = "xiuxian:block/"+filename
        if isinstance(geo,str):
            model = {"parent":"minecraft:block/cube_all","textures":{"all":model_textures[geo]}}
            variants = {"":{"model":"xiuxian:block/"+key}}
            factory = "new Block(properties)"
        else:
            model_textures["particle"] = model_textures[geo[0][1]]
            model = {"parent":"minecraft:block/block","textures":model_textures,"render_type":"minecraft:cutout",
                     "elements":[{"from":coords[:3],"to":coords[3:],"faces":{face:{"texture":"#"+tone}
                        for face in ("north","south","east","west","up","down")}} for coords,tone in geo]}
            variants = {}
            for direction,angle in (("north",0),("east",90),("south",180),("west",270)):
                for value in ("false","true") if state_name else (None,):
                    selector = "facing="+direction+(f",{state_name}={value}" if state_name else "")
                    variants[selector] = {"model":"xiuxian:block/"+key,"y":angle}
            boxes = "new double[][]{"+", ".join("{"+", ".join(map(str,coords))+"}" for coords,_ in geo)+"}"
            if category=="lights": factory=f"new SectLampBlock(properties, {boxes})"
            elif category=="music":
                voice = VOICES[key.removeprefix("sect_")]
                factory=f"new SectInstrumentBlock(properties, {boxes}, {voice}, {6 if voice==3 else 8})"
            elif kind in ("seat","bench","cushion"):
                factory=f"new SectSeatBlock(properties, {boxes}, {0.25 if kind=='cushion' else 0.5})"
            else: factory=f"new OrientalDecorationBlock(properties, {boxes})"
        save_json(ASSETS/"models/block"/(key+".json"),model)
        if category=="lights":
            unlit = json.loads(json.dumps(model))
            # The unlit version has a visibly dim body in addition to emitting no light.
            tone = entry["texture"]
            filename = f"oriental_{key}_unlit"
            im = texture_image(key,tone,kind).point(lambda p:int(p*0.4))
            im.save(textures/(filename+".png"))
            unlit["textures"][tone] = "xiuxian:block/"+filename
            save_json(ASSETS/"models/block"/(key+"_unlit.json"),unlit)
            for selector,variant in variants.items():
                if selector.endswith("lit=false"): variant["model"]+="_unlit"
        save_json(ASSETS/"blockstates"/(key+".json"),{"variants":variants})
        save_json(ASSETS/"models/item"/(key+".json"),{"parent":"xiuxian:block/"+key})
        save_json(DATA/"xiuxian/loot_tables/blocks"/(key+".json"),{"type":"minecraft:block","pools":[{"rolls":1,
            "entries":[{"type":"minecraft:item","name":"xiuxian:"+key}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]})
        java.append(f'        add("{key}", properties -> {factory}, SoundType.{sound});')
    music = [key for key,v in DEFINITIONS.items() if v["category"]=="music"]
    java += ['    }',
        '    public static final RegistryObject<BlockEntityType<SectInstrumentBlockEntity>> INSTRUMENT_ENTITY =',
        '            BLOCK_ENTITIES.register("sect_instrument", () -> BlockEntityType.Builder.of(SectInstrumentBlockEntity::new,',
        '                    '+', '.join(f'RESOURCES.get("{key}").get()' for key in music)+').build(null));',
        '    public static final RegistryObject<EntityType<SectSeatEntity>> SEAT_ENTITY =',
        '            ENTITIES.register("sect_seat", () -> EntityType.Builder.<SectSeatEntity>of(SectSeatEntity::new, MobCategory.MISC)',
        '                    .noSave().sized(0.05F, 0.05F).clientTrackingRange(8).updateInterval(20).build("xiuxian:sect_seat"));',
        '    private SectBlocks() {}',
        '    private static void add(String id, java.util.function.Function<Block.Properties, Block> factory, SoundType sound) {',
        '        var block = BLOCKS.register(id, () -> factory.apply(Block.Properties.of().strength(2.0F).sound(sound)));',
        '        RESOURCES.put(id, block);',
        '        ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));',
        '    }',
        '    public static BlockState state(String id) {',
        '        var block = RESOURCES.get(id);',
        '        if (block == null) throw new IllegalArgumentException("Unknown sect block: " + id);',
        '        return block.get().defaultBlockState();',
        '    }',
        '    public static Map<String, RegistryObject<Block>> resources() { return java.util.Collections.unmodifiableMap(RESOURCES); }',
        '    public static void register(IEventBus bus) {',
        '        BLOCKS.register(bus); ITEMS.register(bus); BLOCK_ENTITIES.register(bus); ENTITIES.register(bus);',
        '        bus.addListener(SectBlocks::creativeContents);',
        '    }',
        '    private static void creativeContents(BuildCreativeModeTabContentsEvent event) {',
        '        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)',
        '            RESOURCES.values().forEach(block -> event.accept(block.get().asItem()));',
        '    }', '}']
    (ROOT/"src/main/java/xiuxian/block/SectBlocks.java").write_text("\n".join(java)+"\n",encoding="utf-8")
    for lang in ("zh_cn","en_us"):
        path = ASSETS/"lang"/(lang+".json")
        content = json.loads(path.read_text(encoding="utf-8-sig"))
        content.update({"block.xiuxian."+key:entry["zh" if lang=="zh_cn" else "en"] for key,entry in DEFINITIONS.items()})
        content["entity.xiuxian.sect_seat"] = "宗门座位" if lang=="zh_cn" else "Sect Seat"
        save_json(path,content)
    for tool,values in tags.items():
        path = DATA/"minecraft/tags/blocks/mineable"/(tool+".json")
        content = json.loads(path.read_text(encoding="utf-8"))
        content["values"] = [v for v in content["values"] if not v.startswith("xiuxian:sect_")] + values
        save_json(path,content)
    catalog = {key:{k:v for k,v in entry.items() if k!="geometry"} for key,entry in DEFINITIONS.items()}
    save_json(ROOT/"build/sect-assets/catalog.json",catalog)
    token = Image.new("RGBA",(32,32),(0,0,0,0))
    d = ImageDraw.Draw(token)
    d.rectangle((4,2,27,29),fill=PALETTE["paper"],outline=PALETTE["gold"],width=2)
    d.rectangle((8,15,23,24),fill=PALETTE["red"])
    d.polygon(((5,15),(11,10),(20,10),(26,15)),fill=PALETTE["roof"])
    d.polygon(((9,10),(14,6),(18,6),(23,10)),fill=PALETTE["roof"])
    d.line((7,15,25,15),fill=PALETTE["gold"],width=2)
    d.rectangle((14,18,17,24),fill=PALETTE["wood"])
    path=ASSETS/"textures/item/sect_complex_inspection_token.png"
    path.parent.mkdir(parents=True,exist_ok=True)
    token.save(path)
    save_json(ASSETS/"models/item/sect_complex_inspection_token.json",
              {"parent":"minecraft:item/generated","textures":{"layer0":"xiuxian:item/sect_complex_inspection_token"}})
    for lang in ("zh_cn","en_us"):
        path=ASSETS/"lang"/(lang+".json")
        content=json.loads(path.read_text(encoding="utf-8"))
        content["item.xiuxian.sect_complex_inspection_token"]="完整宗门验收令" if lang=="zh_cn" else "Sect Complex Inspection Token"
        content["item.xiuxian.sect_complex_inspection_token.site"]="创造管理员右键地面，建造 449×513 完整宗门。" if lang=="zh_cn" else "Creative operator: right-click ground to build the 449x513 sect."
        content["item.xiuxian.sect_complex_inspection_token.clear"]="场地内从基准 Y-24 至世界顶端全部清空。" if lang=="zh_cn" else "Clears the entire site from origin Y-24 to world height."
        save_json(path,content)
    print("Generated 144 sect BlockItems and complete model/texture/blockstate/loot/language chains.")


if __name__=="__main__": generate()
