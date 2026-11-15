#!/usr/bin/env python3
"""Inventory exact dependency JARs offline; report resource collisions without guessing load order."""
import argparse
import hashlib
import json
import re
import tomllib
import zipfile
from collections import defaultdict
from pathlib import Path


def json_resource(data):
    text = data.decode("utf-8-sig")
    # Gson accepts comments and trailing commas in many shipped worldgen files.
    # Match quoted strings first so URLs and comment-looking string values survive.
    text = re.sub(r'"(?:\\.|[^"\\])*"|(/\*.*?\*/|//[^\r\n]*)',
                  lambda match: "" if match.group(1) else match.group(0), text, flags=re.S)
    text = re.sub(r',(?=\s*[}\]])', "", text)
    return json.loads(text)


def resource_key(path, folder):
    return path.split("/")[1] + ":" + path.split(folder, 1)[1][:-5]


def inventory(jar):
    result = {"jar": jar.name, "sha256": hashlib.file_digest(jar.open("rb"), "sha256").hexdigest(),
              "bytes": jar.stat().st_size, "mods": [], "structureSets": {}, "structures": {},
              "biomes": {}, "biomeTags": {}, "biomeModifiers": {}, "parseErrors": []}
    with zipfile.ZipFile(jar) as archive:
        for manifest in ("META-INF/neoforge.mods.toml", "META-INF/mods.toml"):
            if manifest in archive.namelist():
                metadata = tomllib.loads(archive.read(manifest).decode())
                result["mods"] = [{key: mod.get(key) for key in ("modId", "version", "displayName")}
                                  for mod in metadata.get("mods", [])]
                result["dependencies"] = metadata.get("dependencies", {})
                break
        for path in archive.namelist():
            if not path.startswith("data/") or not path.endswith(".json"):
                continue
            bucket = folder = None
            if "/tags/worldgen/biome/" in path:
                bucket, folder = "biomeTags", "/tags/worldgen/biome/"
            elif path.split("/")[2:4] == ["worldgen", "biome"]:
                bucket, folder = "biomes", "/worldgen/biome/"
            elif "/tags/" not in path:
                for marker, target in (("/worldgen/structure_set/", "structureSets"),
                                       ("/worldgen/structure/", "structures"),
                                       ("/neoforge/biome_modifier/", "biomeModifiers")):
                    if marker in path:
                        bucket, folder = target, marker
                        break
            if bucket:
                try:
                    result[bucket][resource_key(path, folder)] = json_resource(archive.read(path))
                except (UnicodeDecodeError, ValueError) as error:
                    result["parseErrors"].append({"resource": path, "error": str(error)})
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jars", nargs="+", type=Path, required=True, help="JARs or directories of JARs")
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    jars = sorted({jar.resolve() for source in args.jars
                   for jar in (source.glob("*.jar") if source.is_dir() else [source])})
    records = [inventory(jar) for jar in jars]
    providers = defaultdict(list)
    for record in records:
        for bucket in ("structureSets", "structures", "biomes", "biomeTags", "biomeModifiers"):
            for key in record[bucket]:
                providers[bucket + "/" + key].append(record["jar"])
    report = {"note": "Static dependency inventory. Collisions can be intentional replacements; actual load order requires a live registry check.",
              "jars": records, "multipleProviders": {key: names for key, names in sorted(providers.items()) if len(names) > 1},
              "counts": {"jars": len(records), "mods": sum(len(record["mods"]) for record in records),
                         **{bucket: sum(len(record[bucket]) for record in records)
                            for bucket in ("structureSets", "structures", "biomes", "biomeTags", "biomeModifiers")}}}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n")
    print(json.dumps(report["counts"]))
    errors = sum(len(record["parseErrors"]) for record in records)
    print(f"Unparsed worldgen resources: {errors}")


if __name__ == "__main__":
    main()
