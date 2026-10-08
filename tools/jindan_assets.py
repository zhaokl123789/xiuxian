"""Original Jin-Dan assets for the furnace palace in references/luoxia/05.png."""
from collections import Counter
import hashlib
import json
import random
from PIL import Image, ImageDraw

DEFINITIONS = {}
CATEGORIES = {}


def b(coords, tex):
    return (list(coords), "jindan_" + tex)


def add(name, zh, geometry, category, material="stone", light=0):
    key = "jindan_" + name
    assert key not in DEFINITIONS
    if isinstance(geometry, str):
        geometry = "jindan_" + geometry
    DEFINITIONS[key] = (zh, name.replace("_", " ").title(), material, light, geometry)
    CATEGORIES[key] = category


def rim(y=0, h=2, outer=1, inner=3, tex="copper"):
    return [b((outer,y,outer,inner,y+h,16-outer),tex),
            b((16-inner,y,outer,16-outer,y+h,16-outer),tex),
            b((inner,y,outer,16-inner,y+h,inner),tex),
            b((inner,y,16-inner,16-inner,y+h,16-outer),tex)]


def base(tex="basalt"):
    return [b((2,0,2,14,2,14),tex), b((4,2,4,12,4,12),"gold")]


def legs(h=7, tex="wood"):
    return [b((x,0,z,x+2,h,z+2),tex) for x in (2,12) for z in (2,12)]


def desk():
    return legs() + [b((0,7,1,16,9,15),"wood"), b((1,6,1,15,7,3),"gold")]


def jar():
    return [b((5,0,5,11,2,11),"porcelain"), b((3,2,3,13,8,13),"porcelain"),
            b((4,8,4,12,12,12),"porcelain"), b((6,12,6,10,14,10),"gold")]


def tripod():
    return [b((3,0,3,5,5,5),"copper"), b((11,0,3,13,5,5),"copper"),
            b((7,0,11,9,5,13),"copper"), b((4,5,4,12,7,12),"copper"),
            b((2,7,2,14,12,14),"copper"), b((3,12,3,13,14,13),"gold")]


def window(style):
    parts = [b((0,0,6,2,16,10),"lacquer"), b((14,0,6,16,16,10),"lacquer"),
             b((2,0,6,14,2,10),"gold"), b((2,14,6,14,16,10),"gold")]
    if style == "sun":
        parts += [b((x,y,7,x+w,y+1,9),"gold")
                  for x,y,w in ((5,3,6),(3,5,10),(2,7,12),(3,10,10),(5,12,6))]
        parts += [b((7,3,7,9,14,9),"gold")]
    elif style == "moon":
        parts += [b((x,y,7,x+w,y+1,9),"jade")
                  for x,y,w in ((7,3,4),(5,5,4),(4,7,3),(5,10,4),(7,12,4))]
    else:
        parts += [b((x,2,7,x+1,14,9),"copper") for x in (4,7,10)]
        parts += [b((3,y,7,13,y+1,9),"copper") for y in (5,9)]
        parts += [b((6,6,6,10,9,7),"flame")]
    return parts


def group(category, entries, material="stone", light=0):
    for name, zh, geometry in entries:
        add(name, zh, geometry, category, material, light)


group("materials", [
    ("cinnabar_brick","\u4e39\u7802\u7089\u7816","cinnabar"),
    ("ashen_brick","\u706b\u5c71\u7070\u7816","ash_brick"),
    ("scorched_stone","\u7126\u708e\u7075\u77f3","scorched"),
    ("red_copper","\u8d64\u94dc\u677f","copper"),
    ("gilded_copper","\u93cf\u91d1\u523b\u94dc","gold"),
    ("furnace_tile","\u7089\u5ead\u70df\u58a8\u7816","court"),
    ("fire_vein_tile","\u5730\u706b\u8109\u7eb9\u7816","fire_vein"),
    ("ember_marble","\u8d64\u971e\u70ec\u7eb9\u77f3","marble"),
    ("gold_flame_tile","\u91d1\u7130\u7eb9\u7816","flame"),
    ("black_iron","\u7384\u94c1\u7089\u6750","iron"),
    ("obsidian_trim","\u66dc\u77f3\u9547\u706b\u7816","obsidian"),
    ("slag_brick","\u7194\u6e23\u77f3\u7816","slag"),
    ("sunset_basalt","\u66ae\u971e\u7384\u6b66\u5ca9","basalt"),
    ("formation_floor","\u4e39\u7089\u516b\u5366\u7816","bagua"),
    ("yang_tile","\u7eaf\u9633\u65e5\u8f6e\u7816","sun"),
    ("yin_tile","\u592a\u9634\u6708\u534e\u7816","moon"),
    ("tribulation_tile","\u96f7\u52ab\u4e91\u7eb9\u7816","tribulation"),
    ("dan_pattern","\u91d1\u4e39\u7ed3\u7b26\u7816","core_seal"),
    ("copper_grate","\u94dc\u683c\u62a4\u706b\u7816","grate"),
    ("ash_paving","\u7070\u7389\u5ead\u94fa","paver"),
    ("white_jade","\u6696\u7389\u6bbf\u77f3","jade"),
    ("red_glazed_tile","\u8d64\u91d1\u7409\u7483\u7816","roof"),
    ("bronze_scale","\u87e0\u9f99\u9cde\u94dc","scale"),
    ("violet_inlay","\u7d2b\u971e\u661f\u7eb9\u7816","stars"),
])

group("architecture", [
    ("roof_tiles","\u8d64\u91d1\u7b52\u74e6",
     [b((0,0,0,16,3,16),"roof")]+[b((x,3,0,x+2,5,16),"gold") for x in (1,5,9,13)]),
    ("roof_ridge","\u4e39\u4e91\u6b63\u810a",
     [b((0,0,4,16,4,12),"roof"),b((0,4,6,16,9,10),"cloud"),b((0,9,7,16,12,9),"gold")]),
    ("eave_corner","\u94dc\u91d1\u98de\u6a90",
     [b((0,0,0,16,2,16),"roof"),b((0,2,0,5,4,16),"roof"),b((0,4,0,3,7,16),"gold"),b((0,7,0,2,10,16),"gold")]),
    ("dougong","\u4e39\u6f06\u93cf\u91d1\u6597\u62f1",
     [b((6,0,6,10,4,10),"lacquer"),b((3,4,5,13,7,11),"lacquer"),b((1,7,6,15,10,10),"gold"),
      b((3,10,0,6,13,16),"lacquer"),b((10,10,0,13,13,16),"lacquer"),b((0,13,0,16,16,16),"roof")]),
    ("pillar_base","\u76d8\u9f99\u67f1\u790e",
     [b((0,0,0,16,3,16),"jade"),b((2,3,2,14,6,14),"scale"),b((4,6,4,12,9,12),"gold")]),
    ("pillar_shaft","\u708e\u7eb9\u94dc\u67f1",
     [b((4,0,4,12,16,12),"copper"),b((3,0,3,13,2,13),"gold"),b((3,14,3,13,16,13),"gold"),b((3,4,5,4,12,11),"flame")]),
    ("pillar_cap","\u91d1\u83b2\u67f1\u51a0",
     [b((5,0,5,11,4,11),"copper"),b((3,4,3,13,7,13),"gold"),b((1,7,1,15,10,15),"jade"),b((0,10,0,16,13,16),"gold")]),
    ("jade_balustrade","\u4e39\u7eb9\u7389\u680f",
     [b((0,0,5,16,2,11),"jade"),b((0,12,5,16,15,11),"core_seal")]+[b((x,2,7,x+2,12,9),"jade") for x in (1,5,9,13)]),
    ("bridge_rail","\u9547\u706b\u94dc\u6865\u680f",
     [b((0,0,6,16,2,10),"iron"),b((0,11,6,16,13,10),"copper"),b((1,0,5,3,16,11),"copper"),
      b((13,0,5,15,16,11),"copper"),b((4,5,7,12,7,9),"gold")]),
    ("gateway_frame","\u4e39\u5bab\u901a\u95e8\u6846",
     [b((0,0,4,2,16,12),"lacquer"),b((14,0,4,16,16,12),"lacquer"),b((2,14,4,14,16,12),"gold")]),
    ("door_panel","\u4e39\u6f06\u94dc\u9489\u95e8\u677f",
     [b((1,0,6,15,16,8),"lacquer"),b((1,0,5,3,16,6),"gold"),b((13,0,5,15,16,6),"gold"),b((6,6,4,10,10,6),"copper")]+
     [b((x,y,5,x+1,y+1,6),"gold") for x in (4,11) for y in (3,7,11,14)]),
    ("sun_window","\u65e5\u8f6e\u82b1\u7a97",window("sun")),
    ("moon_window","\u6708\u5f27\u7389\u7a97",window("moon")),
    ("flame_window","\u706b\u7eb9\u900f\u82b1\u7a97",window("flame")),
    ("cloud_transom","\u94dc\u4e91\u6a2a\u6963",
     [b((0,4,6,16,6,10),"lacquer"),b((0,12,6,16,14,10),"gold")]+[b((x,6,7,x+2,12,9),"cloud") for x in (0,4,8,12)]),
    ("fireguard_screen","\u9557\u94dc\u62a4\u706b\u5c4f",
     [b((1,0,4,3,16,12),"copper"),b((13,0,4,15,16,12),"copper"),b((3,3,7,13,14,9),"grate"),b((3,14,6,13,16,10),"gold")]),
    ("palace_plaque","\u4e39\u9619\u91d1\u533e",
     [b((0,3,6,16,13,10),"wood"),b((1,4,5,15,12,6),"script"),b((0,2,5,16,4,11),"gold")]),
    ("dragon_ridge","\u87e0\u9f99\u541e\u706b\u810a\u9970",
     [b((2,0,3,14,3,13),"roof"),b((3,3,7,13,6,11),"scale"),b((8,6,5,12,12,10),"scale"),
      b((9,9,1,13,12,6),"gold"),b((8,12,4,9,16,6),"gold"),b((11,12,4,12,16,6),"gold")]),
    ("phoenix_ridge","\u91d1\u51e4\u5c55\u7fbd\u810a\u9970",
     [b((2,0,2,14,2,14),"roof"),b((6,2,6,10,10,11),"phoenix"),b((1,6,7,6,8,13),"gold"),
      b((10,6,7,15,8,13),"gold"),b((7,10,4,9,14,7),"phoenix"),b((7,12,2,9,13,4),"gold"),b((5,3,11,11,5,16),"phoenix")]),
    ("eave_bell","\u98ce\u706b\u6a90\u94c3",
     [b((7,13,7,9,16,9),"copper"),b((5,10,5,11,13,11),"gold"),b((3,4,3,13,10,13),"copper"),
      b((7,1,7,9,4,9),"gold"),b((7,0,7,9,1,12),"silk")]),
])

group("furnaces", [
    ("tripod_cauldron","\u8d64\u94dc\u4e09\u8db3\u4e39\u9f0e",tripod()+rim(14,1,2,4)+
     [b((0,9,5,2,13,9),"gold"),b((14,9,5,16,13,9),"gold")]),
    ("sun_furnace","\u7eaf\u9633\u65e5\u7089",base()+[b((3,4,3,13,11,13),"sun"),b((5,11,5,11,14,11),"copper"),b((7,14,7,9,16,9),"gold_fire")]),
    ("four_symbol_cauldron","\u56db\u8c61\u5408\u4e39\u7089",
     [b((3,0,3,13,3,13),"obsidian"),b((2,3,2,14,12,14),"bagua"),b((4,12,4,12,14,12),"gold")]+
     [b((x,5,z,x+2,13,z+2),"scale") for x,z in ((0,7),(14,7),(7,0),(7,14))]),
    ("furnace_belly","\u4e39\u7089\u9cde\u8179",
     [b((2,0,2,14,3,14),"copper"),b((0,3,0,16,11,16),"scale"),b((2,11,2,14,16,14),"copper")]),
    ("furnace_rim","\u4e39\u7089\u93cf\u91d1\u53e3\u6cbf",rim(0,5,0,3,"gold")),
    ("furnace_lid","\u4e91\u7eb9\u7089\u76d6",
     [b((0,0,0,16,2,16),"copper"),b((2,2,2,14,5,14),"cloud"),b((4,5,4,12,8,12),"copper"),b((7,8,7,9,13,9),"gold")]),
    ("furnace_foot","\u541e\u706b\u517d\u8db3",
     [b((3,0,3,13,2,13),"iron"),b((5,2,6,11,10,12),"scale"),b((4,10,3,12,16,11),"scale"),b((5,11,1,11,14,3),"gold")]),
    ("furnace_ear","\u4e39\u7089\u73af\u8033",
     [b((2,1,6,5,15,10),"copper"),b((5,1,6,13,4,10),"gold"),b((5,12,6,13,15,10),"gold"),b((10,4,6,13,12,10),"gold")]),
    ("furnace_grate","\u4e39\u7089\u9547\u706b\u683c",
     [b((0,0,4,16,2,12),"iron"),b((0,14,4,16,16,12),"iron")]+[b((x,2,6,x+1,14,10),"copper") for x in (1,4,7,10,13)]),
    ("furnace_mouth","\u5730\u706b\u7089\u53e3",
     [b((0,0,4,3,16,12),"obsidian"),b((13,0,4,16,16,12),"obsidian"),b((3,0,4,13,3,12),"iron"),
      b((3,13,4,13,16,12),"iron"),b((3,3,10,13,13,12),"orange_fire")]),
    ("fire_channel","\u5730\u706b\u5bfc\u6e20",
     [b((0,0,0,16,2,16),"obsidian"),b((0,2,0,4,6,16),"copper"),b((12,2,0,16,6,16),"copper"),b((4,2,0,12,3,16),"orange_fire")]),
    ("fire_channel_corner","\u5730\u706b\u8f6c\u89d2\u6e20",
     [b((0,0,0,16,2,16),"obsidian"),b((0,2,0,4,6,16),"copper"),b((4,2,12,16,6,16),"copper"),
      b((12,2,0,16,6,4),"copper"),b((4,2,0,12,3,12),"orange_fire"),b((12,2,4,16,3,12),"orange_fire")]),
    ("copper_pipe","\u8d64\u94dc\u706b\u7ba1",
     [b((4,4,0,12,12,16),"copper"),b((3,3,0,13,13,2),"gold"),b((3,3,14,13,13,16),"gold")]),
    ("pipe_elbow","\u8d64\u94dc\u66f2\u7ba1",
     [b((4,4,0,12,12,12),"copper"),b((4,4,4,16,12,12),"copper"),b((3,3,0,13,13,2),"gold"),b((14,3,3,16,13,13),"gold")]),
    ("fire_vent","\u6cc9\u773c\u5730\u706b\u53e3",
     base("obsidian")+rim(4,3,2,5,"iron")+[b((5,4,5,11,5,11),"orange_fire"),b((7,5,7,9,11,9),"gold_fire")]),
    ("blue_flame_basin","\u9752\u7130\u517b\u706b\u76c6",
     base("jade")+rim(4,4,1,4)+[b((4,4,4,12,5,12),"blue_fire"),b((6,5,6,10,13,10),"blue_fire")]),
    ("ash_collector","\u4e39\u7089\u6536\u7070\u69fd",
     [b((1,0,2,15,2,14),"iron")]+rim(2,5,1,3,"iron")+[b((3,2,3,13,4,13),"ash"),b((6,7,1,10,9,3),"copper")]),
    ("bellows","\u9f13\u98ce\u7145\u706b\u7bb1",
     [b((1,0,3,12,9,13),"wood"),b((2,2,2,11,7,3),"silk"),b((12,3,6,16,7,10),"copper"),b((3,9,5,9,11,7),"iron")]),
    ("heat_shield","\u7384\u94c1\u9694\u70ed\u677f",
     [b((2,0,4,4,16,12),"iron"),b((12,0,4,14,16,12),"iron"),b((4,3,7,12,15,9),"obsidian"),b((5,5,6,11,12,7),"guard")]),
    ("furnace_inscription","\u4e39\u7089\u94ed\u6587\u7891",
     base("obsidian")+[b((3,4,5,13,14,11),"script"),b((2,14,4,14,16,12),"gold")]),
])

for name, zh, texture in [
    ("solar_seal","\u65e5\u8f6e\u9547\u706b\u5370","sun"),
    ("moon_seal","\u6708\u534e\u51dd\u4e39\u5370","moon"),
    ("five_element_array","\u4e94\u884c\u70bc\u4e39\u9635","five_elements"),
    ("nine_flame_array","\u4e5d\u7130\u5f52\u4e00\u9635","nine_flames"),
    ("tribulation_mark","\u96f7\u52ab\u9547\u7b26","tribulation"),
    ("core_seal","\u91d1\u4e39\u7ed3\u5370","core_seal"),
    ("meridian_line","\u7075\u8109\u5bfc\u5f15\u7ebf","meridian"),
    ("heaven_cycle_seal","\u5468\u5929\u8fd0\u4e39\u5370","cycle"),
]:
    add(name,zh,[b((0,0,0,16,1,16),"obsidian"),b((1,1,1,15,1.5,15),texture)],"formations",light=3)
group("formations", [
    ("core_orb","\u91d1\u4e39\u5149\u6838",
     [b((5,2,5,11,14,11),"core"),b((3,4,3,13,12,13),"core"),b((1,6,6,15,10,10),"gold_fire"),b((6,6,1,10,10,15),"gold_fire")]),
    ("core_pedestal","\u91d1\u4e39\u4f9b\u53f0",
     base("jade")+[b((6,4,6,10,10,10),"copper"),b((3,10,3,13,12,13),"core_seal"),b((5,12,5,11,16,11),"core")]),
    ("solar_halo_segment","\u65e5\u8f6e\u5149\u73af\u6bb5",
     [b((0,5,6,16,8,10),"gold"),b((2,8,6,14,11,10),"gold_fire"),b((4,11,6,12,13,10),"gold")]),
    ("violet_halo_segment","\u7d2b\u971e\u73af\u5e26\u6bb5",
     [b((0,6,5,16,8,11),"violet_fire"),b((0,8,6,5,9,10),"stars"),b((11,8,6,16,9,10),"stars")]),
    ("formation_pillar","\u9547\u706b\u9635\u67f1",
     base()+[b((5,4,5,11,14,11),"meridian"),b((4,14,4,12,16,12),"core_seal")]),
    ("scripture_stele","\u91d1\u4e39\u7ecf\u7891",
     [b((1,0,2,15,2,14),"jade"),b((3,2,5,13,15,11),"script"),b((4,15,5,12,16,11),"copper")]),
    ("astrolabe","\u5468\u5929\u4e39\u4eea",
     base()+[b((7,4,7,9,12,9),"gold")]+rim(12,1,0,2,"gold")+[b((5,13,5,11,14,11),"cycle")]),
    ("ritual_compass","\u4e39\u706b\u7f57\u76d8",
     [b((2,0,2,14,2,14),"wood"),b((3,2,3,13,3,13),"bagua"),b((7,3,4,9,4,12),"gold"),b((4,3,7,12,4,9),"copper")]),
],light=3)

group("lights", [
    ("red_lantern","\u4e39\u5bab\u7ea2\u706f\u7b3c",
     [b((7,14,7,9,16,9),"copper"),b((3,12,3,13,14,13),"gold"),b((2,4,2,14,12,14),"lantern"),
      b((3,2,3,13,4,13),"gold"),b((7,0,7,9,2,9),"silk")]),
    ("copper_lantern","\u516d\u89d2\u94dc\u706f",
     [b((4,0,4,12,2,12),"copper"),b((3,2,3,13,11,13),"gold_fire"),b((2,11,2,14,13,14),"copper"),b((6,13,6,10,16,10),"copper")]+
     [b((x,2,z,x+1,11,z+1),"copper") for x in (2,13) for z in (2,13)]),
    ("lotus_fire_lamp","\u91d1\u83b2\u4e39\u706f",
     base("jade")+[b((5,4,5,11,11,11),"gold_fire"),b((2,4,5,5,7,11),"gold"),b((11,4,5,14,7,11),"gold"),
                   b((5,4,2,11,7,5),"gold"),b((5,4,11,11,7,14),"gold")]),
    ("azure_lamp","\u9752\u708e\u7389\u706f",
     base("jade")+[b((6,4,6,10,8,10),"jade"),b((4,8,4,12,11,12),"copper"),b((6,11,6,10,16,10),"blue_fire")]),
    ("ember_lamp","\u70ec\u706b\u89c2\u7089\u706f",
     [b((2,0,2,14,3,14),"iron"),b((5,3,5,11,9,11),"orange_fire"),b((4,9,4,12,11,12),"grate"),b((7,11,7,9,14,9),"copper")]),
    ("wall_sconce","\u8d64\u94dc\u58c1\u706f",
     [b((5,2,13,11,15,16),"copper"),b((6,5,5,10,7,13),"copper"),b((3,7,3,13,9,11),"gold"),b((6,9,6,10,15,10),"orange_fire")]),
    ("hanging_lamp","\u4e39\u6bbf\u5782\u706f",
     [b((7,12,7,9,16,9),"iron"),b((1,10,1,15,12,15),"gold"),b((3,3,3,13,10,13),"lantern"),b((2,1,2,14,3,14),"copper")]),
    ("sun_lamp","\u65e5\u8f6e\u94dc\u706f",
     [b((3,0,5,13,2,11),"copper"),b((7,2,7,9,6,9),"copper"),b((2,6,6,14,14,10),"sun"),b((4,14,6,12,16,10),"gold")]),
    ("moon_lamp","\u6708\u534e\u51dd\u5149\u706f",
     base("jade")+[b((5,4,6,7,14,10),"jade"),b((7,13,6,12,15,10),"jade"),b((7,4,6,12,6,10),"jade"),b((9,7,7,12,11,10),"blue_fire")]),
    ("pillar_lamp","\u62a4\u706b\u67f1\u706f",
     [b((1,0,1,15,2,15),"basalt"),b((5,2,5,11,10,11),"copper"),b((3,10,3,13,12,13),"gold"),b((5,12,5,11,16,11),"gold_fire")]),
    ("pagoda_lamp","\u4e39\u4ead\u5854\u706f",
     base()+[b((5,4,5,11,8,11),"jade"),b((3,8,3,13,12,13),"gold_fire"),b((1,12,1,15,14,15),"roof"),b((5,14,5,11,16,11),"roof")]),
    ("ring_lamp","\u73af\u706b\u4e39\u706f",
     base("iron")+rim(6,2,1,4,"gold")+[b((7,4,7,9,6,9),"copper"),b((5,8,5,11,12,11),"gold_fire")]),
    ("violet_lamp","\u7d2b\u971e\u7075\u706f",
     [b((3,0,3,13,2,13),"obsidian"),b((6,2,6,10,5,10),"copper"),b((4,5,4,12,12,12),"violet_fire"),b((6,12,6,10,15,10),"stars")]),
    ("altar_lamp","\u7ed3\u4e39\u796d\u706f",
     [b((0,0,0,16,2,16),"jade"),b((2,2,2,14,4,14),"core_seal"),b((4,4,4,12,6,12),"gold"),b((7,6,7,9,13,9),"gold_fire")]),
    ("copper_candle","\u94dc\u53f0\u957f\u660e\u70db",
     base("copper")+[b((7,4,7,9,10,9),"copper"),b((4,10,4,12,11,12),"gold"),b((6,11,6,10,14,10),"wax"),b((7,14,7,9,16,9),"gold_fire")]),
    ("triple_candle","\u4e09\u624d\u70db\u67b6",
     [b((4,0,4,12,2,12),"copper"),b((7,2,7,9,8,9),"copper"),b((2,8,7,14,10,9),"copper")]+
     [b((x,10,7,x+2,14,9),"wax") for x in (2,7,12)]+[b((x,14,7,x+2,16,9),"gold_fire") for x in (2,7,12)]),
    ("floor_light","\u91d1\u7eb9\u5d4c\u5730\u706f",
     [b((0,0,0,16,1,16),"copper"),b((2,1,2,14,2,14),"gold_fire"),b((7,2,2,9,3,14),"gold"),b((2,2,7,14,3,9),"gold")]),
    ("bridge_beacon","\u4ed9\u6865\u5f15\u8def\u706f",
     [b((1,0,1,15,3,15),"jade"),b((5,3,5,11,11,11),"scale"),b((4,11,4,12,13,12),"copper"),b((6,13,6,10,16,10),"blue_fire")]),
    ("skyfire_lamp","\u5929\u706b\u91d1\u706f",
     base("gold")+[b((6,4,6,10,9,10),"sun"),b((4,9,4,12,12,12),"gold"),b((6,12,6,10,16,10),"gold_fire")]),
    ("medicine_lamp","\u836f\u9999\u6696\u706f",
     [b((2,0,2,14,2,14),"porcelain"),b((4,2,4,12,8,12),"celadon"),b((3,8,3,13,10,13),"gold"),b((6,10,6,10,13,10),"orange_fire")]),
],light=15)

group("furniture", [
    ("pill_shelf","\u4e39\u74f6\u9648\u5217\u67b6",
     [b((0,0,2,2,16,15),"wood"),b((14,0,2,16,16,15),"wood"),b((2,0,13,14,16,15),"wood")]+
     [b((2,y,2,14,y+1,13),"gold") for y in (0,7,15)]+
     [b((x,y,5,x+3,y+4,8),"celadon") for x in (3,8) for y in (1,8)]),
    ("pill_cabinet","\u4e39\u836f\u73cd\u85cf\u67dc",
     [b((1,0,2,15,14,15),"wood"),b((0,14,1,16,16,16),"gold"),b((2,2,1,7,13,2),"core_seal"),
      b((9,2,1,14,13,2),"cloud"),b((6,7,0,7,9,1),"gold"),b((9,7,0,10,9,1),"gold")]),
    ("herb_cabinet","\u767e\u8349\u836f\u6597\u67dc",
     [b((0,0,3,16,16,15),"wood")]+[b((x,y,2,x+3,y+3,3),"herb") for x in (1,5,9,13) for y in (1,5,9,13)]+
     [b((x+1,y+1,1,x+2,y+2,2),"gold") for x in (1,5,9,13) for y in (1,5,9,13)]),
    ("herb_table","\u7075\u836f\u62e3\u9009\u6848",
     desk()+[b((2,9,3,7,10,11),"herb"),b((9,9,3,14,10,8),"bamboo"),b((10,10,4,13,12,7),"herb")]),
    ("grinding_table","\u7075\u836f\u78be\u53f0",
     desk()+[b((4,9,4,12,11,12),"basalt"),b((6,11,3,10,15,13),"jade"),b((2,12,7,14,13,9),"copper")]),
    ("mortar","\u7389\u6775\u836f\u81fc",
     [b((3,0,3,13,2,13),"jade")]+rim(2,5,3,5,"jade")+[b((9,3,8,11,13,10),"jade")]),
    ("bottle_rack","\u5c0f\u4e39\u74f6\u67b6",
     [b((1,0,3,15,2,13),"wood"),b((1,2,11,15,8,13),"wood"),b((3,2,5,6,7,8),"celadon"),
      b((9,2,5,12,9,8),"porcelain"),b((4,7,6,5,9,7),"cork"),b((10,9,6,11,11,7),"cork")]),
    ("pill_vial","\u9752\u7389\u4e39\u74f6",
     [b((5,0,5,10,6,10),"celadon"),b((6,6,6,9,9,9),"celadon"),b((6,9,6,9,10,9),"cork")]),
    ("spirit_water_jar","\u7075\u6cc9\u50a8\u6c34\u7f38",jar()+[b((6,13,6,10,13.5,10),"water")]),
    ("copper_basin","\u51c0\u624b\u8d64\u94dc\u76c6",
     [b((2,0,2,14,2,14),"copper")]+rim(2,4,1,3)+[b((3,2,3,13,3,13),"water")]),
    ("tea_table","\u89c2\u706b\u6e05\u8336\u6848",
     desk()+[b((6,9,5,10,12,9),"porcelain"),b((10,10,6,13,11,8),"porcelain"),
             b((2,9,3,5,10,6),"celadon"),b((11,9,10,14,10,13),"celadon")]),
    ("meditation_mat","\u51dd\u4e39\u84b2\u56e2",
     [b((2,0,2,14,1,14),"bamboo"),b((3,1,3,13,3,13),"silk"),b((5,3,5,11,4,11),"core_seal")]),
    ("robe_stand","\u4e39\u5e08\u8863\u67b6",
     [b((3,0,3,13,2,13),"wood"),b((7,2,7,9,16,9),"wood"),b((2,13,7,14,15,9),"gold"),b((3,5,6,13,13,7),"silk")]),
    ("recipe_desk","\u4e39\u65b9\u8a8a\u5f55\u6848",
     desk()+[b((2,9,3,11,9.5,12),"paper"),b((12,9,4,14,11,7),"iron"),b((3,9.5,4,4,10,11),"copper")]),
    ("recipe_stand","\u4e39\u7ecf\u8bb2\u67b6",
     [b((3,0,3,13,2,13),"wood"),b((6,2,6,10,9,10),"wood"),b((1,9,2,15,11,14),"gold"),b((3,11,4,13,12,12),"paper")]),
    ("hourglass","\u706b\u5019\u6f0f\u523b",
     [b((3,0,3,13,2,13),"copper"),b((3,14,3,13,16,13),"copper"),b((5,2,5,11,6,11),"sand"),
      b((7,6,7,9,10,9),"jade"),b((5,10,5,11,14,11),"jade")]+
     [b((x,2,z,x+1,14,z+1),"copper") for x in (3,12) for z in (3,12)]),
    ("ritual_bell","\u8d77\u7089\u793c\u949f",
     [b((4,0,4,12,2,12),"copper"),b((2,2,2,14,10,14),"copper"),b((4,10,4,12,13,12),"gold"),b((7,13,7,9,16,9),"copper")]),
    ("ritual_banner","\u4e39\u706b\u4eea\u65d7",
     [b((3,0,5,13,2,11),"basalt"),b((7,2,7,9,16,9),"wood"),b((2,14,6,14,16,10),"gold"),b((3,4,6,13,14,7),"flame")]),
    ("incense_burner","\u4e39\u9999\u94dc\u718f\u7089",
     [b((3,0,3,5,3,5),"copper"),b((11,0,3,13,3,5),"copper"),b((7,0,11,9,3,13),"copper"),
      b((3,3,3,13,7,13),"copper"),b((4,7,4,12,10,12),"grate"),b((7,10,7,9,12,9),"gold")]),
    ("ash_bucket","\u6e05\u7089\u7070\u6876",
     [b((4,0,4,12,2,12),"iron")]+rim(2,8,3,5,"iron")+
     [b((5,2,5,11,5,11),"ash"),b((4,10,7,5,15,9),"copper"),b((11,10,7,12,15,9),"copper"),b((5,14,7,11,15,9),"copper")]),
    ("tool_rack","\u94b3\u52fa\u7089\u5177\u67b6",
     [b((2,0,4,4,16,12),"wood"),b((12,0,4,14,16,12),"wood"),b((2,13,6,14,15,10),"copper"),
      b((5,5,7,6,13,8),"iron"),b((8,4,7,9,13,8),"iron"),b((10,7,6,13,9,9),"copper"),b((11,9,7,12,13,8),"copper")]),
    ("sealed_jar","\u5c01\u7b26\u836f\u7f50",jar()+[b((4,14,4,12,16,12),"cork"),b((7,5,2,9,12,3),"talisman")]),
    ("pill_display","\u91d1\u4e39\u73cd\u73e0\u76d8",
     [b((2,0,2,14,2,14),"jade"),b((3,2,3,13,3,13),"gold")]+[b((x,3,z,x+2,5,z+2),"pill") for x,z in ((4,4),(9,4),(6,9))]),
    ("herb_drying_rack","\u7075\u8349\u667e\u67b6",
     legs(15,"bamboo")+[b((1,y,2,15,y+1,14),"bamboo") for y in (4,9,14)]+
     [b((x,y,4,x+4,y+1,12),"herb") for x in (3,9) for y in (5,10,15)]),
],material="wood")

group("landscape", [
    ("fire_pool","\u4e39\u706b\u89c2\u6c60",
     [b((0,0,0,16,2,16),"obsidian")]+rim(2,3,0,3)+[b((3,2,3,13,3,13),"orange_fire")]),
    ("spirit_spring","\u51dd\u4e39\u7075\u6cc9",
     [b((0,0,0,16,2,16),"jade")]+rim(2,3,0,2,"jade")+[b((2,2,2,14,3,14),"water"),b((6,3,6,10,9,10),"jade")]),
    ("red_pine_bonsai","\u8d64\u677e\u76c6\u666f",
     [b((2,0,3,14,3,13),"celadon"),b((3,3,4,13,4,12),"ash"),b((7,4,7,9,11,9),"wood"),
      b((4,8,7,8,10,9),"wood"),b((2,10,4,8,12,11),"leaf"),b((7,13,4,14,15,11),"leaf"),b((8,11,7,10,14,9),"wood")]),
    ("flame_bamboo","\u706b\u7af9\u7075\u76c6",
     [b((2,0,2,14,4,14),"porcelain")]+[b((x,4,z,x+1,16,z+1),"bamboo") for x,z in ((4,5),(8,8),(11,4))]+
     [b((2,10,4,7,11,5),"leaf"),b((8,13,7,13,14,8),"leaf")]),
    ("lotus_basin","\u91d1\u83b2\u51c0\u6c60",
     [b((1,0,1,15,2,15),"jade")]+rim(2,4,0,2,"jade")+
     [b((2,2,2,14,3,14),"water"),b((5,3,5,11,4,11),"leaf"),b((6,4,6,10,6,10),"gold"),b((7,6,7,9,8,9),"gold_fire")]),
    ("mountain_stele","\u4e39\u5c71\u9898\u540d\u7891",
     [b((0,0,0,16,3,16),"basalt"),b((2,3,4,14,13,12),"basalt"),b((4,13,5,12,16,11),"basalt"),b((3,5,3,13,12,4),"script")]),
    ("alchemy_well","\u4e39\u6cc9\u7389\u4e95",
     [b((0,0,0,16,2,16),"paver")]+rim(2,7,1,4,"jade")+
     [b((4,3,4,12,4,12),"water"),b((2,9,7,3,15,9),"copper"),b((13,9,7,14,15,9),"copper"),b((2,14,7,14,16,9),"copper")]),
    ("guardian_lion","\u9547\u7089\u94dc\u72ee",
     base()+[b((4,4,6,12,10,13),"copper"),b((3,4,2,6,8,6),"copper"),b((10,4,2,13,8,6),"copper"),
             b((3,10,3,13,15,12),"scale"),b((5,10,1,11,13,4),"copper"),b((3,14,4,5,16,6),"gold"),b((11,14,4,13,16,6),"gold")]),
    ("tribulation_obelisk","\u96f7\u52ab\u9547\u5c71\u67f1",
     [b((1,0,1,15,2,15),"obsidian"),b((4,2,4,12,5,12),"obsidian"),b((5,5,5,11,14,11),"tribulation"),b((7,14,7,9,16,9),"violet_fire")]),
    ("cloud_incense_vessel","\u4e91\u9999\u5ead\u9f0e",tripod()+[b((5,14,5,11,15,11),"grate"),b((7,15,7,9,16,9),"copper")]),
    ("herb_planter","\u7075\u836f\u57f9\u5143\u76c6",
     [b((1,0,3,15,4,13),"celadon"),b((2,4,4,14,5,12),"ash")]+
     [b((x,5,6,x+1,12,7),"bamboo") for x in (4,8,11)]+[b((x-1,9,5,x+2,11,8),"herb") for x in (4,8,11)]),
    ("copper_bridge_step","\u4ed9\u6865\u94dc\u9636",
     [b((0,0,0,16,4,16),"basalt"),b((0,4,8,16,8,16),"basalt"),b((0,8,12,16,12,16),"basalt"),
      b((0,4,0,16,5,2),"gold"),b((0,8,8,16,9,10),"gold")]),
])

# Explicit properties for decorative ceramic/metal furniture and luminous fire.
for name in ("mortar","pill_vial","spirit_water_jar","copper_basin","ritual_bell",
             "incense_burner","ash_bucket","sealed_jar","pill_display"):
    zh,en,_,light,geometry=DEFINITIONS["jindan_"+name]
    DEFINITIONS["jindan_"+name]=(zh,en,"stone",light,geometry)
for name, emission in {"furnace_mouth":12,"fire_channel":8,"fire_channel_corner":8,
                       "fire_vent":15,"blue_flame_basin":15,"core_orb":15,
                       "core_pedestal":12,"solar_halo_segment":15,"violet_halo_segment":12,
                       "fire_pool":12,"tribulation_obelisk":5}.items():
    zh,en,material,_,geometry=DEFINITIONS["jindan_"+name]
    DEFINITIONS["jindan_"+name]=(zh,en,material,emission,geometry)

PALETTE = {
    "cinnabar":((141,49,44),"brick"), "ash_brick":((104,106,101),"brick"),
    "scorched":((54,56,54),"cracks"), "copper":((161,95,66),"metal"),
    "gold":((200,158,66),"cloud"), "court":((63,70,73),"paver"),
    "fire_vein":((75,68,64),"vein"), "marble":((157,118,100),"marble"),
    "flame":((127,38,40),"flame"), "iron":((48,56,62),"metal"),
    "obsidian":((48,42,52),"cracks"), "slag":((81,83,79),"cracks"),
    "basalt":((73,80,80),"basalt"), "bagua":((78,79,77),"bagua"),
    "sun":((123,52,40),"sun"), "moon":((210,224,216),"moon"),
    "tribulation":((68,58,87),"lightning"), "core_seal":((103,32,40),"core"),
    "grate":((101,79,60),"grate"), "paver":((157,169,163),"paver"),
    "jade":((217,230,220),"marble"), "roof":((123,45,43),"roof"),
    "scale":((88,122,108),"scale"), "stars":((85,61,107),"stars"),
    "cloud":((82,114,103),"cloud"), "wood":((64,43,47),"wood"),
    "lacquer":((130,34,36),"wood"), "phoenix":((155,61,47),"phoenix"),
    "script":((178,156,105),"script"), "orange_fire":((246,144,63),"fire"),
    "blue_fire":((115,205,211),"fire"), "gold_fire":((255,220,124),"fire"),
    "violet_fire":((175,127,205),"fire"), "core":((244,188,72),"core"),
    "five_elements":((227,226,201),"five_elements"), "nine_flames":((89,46,52),"nine_flames"),
    "meridian":((67,89,85),"meridian"), "cycle":((77,64,94),"cycle"),
    "lantern":((192,49,43),"lantern"), "silk":((118,47,51),"silk"),
    "wax":((231,216,174),"speckle"), "herb":((75,124,79),"herb"),
    "porcelain":((226,234,227),"porcelain"), "celadon":((127,181,172),"porcelain"),
    "cork":((129,96,66),"speckle"), "water":((74,143,159),"water"),
    "paper":((236,226,195),"script"), "sand":((193,180,111),"speckle"),
    "ash":((98,96,88),"speckle"), "guard":((56,68,72),"guard"),
    "talisman":((238,211,151),"talisman"), "pill":((240,210,100),"pill"),
    "bamboo":((132,149,85),"bamboo"), "leaf":((65,119,95),"leaf"),
}


def generate_textures(assets):
    import math
    target=assets/"textures/block"
    target.mkdir(parents=True,exist_ok=True)
    gold,ink=(237,198,102),(53,54,54)
    for name,(color,pattern) in PALETTE.items():
        rng=random.Random("jindan:"+name)
        image=Image.new("RGB",(32,32))
        for y in range(32):
            for x in range(32):
                n=rng.randrange(-6,7)
                image.putpixel((x,y),tuple(max(0,min(255,c+n)) for c in color))
        draw=ImageDraw.Draw(image)
        dark=tuple(max(0,c-27) for c in color)
        pale=tuple(min(255,c+32) for c in color)
        if pattern in ("brick","paver"):
            spacing=8 if pattern=="brick" else 16
            for y in range(0,32,spacing):
                draw.line((0,y,31,y),fill=dark)
                for x in range((y//spacing%2)*8,32,16):
                    draw.line((x,y,x,y+spacing-1),fill=dark)
        elif pattern in ("cracks","vein","marble"):
            for y in (3,14,25):
                draw.line((0,y,7,y+3,16,y-2,23,y+1,31,y-1),
                          fill=(237,147,75) if pattern=="vein" else pale)
            if pattern=="cracks": draw.line((13,0,16,8,11,15,14,24,9,31),fill=dark)
        elif pattern=="basalt":
            for x in (1,8,17,26):
                draw.line((x,0,x,31),fill=dark,width=2)
                draw.line((x+2,0,x+2,31),fill=pale)
        elif pattern=="metal":
            draw.rectangle((1,1,30,30),outline=dark)
            for x in (3,27):
                for y in (3,27): draw.rectangle((x,y,x+1,y+1),fill=pale)
        elif pattern in ("wood","bamboo","roof"):
            for x in range(2,32,5 if pattern=="wood" else 8):
                draw.line((x,0,x,31),fill=dark,width=2)
                draw.line((x+2,0,x+2,31),fill=pale)
            if pattern=="roof":
                for y in (0,15,31): draw.line((0,y,31,y),fill=gold)
            elif pattern=="bamboo":
                for y in (7,21): draw.line((0,y,31,y),fill=dark,width=2)
        elif pattern=="grate":
            for x in range(2,32,6): draw.line((x,0,x,31),fill=ink,width=2)
            for y in range(2,32,8): draw.line((0,y,31,y),fill=gold)
        elif pattern in ("cloud","scale"):
            for y in (2,12,22):
                for x in (-3,9,21):
                    draw.arc((x,y,x+13,y+11),0 if pattern=="scale" else 25,310,fill=gold)
        elif pattern in ("fire","flame","nine_flames"):
            count=9 if pattern=="nine_flames" else 3
            for i in range(count):
                x,y=(i%3)*10+1,(i//3)*10 if count==9 else 2
                h=9 if count==9 else 26
                draw.polygon([(x,y+h),(x+6,y+h),(x+9,y+h-6),(x+5,y+3),(x+4,y),(x+3,y+h-6)],fill=pale)
                draw.line((x+4,y+h-2,x+5,y+h-7),fill=gold,width=2)
        elif pattern in ("sun","moon","core","bagua","cycle"):
            draw.ellipse((5,5,26,26),outline=gold,width=2)
            if pattern=="moon":
                draw.ellipse((9,8,22,23),fill=ink)
                draw.ellipse((13,6,24,20),fill=color)
            elif pattern=="bagua":
                for i in range(8):
                    a=i*math.pi/4
                    x,y=16+12*math.cos(a),16+12*math.sin(a)
                    draw.line((x-2,y,x+2,y),fill=gold,width=2)
                draw.ellipse((11,11,20,20),fill=ink)
                draw.pieslice((11,11,20,20),90,270,fill=gold)
            elif pattern=="cycle":
                for i in range(12):
                    a=i*math.pi/6
                    x,y=16+12*math.cos(a),16+12*math.sin(a)
                    draw.rectangle((x-1,y-1,x+1,y+1),fill=gold)
                draw.arc((10,10,22,22),30,300,fill=gold,width=2)
            else:
                draw.ellipse((10,10,21,21),fill=gold)
                if pattern=="sun":
                    for i in range(8):
                        a=i*math.pi/4
                        draw.line((16+10*math.cos(a),16+10*math.sin(a),16+15*math.cos(a),16+15*math.sin(a)),fill=gold)
        elif pattern=="five_elements":
            colors=[(66,131,103),(196,57,42),(207,160,68),(150,177,181),(57,103,148)]
            points=[(16,4),(27,12),(23,26),(8,26),(4,12)]
            draw.line(points+[points[0]],fill=dark)
            for (x,y),c in zip(points,colors): draw.ellipse((x-3,y-3,x+3,y+3),fill=c)
        elif pattern=="lightning":
            draw.line((22,2,12,11,21,13,8,29),fill=(220,185,246),width=2)
            draw.line((2,23,7,19,12,22,17,19),fill=gold)
        elif pattern in ("meridian","guard"):
            for x in (5,25): draw.line((x,0,x,31),fill=gold,width=2)
            draw.line((15,0,15,10,10,15,20,20,15,25,15,31),fill=gold,width=2)
            if pattern=="guard": draw.rectangle((9,6,22,25),outline=gold)
        elif pattern=="stars":
            points=[(4,10),(12,5),(18,13),(26,8),(23,25),(10,27)]
            draw.line(points,fill=(156,137,190))
            for x,y in points: draw.rectangle((x-1,y-1,x+1,y+1),fill=gold)
        elif pattern in ("script","talisman"):
            draw.rectangle((1,1,30,30),outline=gold)
            for x in (7,15,23):
                for y in (5,13,21):
                    draw.line((x-2,y,x+2,y,x,y+3,x+2,y+5,x-2,y+5),
                              fill=ink if pattern=="script" else (178,42,34))
        elif pattern in ("porcelain","water"):
            for y in (3,26): draw.line((0,y,31,y),fill=(60,105,140),width=2)
            for x in (1,17): draw.arc((x,8,x+13,24),10,300,fill=(62,117,143),width=2)
        elif pattern=="lantern":
            for x in (3,11,19,27): draw.line((x,0,x,31),fill=pale,width=2)
            for y in (0,29): draw.rectangle((0,y,31,y+2),fill=gold)
            draw.ellipse((10,10,22,22),outline=gold,width=2)
        elif pattern=="phoenix":
            draw.line((3,24,10,17,17,12,23,6,28,10),fill=gold,width=3)
            for y in (16,21,26): draw.line((15,15,6,y,2,y),fill=gold)
        elif pattern in ("herb","leaf"):
            for x in (6,16,25):
                draw.line((x,4,x-1,29),fill=pale)
                for y in (9,19): draw.line((x,y,x+4,y-4,x+2,y),fill=pale,width=2)
        elif pattern=="silk":
            for y in range(0,32,4): draw.line((0,y,31,y),fill=pale)
            draw.rectangle((2,2,29,29),outline=gold)
        elif pattern=="speckle":
            for i in range(28): draw.point((rng.randrange(32),rng.randrange(32)),fill=pale)
        elif pattern=="pill":
            for x,y in ((7,7),(23,10),(15,23)):
                draw.ellipse((x-3,y-3,x+3,y+3),fill=gold)
                draw.point((x-1,y-1),fill=(255,244,207))
        else:
            raise ValueError(pattern)
        image.save(target/f"oriental_jindan_{name}.png")


def validate():
    assert len(DEFINITIONS)==136, len(DEFINITIONS)
    signatures=[]
    for key,(zh,en,material,light,geometry) in DEFINITIONS.items():
        assert material in ("stone","wood") and 0<=light<=15
        assert "\\u" not in zh and "\ufffd" not in zh
        textures={geometry} if isinstance(geometry,str) else {tex for _,tex in geometry}
        assert all(t.removeprefix("jindan_") in PALETTE for t in textures),key
        if isinstance(geometry,list):
            for coords,_ in geometry:
                assert all(0<=a<c<=16 for a,c in zip(coords[:3],coords[3:])),(key,coords)
        signatures.append(hashlib.sha256(json.dumps(geometry,sort_keys=True).encode()).hexdigest())
    assert len(set(signatures))==len(signatures),"Duplicate Jin-Dan models"
    assert len({v[0] for v in DEFINITIONS.values()})==len(DEFINITIONS),"Duplicate labels"
    assert all(v[3]>=14 for k,v in DEFINITIONS.items() if CATEGORIES[k]=="lights")
    return dict(Counter(CATEGORIES.values()))


if __name__=="__main__":
    print("PASS:",len(DEFINITIONS),"distinct Jin-Dan assets;",validate())
