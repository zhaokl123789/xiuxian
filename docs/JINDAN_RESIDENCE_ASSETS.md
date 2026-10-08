# Jin-Dan Residence Assets

Reference: `G:/mc/references/luoxia/05.png`. This stage supplies resources only.
The future inspection building must be slightly smaller than the Dao-Tai
residence and larger than the Luoxia exterior.

The package contains 136 blocks and their 136 placeable BlockItems:

| Category | Count | Purpose |
| --- | ---: | --- |
| Materials | 24 | Cinnabar, volcanic ash, copper, iron, jade and patterned floors |
| Architecture | 20 | Roofs, dougong, columns, gateway frames, lattice windows and rails |
| Furnaces | 20 | Cauldrons, furnace sections, bellows and earthfire channels |
| Formations | 16 | Golden core seals, solar/lunar motifs and ritual arrays |
| Lights | 20 | Copper, gold/blue flame, lotus, pillar and hanging lamps |
| Furniture | 24 | Medicine cabinets, vial racks, recipe desks and domestic props |
| Landscape | 12 | Spirit springs, planters, volcanic monuments and guardians |

All IDs use `jindan_`. The 54 original 32x32 textures are shared by the models.
Each asset has a distinct model/texture definition, a blockstate, an item model,
a loot table, Chinese/English labels and a mining tag. Decorative collision
boxes are generated from the same geometry as the visible models.
All 20 light fixtures emit light level 14 or 15.

These are building and decoration resources. Furnaces, cabinets, doors and
medicine props do not yet implement alchemy, inventories or opening behavior.
No Jin-Dan building generation or inner-realm transplantation is introduced.

Generate and verify with Python + Pillow:

```powershell
python tools/jindan_assets.py
python tools/generate_oriental_assets.py
.\gradlew.bat build --no-daemon --console=plain
python tools/audit_oriental_package.py
python tools/preview_oriental_assets.py
```

The package audit checks the built jar. Preview output:
`build/oriental-preview/jindan-resources.png`.
The preview uses actual models/textures; it is not a Minecraft screenshot and
does not simulate light emission.

In creative mode, find the resources in Building Blocks by their Chinese names
or use `/give @s xiuxian:jindan_tripod_cauldron` and
`/give @s xiuxian:jindan_copper_lantern` as examples.
