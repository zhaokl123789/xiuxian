"""Audit dedicated vein resources in the source tree and optionally a built jar."""
import argparse
import hashlib
import io
import json
from pathlib import Path
import zipfile
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]


def audit(read, exists):
    catalog = json.loads((ROOT / "docs/vein-resources.json").read_text(encoding="utf-8"))
    blocks, items = catalog["blocks"], catalog["items"]
    assert len(blocks) == 81 and len(items) == 29
    assets = "assets/xiuxian/"
    data = "data/xiuxian/"
    hashes = set()
    checked_models = set()

    def obj(path):
        assert exists(path), f"Missing {path}"
        return json.loads(read(path))

    def model(ref):
        if not ref.startswith("xiuxian:"):
            return
        path = assets + "models/" + ref.split(":", 1)[1] + ".json"
        if path in checked_models:
            return
        checked_models.add(path)
        content = obj(path)
        if "parent" in content:
            model(content["parent"])
        for value in content.get("textures", {}).values():
            if value.startswith("xiuxian:"):
                assert exists(assets + "textures/" + value.split(":", 1)[1] + ".png"), f"Missing texture {value}"
        for element in content.get("elements", []):
            assert all(0 <= a <= b <= 16 for a, b in zip(element["from"], element["to"]))
            assert set(element["faces"]) == {"north", "south", "east", "west", "up", "down"}

    zh = obj(assets + "lang/zh_cn.json")
    en = obj(assets + "lang/en_us.json")
    mineable = obj("data/minecraft/tags/blocks/mineable/pickaxe.json")["values"]
    for key, entry in {**blocks, **items}.items():
        kind = "block" if key in blocks else "item"
        texture = read(assets + f"textures/{kind}/{key}.png")
        image = Image.open(io.BytesIO(texture))
        assert image.size == (32, 32) and image.getbbox(), f"Invalid bitmap {key}"
        digest = hashlib.sha256(image.tobytes()).digest()
        assert digest not in hashes, f"Duplicate texture {key}"
        hashes.add(digest)
        assert zh[kind + ".xiuxian." + key] and en[kind + ".xiuxian." + key]
        model("xiuxian:item/" + key)
        if kind == "block":
            variants = obj(assets + "blockstates/" + key + ".json")["variants"]
            for variant in variants.values():
                model(variant["model"])
            obj(data + "loot_tables/blocks/" + key + ".json")
            assert "xiuxian:" + key in mineable
    # Identical inputs to the same processing/crafting recipe must never select different outputs.
    signatures = {}
    recipes = sorted((ROOT / "src/main/resources/data/xiuxian/recipes").glob("vein_*.json"))
    processing = 0
    for file in recipes:
        recipe = obj(data + "recipes/" + file.name)
        recipe_type = recipe["type"]
        signature = json.dumps({k: v for k, v in recipe.items() if k not in ("result", "residue", "ticks", "experience", "cookingtime")}, sort_keys=True)
        if recipe_type != "minecraft:stonecutting":
            assert signature not in signatures, f"Ambiguous recipe {file.name} / {signatures.get(signature)}"
            signatures[signature] = file.name
        if recipe_type == "xiuxian:vein_processing":
            processing += 1
            assert 1 <= recipe["ticks"] <= 72000
    assert processing == 40
    assert len(obj(data + "vein/layers.json")) == 9
    print(f"PASS: 81 blocks, 29 materials, {len(checked_models)} models, {len(recipes)} recipes, 40 processing recipes.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", type=Path)
    args = parser.parse_args()
    if args.jar:
        with zipfile.ZipFile(args.jar) as archive:
            audit(archive.read, lambda p: p in archive.namelist())
    else:
        root = ROOT / "src/main/resources"
        audit(lambda p: (root / p).read_bytes(), lambda p: (root / p).is_file())
