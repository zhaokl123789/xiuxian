"""Original cultivation architecture blocks and the Dao-Tai residence library.

Geometry is authored in Minecraft's 0..16 block coordinates. Generated models
and collision boxes use the same parts; everything is static decoration. The
legacy 56-block set is shared by existing buildings, while the generated
``daotai_*`` set is reserved for the future Dao-Tai residence.
"""
import random
from PIL import Image, ImageDraw


def part(coords, texture):
    return (list(coords), texture)


def box(x1, y1, z1, x2, y2, z2, tex):
    return part((x1, y1, z1, x2, y2, z2), tex)


def feet(y=7, tex='rosewood'):
    return [box(x, 0, z, x+2, y, z+2, tex) for x in (2, 12) for z in (2, 12)]


def tabletop(tex='rosewood'):
    return feet() + [box(0, 7, 1, 16, 9, 15, tex)]


def plinth(tex='jade'):
    return [box(2, 0, 2, 14, 2, 14, tex), box(4, 2, 4, 12, 4, 12, tex)]


def vase(tex='porcelain'):
    return [box(5,0,5,11,2,11,tex), box(3,2,3,13,8,13,tex),
            box(4,8,4,12,11,12,tex), box(6,11,6,10,14,10,tex),
            box(5,14,5,11,16,11,tex)]


def window(kind):
    result = [box(0,0,6,2,16,10,'rosewood'), box(14,0,6,16,16,10,'rosewood'),
              box(2,0,6,14,2,10,'gold'), box(2,14,6,14,16,10,'gold')]
    if kind == 'moon':
        for x,y,w in [(4,3,8),(3,5,10),(2,7,12),(3,10,10),(4,12,8)]:
            result.append(box(x,y,7,x+w,y+1,9,'jade'))
        result += [box(7,2,7,9,14,9,'gold')]
    elif kind == 'bamboo':
        for x in (4,8,12):
            result += [box(x,2,7,x+1,14,9,'bamboo'), box(x-1,5,7,x+2,6,9,'bamboo'),
                       box(x-1,10,7,x+2,11,9,'bamboo')]
    else:
        for x,y in ((4,4),(9,4),(6,8),(3,11),(10,11)):
            result += [box(x,y+1,7,x+3,y+2,9,'sunset'), box(x+1,y,7,x+2,y+3,9,'sunset')]
        result += [box(7,2,8,8,14,9,'rosewood')]
    return result


def lantern(tex, shape):
    if shape == 'palace':
        return [box(7,14,7,9,16,9,'gold'), box(2,12,2,14,14,14,'gold'),
                box(3,4,3,13,12,13,tex), box(2,2,2,14,4,14,'gold'),
                box(5,0,5,6,2,6,'vermilion'), box(10,0,10,11,2,11,'vermilion')]
    if shape == 'lotus':
        return plinth() + [box(5,4,5,11,10,11,tex),
            box(2,4,5,5,7,11,'sunset'), box(11,4,5,14,7,11,'sunset'),
            box(5,4,2,11,7,5,'sunset'), box(5,4,11,11,7,14,'sunset'),
            box(4,10,4,12,12,12,'gold')]
    return plinth('bronze') + [box(6,4,6,10,7,10,'bronze'),
        box(3,7,3,13,12,13,tex), box(2,12,2,14,14,14,'roof_blue'),
        box(5,14,5,11,16,11,'gold')]


# Eight textured structural materials, forty-eight sculpted furnishings/details.
DEFINITIONS = {
    'xian_sunset_marble': ('霞纹灵玉石', 'Sunset Veined Spirit Marble', 'stone', 0, 'sunset'),
    'xian_lotus_floor': ('莲纹玉地砖', 'Lotus Jade Floor Tile', 'stone', 0, 'lotus'),
    'xian_star_inlay': ('星宿嵌金砖', 'Gilded Constellation Tile', 'stone', 0, 'stars'),
    'xian_taiji_tile': ('阴阳太极砖', 'Yin Yang Taiji Tile', 'stone', 0, 'taiji'),
    'xian_cloud_carved_stone': ('浮雕流云石', 'Cloud Relief Stone', 'stone', 0, 'cloud_stone'),
    'xian_dragon_relief': ('蟠龙照壁砖', 'Coiling Dragon Relief', 'stone', 0, 'dragon'),
    'xian_bamboo_wall': ('竹影粉壁', 'Bamboo Silhouette Plaster', 'stone', 0, 'bamboo_wall'),
    'xian_cinnabar_rune_brick': ('朱砂符纹砖', 'Cinnabar Rune Brick', 'stone', 0, 'runes'),
    'xian_lotus_column_base': ('覆莲柱础', 'Lotus Column Base', 'stone', 0,
        plinth('cloud_stone') + [box(2,4,5,14,6,11,'jade'),box(5,4,2,11,6,14,'jade'),box(4,6,4,12,16,12,'sunset')]),
    'xian_jade_column_cap': ('承云柱头', 'Cloud Bearing Capital', 'stone', 0,
        [box(5,0,5,11,4,11,'jade'),box(3,4,3,13,7,13,'cloud_stone'),
         box(0,7,0,16,10,16,'sunset'),box(1,10,1,15,16,15,'gold')]),
    'xian_sunset_eave': ('落霞彩绘檐枋', 'Sunset Painted Eave Beam', 'wood', 0,
        [box(0,3,3,16,9,13,'vermilion'),box(0,9,2,16,12,14,'sunset'),
         box(0,12,0,16,15,16,'roof_red'),box(0,2,2,16,3,3,'gold')]),
    'xian_phoenix_ridge': ('凤翎琉璃脊', 'Phoenix Feather Roof Crest', 'stone', 0,
        [box(0,0,4,16,3,12,'roof_red'),box(0,3,6,16,6,10,'gold')]
        + [box(x,6,7,x+2,9+x/2,9,'sunset') for x in (1,4,7,10,13)]),
    'xian_bronze_corner_beast': ('青铜镇檐兽', 'Bronze Eave Guardian', 'stone', 0,
        plinth('roof_blue') + [box(4,4,5,12,9,13,'bronze'),box(3,9,3,11,14,10,'bronze'),
        box(4,10,1,10,12,4,'gold'),box(3,14,4,5,16,6,'gold'),box(9,14,4,11,16,6,'gold'),
        box(11,7,12,13,13,14,'bronze')]),
    'xian_moon_window': ('冰裂月纹花窗', 'Moon Lattice Window', 'wood', 0, window('moon')),
    'xian_bamboo_window': ('翠竹花窗', 'Bamboo Lattice Window', 'wood', 0, window('bamboo')),
    'xian_plum_window': ('梅花漏窗', 'Plum Blossom Window', 'wood', 0, window('plum')),
    'xian_carved_door_panel': ('嵌金云纹门扇', 'Gilded Carved Door Panel', 'wood', 0,
        [box(0,0,6,16,16,10,'rosewood'),box(2,2,5,14,14,6,'cloud'),
         box(1,0,4,2,16,5,'gold'),box(14,0,4,15,16,5,'gold'),box(11,7,3,13,9,5,'bronze')]),
    'xian_cloud_transom': ('如意透雕楣', 'Openwork Cloud Transom', 'wood', 0,
        [box(0,1,6,16,3,10,'rosewood'),box(0,13,6,16,15,10,'rosewood')]
        + [box(x,3,7,x+2,13,9,'gold') for x in (0,7,14)]
        + [box(2,6,7,7,8,9,'sunset'),box(9,9,7,14,11,9,'sunset')]),
    'xian_palace_lantern': ('八宝宫灯', 'Eight Treasure Palace Lantern', 'wood', 15, lantern('lantern','palace')),
    'xian_lotus_lamp': ('莲台灵灯', 'Lotus Spirit Lamp', 'stone', 15, lantern('glow','lotus')),
    'xian_bronze_candle': ('青铜烛台', 'Bronze Candle Stand', 'stone', 14,
        plinth('bronze')+[box(7,4,7,9,10,9,'bronze'),box(3,10,5,13,11,11,'gold')]
        +[box(x,11,7,x+2,14,9,'plaster') for x in (3,7,11)]
        +[box(x,14,7,x+1,16,8,'glow') for x in (3,7,11)]),
    'xian_wall_sconce': ('云纹壁灯', 'Cloud Wall Sconce', 'stone', 15,
        [box(4,1,13,12,15,16,'cloud_stone'),box(7,5,6,9,7,14,'bronze'),
         box(3,7,3,13,9,12,'gold'),box(5,9,5,11,13,10,'glow'),box(3,13,3,13,15,12,'roof_red')]),
    'xian_spirit_crystal_lamp': ('灵晶镇灯', 'Spirit Crystal Lamp', 'stone', 15,
        plinth('bronze')+[box(5,4,5,11,7,11,'gold'),box(4,7,4,12,12,12,'crystal'),
         box(6,12,6,10,16,10,'crystal')]),
    'xian_incense_lamp': ('香云悬灯', 'Incense Cloud Hanging Lamp', 'stone', 15,
        [box(7,12,7,9,16,9,'bronze'),box(4,10,4,12,12,12,'gold'),
         box(3,4,3,13,10,13,'runes'),box(5,2,5,11,4,11,'glow'),box(7,0,7,9,2,9,'bronze')]),
    'xian_garden_lamp': ('竹影庭灯', 'Bamboo Garden Lamp', 'stone', 15, lantern('bamboo_wall','garden')),
    'xian_pagoda_lamp': ('宝塔长明灯', 'Pagoda Eternal Lamp', 'stone', 15,
        plinth('jade')+[box(5,4,5,11,8,11,'glow'),box(2,8,2,14,10,14,'roof_blue'),
         box(6,10,6,10,13,10,'glow'),box(4,13,4,12,15,12,'roof_blue'),box(7,15,7,9,16,9,'gold')]),
    'xian_bronze_incense_burner': ('三足云纹香炉', 'Tripod Cloud Incense Burner', 'stone', 0,
        [box(3,0,3,5,4,5,'bronze'),box(11,0,3,13,4,5,'bronze'),box(7,0,11,9,4,13,'bronze'),
         box(3,4,3,13,8,13,'bronze'),box(2,8,2,14,10,14,'gold'),box(5,10,5,11,12,11,'bronze'),
         box(7,12,7,9,15,9,'gold'),box(0,5,6,3,8,10,'bronze'),box(13,5,6,16,8,10,'bronze')]),
    'xian_alchemy_furnace': ('赤铜炼丹炉陈设', 'Copper Alchemy Furnace Decoration', 'stone', 7,
        feet(4,'bronze')+[box(2,4,2,14,11,14,'bronze'),box(3,11,3,13,13,13,'runes'),
         box(5,13,5,11,15,11,'bronze'),box(7,15,7,9,16,9,'gold'),
         box(5,5,1,11,9,2,'glow'),box(0,7,5,2,10,11,'gold'),box(14,7,5,16,10,11,'gold')]),
    'xian_spirit_cauldron': ('四象灵鼎陈设', 'Four Symbols Spirit Cauldron', 'stone', 0,
        feet(5,'bronze')+[box(2,5,2,14,6,14,'bronze'),box(2,6,2,4,12,14,'bronze'),
         box(12,6,2,14,12,14,'bronze'),box(4,6,2,12,12,4,'runes'),box(4,6,12,12,12,14,'runes'),
         box(0,10,5,2,15,11,'gold'),box(14,10,5,16,15,11,'gold')]),
    'xian_meditation_cushion': ('莲纹蒲团', 'Lotus Meditation Cushion', 'wood', 0,
        [box(3,0,1,13,3,15,'silk'),box(1,0,3,15,3,13,'silk'),box(4,3,4,12,4,12,'lotus')]),
    'xian_sword_rack': ('玄木藏剑架', 'Darkwood Sword Rack', 'wood', 0,
        [box(1,0,2,4,2,14,'rosewood'),box(12,0,2,15,2,14,'rosewood'),
         box(2,2,10,4,15,12,'rosewood'),box(12,2,10,14,15,12,'rosewood'),
         box(2,6,7,14,8,11,'gold'),box(2,12,7,14,14,11,'gold')]
        + [box(x,3,5,x+1,13,7,'steel') for x in (5,8,11)]
        + [box(x-1,12,4,x+2,13,7,'gold') for x in (5,8,11)]),
    'xian_talisman_desk': ('朱砂制符案', 'Cinnabar Talisman Desk', 'wood', 0,
        tabletop()+[box(3,9,4,10,9.5,12,'talisman'),box(12,9,5,14,11,7,'runes'),
        box(11,9,10,12,10,14,'bamboo')]),
    'xian_scroll_stand': ('玉轴经卷架', 'Jade Scroll Stand', 'wood', 0,
        [box(2,0,2,14,2,14,'rosewood'),box(6,2,6,10,9,10,'rosewood'),
         box(2,9,3,14,11,13,'gold'),box(3,11,4,13,12,12,'scrolls'),
         box(2,11,3,4,14,13,'jade'),box(12,11,3,14,14,13,'jade')]),
    'xian_astrolabe': ('周天星仪', 'Celestial Astrolabe', 'stone', 3,
        plinth('bronze')+[box(7,4,7,9,7,9,'bronze'),box(1,7,7,15,9,9,'gold'),
         box(7,7,1,9,9,15,'gold'),box(2,9,7,4,14,9,'gold'),box(12,9,7,14,14,9,'gold'),
         box(4,14,7,12,16,9,'gold'),box(6,9,6,10,13,10,'crystal')]),
    'xian_jade_orb_pedestal': ('灵珠承露台', 'Spirit Pearl Pedestal', 'stone', 9,
        plinth('cloud_stone')+[box(6,4,6,10,8,10,'jade'),box(3,8,3,13,10,13,'gold'),
         box(5,10,5,11,14,11,'crystal'),box(6,14,6,10,16,10,'crystal')]),
    'xian_chime_rack': ('金玉编磬', 'Jade Ritual Chime Rack', 'wood', 0,
        [box(1,0,2,3,15,14,'rosewood'),box(13,0,2,15,15,14,'rosewood'),
         box(0,14,5,16,16,11,'gold')]
        + [box(x,7+(x%3),7,x+2,13,9,'jade') for x in (3,6,9,12)]),
    'xian_sect_banner': ('落霞宗幡', 'Luoxia Sect Banner', 'wood', 0,
        plinth('jade')+[box(7,4,7,9,16,9,'bronze'),box(2,14,6,14,16,9,'gold'),
         box(3,5,6,13,14,7,'banner'),box(4,3,6,6,5,7,'vermilion'),box(10,3,6,12,5,7,'vermilion')]),
    'xian_ritual_bell': ('镇山悬钟', 'Mountain Ritual Bell', 'stone', 0,
        [box(1,0,5,3,16,11,'rosewood'),box(13,0,5,15,16,11,'rosewood'),
         box(1,14,5,15,16,11,'gold'),box(7,12,7,9,14,9,'bronze'),
         box(5,8,5,11,12,11,'bronze'),box(4,5,4,12,8,12,'bronze'),box(3,3,3,13,5,13,'gold')]),
    'xian_scripture_pedestal': ('传法玉座', 'Scripture Jade Pedestal', 'stone', 0,
        plinth('jade')+[box(5,4,5,11,9,11,'cloud_stone'),box(1,9,2,15,11,14,'jade'),
         box(3,11,4,13,12,12,'scrolls')]),
    'xian_spirit_tablet': ('仙祖灵牌', 'Ancestral Spirit Tablet', 'wood', 0,
        [box(2,0,4,14,2,12,'rosewood'),box(5,2,6,11,13,10,'runes'),
         box(6,13,6,10,16,10,'gold'),box(4,3,5,12,4,6,'gold')]),
    'xian_tea_table': ('青瓷茶席', 'Celadon Tea Table', 'wood', 0,
        tabletop()+[box(3,9,4,11,10,12,'bamboo'),box(5,10,6,8,13,9,'porcelain'),
         box(8,11,7,10,12,8,'porcelain'),box(3,10,10,5,12,12,'porcelain'),
         box(10,10,4,12,12,6,'porcelain')]),
    'xian_qin_table': ('七弦古琴案', 'Seven String Qin Table', 'wood', 0,
        tabletop()+[box(1,9,5,15,11,11,'rosewood')]
        +[box(2,11,z,14,11.25,z+.35,'gold') for z in (5.5,6.2,6.9,7.6,8.3,9,9.7)]),
    'xian_apothecary_drawers': ('百草药斗柜', 'Herbal Apothecary Drawers', 'wood', 0,
        [box(0,0,3,16,16,15,'rosewood')]
        +[box(x,y,2,x+3,y+3,3,'herbs') for x in (1,5,9,13) for y in (1,5,9,13)]),
    'xian_herb_drying_rack': ('灵药晾架', 'Spirit Herb Drying Rack', 'wood', 0,
        feet(15,'bamboo')+[box(1,y,2,15,y+1,14,'bamboo') for y in (4,9,14)]
        +[box(3,y,4,7,y+1,12,'herbs') for y in (5,10,15)]
        +[box(9,y,4,13,y+1,12,'leaves') for y in (5,10,15)]),
    'xian_porcelain_vase': ('青花长颈瓶', 'Blue and White Porcelain Vase', 'stone', 0, vase()),
    'xian_wine_jars': ('封泥酒坛', 'Sealed Wine Jars', 'stone', 0,
        [box(1,0,2,8,9,9,'pottery'),box(2,9,3,7,11,8,'vermilion'),
         box(9,0,7,15,7,13,'pottery'),box(10,7,8,14,9,12,'vermilion')]),
    'xian_rice_sacks': ('灵谷麻袋', 'Spirit Rice Sacks', 'wood', 0,
        [box(1,0,1,9,8,10,'burlap'),box(3,8,4,7,10,8,'bamboo'),
         box(8,0,7,16,6,15,'burlap'),box(10,6,9,14,8,13,'bamboo')]),
    'xian_fruit_basket': ('灵果竹筐', 'Spirit Fruit Basket', 'wood', 0,
        [box(2,0,2,14,5,14,'bamboo'),box(1,5,1,15,6,15,'bamboo')]
        +[box(x,6,z,x+3,9,z+3,'fruit') for x,z in ((3,3),(8,3),(3,8),(8,8))]),
    'xian_silk_display': ('锦缎货架', 'Brocade Display', 'wood', 0,
        [box(1,0,3,3,16,13,'rosewood'),box(13,0,3,15,16,13,'rosewood'),
         box(1,14,4,15,16,12,'gold'),box(3,3,4,7,14,6,'silk'),
         box(8,5,4,12,14,6,'banner'),box(2,1,3,14,3,13,'rosewood')]),
    'xian_abacus_counter': ('算盘账房柜', 'Abacus Accounting Counter', 'wood', 0,
        [box(1,0,2,15,10,14,'rosewood'),box(0,10,1,16,12,15,'cloud'),
         box(2,12,3,13,13,11,'rosewood')]
        +[box(x,13,z,x+1,14,z+2,'gold') for x in (3,5,7,9,11) for z in (4,8)]),
    'xian_writing_set': ('笔墨砚台', 'Brush and Ink Writing Set', 'wood', 0,
        [box(2,0,4,12,1,13,'scrolls'),box(10,1,5,15,3,10,'black_brick'),
         box(2,1,2,5,5,5,'porcelain'),box(3,5,3,4,11,4,'bamboo'),box(5,1,11,14,2,12,'bamboo')]),
    'xian_bamboo_screen': ('湘竹隔扇', 'Bamboo Partition Screen', 'wood', 0,
        [box(0,0,6,2,16,10,'rosewood'),box(14,0,6,16,16,10,'rosewood'),
         box(2,1,6,14,3,10,'rosewood'),box(2,14,6,14,16,10,'rosewood')]
        +[box(x,3,7,x+1,14,9,'bamboo') for x in range(3,14,2)]),
    'xian_lotus_basin': ('青莲水钵', 'Lotus Water Basin', 'stone', 0,
        [box(2,0,2,14,2,14,'cloud_stone'),box(1,2,1,3,6,15,'jade'),
         box(13,2,1,15,6,15,'jade'),box(3,2,1,13,6,3,'jade'),box(3,2,13,13,6,15,'jade'),
         box(3,3,3,13,4,13,'water_art'),box(6,4,6,10,5,10,'leaves'),box(7,5,7,9,7,9,'sunset')]),
    'xian_bonsai_pine': ('苍松盆景', 'Ancient Pine Bonsai', 'stone', 0,
        [box(2,0,3,14,3,13,'porcelain'),box(3,3,4,13,4,12,'rosewood'),
         box(7,4,7,9,11,9,'rosewood'),box(4,8,7,8,10,9,'rosewood'),
         box(2,10,4,8,12,11,'leaves'),box(7,13,4,14,15,11,'leaves'),box(8,11,7,10,14,9,'rosewood')]),
    'xian_crane_statue': ('白玉仙鹤', 'White Jade Crane', 'stone', 0,
        plinth('cloud_stone')+[box(5,4,8,6,9,9,'gold'),box(10,4,8,11,9,9,'gold'),
         box(4,9,5,12,12,13,'jade'),box(6,12,4,8,16,6,'jade'),
         box(6,14,1,8,15,4,'gold'),box(3,9,11,13,10,15,'jade')]),
    'xian_stone_stele': ('云篆石碑', 'Cloud Script Stele', 'stone', 0,
        plinth('cloud_stone')+[box(4,4,5,12,14,11,'runes'),box(5,14,5,11,16,11,'cloud_stone')]),
}


# Dao-Tai residence material library.  These are deliberately kept separate
# from the Luoxia and mortal-town sets so the future residence can be themed
# and validated without changing either existing building.
DAO_TAI_DEFINITIONS = {}
_dao_zh = "\u9053\u80ce"


def _dao_add(suffix, title, material, light, geometry, zh_title=None):
    label = zh_title or title
    DAO_TAI_DEFINITIONS["daotai_" + suffix] = (
        _dao_zh + label,
        "Dao-Tai " + title,
        material,
        light,
        geometry,
    )


def _dao_panel(texture, accent, inset=2):
    return [box(0, 0, 0, 16, 2, 16, texture),
            box(inset, 2, inset, 16-inset, 4, 16-inset, accent),
            box(4, 4, 4, 12, 5, 12, texture)]


def _dao_pillar(texture, accent, crown):
    return [box(3, 0, 3, 13, 2, 13, accent),
            box(5, 2, 5, 11, 13, 11, texture),
            box(3, 13, 3, 13, 15, 13, accent),
            box(1, 15, 1, 15, 16, 15, crown)]


def _dao_roof(texture, accent, finial):
    return [box(0, 0, 2, 16, 3, 14, texture),
            box(1, 3, 4, 15, 6, 12, accent),
            box(3, 6, 6, 13, 9, 10, texture),
            box(6, 9, 7, 10, 14, 9, finial),
            box(4, 13, 4, 12, 15, 12, accent)]


def _dao_gate(frame, panel, crest):
    return [box(0, 0, 2, 3, 16, 14, frame),
            box(13, 0, 2, 16, 16, 14, frame),
            box(3, 13, 2, 13, 16, 14, frame),
            box(4, 2, 5, 12, 14, 7, panel),
            box(5, 5, 4, 11, 11, 6, crest)]


def _dao_table(frame, top, inlay):
    return [box(1, 0, 2, 4, 8, 5, frame), box(12, 0, 2, 15, 8, 5, frame),
            box(1, 0, 11, 4, 8, 14, frame), box(12, 0, 11, 15, 8, 14, frame),
            box(0, 8, 1, 16, 10, 15, top), box(3, 10, 3, 13, 11, 13, inlay)]


def _dao_lamp(frame, shade, glow_texture):
    return [box(7, 0, 7, 9, 4, 9, frame), box(5, 4, 5, 11, 6, 11, frame),
            box(3, 6, 3, 13, 13, 13, shade), box(5, 13, 5, 11, 15, 11, frame),
            box(7, 15, 7, 9, 16, 9, glow_texture)]


def _dao_garden(base, water, plant):
    return [box(1, 0, 1, 15, 2, 15, base), box(3, 2, 3, 13, 4, 13, water),
            box(5, 4, 5, 11, 6, 11, plant), box(7, 6, 7, 9, 14, 9, plant),
            box(4, 11, 4, 12, 13, 12, plant)]


def _dao_array(prefix, title, count, material, light, factory, zh_title):
    for index in range(count):
        _dao_add(f"{prefix}_{index+1:02d}", f"{title} {index+1:02d}", material,
                 light, factory(index), zh_title)


_dao_array("sky_floor", "Celestial Meridian Floor", 16, "stone", 0,
           lambda i: _dao_panel(('stars', 'taiji', 'lotus', 'cloud_stone')[i % 4],
                                ('gold', 'jade', 'sunset', 'runes')[i % 4], 1 + i % 3),
           "\u5929\u7eb3\u661f\u7eb9\u5730\u7816")
_dao_array("pillar", "Origin Pillar", 16, "stone", 0,
           lambda i: _dao_pillar(('jade', 'cloud_stone', 'sunset', 'dragon')[i % 4],
                                 ('gold', 'bronze', 'runes', 'crystal')[i % 4],
                                 ('lotus', 'stars', 'taiji', 'cloud')[i % 4]),
           "\u9053\u6e90\u5929\u67f1")
_dao_array("roof", "Nine Heavens Eave", 16, "stone", 0,
           lambda i: _dao_roof(('roof_red', 'roof_blue', 'cloud_stone', 'sunset')[i % 4],
                               ('gold', 'jade', 'bronze', 'dragon')[i % 4],
                               ('crystal', 'lotus', 'stars', 'glow')[i % 4]),
           "\u4e5d\u5929\u91cd\u6a90")
_dao_array("gate", "Immortal Threshold Gate", 16, "wood", 0,
           lambda i: _dao_gate(('rosewood', 'vermilion', 'bamboo', 'cloud_stone')[i % 4],
                               ('cloud', 'banner', 'silk', 'runes')[i % 4],
                               ('gold', 'jade', 'dragon', 'taiji')[i % 4]),
           "\u4ed9\u5e9c\u7389\u9619")
_dao_array("furniture", "Inner Abode Furnishing", 20, "wood", 0,
           lambda i: (_dao_table(('rosewood', 'bamboo', 'vermilion', 'cloud_stone')[i % 4],
                                 ('silk', 'jade', 'porcelain', 'scrolls')[i % 4],
                                 ('gold', 'runes', 'talisman', 'lotus')[i % 4])
                      if i % 2 == 0 else
                      [box(1, 0, 2, 15, 2, 14, ('rosewood', 'bamboo', 'vermilion', 'cloud_stone')[i % 4]),
                       box(3, 2, 3, 13, 12, 13, ('scrolls', 'herbs', 'silk', 'porcelain')[i % 4]),
                       box(4, 12, 4, 12, 15, 12, ('gold', 'jade', 'bronze', 'crystal')[i % 4])]),
           "\u9053\u5c45\u6e05\u4f9b\u9648\u8bbe")
_dao_array("lamp", "Primordial Spirit Lamp", 16, "stone", 15,
           lambda i: _dao_lamp(('bronze', 'gold', 'jade', 'cloud_stone')[i % 4],
                               ('lantern', 'glow', 'crystal', 'silk')[i % 4],
                               ('glow', 'crystal', 'sunset', 'lotus')[i % 4]),
           "\u5143\u795e\u7075\u706f")
_dao_array("garden", "Spirit Garden Feature", 16, "stone", 0,
           lambda i: _dao_garden(('cloud_stone', 'jade', 'bronze', 'porcelain')[i % 4],
                                 ('water_art', 'lotus', 'taiji', 'crystal')[i % 4],
                                 ('leaves', 'bamboo', 'herbs', 'sunset')[i % 4]),
           "\u7075\u5883\u5ead\u666f")
_dao_array("formation", "Heavenly Formation Seal", 16, "stone", 3,
           lambda i: [box(0, 0, 0, 16, 1, 16, ('runes', 'taiji', 'stars', 'dragon')[i % 4]),
                      box(2, 1, 2, 14, 3, 14, ('gold', 'jade', 'crystal', 'bronze')[i % 4]),
                      box(4, 3, 4, 12, 5, 12, ('sunset', 'lotus', 'cloud_stone', 'glow')[i % 4]),
                      box(7, 5, 7, 9, 12, 9, ('crystal', 'jade', 'gold', 'glow')[i % 4])],
           "\u5929\u6f14\u9635\u7eb9")


DEFINITIONS.update(DAO_TAI_DEFINITIONS)


def generate_textures(assets):
    colors = {'sunset':(186,114,87),'lotus':(168,203,183),'stars':(29,48,76),
        'taiji':(202,209,192),'cloud_stone':(138,168,168),'dragon':(94,129,128),
        'bamboo_wall':(220,218,194),'runes':(103,42,40),'bronze':(102,111,78),
        'bamboo':(149,150,87),'crystal':(117,195,207),'silk':(153,78,110),
        'banner':(173,61,44),'talisman':(225,201,147),'herbs':(88,114,63),
        'steel':(158,182,186),'pottery':(133,79,52),'burlap':(183,158,105),
        'fruit':(216,124,46),'water_art':(55,135,159)}
    for i,(name,color) in enumerate(colors.items()):
        rng = random.Random(8880+i)
        image = Image.new('RGB',(32,32)); draw=ImageDraw.Draw(image)
        for y in range(32):
            for x in range(32):
                n=rng.randrange(-5,6)
                image.putpixel((x,y),tuple(max(0,min(255,c+n)) for c in color))
        gold=(221,184,98); dark=tuple(max(0,c-38) for c in color)
        if name in ('lotus','taiji','stars','dragon','runes','cloud_stone','banner','silk','talisman'):
            draw.rectangle((1,1,30,30),outline=gold)
        if name=='lotus':
            for bounds in [(10,4,21,27),(4,10,27,21),(6,6,25,25)]:
                draw.ellipse(bounds,outline=(225,224,181),width=2)
            draw.ellipse((13,13,18,18),fill=gold)
        elif name=='taiji':
            draw.ellipse((4,4,27,27),fill=(35,48,57));draw.pieslice((4,4,27,27),90,270,fill=(230,230,207))
            draw.ellipse((10,4,21,15),fill=(230,230,207));draw.ellipse((10,16,21,27),fill=(35,48,57))
            draw.ellipse((14,8,17,11),fill=(35,48,57));draw.ellipse((14,20,17,23),fill=(230,230,207))
        elif name=='stars':
            points=[(5,8),(11,6),(17,10),(20,17),(26,21),(23,27)]
            draw.line(points,fill=(119,158,183))
            for x,y in points: draw.rectangle((x-1,y-1,x+1,y+1),fill=gold)
        elif name=='dragon':
            draw.line((5,23,8,20,15,22,23,18,24,12,18,8,11,12,12,16,18,17,21,14),fill=gold,width=3)
            draw.rectangle((7,7,13,12),fill=gold);draw.line((8,7,6,4,11,6,13,4),fill=gold,width=2)
            draw.point((10,9),fill=(36,45,41));draw.line((15,23,13,27,18,25),fill=gold,width=2)
        elif name in ('runes','talisman'):
            ink=gold if name=='runes' else (161,44,38)
            draw.line((9,5,23,5,17,10,9,13,23,13,12,18,20,18,14,26),fill=ink,width=2)
            draw.line((7,9,5,23,9,27),fill=ink);draw.line((25,9,27,23,23,27),fill=ink)
        elif name in ('banner','cloud_stone','silk'):
            for y in (6,19):
                draw.arc((4,y,20,y+9),20,320,fill=gold,width=2)
                draw.arc((15,y+1,28,y+10),70,310,fill=gold,width=2)
            if name=='banner':draw.line((7,25,13,18,17,21,22,15,27,25),fill=gold,width=2)
        elif name=='bamboo_wall':
            for x in (6,17,25):
                draw.line((x,2,x-2,30),fill=(93,127,90),width=2)
                for y in (8,18):
                    draw.line((x-3,y,x+1,y),fill=(62,106,78))
                    draw.line((x,y,x+5,y-4,x+2,y-1),fill=(93,127,90),width=2)
        elif name=='sunset':
            for y in (4,13,25):
                draw.line((0,y,8,y+2,17,y-2,31,y+1),fill=(233,184,131),width=2)
        elif name in ('bamboo','burlap'):
            for x in range(0,32,4):draw.line((x,0,x,31),fill=dark)
            for y in range(0,32,8 if name=='bamboo' else 4):draw.line((0,y,31,y),fill=dark)
        elif name=='herbs':
            draw.rectangle((8,8,23,23),outline=(197,177,113))
            draw.line((13,13,19,13,16,18,12,20,20,20),fill=(217,199,149),width=2)
        elif name=='crystal':
            draw.polygon([(16,2),(29,15),(16,29),(3,15)],outline=(213,241,222),width=2)
            draw.line((16,2,16,29,3,15,29,15),fill=(166,222,224))
        elif name=='pottery':
            for y in (3,25):draw.line((0,y,31,y),fill=(190,132,77),width=2)
        elif name=='fruit':
            draw.arc((6,5,26,28),190,330,fill=(249,187,68),width=3)
            draw.rectangle((14,0,17,5),fill=(76,112,49))
        elif name=='water_art':
            for y in (5,15,25):draw.line((3,y,12,y,15,y+1,27,y+1),fill=(137,203,198))
        else:
            for y in (6,22):draw.line((0,y,31,y),fill=dark)
        image.save(assets/'textures/block'/f'oriental_{name}.png')
