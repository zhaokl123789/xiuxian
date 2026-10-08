"""Audit the shipped sect registry, models, variants, textures, labels, loot, and interaction classes."""
from collections import Counter
from io import BytesIO
import hashlib
import json
from pathlib import Path
from zipfile import ZipFile
from PIL import Image
from sect_assets import DEFINITIONS, validate

ROOT = Path(__file__).resolve().parents[1]
ASSETS = "assets/xiuxian/"


def audit():
    validate()
    with ZipFile(ROOT / "build/libs/xiuxian-1.0.0-SNAPSHOT.jar") as jar:
        names = set(jar.namelist())
        ids = {Path(p).stem for p in names if p.startswith(ASSETS + "blockstates/sect_")}
        assert ids == set(DEFINITIONS), "Shipped sect block IDs differ from the 144-resource catalog"
        for class_name in ("block/SectBlocks", "block/SectSeatBlock", "block/SectLampBlock",
                           "block/SectInstrumentBlock", "block/SectInstrumentBlockEntity",
                           "entity/SectSeatEntity", "client/SectSeatRenderer",
                           "sect/SectComplexGenerator", "sect/SectComplexConstruction", "sect/SectComplexEvents",
                           "item/SectComplexInspectionItem"):
            assert "xiuxian/" + class_name + ".class" in names, class_name
        texture_hashes = set()
        for key, entry in DEFINITIONS.items():
            load = lambda path: json.loads(jar.read(path))
            state = load(ASSETS + "blockstates/" + key + ".json")
            item = load(ASSETS + "models/item/" + key + ".json")
            assert item["parent"] == "xiuxian:block/" + key
            property_name = "lit" if entry["category"] == "lights" else "playing" if entry["category"] == "music" else None
            expected = {""} if entry["kind"] == "cube" else {
                "facing=" + direction + ("," + property_name + "=" + value if property_name else "")
                for direction in ("north", "east", "south", "west")
                for value in (("false", "true") if property_name else (None,))}
            assert set(state["variants"]) == expected, key
            for selector, variant in state["variants"].items():
                model_id = key + ("_unlit" if property_name == "lit" and selector.endswith("lit=false") else "")
                assert variant["model"] == "xiuxian:block/" + model_id
                model = load(ASSETS + "models/block/" + model_id + ".json")
                for texture in model["textures"].values():
                    assert texture.startswith("xiuxian:block/")
                    path = ASSETS + "textures/" + texture.split(":", 1)[1] + ".png"
                    image = Image.open(BytesIO(jar.read(path)))
                    assert image.size == (32, 32), path
                    texture_hashes.add(hashlib.sha256(image.tobytes()).hexdigest())
                for element in model.get("elements", []):
                    assert all(0 <= a < b <= 16 for a, b in zip(element["from"], element["to"]))
                    assert all(face["texture"][1:] in model["textures"] for face in element["faces"].values())
                if model.get("elements"):
                    assert "particle" in model["textures"]
            loot = load("data/xiuxian/loot_tables/blocks/" + key + ".json")
            assert loot["pools"][0]["entries"][0]["name"] == "xiuxian:" + key
        for lang in ("zh_cn", "en_us"):
            labels = json.loads(jar.read(ASSETS + "lang/" + lang + ".json"))
            values = [labels["block.xiuxian." + key] for key in ids]
            assert len(set(values)) == 144 and all(value and "\ufffd" not in value for value in values)
            assert labels["item.xiuxian.sect_complex_inspection_token"]
        token = json.loads(jar.read(ASSETS + "models/item/sect_complex_inspection_token.json"))
        assert token["textures"]["layer0"] == "xiuxian:item/sect_complex_inspection_token"
        assert ASSETS + "textures/item/sect_complex_inspection_token.png" in names
        tools = [json.loads(jar.read("data/minecraft/tags/blocks/mineable/" + tool + ".json"))["values"]
                 for tool in ("axe", "pickaxe")]
        assert all(sum("xiuxian:" + key in tag for tag in tools) == 1 for key in ids)
        assert len(texture_hashes) >= 144, "Resource textures lack variation"
        print("PASS: 144 packaged sect resources; models, variants, 32x32 textures, loot, labels, tool tags and interaction classes valid.")
        print("Categories:", dict(Counter(entry["category"] for entry in DEFINITIONS.values())))
        print("Distinct texture images:", len(texture_hashes))


if __name__ == "__main__":
    audit()
