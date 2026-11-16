#!/usr/bin/env python3
"""Reproduce the full-list *static* compatibility inventory from pinned public JARs.

Does not start Minecraft, resolve dependency closure, or claim runtime compatibility.
Third-party JARs remain in --download-dir and must not be redistributed as artifacts.
"""
import argparse
import concurrent.futures
import csv
import hashlib
import importlib.util
import json
import re
import zipfile
from pathlib import Path
from urllib.request import Request, urlopen
from collections import defaultdict

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_LOCK = ROOT / 'docs/audit/full-compat-1.0.5/dependency-lock.json'
spec = importlib.util.spec_from_file_location('jar_audit', ROOT / 'tools/audit-compat-jars.py')
jar_audit = importlib.util.module_from_spec(spec)
spec.loader.exec_module(jar_audit)


def fetch_or_verify(record, directory, download):
    path = directory / record['jar']
    if not path.exists():
        if not download or not record.get('url'):
            return record, None, 'artifact bytes unavailable; provide the pinned file locally'
        try:
            request = Request(record['url'], headers={'User-Agent': 'Wildshape/1.0.5-static-audit'})
            with urlopen(request, timeout=120) as stream:
                data = stream.read()
            temporary = path.with_suffix('.part')
            temporary.write_bytes(data)
            if hashlib.sha256(data).hexdigest() != record['sha256']:
                temporary.unlink(missing_ok=True)
                return record, None, 'SHA-256 mismatch; artifact was not accepted'
            temporary.replace(path)
        except Exception as error:
            return record, None, str(error)
    if hashlib.file_digest(path.open('rb'), 'sha256').hexdigest() != record['sha256']:
        return record, None, 'SHA-256 mismatch; artifact was not accepted'
    result = jar_audit.inventory(path)
    # NeoForge expands ${file.jarVersion} from the JAR manifest at load time.
    with zipfile.ZipFile(path) as archive:
        manifest = archive.read('META-INF/MANIFEST.MF').decode() if 'META-INF/MANIFEST.MF' in archive.namelist() else ''
    version_match = re.search(r'^Implementation-Version: (.+)$', manifest, re.M)
    if version_match:
        for mod in result['mods']:
            if mod.get('version') == '${file.jarVersion}':
                mod['versionRaw'] = mod['version']
                mod['version'] = version_match.group(1).strip()
    return record, result, None


def write_csv(path, rows, fields):
    with path.open('w', newline='') as stream:
        writer = csv.DictWriter(stream, fieldnames=fields)
        writer.writeheader()
        writer.writerows(rows)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--lock', type=Path, default=DEFAULT_LOCK)
    parser.add_argument('--download-dir', type=Path, required=True)
    parser.add_argument('--output-dir', type=Path, required=True)
    parser.add_argument('--download', action='store_true')
    args = parser.parse_args()
    args.download_dir.mkdir(parents=True, exist_ok=True)
    args.output_dir.mkdir(parents=True, exist_ok=True)
    lock = json.loads(args.lock.read_text())
    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as executor:
        results = list(executor.map(lambda r: fetch_or_verify(r, args.download_dir, args.download), lock['artifacts']))
    jars = [inventory for record, inventory, error in results if inventory]
    errors = [{'mod': record['mod_id'], 'jar': record['jar'], 'reason': error}
              for record, inventory, error in results if error]
    providers = defaultdict(list)
    set_rows, structure_rows, matrix = [], [], []
    for record, inventory, error in results:
        if not inventory:
            matrix.append({'requested_id': record['mod_id'], 'actual_mod_ids': '', 'versions': record['version'],
                           'jar': record['jar'], 'sha256': record['sha256'], 'status': 'missing bytes',
                           'structure_sets': '', 'structures': '', 'biomes': '', 'biome_tags': '',
                           'biome_modifiers': '', 'source': record['source']})
            continue
        matrix.append({'requested_id': record['mod_id'],
                       'actual_mod_ids': ';'.join(m.get('modId') or '' for m in inventory['mods']),
                       'versions': ';'.join(str(m.get('version') or '') for m in inventory['mods']),
                       'jar': record['jar'], 'sha256': inventory['sha256'], 'status': 'static JAR inspection; runtime unverified',
                       'structure_sets': len(inventory['structureSets']), 'structures': len(inventory['structures']),
                       'biomes': len(inventory['biomes']), 'biome_tags': len(inventory['biomeTags']),
                       'biome_modifiers': len(inventory['biomeModifiers']), 'source': record['source']})
        for bucket in ('structureSets', 'structures', 'biomes', 'biomeTags', 'biomeModifiers'):
            for key, value in inventory[bucket].items():
                providers[bucket + '/' + key].append({'jar': inventory['jar'], 'payload': value})
        for key, value in inventory['structureSets'].items():
            placement = value.get('placement', {})
            set_rows.append({'set_id': key, 'source_jar': inventory['jar'],
                             'placement_type': placement.get('type', ''), 'salt': placement.get('salt', ''),
                             'spacing': placement.get('spacing', ''), 'separation': placement.get('separation', ''),
                             'spread_type': placement.get('spread_type', 'linear'),
                             'structures': ';'.join(x.get('structure', '') for x in value.get('structures', [])),
                             'native_exclusion': json.dumps({k: v for k, v in placement.items() if 'exclusion' in k}, sort_keys=True)})
        for key, value in inventory['structures'].items():
            structure_rows.append({'structure_id': key, 'source_jar': inventory['jar'], 'type': value.get('type', ''),
                                   'step': value.get('step', ''), 'terrain_adaptation': value.get('terrain_adaptation', 'none'),
                                   'heightmap': value.get('project_start_to_heightmap', ''),
                                   'start_height': json.dumps(value.get('start_height', {}), sort_keys=True),
                                   'biomes': json.dumps(value.get('biomes', None)),
                                   'max_distance_from_center': value.get('max_distance_from_center', '')})
    collisions = {key: {'providers': [v['jar'] for v in values],
                        'identical_json': all(v['payload'] == values[0]['payload'] for v in values)}
                  for key, values in sorted(providers.items()) if len(values) > 1}
    salt_groups = defaultdict(list)
    salts = defaultdict(list)
    for row in set_rows:
        if row['salt'] != '':
            salts[row['salt']].append(row)
        # A shared candidate layout is stronger evidence than a repeated salt alone.
        salt_groups[(row['salt'], row['spacing'], row['separation'], row['spread_type'])].append(row)
    shared_layouts = [{'salt': salt, 'spacing': spacing, 'separation': separation, 'spread_type': spread,
                       'sets': sorted({r['set_id'] for r in rows}), 'providers': sorted({r['source_jar'] for r in rows})}
                      for (salt, spacing, separation, spread), rows in salt_groups.items()
                      if salt != '' and len({r['set_id'] for r in rows}) > 1]
    summary = {'note': 'Static resource inspection only. Optional dependency closure and live registry load order are not resolved. Repeated salts/layouts may be deliberate, especially Epic dungeon/obelisk pairs. Independent salts do not enforce cross-set clearance.',
               'locked_artifacts': len(lock['artifacts']), 'inspected_artifacts': len(jars), 'missing_or_mismatch': errors,
               'counts': {bucket: sum(len(j[bucket]) for j in jars) for bucket in ('structureSets', 'structures', 'biomes', 'biomeTags', 'biomeModifiers')},
               'parse_errors': sum(len(j['parseErrors']) for j in jars), 'multiple_provider_count': len(collisions),
               'unique_structure_set_ids': len({r['set_id'] for r in set_rows}),
               'repeated_salt_group_count': sum(1 for rows in salts.values() if len({r['set_id'] for r in rows}) > 1),
               'shared_candidate_layouts': shared_layouts}
    write_csv(args.output_dir/'compatibility-matrix.csv', matrix, list(matrix[0]) if matrix else [])
    write_csv(args.output_dir/'structure-sets.csv', set_rows, list(set_rows[0]) if set_rows else [])
    write_csv(args.output_dir/'structures.csv', structure_rows, list(structure_rows[0]) if structure_rows else [])
    for filename, content in [('resource-inventory.json', {'jars': jars}), ('resource-collisions.json', collisions), ('inventory-summary.json', summary),
                              ('repeated-salts.json', [{'salt': salt, 'sets': rows} for salt, rows in salts.items() if len({r['set_id'] for r in rows}) > 1])]:
        (args.output_dir/filename).write_text(json.dumps(content, indent=2, ensure_ascii=False)+'\n')
    print(json.dumps(summary, indent=2))
    if errors or summary['parse_errors']:
        raise SystemExit(1)


if __name__ == '__main__':
    main()
