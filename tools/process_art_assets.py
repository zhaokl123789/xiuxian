from collections import deque
from pathlib import Path
import collections.abc

if not hasattr(collections.abc, "Buffer"):
    collections.abc.Buffer = object

from PIL import Image, ImageDraw


SOURCE = Path(r"G:\mc\道具")
ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/xiuxian"
BLOCK_TEXTURES = ASSETS / "textures/block"
ITEM_TEXTURES = ASSETS / "textures/item"
BLOCK_TEXTURES.mkdir(parents=True, exist_ok=True)
ITEM_TEXTURES.mkdir(parents=True, exist_ok=True)


def source_image(name: str) -> Image.Image:
    return Image.open(SOURCE / name).convert("RGB")


def save_block(source_name: str, output_name: str, tint: tuple[int, int, int] | None = None) -> None:
    image = source_image(source_name).resize((16, 16), Image.Resampling.NEAREST).convert("RGB")
    if tint is not None:
        overlay = Image.new("RGB", image.size, tint)
        image = Image.blend(image, overlay, 0.24)
    image.save(BLOCK_TEXTURES / output_name, optimize=True)


def is_background(pixel: tuple[int, int, int]) -> bool:
    high, low = max(pixel), min(pixel)
    return high - low <= 10 and sum(pixel) // 3 >= 178


def transparent_item(source_name: str, output_name: str, tint: tuple[int, int, int] | None = None) -> None:
    source = source_image(source_name)
    source.thumbnail((256, 256), Image.Resampling.NEAREST)
    width, height = source.size
    pixels = list(source.getdata())
    mask = bytearray(width * height)
    pending: deque[int] = deque()

    def add_background(index: int) -> None:
        if not mask[index] and is_background(pixels[index]):
            mask[index] = 1
            pending.append(index)

    for x in range(width):
        add_background(x)
        add_background((height - 1) * width + x)
    for y in range(height):
        add_background(y * width)
        add_background(y * width + width - 1)
    while pending:
        index = pending.popleft()
        x, y = index % width, index // width
        for neighbor in (index - 1 if x else -1, index + 1 if x + 1 < width else -1,
                         index - width if y else -1, index + width if y + 1 < height else -1):
            if neighbor >= 0:
                add_background(neighbor)

    alpha = Image.new("L", source.size)
    alpha.putdata([0 if pixel else 255 for pixel in mask])
    bounds = alpha.getbbox()
    if bounds is None:
        raise ValueError(f"No foreground found in {source_name}")
    rgba = source.convert("RGBA")
    rgba.putalpha(alpha)
    rgba = rgba.crop(bounds)
    side = max(rgba.size)
    canvas = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    canvas.alpha_composite(rgba, ((side - rgba.width) // 2, (side - rgba.height) // 2))
    if tint is not None:
        pixels = canvas.load()
        for y in range(side):
            for x in range(side):
                red, green, blue, opacity = pixels[x, y]
                if opacity:
                    pixels[x, y] = (
                        min(255, red * tint[0] // 180),
                        min(255, green * tint[1] // 180),
                        min(255, blue * tint[2] // 180),
                        opacity,
                    )
    canvas.resize((32, 32), Image.Resampling.NEAREST).save(ITEM_TEXTURES / output_name, optimize=True)


def make_manual_cover(filename: str, base: tuple[int, int, int], symbol: str) -> None:
    size = 32
    image = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    dark = tuple(max(0, c - 54) for c in base)
    light = tuple(min(255, c + 45) for c in base)
    gold = (229, 192, 112, 255)
    draw.polygon(((7, 3), (25, 3), (29, 7), (29, 27), (8, 29), (4, 25), (4, 7)), fill=dark + (255,))
    draw.polygon(((8, 4), (24, 4), (27, 7), (27, 25), (8, 27)), fill=base + (255,))
    draw.line((7, 4, 7, 26), fill=light + (255,), width=1)
    draw.line((9, 7, 24, 7), fill=gold, width=1)
    draw.line((9, 25, 25, 24), fill=dark + (255,), width=1)
    draw.line((10, 9, 24, 9), fill=light + (255,), width=1)
    draw.line((10, 23, 23, 23), fill=dark + (255,), width=1)
    cx, cy = 17, 16
    if symbol == "circle":
        draw.ellipse((12, 11, 22, 21), outline=gold, width=1)
        draw.point((cx, cy), fill=(244, 224, 167, 255))
    elif symbol == "wave":
        draw.arc((11, 12, 23, 20), 195, 345, fill=gold, width=1)
        draw.arc((11, 15, 23, 23), 195, 345, fill=light + (255,), width=1)
    elif symbol == "root":
        draw.line((17, 11, 17, 19), fill=gold, width=1)
        draw.line((17, 15, 12, 12), fill=gold, width=1)
        draw.line((17, 15, 22, 12), fill=gold, width=1)
        draw.line((17, 18, 13, 22), fill=light + (255,), width=1)
        draw.line((17, 18, 21, 22), fill=light + (255,), width=1)
    elif symbol == "spiral":
        draw.arc((11, 11, 23, 23), 10, 300, fill=gold, width=1)
        draw.arc((14, 14, 20, 20), 20, 310, fill=light + (255,), width=1)
    elif symbol == "mountain":
        draw.line((11, 21, 16, 12, 18, 17, 21, 13, 24, 21), fill=gold, width=1)
    elif symbol == "droplet":
        draw.polygon(((17, 11), (12, 18), (13, 21), (17, 23), (21, 21), (22, 18)), outline=gold)
        draw.point((17, 18), fill=light + (255,))
    elif symbol == "gate":
        draw.rectangle((12, 11, 22, 21), outline=gold, width=1)
        draw.line((15, 21, 15, 15, 19, 15, 19, 21), fill=light + (255,), width=1)
    elif symbol == "seed":
        draw.ellipse((13, 12, 21, 21), outline=gold, width=1)
        draw.line((17, 19, 19, 13), fill=light + (255,), width=1)
    elif symbol == "yin":
        draw.arc((12, 11, 22, 21), 90, 270, fill=gold, width=1)
        draw.arc((12, 11, 22, 21), 270, 90, fill=light + (255,), width=1)
        draw.point((15, 16), fill=gold)
        draw.point((19, 16), fill=light + (255,))
    elif symbol == "hourglass":
        draw.line((13, 11, 21, 11, 17, 16, 21, 21, 13, 21, 17, 16), fill=gold, width=1)
    elif symbol == "plain":
        draw.rectangle((13, 12, 21, 20), outline=gold, width=1)
        draw.line((15, 16, 19, 16), fill=light + (255,), width=1)
    elif symbol == "orbit":
        draw.ellipse((11, 14, 23, 19), outline=gold, width=1)
        draw.ellipse((15, 11, 19, 23), outline=light + (255,), width=1)
    image.save(ITEM_TEXTURES / filename, optimize=True)


for ore in ("spirit_stone_ore", "mid_spirit_stone_ore", "high_spirit_stone_ore", "supreme_spirit_stone_ore"):
    save_block(f"{ore}.png", f"{ore}.png")

for stone in ("spirit_stone", "mid_spirit_stone", "high_spirit_stone", "supreme_spirit_stone"):
    transparent_item(f"{stone}.png", f"{stone}.png")

for pill in ("qi_gathering_pill", "mid_qi_gathering_pill", "high_qi_gathering_pill", "supreme_qi_gathering_pill",
             "qi_gathering_pill_base", "mid_pill_base", "high_pill_base", "supreme_pill_base"):
    transparent_item(f"{pill}.png", f"{pill}.png")

extra_pills = {
    "rejuvenation_pill": ("qi_gathering_pill.png", (142, 224, 165)),
    "fasting_pill": ("mid_qi_gathering_pill.png", (233, 205, 112)),
    "clearing_pill": ("qi_gathering_pill.png", (145, 213, 225)),
    "protective_pill": ("high_qi_gathering_pill.png", (188, 136, 220)),
    "lightness_pill": ("mid_qi_gathering_pill.png", (193, 231, 166)),
    "fire_warding_pill": ("high_qi_gathering_pill.png", (238, 145, 94)),
    "water_breathing_pill": ("mid_qi_gathering_pill.png", (105, 190, 225)),
    "bright_sight_pill": ("qi_gathering_pill.png", (245, 213, 112)),
    "rejuvenation_pill_base": ("qi_gathering_pill_base.png", (149, 195, 154)),
    "fasting_pill_base": ("mid_pill_base.png", (210, 182, 117)),
    "clearing_pill_base": ("qi_gathering_pill_base.png", (140, 198, 206)),
    "protective_pill_base": ("high_pill_base.png", (180, 130, 193)),
    "lightness_pill_base": ("mid_pill_base.png", (173, 207, 140)),
    "fire_warding_pill_base": ("high_pill_base.png", (221, 137, 96)),
    "water_breathing_pill_base": ("mid_pill_base.png", (97, 172, 205)),
    "bright_sight_pill_base": ("qi_gathering_pill_base.png", (226, 195, 101)),
}
for output, (source, tint) in extra_pills.items():
    transparent_item(source, f"{output}.png", tint)

manuals = {
    "manual_basic_breathing.png": ((74, 101, 100), "circle"),
    "manual_clear_origin.png": ((66, 113, 105), "orbit"),
    "manual_wuwei_breath.png": ((77, 111, 133), "wave"),
    "manual_embrace_one.png": ((122, 86, 56), "circle"),
    "manual_valley_spirit.png": ((82, 105, 70), "mountain"),
    "manual_water_virtue.png": ((60, 113, 133), "droplet"),
    "manual_return_to_root.png": ((111, 76, 65), "root"),
    "manual_mysterious_gate.png": ((63, 68, 91), "gate"),
    "manual_less_private.png": ((117, 101, 59), "seed"),
    "manual_female_spirit.png": ((123, 73, 94), "yin"),
    "manual_know_stop.png": ((78, 87, 104), "hourglass"),
    "manual_return_nature.png": ((100, 103, 78), "plain"),
}
for filename, (color, symbol) in manuals.items():
    make_manual_cover(filename, color, symbol)

furnace_sources = {
    "top": "alchemy_furnace_top.png",
    "side": "alchemy_furnace_side.png",
    "front": "alchemy_furnace_front.png",
    "front_on": "alchemy_furnace_front_on.png",
}
tiers = {
    "": None,
    "_spirit": (91, 161, 139),
    "_earth": (194, 146, 75),
    "_heaven": (162, 181, 161),
}
for suffix, tint in tiers.items():
    for face, source in furnace_sources.items():
        save_block(source, f"alchemy_furnace{suffix}_{face}.png", tint)

print(f"Generated texture assets in {ASSETS}")
