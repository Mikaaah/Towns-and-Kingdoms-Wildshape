"""Fetch optional smoke-test mods; never bundle them into the distribution."""
import hashlib
import json
from pathlib import Path
from urllib.request import Request, urlopen
from urllib.parse import urlencode

out = Path('neoforge/.smoke-mods')
out.mkdir(parents=True, exist_ok=True)
expected = 'f022b34217c7ce56dd0bac1da253ae7e7a4017f1a3595bc20b434798a22991a3761fa5341874d0f4f1030dde8a18de3af6b269a93c7b5c3b6326f7a27151fb70'
for project in ['natures-spirit', 'lithostitched', 'terrablender']:
    query = urlencode({'game_versions': '["1.21.1"]', 'loaders': '["neoforge"]'})
    request = Request(f'https://api.modrinth.com/v2/project/{project}/version?{query}',
                      headers={'User-Agent': 'TNKTerraForged/compatibility-smoke'})
    versions = json.load(urlopen(request, timeout=60))
    if project == 'natures-spirit':
        files = [(v, f) for v in versions for f in v['files'] if f['hashes'].get('sha512') == expected]
    elif project == 'terrablender':
        versions = [v for v in versions if '4.1.0.8' in v['version_number']]
        assert versions, 'Pinned TerraBlender 4.1.0.8 missing'
        files = [(versions[0], f) for f in versions[0]['files'] if f.get('primary')]
    else:
        versions = [v for v in versions if v['version_type'] == 'release']
        files = [(versions[0], f) for f in versions[0]['files'] if f.get('primary')]
    assert files, f'Cannot locate exact compatible {project} smoke-test artifact'
    version, file = files[0]
    data = urlopen(file['url'], timeout=60).read()
    assert hashlib.sha512(data).hexdigest() == file['hashes']['sha512']
    (out / file['filename']).write_bytes(data)
    print(project, version['id'], version['version_number'], file['hashes']['sha512'])
