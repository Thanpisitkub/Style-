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
    def test_stats_in_range_and_ordered(self):
        self.assertEqual(len(STYLES), 10)
        self.assertEqual([s["n"] for s in STYLES], list(range(1, 11)))
        self.assertEqual((STYLES[0]["damage"], STYLES[0]["speed"]), (4.0, 2.5))
        self.assertEqual((STYLES[9]["damage"], STYLES[9]["speed"]), (8.0, 1.5))
        for s in STYLES:
            self.assertTrue(4.0 <= s["damage"] <= 8.0, s["n"])
            self.assertTrue(1.5 <= s["speed"] <= 2.5, s["n"])
        for a, b in zip(STYLES, STYLES[1:]):
            self.assertLessEqual(a["damage"], b["damage"])
            self.assertGreaterEqual(a["speed"], b["speed"])

    def test_combo_shape(self):
        for s in STYLES:
            combos = generate.build_type(s, KNOWN)["combos"]
            self.assertEqual(combos["one_hand"], s["combos"])
            self.assertEqual(combos["two_hand"], s["combos"])
            self.assertEqual(len(combos["one_hand"]), 8)
            self.assertEqual(combos["mount"], ["epicfight:biped/combat/sword_mount_attack"])

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
            living = generate.build_type(s, KNOWN)["livingmotion_modifier"]
            self.assertEqual(living["one_hand"]["idle"], f"wom:biped/living/{s['idle']}_idle")
            self.assertEqual(living["two_hand"]["idle"], f"wom:biped/living/{s['idle']}_idle")
            self.assertEqual(living["one_hand"]["block"], "epicfight:biped/skill/guard_uchigatana")
        for hand in ("one_hand", "two_hand"):
            living = generate.build_type(STYLES[1], KNOWN)["livingmotion_modifier"][hand]
            self.assertNotIn("run", living)
            self.assertNotIn("chase", living)
            living = generate.build_type(STYLES[0], KNOWN)["livingmotion_modifier"][hand]
            self.assertIn("run", living)
            self.assertIn("chase", living)
            self.assertEqual(living["chase"], living["run"])

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
                    {"armor_negation": 0.0, "impact": round(0.5 + 0.15 * (n - 1), 2), "max_strikes": 1},
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
        self.assertEqual(lang["item.fiststyle.style_10"], "Style 10")
        self.assertEqual(lang["itemGroup.fiststyle"], "Style")

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
            # 10 x (capability, type, model) + lang + texture + java
            self.assertEqual(len(first), 33)


if __name__ == "__main__":
    unittest.main()
