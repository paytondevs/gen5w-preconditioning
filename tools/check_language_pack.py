#!/usr/bin/env python3
"""Check language pack format and placeholder compatibility with English."""
import json
import re
import sys
from collections import Counter
from pathlib import Path

PLACEHOLDER = re.compile(r"%(?:\d+\$)?[-+# 0,(<]*\d*(?:\.\d+)?[a-zA-Z%]")
TAG = re.compile(r"[a-z]{2,3}(?:-[A-Za-z0-9]{2,8})*")
KEY = re.compile(r"[a-z][a-z0-9_.-]*")


def load(path):
    data = json.loads(Path(path).read_text(encoding="utf-8"))
    if not isinstance(data, dict) or data.get("format") != 1:
        raise ValueError(f"{path}: invalid format")
    if not isinstance(data.get("locale"), str) or not TAG.fullmatch(data["locale"]):
        raise ValueError(f"{path}: invalid locale")
    if not isinstance(data.get("name"), str) or not data["name"].strip():
        raise ValueError(f"{path}: invalid display name")
    strings = data.get("strings")
    if not isinstance(strings, dict) or not all(isinstance(k, str) and KEY.fullmatch(k) and isinstance(v, str) for k, v in strings.items()):
        raise ValueError(f"{path}: invalid strings")
    return data


def main(paths):
    root = Path(__file__).resolve().parents[1]
    base = load(root / "languages/en.json")["strings"]
    seen = set()
    for path in paths:
        pack = load(path)
        bundled = {root / 'languages/en.json', root / 'languages/ko.json',
                   root / 'app/src/main/assets/languages/en.json', root / 'app/src/main/assets/languages/ko.json'}
        if pack['locale'] in {'en', 'ko'} and Path(path).resolve() not in bundled:
            raise ValueError(f"{path}: reserved locale")
        if pack["locale"] in seen:
            raise ValueError(f"{path}: duplicate locale")
        seen.add(pack["locale"])
        for key, value in pack["strings"].items():
            if key not in base:
                raise ValueError(f"{path}: unknown key {key}")
            if Counter(PLACEHOLDER.findall(value)) != Counter(PLACEHOLDER.findall(base[key])):
                raise ValueError(f"{path}: placeholders differ for {key}")
        print(f"{path}: {len(pack['strings'])}/{len(base)} strings valid")


if __name__ == "__main__":
    try:
        main(sys.argv[1:])
    except (ValueError, OSError) as e:
        print(e, file=sys.stderr)
        sys.exit(1)
