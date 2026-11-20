import json
import re
import tempfile
import unittest
from pathlib import Path

import generate

TOOLS = Path(__file__).resolve().parent
STYLES = generate.load_styles(TOOLS / "styles.json")
KNOWN = set(json.loads((TOOLS / "known_animations.json").read_text(encoding="utf-8")))
HELD_ITEM_DISPLAY_KEYS = (
    "thirdperson_righthand",
    "thirdperson_lefthand",
    "firstperson_righthand",
    "firstperson_lefthand",
)


def snapshot(root: Path) -> dict:
    return {
        str(p.relative_to(root)): p.read_bytes()
        for p in sorted(root.rglob("*"))
        if p.is_file()
    }


class GenerateTest(unittest.TestCase):
    def test_stats_in_range(self):
        self.assertEqual([s["n"] for s in STYLES], list(range(1, len(STYLES) + 1)))
        for s in STYLES:
            self.assertTrue(4.0 <= s["damage"] <= 8.0, s["n"])
            self.assertTrue(1.5 <= s["speed"] <= 2.5, s["n"])
        self.assertEqual(STYLES[0]["damage"], 4.0)

    def test_light_styles_are_faster_than_heavy_ones(self):
        ordered = sorted(STYLES, key=lambda s: s["damage"])
        speeds = [s["speed"] for s in ordered]
        self.assertEqual(speeds, sorted(speeds, reverse=True))

    def test_styles_differ_and_share_few_animations(self):
        for a in STYLES:
            for b in STYLES:
                if a["n"] >= b["n"]:
                    continue
                self.assertNotEqual(a["combos"], b["combos"], (a["n"], b["n"]))
                if not (a.get("custom") or b.get("custom")):
                    self.assertNotEqual(a["idle"], b["idle"], (a["n"], b["n"]))
                shared = set(a["combos"]) & set(b["combos"])
                self.assertLessEqual(len(shared), 2, (a["n"], b["n"], sorted(shared)))

    def test_combo_shape(self):
        for s in STYLES:
            combos = generate.build_type(s, KNOWN)["combos"]
            self.assertEqual(combos["one_hand"], s["combos"])
            self.assertEqual(combos["two_hand"], s["combos"])
            self.assertEqual(len(combos["one_hand"]), 7 if s.get("custom") else 8)
            self.assertNotIn("mount", combos)

    def test_no_crashing_animations(self):
        # These animations fire events that touch skill data of a passive the player
        # does not have (Optional.get(), getSkill(...) == null, or a SkillDataKey that
        # is "unregistered"), which crashes the server tick when a plain player uses
        # them (seen in real game logs 2026-11-05 and 2026-11-20).
        with open(Path(__file__).parent / "unsafe_animations.json", encoding="utf-8") as fh:
            unsafe = set(json.load(fh)["unsafe"])
        auto_java = (Path(__file__).parent.parent / "src/main/java/dev/fiststyle/AutoCombat.java").read_text(encoding="utf-8")
        for animation in re.findall(r'"((?:wom|epicsanji):biped/[\w/]+)"', auto_java):
            self.assertNotIn(animation, unsafe, f"auto parry/dodge uses a crashing animation: {animation}")
        for s in STYLES:
            built = generate.build_type(s, KNOWN)
            ids = list(s["combos"])
            ids += [v for m in built["livingmotion_modifier"].values() for v in m.values()]
            for animation in ids:
                self.assertNotIn(animation, unsafe, f"style {s['n']} uses a crashing animation")

    def test_only_wom_and_epicsanji_animations_no_scythe(self):
        for s in STYLES:
            ids = list(s["combos"]) + [f"wom:biped/living/{s['idle']}_idle"]
            built = generate.build_type(s, KNOWN)
            ids += [v for m in built["livingmotion_modifier"].values() for v in m.values()]
            for animation in ids:
                self.assertRegex(animation, r"^(wom|epicsanji):", f"style {s['n']}: {animation}")
                # Antitheus is WOM's scythe; the user does not want it
                self.assertNotIn("antitheus", animation, f"style {s['n']}")
            self.assertNotIn("innate_skills", built)

    def test_uppercuts_are_distinct(self):
        self.assertEqual(len({s["combos"][3] for s in STYLES}), len(STYLES))

    def test_all_animation_ids_known(self):
        for s in STYLES:
            for animation in s["combos"]:
                self.assertIn(animation, KNOWN, f"style {s['n']}")

    def test_build_type_rejects_unknown_animation(self):
        bad = dict(STYLES[0], combos=STYLES[0]["combos"][:7] + ["wom:biped/combat/no_such_animation"])
        with self.assertRaises(ValueError):
            generate.build_type(bad, KNOWN)

    def test_living_motion(self):
        for s in STYLES:
            if s.get("custom"):
                continue
            living = generate.build_type(s, KNOWN)["livingmotion_modifier"]
            self.assertEqual(living["one_hand"]["idle"], f"wom:biped/living/{s['idle']}_idle")
            self.assertEqual(living["two_hand"]["idle"], f"wom:biped/living/{s['idle']}_idle")
            self.assertEqual(living["one_hand"]["block"], "wom:biped/skill/solar_guard")
        # run/walk are only written when WOM really has that animation for the idle set
        for s in STYLES:
            if s.get("custom"):
                continue
            for hand in ("one_hand", "two_hand"):
                living = generate.build_type(s, KNOWN)["livingmotion_modifier"][hand]
                for key in ("run", "walk"):
                    has = f"wom:biped/living/{s['idle']}_{key}" in KNOWN
                    self.assertEqual(key in living, has, (s["n"], key))
                if "run" in living:
                    self.assertEqual(living["chase"], living["run"])
                else:
                    self.assertNotIn("chase", living)

    def test_capability_type_matches_file(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            generate.write_all(root, STYLES, KNOWN)
            weapons = root / "src/main/resources/data/fiststyle/capabilities/weapons"
            for s in STYLES:
                n = s["n"]
                capability = generate.build_capability(s)
                self.assertEqual(capability["type"], f"fiststyle:style_{n}")
                self.assertEqual(
                    capability["attributes"]["common"],
                    {"armor_negation": s.get("armor_negation", 0.0), "impact": s.get("impact", round(0.5 + 0.15 * (n - 1), 2)), "max_strikes": s.get("max_strikes", 1)},
                )
                self.assertTrue((weapons / "types" / f"style_{n}.json").is_file())
                written = json.loads((weapons / f"style_{n}.json").read_text(encoding="utf-8"))
                self.assertEqual(written, capability)

    def test_item_model_hides_held_item(self):
        model = generate.build_item_model()
        self.assertEqual(model["parent"], "item/handheld")
        self.assertEqual(model["textures"]["layer0"], "fiststyle:item/style_s")
        for key in HELD_ITEM_DISPLAY_KEYS:
            self.assertEqual(model["display"][key]["scale"], [0, 0, 0], key)

    def test_texture(self):
        image = generate.build_texture()
        self.assertEqual(image.mode, "RGBA")
        self.assertEqual(image.size, (16, 16))
        pixels = [image.getpixel((x, y)) for y in range(16) for x in range(16)]
        self.assertTrue(all(p in ((0, 0, 0, 255), (255, 255, 255, 255)) for p in pixels))
        self.assertGreaterEqual(pixels.count((255, 255, 255, 255)), 10)
        self.assertEqual(image.getpixel((0, 0)), (0, 0, 0, 255))

    def test_lang(self):
        lang = generate.build_lang(STYLES)
        self.assertEqual(lang["item.fiststyle.style_1"], "Style 1")
        self.assertEqual(lang["itemGroup.fiststyle"], "Style")
        for key in ("key.style.parry", "key.style.dodge", "key.style.menu", "gui.style.title",
                    "gui.style.parry", "gui.style.dodge", "gui.style.on", "gui.style.off"):
            self.assertIn(key, lang)
        thai = generate.build_lang_th(STYLES)
        self.assertEqual(set(thai), set(lang))
        self.assertEqual(thai["gui.style.on"], "เปิด")

    def test_stats_java(self):
        java = generate.build_stats_java(STYLES)
        self.assertIn("package dev.fiststyle;", java)
        damage = re.search(r"double\[\] DAMAGE\s*=\s*\{([^}]*)\}", java).group(1)
        self.assertEqual([float(v) for v in damage.split(",")], [s["damage"] for s in STYLES])
        speed = re.search(r"double\[\] SPEED\s*=\s*\{([^}]*)\}", java).group(1)
        self.assertEqual([float(v) for v in speed.split(",")], [s["speed"] for s in STYLES])
        uppercut = re.search(r"String\[\] UPPERCUT\s*=\s*\{([^}]*)\}", java).group(1)
        self.assertEqual(re.findall(r'"([^"]+)"', uppercut), [s["combos"][3] for s in STYLES])

    def test_write_all_idempotent(self):
        with tempfile.TemporaryDirectory() as a, tempfile.TemporaryDirectory() as b:
            generate.write_all(Path(a), STYLES, KNOWN)
            generate.write_all(Path(b), STYLES, KNOWN)
            first, second = snapshot(Path(a)), snapshot(Path(b))
            self.assertEqual(first, second)
            # 12 x (capability, type, model) + 2 lang files + texture + java
            self.assertEqual(len(first), 7)


if __name__ == "__main__":
    unittest.main()
