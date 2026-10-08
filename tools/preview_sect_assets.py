"""Render actual sect model JSON and textures; these sheets are not in-game screenshots."""
from preview_oriental_assets import sheet_for
from sect_assets import DEFINITIONS

CATEGORIES = {
    "materials": "宗门基础材质", "architecture": "宗门梁柱屋面与门窗",
    "ritual": "宗门仪式与护山陈设", "lights": "宗门可开关灯具",
    "furniture": "宗门家具与可入座陈设", "music": "宗门可演奏乐器",
    "garden": "宗门药园与水庭陈设",
}

if __name__ == "__main__":
    for category, title in CATEGORIES.items():
        sheet_for([key for key, entry in DEFINITIONS.items() if entry["category"] == category],
                  title, "sect-" + category + ".png")
    featured = ["hall_pillar", "blue_roof_tiles", "hall_dougong", "cloud_window", "guardian_crane", "dragon_guardian",
                "grand_incense_burner", "sunset_banner", "formation_core", "red_lantern", "hall_lamp", "hall_sconce",
                "elder_throne", "master_chair", "watercourt_bench", "meditation_cushion", "scroll_shelf", "cloud_screen",
                "qin_table", "moon_pipa", "cloud_sheng", "ritual_chimes", "morning_bell", "evening_drum"]
    sheet_for(["sect_" + key for key in featured], "落霞洞天宗门专属资源选览", "sect-featured.png")
