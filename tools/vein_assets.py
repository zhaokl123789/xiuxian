"""Deterministic resource catalog and bitmap/model pipeline for the nine-layer vein."""
import hashlib
import json
from pathlib import Path
import random
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/xiuxian"
DATA = ROOT / "src/main/resources/data"
LAYERS = [
    ("dew", "露泉", (71, 153, 156), 50, 63, "spirit_stone"),
    ("frost", "霜晶", (151, 201, 221), 36, 49, "spirit_stone"),
    ("azure", "苍蓝", (49, 103, 194), 22, 35, "mid_spirit_stone"),
    ("jade", "青髓", (50, 153, 103), 8, 21, "mid_spirit_stone"),
    ("ember", "地火", (225, 97, 59), -6, 7, "high_spirit_stone"),
    ("cinnabar", "赤汞", (172, 44, 75), -20, -7, "high_spirit_stone"),
    ("gold", "金曜", (216, 176, 51), -34, -21, "high_spirit_stone"),
    ("violet", "紫霄", (154, 107, 193), -48, -35, "supreme_spirit_stone"),
    ("origin", "地心", (230, 223, 167), -64, -49, "supreme_spirit_stone"),
]
BLOCKS = {}
ITEMS = {}


def add(key, zh, kind, color, category, layer=0):
    BLOCKS["vein_" + key] = dict(zh=zh, kind=kind, color=color, category=category, layer=layer)


for index, (name, zh, color, low, high, spirit) in enumerate(LAYERS, 1):
    for suffix, label, kind in [("rock", "围岩", "ROCK"), ("ore", "富矿", "ORE"),
                                ("cluster", "晶簇", "CLUSTER"), ("block", "浓缩晶块", "CUBE")]:
        add(name + "_" + suffix, zh + label, kind, color, "layer", index)
    ITEMS["vein_" + name + "_shard"] = dict(zh=zh + "原晶", color=color, kind="shard")
    ITEMS["vein_" + name + "_concentrate"] = dict(zh=zh + "精晶", color=color, kind="concentrate")

for key, zh, color in [
    ("strata_slate", "层理页岩", (76, 88, 97)), ("porous_limestone", "孔隙灰岩", (168, 172, 154)),
    ("mica_schist", "云母片岩", (112, 137, 150)), ("magnetite", "磁铁岩", (39, 48, 58)),
    ("sulfur_rock", "硫晶围岩", (184, 168, 53)), ("salt_rock", "盐晶沉积岩", (177, 183, 194)),
    ("red_ironstone", "赤铁层岩", (132, 74, 67)), ("obsidian_breccia", "黑曜角砾", (53, 44, 70)),
    ("calcite_ribs", "方解晶肋", (212, 220, 213)), ("fossil_shale", "化石页岩", (124, 134, 114)),
    ("aquifer_rock", "含水层岩", (52, 116, 132)), ("rootstone", "盘根岩", (69, 100, 75))]:
    add(key, zh, "ROCK", color, "geology")

for key, zh, kind in [
    ("reinforced_floor", "采掘承重板", "CUBE"), ("survey_tile", "勘测刻度砖", "CUBE"),
    ("support_brace", "矿巷支撑架", "BRACE"), ("safety_rail", "矿道护栏", "RAIL"),
    ("bridge_slab", "采掘栈桥板", "SLAB"), ("anchor_chain", "矿井锚链", "CHAIN"),
    ("drain_grate", "矿道排水格栅", "GRATE"), ("pressure_pillar", "耐压矿柱", "PILLAR"),
    ("survey_bench", "矿物装配台", "CRAFT")]:
    add(key, zh, kind, (96, 128, 133), "support")

for key, zh, kind, color in [
    ("arena_basalt", "禁区耐震岩", "ROCK", (50, 59, 65)),
    ("arena_floor", "禁区战场板", "CUBE", (111, 98, 122)),
    ("fracture_tile", "裂隙阵纹砖", "CUBE", (99, 57, 108)),
    ("shield_pillar", "封域承压柱", "PILLAR", (145, 148, 169)),
    ("seal_cage", "封印晶笼", "CAGE", (138, 120, 165)),
    ("resonance_pedestal", "共鸣触发台", "SWITCH", (102, 155, 175)),
    ("pulse_beacon", "禁区脉冲灯", "LAMP", (182, 95, 145)),
    ("reward_cache", "禁区战利储箱", "CACHE", (186, 165, 99)),
    ("azure_seal_gate", "苍蓝封印门", "GATE", (61, 126, 200)),
    ("ember_seal_gate", "地火封印门", "GATE", (203, 87, 61)),
    ("origin_seal_gate", "地心封印门", "GATE", (222, 196, 122)),
    ("arena_sensor", "禁区共鸣检测台", "SENSOR", (120, 152, 157))]:
    add(key, zh, kind, color, "boss")

for key, zh, kind, color in [
    ("crusher", "原晶破碎机", "STATION", (118, 128, 139)),
    ("washer", "矿晶洗选槽", "STATION", (69, 146, 157)),
    ("condenser", "灵石凝炼器", "STATION", (100, 165, 122)),
    ("forge", "矿髓熔炼炉", "STATION", (185, 91, 65)),
    ("distiller", "灵泉蒸馏器", "STATION", (130, 186, 197)),
    ("charger", "封印充能台", "STATION", (165, 135, 187)),
    ("ore_cache", "矿石储箱", "CACHE", (90, 109, 126)),
    ("reagent_cache", "炼制材料储箱", "CACHE", (121, 143, 113)),
    ("mining_lamp", "矿道巡检灯", "LAMP", (217, 198, 111)),
    ("crystal_beacon", "晶脉定位灯", "LAMP", (87, 171, 189)),
    ("rest_seat", "矿巷休息座", "SEAT", (101, 115, 106)),
    ("assay_table", "矿层分析台", "SENSOR", (88, 142, 166))]:
    add(key, zh, kind, color, "equipment")

for key, zh, color, kind in [
    ("mineral_dust", "混合矿粉", (143, 162, 159), "dust"),
    ("filter_salt", "洗选盐", (215, 225, 230), "dust"),
    ("binding_resin", "晶脉黏结剂", (163, 172, 86), "dust"),
    ("spirit_coke", "地火晶炭", (209, 96, 59), "fuel"),
    ("blank_key", "空白共鸣钥", (129, 142, 153), "key"),
    ("azure_key", "苍蓝共鸣钥", (64, 130, 218), "key"),
    ("ember_key", "地火共鸣钥", (235, 109, 53), "key"),
    ("origin_key", "地心共鸣钥", (238, 206, 111), "key"),
    ("spring_tonic", "灵泉净液", (82, 178, 183), "drink"),
    ("ember_tonic", "地火御息液", (224, 128, 74), "drink"),
    ("azure_tonic", "苍蓝明目液", (81, 135, 223), "drink")]:
    ITEMS["vein_" + key] = dict(zh=zh, color=color, kind=kind)


def save(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def bitmap(key, entry, active=True):
    color = entry["color"]
    rng = random.Random(int(hashlib.sha256(key.encode()).hexdigest()[:16], 16))
    image = Image.new("RGB", (32, 32))
    for y in range(32):
        for x in range(32):
            n = rng.randrange(-14, 15)
            base = tuple(int(c * .43) for c in color) if entry["kind"] in ("ORE", "ROCK") else color
            image.putpixel((x, y), tuple(max(0, min(255, c + n)) for c in base))
    draw = ImageDraw.Draw(image)
    glow = tuple(min(255, c + 60) for c in color) if active else tuple(c // 3 for c in color)
    dark = tuple(c // 3 for c in color)
    kind = entry["kind"]
    if kind == "ORE":
        for _ in range(14):
            x, y = rng.randrange(2, 28), rng.randrange(2, 28)
            draw.polygon([(x, y), (x+2, y-2), (x+4, y+1), (x+2, y+4)], fill=color)
            draw.line((x+1, y, x+2, y-1), fill=glow)
        # Each depth has a characteristic mineral habit, beyond its colour.
        layer=entry["layer"]
        if layer in (2,3):
            for x,y in [(5,6),(18,19),(24,8)]:draw.line((x,y-3,x,y+4),fill=glow,width=2)
        elif layer in (4,8):
            for x,y in [(7,7),(21,21)]:draw.rectangle((x-3,y-3,x+3,y+3),outline=glow)
        elif layer in (5,6):
            draw.line((2,25,9,19,14,22,22,12,30,14),fill=color,width=2)
        elif layer==7:
            draw.line((1,12,31,16),fill=glow,width=2)
        elif layer==9:
            draw.ellipse((9,9,23,23),outline=glow,width=2)
    elif kind == "ROCK":
        for y in range(4, 32, 7):
            draw.line((0, y, 10, y-2, 21, y+2, 31, y), fill=dark)
        for _ in range(8):
            x, y = rng.randrange(32), rng.randrange(32)
            draw.line((x, y, min(31, x+4), min(31, y+2)), fill=color)
    elif kind == "CUBE" and entry["category"] == "layer":
        for y in range(0,32,8):
            for x in range(0,32,8):
                draw.polygon([(x+4,y),(x+8,y+4),(x+4,y+8),(x,y+4)],outline=dark,fill=color)
                draw.line((x+1,y+4,x+4,y+1),fill=glow)
    elif kind == "CLUSTER":
        for x in range(0, 32, 8):
            draw.polygon([(x+1, 29), (x+1, 7), (x+4, 2), (x+7, 8), (x+7, 29)], fill=glow, outline=dark)
            draw.line((x+4, 5, x+4, 28), fill=color)
    else:
        draw.rectangle((1, 1, 30, 30), outline=dark, width=2)
        draw.rectangle((5, 5, 26, 26), outline=glow)
        if kind in ("STATION", "CACHE"):
            draw.rectangle((7, 11, 24, 24), fill=dark)
            for x in range(9, 24, 4): draw.line((x, 13, x, 21), fill=glow)
            draw.rectangle((12, 4, 19, 8), fill=glow)
            if key.endswith("crusher"):
                draw.ellipse((9,12,22,25),outline=glow,width=2);draw.line((10,13,21,24),fill=color,width=2)
            elif key.endswith("washer"):
                for x,y in [(10,14),(18,17),(13,22)]:draw.ellipse((x,y,x+4,y+4),fill=color)
            elif key.endswith("condenser"):
                draw.polygon([(16,12),(23,18),(16,25),(9,18)],fill=color,outline=glow)
            elif key.endswith("forge"):
                draw.polygon([(10,24),(12,16),(16,21),(19,12),(22,24)],fill=color)
            elif key.endswith("distiller"):
                draw.line((12,13,12,21,21,21,21,15),fill=color,width=3)
            elif key.endswith("charger"):
                draw.line((19,12,13,18,19,18,13,25),fill=glow,width=2)
            elif kind=="CACHE":
                draw.rectangle((8,15,23,18),fill=color);draw.rectangle((14,15,17,21),fill=glow)
        elif kind == "GATE":
            for y in (8, 16, 24): draw.line((4, y, 15, y-4, 27, y), fill=glow, width=2)
        elif kind in ("LAMP", "SWITCH", "SENSOR"):
            draw.ellipse((7, 7, 24, 24), fill=glow, outline=dark, width=2)
        else:
            for y in (9, 19): draw.line((3, y, 29, y), fill=dark, width=2)
    return image


def boxes(entry, stage=3, opened=False):
    kind = entry["kind"]
    if kind == "CLUSTER":
        h = [4, 7, 11, 16][stage]
        return [[2,0,2,14,2,14], [6,2,6,10,h,10], [2,2,3,5,max(3,h-3),6], [11,2,9,14,max(3,h-5),12]]
    if kind == "GATE":
        return [[0,0,5,3,16,11], [13,0,5,16,16,11]] if opened else [[0,0,5,16,16,11]]
    if kind == "BRACE": return [[0,0,0,3,16,3],[13,0,0,16,16,3],[0,13,0,16,16,5]]
    if kind == "RAIL": return [[0,0,6,2,16,10],[14,0,6,16,16,10],[2,10,7,14,13,9],[2,3,7,14,6,9]]
    if kind == "CHAIN": return [[6,0,7,10,16,9],[7,1,5,9,6,11],[7,10,5,9,15,11]]
    if kind == "GRATE": return [[0,0,0,16,2,2],[0,0,14,16,2,16]]+[[x,0,2,x+2,2,14] for x in (0,4,8,12)]
    if kind == "PILLAR": return [[1,0,1,15,2,15],[4,2,4,12,14,12],[1,14,1,15,16,15]]
    if kind == "CAGE": return [[1,0,1,15,2,15],[1,14,1,15,16,15]]+[[x,2,z,x+2,14,z+2] for x in (1,13) for z in (1,13)]
    if kind == "LAMP": return [[3,0,3,13,2,13],[6,2,6,10,5,10],[3,5,3,13,14,13],[2,14,2,14,16,14]]
    if kind in ("SWITCH", "SENSOR"): return [[1,0,1,15,3,15],[4,3,4,12,7,12],[6,7,6,10,11,10]]
    if kind == "SEAT": return [[2,0,2,4,7,4],[12,0,2,14,7,4],[2,0,12,4,7,14],[12,0,12,14,7,14],[1,7,1,15,10,15]]
    return [[0,0,0,16,16,16]]


def model(key, entry, stage=3, opened=False, suffix=""):
    texture = "xiuxian:block/" + key + suffix
    def face(direction, bounds):
        axis,index,edge={"north":(2,0,0),"south":(2,3,16),"west":(0,0,0),"east":(0,3,16),"down":(1,0,0),"up":(1,3,16)}[direction]
        return {"texture":"#all",**({"cullface":direction} if bounds[axis+index]==edge else {})}
    return {"parent":"minecraft:block/block","textures":{"all":texture,"particle":texture},"elements":[
        {"from":b[:3],"to":b[3:],"faces":{f:face(f,b) for f in ("north","east","south","west","up","down")}}
        for b in boxes(entry,stage,opened)]}


def loot(key, entry):
    item = {"type":"minecraft:item","name":"xiuxian:"+key}
    if entry["kind"] == "SLAB":
        item["functions"]=[{"function":"minecraft:set_count","count":2,"conditions":[{"condition":"minecraft:block_state_property","block":"xiuxian:"+key,"properties":{"type":"double"}}]}]
    if entry["kind"] in ("ORE", "CLUSTER"):
        layer = LAYERS[entry["layer"]-1][0]
        raw = {"type":"minecraft:item","name":"xiuxian:vein_"+layer+"_shard",
               "functions":[{"function":"minecraft:set_count","count":2 if entry["kind"]=="ORE" else 1},
                            {"function":"minecraft:apply_bonus","enchantment":"minecraft:fortune","formula":"minecraft:ore_drops"}]}
        if entry["kind"] == "CLUSTER":
            raw["conditions"]=[{"condition":"minecraft:block_state_property","block":"xiuxian:"+key,"properties":{"age":"3"}}]
        silk = dict(item, conditions=[{"condition":"minecraft:match_tool","predicate":{"enchantments":[{"enchantment":"minecraft:silk_touch","levels":{"min":1}}]}}])
        item={"type":"minecraft:alternatives","children":[silk,raw]}
    return {"type":"minecraft:block","pools":[{"rolls":1,"conditions":[{"condition":"minecraft:survives_explosion"}],"entries":[item]}]}


def recipe(key, value): save(DATA/"xiuxian/recipes"/("vein_"+key+".json"), value)


def shaped(key, pattern, keys, result, count=1):
    recipe(key,{"type":"minecraft:crafting_shaped","pattern":pattern,"key":{k:{"item":v} for k,v in keys.items()},"result":{"item":result,"count":count}})


def process(key, station, ingredient, reagent, result, count=1, input_count=1, reagent_count=1, ticks=100, residue=None):
    value={"type":"xiuxian:vein_processing","station":station,"ingredient":{"item":ingredient},"input_count":input_count,
           "reagent":{"item":reagent},"reagent_count":reagent_count,"result":{"item":result,"count":count},"ticks":ticks}
    if residue:value["residue"]={"item":residue}
    recipe(key,value)


def generate():
    assert len(BLOCKS)==81
    tags=[]
    java=[]
    for key, entry in BLOCKS.items():
        texture=bitmap(key,entry)
        path=ASSETS/"textures/block"/(key+".png");path.parent.mkdir(parents=True,exist_ok=True);texture.save(path)
        variants={}
        kind=entry["kind"]
        flags = ["true", "false"] if kind in ("LAMP","STATION","GATE","SWITCH","CACHE") else [None]
        ages=range(4) if kind=="CLUSTER" else [None]
        prop={"LAMP":"lit","STATION":"lit","CACHE":"lit","GATE":"open","SWITCH":"powered"}.get(kind)
        if prop:
            bitmap(key,entry,False).save(ASSETS/"textures/block"/(key+"_off.png"))
        if kind=="SLAB":
            for state,parent in [("bottom","slab"),("top","slab_top"),("double","cube_all")]:
                mid=key+"_"+state
                save(ASSETS/"models/block"/(mid+".json"),{"parent":"minecraft:block/"+parent,"textures":{t:"xiuxian:block/"+key for t in ("all","bottom","top","side")}})
                variants["type="+state]={"model":"xiuxian:block/"+mid}
            item_model=key+"_bottom"
        else:
            facing=kind not in ("ROCK","ORE","CUBE")
            for flag in flags:
                for age in ages:
                    suffix=("_off" if flag=="false" else "")+("_"+str(age) if age is not None else "")
                    mid=key+suffix
                    save(ASSETS/"models/block"/(mid+".json"),model(key,entry,age if age is not None else 3,kind=="GATE" and flag=="true", "_off" if flag=="false" else ""))
                    for i,direction in enumerate(["north","east","south","west"] if facing else [None]):
                        selector=",".join(x for x in [("facing="+direction if direction else None),(prop+"="+flag if prop else None),("age="+str(age) if age is not None else None)] if x)
                        variants[selector]={"model":"xiuxian:block/"+mid,**({"y":i*90} if i else {})}
            item_model=key+("_3" if kind=="CLUSTER" else "_off" if kind in ("GATE", "CACHE", "STATION", "SWITCH") else "")
        save(ASSETS/"blockstates"/(key+".json"),{"variants":variants})
        save(ASSETS/"models/item"/(key+".json"),{"parent":"xiuxian:block/"+item_model})
        save(DATA/"xiuxian/loot_tables/blocks"/(key+".json"),loot(key,entry))
        tags.append("xiuxian:"+key)
        geo="new double[][]{"+",".join("{"+",".join(str(n) for n in b)+"}" for b in boxes(entry))+"}"
        java.append(f'        add("{key}", Kind.{kind}, {entry["layer"]}, {geo});')
    template=(ROOT/"tools/vein_blocks.java.template").read_text(encoding="utf-8")
    (ROOT/"src/main/java/xiuxian/block/VeinBlocks.java").write_text(template.replace("// CATALOG", "\n".join(java)).replace("// MATERIALS", "\n".join(f'        material("{k}", "{v["kind"]}");' for k,v in ITEMS.items())),encoding="utf-8")
    for key,entry in ITEMS.items():
        im=Image.new("RGBA",(32,32),(0,0,0,0));d=ImageDraw.Draw(im);c=entry["color"];kind=entry["kind"]
        if kind=="key":
            d.ellipse((3,3,17,17),outline=c,width=4);d.line((14,14,27,27),fill=c,width=4);d.line((22,24,25,20),fill=c,width=3)
        elif kind=="drink":
            d.rectangle((12,2,19,7),fill=(173,183,187));d.polygon([(12,7),(8,13),(8,28),(23,28),(23,13),(19,7)],fill=(*c,230),outline=(221,238,239));d.rectangle((10,20,21,26),fill=c)
        elif kind in ("dust","fuel"):
            d.polygon([(3,27),(6,18),(12,16),(15,9),(21,15),(28,26)],fill=c,outline=tuple(max(0,n-45) for n in c))
        else:
            d.polygon([(8,24),(6,10),(14,2),(25,11),(23,24),(16,30)],fill=c,outline=tuple(min(255,n+40) for n in c));d.line((14,5,16,26,22,13),fill=tuple(min(255,n+55) for n in c),width=2)
            if kind=="concentrate":d.rectangle((3,26,28,29),fill=(160,180,186))
        path=ASSETS/"textures/item"/(key+".png");path.parent.mkdir(parents=True,exist_ok=True);im.save(path)
        save(ASSETS/"models/item"/(key+".json"),{"parent":"minecraft:item/generated","textures":{"layer0":"xiuxian:item/"+key}})
    for lang in ("zh_cn","en_us"):
        p=ASSETS/"lang"/(lang+".json");content=json.loads(p.read_text(encoding="utf-8-sig"))
        for key,e in BLOCKS.items():content["block.xiuxian."+key]=e["zh"] if lang=="zh_cn" else key.removeprefix("vein_").replace("_"," ").title()
        for key,e in ITEMS.items():content["item.xiuxian."+key]=e["zh"] if lang=="zh_cn" else key.removeprefix("vein_").replace("_"," ").title()
        content.update({"vein.station.slots":"原料 / 媒剂 / 燃料 / 产物 / 副产物" if lang=="zh_cn" else "Input / Reagent / Fuel / Output / Byproduct",
                        "vein.sensor.reading":"层级 %s | 周边矿点 %s | 比较器强度 %s" if lang=="zh_cn" else "Layer %s | Nearby ore nodes %s | Comparator strength %s",
                        "vein.sensor.arena":"层级 %s | 周边矿点 %s | 场内生物信号 %s" if lang=="zh_cn" else "Layer %s | Nearby ore nodes %s | Arena creature signal %s"})
        for slot,zh,en in [("input","原料","In"),("reagent","媒剂","Add"),("fuel","燃料","Fuel"),("output","产物","Out"),("residue","副产","Extra")]:
            content["vein.slot."+slot]=zh if lang=="zh_cn" else en
        content["vein.station.paused"]="红石锁定" if lang=="zh_cn" else "Redstone lock"
        content["vein.station.progress"]="加工 %s%% | 余热 %s" if lang=="zh_cn" else "Progress %s%% | Fuel %s"
        save(p,content)
    for tag in ("mineable/pickaxe",):
        p=DATA/"minecraft/tags/blocks"/(tag+".json");content=json.loads(p.read_text(encoding="utf-8"));content["values"]=[v for v in content["values"] if not v.startswith("xiuxian:vein_")]+tags;save(p,content)
    for tag,min_layer in [("needs_iron_tool",1),("needs_diamond_tool",7)]:
        p=DATA/"minecraft/tags/blocks"/(tag+".json");content=json.loads(p.read_text()) if p.exists() else {"replace":False,"values":[]}
        content["values"]=[v for v in content["values"] if not v.startswith("xiuxian:vein_")]+["xiuxian:"+k for k,v in BLOCKS.items() if v["kind"]=="ORE" and (v["layer"]>=7 if min_layer==7 else v["layer"]<7)]
        save(p,content)
    for tag,ids in {"vein_ores":[k for k,v in BLOCKS.items() if v["kind"]=="ORE"],"vein_hosts":[k for k,v in BLOCKS.items() if v["kind"]=="ROCK"],"vein_seals":[k for k,v in BLOCKS.items() if v["kind"]=="GATE"],"vein_stations":[k for k,v in BLOCKS.items() if v["kind"]=="STATION"]}.items():
        save(DATA/"xiuxian/tags/blocks"/(tag+".json"),{"replace":False,"values":["xiuxian:"+k for k in ids]})
    for tag,suffix in [("vein_raw_crystals","shard"),("vein_concentrates","concentrate")]:
        save(DATA/"xiuxian/tags/items"/(tag+".json"),{"replace":False,"values":["xiuxian:"+k for k in ITEMS if k.endswith(suffix)]})
    for i,(name,zh,color,low,high,spirit) in enumerate(LAYERS,1):
        raw="xiuxian:vein_"+name+"_shard"; refined="xiuxian:vein_"+name+"_concentrate";block="xiuxian:vein_"+name+"_block"
        shaped(name+"_compress",["XXX","XXX","XXX"],{"X":refined},block)
        recipe(name+"_unpack",{"type":"minecraft:crafting_shapeless","ingredients":[{"item":block}],"result":{"item":refined,"count":9}})
        recipe(name+"_smelt",{"type":"minecraft:smelting","ingredient":{"item":raw},"result":refined,"experience":.2,"cookingtime":200})
        process(name+"_crush","crusher",raw,"minecraft:flint","xiuxian:vein_mineral_dust",2,ticks=80)
        process(name+"_wash","washer",raw,"xiuxian:vein_filter_salt",refined,2,ticks=120)
        process(name+"_condense","condenser",refined,"xiuxian:vein_binding_resin","xiuxian:"+spirit,
                input_count=4 if i>=8 else 2,reagent_count=2 if i>=8 else 1,ticks=160+20*i)
    recipe("salt",{"type":"minecraft:crafting_shapeless","ingredients":[{"item":"minecraft:quartz"},{"item":"minecraft:bone_meal"}],"result":{"item":"xiuxian:vein_filter_salt","count":4}})
    recipe("resin",{"type":"minecraft:crafting_shapeless","ingredients":[{"item":"minecraft:slime_ball"},{"item":"xiuxian:vein_mineral_dust"}],"result":{"item":"xiuxian:vein_binding_resin","count":4}})
    process("iron","forge","xiuxian:vein_mineral_dust","minecraft:raw_iron","minecraft:iron_ingot",2,ticks=200)
    process("copper","forge","xiuxian:vein_mineral_dust","minecraft:raw_copper","minecraft:copper_ingot",3,ticks=180)
    process("gold","forge","xiuxian:vein_gold_concentrate","minecraft:raw_gold","minecraft:gold_ingot",2,ticks=240)
    process("coke","forge","xiuxian:vein_ember_shard","minecraft:coal","xiuxian:vein_spirit_coke",4,ticks=120)
    process("dew_water_wash","washer","xiuxian:vein_dew_shard","minecraft:water_bucket","xiuxian:vein_dew_concentrate",ticks=120)
    process("salt_rock_filter","crusher","xiuxian:vein_salt_rock","minecraft:flint","xiuxian:vein_filter_salt",2,ticks=100)
    process("rootstone_resin","distiller","xiuxian:vein_rootstone","minecraft:slime_ball","xiuxian:vein_binding_resin",4,ticks=140)
    for name,reagent in [("spring","dew"),("ember","ember"),("azure","azure")]:
        process(name+"_tonic","distiller","minecraft:glass_bottle","xiuxian:vein_"+reagent+"_concentrate","xiuxian:vein_"+name+"_tonic",ticks=160)
    shaped("blank_key",[" I ","ICI"," I "],{"I":"minecraft:iron_ingot","C":"xiuxian:vein_mineral_dust"},"xiuxian:vein_blank_key")
    for name in ("azure","ember","origin"):
        process(name+"_key","charger","xiuxian:vein_blank_key","xiuxian:vein_"+name+"_concentrate","xiuxian:vein_"+name+"_key",reagent_count=4 if name=="origin" else 2,ticks=200)
    for key,entry in BLOCKS.items():
        if entry["category"]=="layer":continue
        if entry["kind"] not in ("STATION", "GATE", "SWITCH", "SENSOR", "LAMP", "CACHE", "SEAT", "CRAFT"):
            recipe(key.removeprefix("vein_")+"_craft",{"type":"minecraft:stonecutting","ingredient":{"item":"minecraft:deepslate"},"result":"xiuxian:"+key,"count":1})
        else:
            reagent={"vein_crusher":"minecraft:stonecutter", "vein_washer":"minecraft:water_bucket",
                     "vein_condenser":"minecraft:amethyst_block", "vein_forge":"minecraft:blast_furnace",
                     "vein_distiller":"minecraft:brewing_stand", "vein_charger":"minecraft:enchanting_table",
                     "vein_azure_seal_gate":"xiuxian:vein_azure_key", "vein_ember_seal_gate":"xiuxian:vein_ember_key",
                     "vein_origin_seal_gate":"xiuxian:vein_origin_key", "vein_resonance_pedestal":"minecraft:lever",
                     "vein_arena_sensor":"minecraft:sculk_sensor", "vein_assay_table":"minecraft:comparator",
                     "vein_pulse_beacon":"minecraft:observer", "vein_mining_lamp":"minecraft:lantern",
                     "vein_crystal_beacon":"minecraft:glowstone", "vein_reward_cache":"minecraft:ender_pearl",
                     "vein_ore_cache":"minecraft:chest", "vein_reagent_cache":"minecraft:barrel",
                     "vein_rest_seat":"minecraft:oak_slab", "vein_survey_bench":"minecraft:crafting_table"}[key]
            shaped(key.removeprefix("vein_")+"_craft",["IRI","CSC","III"],{"I":"minecraft:iron_ingot","R":reagent,"C":"minecraft:cobblestone","S":"xiuxian:vein_binding_resin"},"xiuxian:"+key)
    for name,base in [("azure","bright_sight_pill_base"),("ember","fire_warding_pill_base"),("dew","rejuvenation_pill_base")]:
        result={"azure":"bright_sight_pill","ember":"fire_warding_pill","dew":"rejuvenation_pill"}[name]
        recipe(name+"_alchemy",{"type":"xiuxian:alchemy","ingredients":[{"item":"xiuxian:"+base},{"item":"xiuxian:vein_"+name+"_concentrate"}],"result":{"item":"xiuxian:"+result},"experience":.3,"cookingtime":400})
    catalog={"blocks":BLOCKS,"items":ITEMS,"layers":[{"index":i,"id":n,"name":z,"min_y":lo,"max_y":hi,"spirit_drop":s} for i,(n,z,c,lo,hi,s) in enumerate(LAYERS,1)]}
    save(ROOT/"docs/vein-resources.json",catalog)
    save(DATA/"xiuxian/vein/layers.json",catalog["layers"])
    gallery=Image.new("RGB",(9*112,9*116),(24,28,31));draw=ImageDraw.Draw(gallery)
    font=ImageFont.truetype("C:/Windows/Fonts/arial.ttf",10)
    for i,(key,entry) in enumerate(BLOCKS.items()):
        x=(i%9)*112;y=(i//9)*116
        gallery.paste(bitmap(key,entry).resize((80,80),Image.Resampling.NEAREST),(x+16,y+4))
        label=key.removeprefix("vein_")
        for line,part in enumerate([label[:18],label[18:]]):draw.text((x+4,y+87+line*12),part,font=font,fill=(219,228,230))
    preview=ROOT/"docs/images/vein-materials.png";preview.parent.mkdir(parents=True,exist_ok=True);gallery.save(preview)
    print(f"Generated {len(BLOCKS)} vein blocks, {len(ITEMS)} materials, nine layer profiles and resource chains.")


if __name__=="__main__":generate()
