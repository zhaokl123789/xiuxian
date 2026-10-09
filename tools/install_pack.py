"""Install the optional FTB/Xaero pack without changing cultivation gameplay."""

import argparse
import hashlib
import io
import json
from pathlib import Path
import shutil
import sys
import tomllib
import urllib.request
import zipfile
from xml.sax.saxutils import escape


ROOT = Path(__file__).resolve().parents[1]
PACK = ROOT / "pack"
CACHE = ROOT / "build" / "pack-downloads"
MAVEN = ROOT / "build" / "pack-maven"
OUTPUT = ROOT / "build" / "pack"


def checked_jar(path, mod):
    content = path.read_bytes()
    for algorithm in ("sha256", "sha512"):
        if algorithm in mod and hashlib.new(algorithm, content).hexdigest() != mod[algorithm]:
            raise ValueError(f"{path.name}: {algorithm} mismatch")
    with zipfile.ZipFile(io.BytesIO(content)) as jar:
        metadata = tomllib.loads(jar.read("META-INF/mods.toml").decode("utf-8"))
        if not any(entry["modId"] == mod["id"] and entry["version"] == mod["version"]
                   for entry in metadata["mods"]):
            raise ValueError(f"{path.name}: unexpected mod ID/version")
    return content, metadata


def download(mod, opener, offline):
    CACHE.mkdir(parents=True, exist_ok=True)
    destination = CACHE / mod["filename"]
    if not destination.exists():
        # Reuse a verified local download, including one made during setup.
        previous = ROOT / "build" / mod["filename"]
        if previous.exists():
            checked_jar(previous, mod)
            shutil.copy2(previous, destination)
        elif offline:
            raise FileNotFoundError(f"Missing cached mod: {mod['filename']}")
        else:
            request = urllib.request.Request(mod["url"], headers={"User-Agent": "Xiuxian-Pack/1.0"})
            with opener.open(request, timeout=90) as response:
                content = response.read()
            temporary = destination.with_suffix(".download")
            temporary.write_bytes(content)
            checked_jar(temporary, mod)
            temporary.replace(destination)
    checked_jar(destination, mod)
    return destination


def publish_local(mod, content):
    directory = MAVEN / "local" / "xiuxian" / "pack" / mod["id"] / mod["version"]
    directory.mkdir(parents=True, exist_ok=True)
    stem = f"{mod['id']}-{mod['version']}"
    (directory / f"{stem}.jar").write_bytes(content)
    (directory / f"{stem}.pom").write_text(
        '<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion>'
        '<groupId>local.xiuxian.pack</groupId>'
        f'<artifactId>{escape(mod["id"])}</artifactId><version>{escape(mod["version"])}</version>'
        '</project>\n', encoding="utf-8")


def write_preserving(path, content):
    path.parent.mkdir(parents=True, exist_ok=True)
    if path.exists():
        previous = path.read_bytes()
        if previous == content:
            return
        digest = hashlib.sha256(previous).hexdigest()[:12]
        backup = path.with_name(f"{path.name}.before-pack-{digest}.bak")
        if not backup.exists():
            shutil.copy2(path, backup)
    path.write_bytes(content)


def quest_chapter(locations):
    quests = []
    for index, location in enumerate(locations["landmarks"], 1):
        x, y, z = location["position"]
        quest = {
            "id": f"4C554F584941{index:04X}",
            "title": location["name"], "icon": location["icon"],
            "x": location["quest_x"], "y": location["quest_y"],
            "description": [location["description"], f"落霞洞天坐标：X {x}，Y {y}，Z {z}。"],
            "tasks": [{"id": f"4C554F584942{index:04X}", "type": "location",
                       "title": location["name"], "dimension": locations["dimension"],
                       "ignore_dimension": False,
                       "position": {"int_array": [x - 16, max(-64, y - 12), z - 16]},
                       "size": {"int_array": [33, 40, 33]}}]
        }
        quests.append(quest)
    # Typed int arrays are required by FTB's LocationTask NBT reader.
    def snbt(value):
        if isinstance(value, dict):
            if set(value) == {"int_array"}:
                return "[I;" + ",".join(str(n) for n in value["int_array"]) + "]"
            return "{" + ",\n".join(json.dumps(k) + ":" + snbt(v) for k, v in value.items()) + "}"
        if isinstance(value, list):
            return "[" + ",\n".join(snbt(v) for v in value) + "]"
        return json.dumps(value, ensure_ascii=False)
    return (snbt({"id": locations["chapter_id"], "filename": "luoxia_navigation",
                  "group": "", "order_index": 0, "title": locations["chapter_title"],
                  "icon": "minecraft:map", "default_quest_shape": "square",
                  "quests": quests}) + "\n").encode("utf-8")


def remove_pack_waypoints(game_dir, locations):
    dimension_dir = "dim%" + locations["dimension"].replace(":", "$", 1).replace("/", "%")
    names = {p["name"] for p in locations["landmarks"]}
    changed = []
    # Xaero migrated waypoint storage in recent versions; clean both layouts.
    for directory in (game_dir / "XaeroWaypoints", game_dir / "xaero" / "minimap"):
        if not directory.exists():
            continue
        for path in directory.rglob("waypoints.txt"):
            if path.parent.name != dimension_dir:
                continue
            lines = path.read_text(encoding="utf-8-sig").splitlines()
            updated = []
            for line in lines:
                fields = line.split(":")
                if (fields[0] == "waypoint" and len(fields) > 9
                        and fields[9] == locations["waypoint_set"] and fields[1] in names):
                    continue
                updated.append(line)
            if updated == lines:
                continue
            has_custom = any(line.startswith("waypoint:") and len(line.split(":")) > 9
                             and line.split(":")[9] == locations["waypoint_set"] for line in updated)
            if not has_custom:
                for index, line in enumerate(updated):
                    if line.startswith("sets:"):
                        sets = [name for name in line.split(":")[1:] if name != locations["waypoint_set"]]
                        updated[index] = "sets:" + ":".join(sets or ["gui.xaero_default"])
            write_preserving(path, ("\n".join(updated) + "\n").encode("utf-8"))
            changed.append(path)
    return changed


def install_config(game_dir, locations, client=True):
    folder = game_dir / "config" / "ftbquests" / "quests"
    if not (folder / "data.snbt").exists():
        write_preserving(folder / "data.snbt", b'{version:13,default_quest_shape:"square",'
                         b'default_autoclaim_rewards:"disabled",default_consume_items:false,'
                         b'drop_loot_crates:false,detection_delay:20,grid_scale:0.5d}\n')
    write_preserving(folder / "chapters" / "luoxia_navigation.snbt", quest_chapter(locations))
    if client:
        install_keys(game_dir)
    return remove_pack_waypoints(game_dir, locations) if client else []


def install_keys(game_dir):
    path = game_dir / "options.txt"
    lines = path.read_text(encoding="utf-8-sig").splitlines() if path.exists() else []
    replacements = {
        "key_gui.xaero_open_map": ("key.keyboard.m", "key.keyboard.j"),
        "key_gui.xaero_new_waypoint": ("key.keyboard.b", "key.keyboard.n"),
        "key_gui.xaero_waypoints_key": (None, "key.keyboard.u"),
        "key_key.ftbquests.quests": ("key.keyboard.unknown", "key.keyboard.o"),
    }
    seen = set()
    updated = []
    for line in lines:
        key, separator, value = line.partition(":")
        if separator and key in replacements:
            old, new = replacements[key]
            seen.add(key)
            if value == old:
                line = key + ":" + new
        updated.append(line)
    for key, (_, new) in replacements.items():
        if key not in seen:
            updated.append(key + ":" + new)
    write_preserving(path, ("\n".join(updated) + "\n").encode("utf-8"))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--game-dir", type=Path, help="Install into a normal Forge instance instead of dev run")
    parser.add_argument("--side", choices=("client", "server"), default="client")
    parser.add_argument("--proxy", help="Optional download proxy URL")
    parser.add_argument("--offline", action="store_true", help="Use verified cached downloads only")
    parser.add_argument("--remove-pack-waypoints", "--waypoints-only", dest="remove_pack_waypoints",
                        action="store_true", help="Remove previously installed pack waypoints only")
    args = parser.parse_args()
    game_dir = (args.game_dir or ROOT / "run").resolve()
    locations = json.loads((PACK / "luoxia-landmarks.json").read_text(encoding="utf-8"))
    if args.remove_pack_waypoints:
        for path in remove_pack_waypoints(game_dir, locations):
            print(f"Removed pack waypoints: {path}")
        return
    lock = json.loads((PACK / "mods.lock.json").read_text(encoding="utf-8"))
    opener = urllib.request.build_opener(urllib.request.ProxyHandler(
        {"https": args.proxy, "http": args.proxy})) if args.proxy else urllib.request.build_opener()
    downloaded = {}
    metadata = {}
    for mod in lock["mods"]:
        path = download(mod, opener, args.offline)
        content, metadata[mod["id"]] = checked_jar(path, mod)
        downloaded[mod["id"]] = path
        publish_local(mod, content)
        print(f"Verified: {mod['name']} {mod['version']}")
    worldmap = downloaded["xaeroworldmap"]
    with zipfile.ZipFile(worldmap) as jar:
        embedded_meta = json.loads(jar.read("META-INF/jarjar/metadata.json"))
        library = next(entry for entry in embedded_meta["jars"]
                       if entry["identifier"]["artifact"] == "xaerolib-forge-1.20.1")
        content = jar.read(library["path"])
        embedded = {"id": "xaerolib", "version": library["version"]["artifactVersion"]}
        if embedded["version"] != "1.7.3":
            raise ValueError("Unexpected embedded XaeroLib version")
        publish_local(embedded, content)
    available = set(downloaded) | {"xaerolib", "minecraft", "forge"}
    for mod_id, data in metadata.items():
        for dependency in data.get("dependencies", {}).get(mod_id, []):
            if dependency.get("mandatory") and dependency["modId"] not in available:
                raise ValueError(f"Missing dependency: {mod_id} -> {dependency['modId']}")
    MAVEN.mkdir(parents=True, exist_ok=True)
    (MAVEN / ".installed").write_text("FTB/Xaero pack installed\n", encoding="ascii")
    for side in ("client", "server"):
        bundle = OUTPUT / side
        for mod in lock["mods"]:
            if side == "server" and mod["side"] != "both":
                continue
            target = bundle / "mods" / mod["filename"]
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(downloaded[mod["id"]], target)
        own_jar = ROOT / "build" / "libs" / "xiuxian-1.0.0-SNAPSHOT.jar"
        if not own_jar.exists():
            raise FileNotFoundError("Build xiuxian first with gradlew.bat build")
        shutil.copy2(own_jar, bundle / "mods" / own_jar.name)
        install_config(bundle, locations, client=False)
        shutil.copy2(PACK / "mods.lock.json", bundle / "mods.lock.json")
        shutil.copy2(PACK / "luoxia-landmarks.json", bundle / "luoxia-landmarks.json")
        shutil.copy2(PACK / "README.md", bundle / "README.md")
        legacy_template = bundle / "luoxia-waypoints.txt"
        if legacy_template.exists():
            backup = legacy_template.with_suffix(".txt.disabled")
            if not backup.exists():
                shutil.copy2(legacy_template, backup)
            legacy_template.unlink()
    if args.game_dir:
        mods_dir = game_dir / "mods"
        mods_dir.mkdir(parents=True, exist_ok=True)
        selected_ids = {mod["id"] for mod in lock["mods"]
                        if args.side == "client" or mod["side"] == "both"} | {"xiuxian"}
        selected_files = {mod["filename"] for mod in lock["mods"]
                          if mod["id"] in selected_ids} | {"xiuxian-1.0.0-SNAPSHOT.jar"}
        # Retire only replaced versions of the selected mods, preserving all others.
        for old in mods_dir.glob("*.jar"):
            try:
                with zipfile.ZipFile(old) as jar:
                    ids = {item["modId"] for item in tomllib.loads(
                        jar.read("META-INF/mods.toml").decode("utf-8"))["mods"]}
                if ids & selected_ids and old.name not in selected_files:
                    old.rename(old.with_name(old.name + ".before-pack.disabled"))
            except (KeyError, zipfile.BadZipFile, tomllib.TOMLDecodeError):
                continue
        for path in (OUTPUT / args.side / "mods").glob("*.jar"):
            write_preserving(mods_dir / path.name, path.read_bytes())
    paths = install_config(game_dir, locations, client=args.side == "client")
    for path in paths:
        print(f"Removed pack waypoints: {path}")
    print(f"Configured game directory: {game_dir}")
    print(f"Forge client/server installation folders: {OUTPUT}")


if __name__ == "__main__":
    try:
        main()
    except Exception as error:
        print(f"Pack installation failed: {error}", file=sys.stderr)
        sys.exit(1)
