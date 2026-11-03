#!/usr/bin/env python3
"""Collect the animation ids registered by one or more mod jars.

Usage: extract_animations.py <jar>... -o tools/known_animations.json

For every jar, string constants shaped like ``biped/combat/foo`` are pulled out
of the ``.class`` files and kept only when ``assets/<ns>/animmodels/animations/
<path>.json`` exists in the same jar. The result is a sorted JSON list of
``"<ns>:<path>"`` ids. Jars are read in memory with ``zipfile`` only; nothing is
extracted or executed.
"""
import argparse
import json
import re
import sys
import zipfile

PATH_RE = re.compile(rb"[a-z_0-9]+/[a-z_0-9/]+")
ANIM_RE = re.compile(r"assets/([^/]+)/animmodels/animations/(.+)\.json$")


def extract(jar_path):
    """Return the set of "<ns>:<path>" animation ids found in one jar."""
    found = set()
    with zipfile.ZipFile(jar_path) as jar:
        names = jar.namelist()
        files = set(names)
        namespaces = {m.group(1) for m in map(ANIM_RE.match, names) if m}
        for name in names:
            if not name.endswith(".class"):
                continue
            for raw in PATH_RE.findall(jar.read(name)):
                text = raw.decode("ascii")
                # The 1-byte length that precedes a constant-pool string can
                # itself be a [a-z0-9_] character, so also try without the
                # first character.
                for candidate in (text, text[1:]):
                    for ns in namespaces:
                        if f"assets/{ns}/animmodels/animations/{candidate}.json" in files:
                            found.add(f"{ns}:{candidate}")
    return found


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("jars", nargs="+", help="mod jars to scan")
    parser.add_argument("-o", "--output", required=True, help="output JSON file")
    args = parser.parse_args(argv)

    ids = set()
    for jar in args.jars:
        ids |= extract(jar)
    with open(args.output, "w", encoding="utf-8") as out:
        json.dump(sorted(ids), out, indent=2)
        out.write("\n")
    print(f"{len(ids)} animation ids -> {args.output}", file=sys.stderr)


if __name__ == "__main__":
    main()
