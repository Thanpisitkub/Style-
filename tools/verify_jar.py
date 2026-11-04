#!/usr/bin/env python3
"""Sanity-check the built mod jar before it is released.

Usage: python3 tools/verify_jar.py <path-to-jar>

Exits 0 when every check passes. On the first failed check it prints
"FAIL: <reason>" to stderr and exits 1.
"""
import json
import sys
import tomllib
import zipfile

MOD_ID = "fiststyle"
STYLE_COUNT = 10
REQUIRED_DEPENDENCIES = ("epicfight", "wom", "ef_cz", "epicsanji")
REQUIRED_CLASSES = (
    "dev/fiststyle/FistStyleMod.class",
    "dev/fiststyle/StyleItem.class",
    "dev/fiststyle/StyleStats.class",
    "dev/fiststyle/LaunchHandler.class",
)
PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"


class VerifyError(Exception):
    pass


def read_entry(jar: zipfile.ZipFile, name: str) -> bytes:
    try:
        return jar.read(name)
    except KeyError:
        raise VerifyError(f"missing entry {name}") from None


def check_json(jar: zipfile.ZipFile, name: str):
    data = read_entry(jar, name)
    try:
        return json.loads(data.decode("utf-8"))
    except (UnicodeDecodeError, ValueError) as error:
        raise VerifyError(f"{name} is not valid JSON ({error})") from None


def check_mods_toml(jar: zipfile.ZipFile) -> None:
    name = "META-INF/mods.toml"
    try:
        toml = tomllib.loads(read_entry(jar, name).decode("utf-8"))
    except (UnicodeDecodeError, tomllib.TOMLDecodeError) as error:
        raise VerifyError(f"{name} is not valid TOML ({error})") from None
    mod_ids = [mod.get("modId") for mod in toml.get("mods", [])]
    if MOD_ID not in mod_ids:
        raise VerifyError(f'{name} does not declare modId="{MOD_ID}" (found {mod_ids})')
    dependencies = toml.get("dependencies", {}).get(MOD_ID, [])
    mandatory = {dep.get("modId") for dep in dependencies if dep.get("mandatory") is True}
    for dep_id in REQUIRED_DEPENDENCIES:
        if dep_id not in mandatory:
            raise VerifyError(f'{name} has no mandatory dependency modId="{dep_id}"')


def check_styles(jar: zipfile.ZipFile) -> None:
    for n in range(1, STYLE_COUNT + 1):
        check_json(jar, f"data/{MOD_ID}/capabilities/weapons/style_{n}.json")
        check_json(jar, f"data/{MOD_ID}/capabilities/weapons/types/style_{n}.json")
        check_json(jar, f"assets/{MOD_ID}/models/item/style_{n}.json")


def check_texture(jar: zipfile.ZipFile) -> None:
    name = f"assets/{MOD_ID}/textures/item/style_s.png"
    if not read_entry(jar, name).startswith(PNG_SIGNATURE):
        raise VerifyError(f"{name} does not start with the PNG signature")


def check_lang(jar: zipfile.ZipFile) -> None:
    name = f"assets/{MOD_ID}/lang/en_us.json"
    lang = check_json(jar, name)
    keys = [f"item.{MOD_ID}.style_{n}" for n in range(1, STYLE_COUNT + 1)]
    keys.append(f"itemGroup.{MOD_ID}")
    for key in keys:
        if key not in lang:
            raise VerifyError(f"{name} is missing key {key}")


def check_classes(jar: zipfile.ZipFile) -> None:
    for name in REQUIRED_CLASSES:
        read_entry(jar, name)


def verify(jar_path: str) -> None:
    try:
        jar = zipfile.ZipFile(jar_path)
    except (OSError, zipfile.BadZipFile) as error:
        raise VerifyError(f"cannot open {jar_path} ({error})") from None
    with jar:
        check_mods_toml(jar)
        check_styles(jar)
        check_texture(jar)
        check_lang(jar)
        check_classes(jar)


def main(argv: list) -> int:
    if len(argv) != 2:
        print("usage: verify_jar.py <jar>", file=sys.stderr)
        return 2
    try:
        verify(argv[1])
    except VerifyError as error:
        print(f"FAIL: {error}", file=sys.stderr)
        return 1
    print(f"OK: {argv[1]}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
