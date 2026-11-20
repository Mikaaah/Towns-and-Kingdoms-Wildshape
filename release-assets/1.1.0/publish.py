"""Publish the existing, verified Wildshape 1.1.0 artifact without rebuilding."""

import hashlib
import json
import os
from pathlib import Path
import urllib.error
import urllib.request
import uuid

ROOT = Path(__file__).resolve().parent
JAR = ROOT / "wildshape-1.1.0-neoforge-1.21.1.jar"
EXPECTED_SHA256 = "c60a7b057b1fd6c4e123dce4c1d819f5e6d7a2f1b71b3e3c46b8b277bff02887"
PROJECT_ID = 1792816
API = "https://minecraft.curseforge.com/api"
TOKEN = os.environ.get("CURSEFORGE_TOKEN", "").strip()

if not TOKEN:
    raise SystemExit("Repository secret CURSEFORGE_TOKEN is unavailable.")

jar_bytes = JAR.read_bytes()
digest = hashlib.sha256(jar_bytes).hexdigest()
if digest != EXPECTED_SHA256:
    raise SystemExit("JAR checksum does not match the verified release artifact.")

headers = {"X-Api-Token": TOKEN, "User-Agent": "Wildshape-Verified-Release/1.1.0"}
game_versions = [11779, 10150]  # Minecraft 1.21.1, NeoForge

metadata = {
    "changelog": (ROOT / "CHANGELOG.html").read_text(encoding="utf-8"),
    "changelogType": "html",
    "displayName": JAR.name,
    "gameVersions": game_versions,
    "releaseType": "release",
    "isMarkedForManualRelease": False,
}
boundary = f"wildshape-{uuid.uuid4().hex}"
body = (
    f'--{boundary}\r\nContent-Disposition: form-data; name="metadata"\r\n'
    'Content-Type: application/json\r\n\r\n'
).encode() + json.dumps(metadata).encode("utf-8") + (
    f'\r\n--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="{JAR.name}"\r\n'
    'Content-Type: application/java-archive\r\n\r\n'
).encode() + jar_bytes + f"\r\n--{boundary}--\r\n".encode()

request = urllib.request.Request(
    f"{API}/projects/{PROJECT_ID}/upload-file",
    data=body,
    headers={**headers, "Content-Type": f"multipart/form-data; boundary={boundary}"},
    method="POST",
)
try:
    with urllib.request.urlopen(request, timeout=120) as response:
        result = json.load(response)
except urllib.error.HTTPError as error:
    detail = error.read().decode("utf-8", errors="replace").replace(TOKEN, "[REDACTED]")
    raise SystemExit(f"CurseForge upload failed: HTTP {error.code}: {detail[:2000]}")

file_id = result.get("id")
if not isinstance(file_id, int) or file_id <= 0:
    raise SystemExit(f"Upload returned no valid file ID: {json.dumps(result)}")

receipt = {
    "project_id": PROJECT_ID,
    "file_id": file_id,
    "filename": JAR.name,
    "sha256": digest,
    "size_bytes": len(jar_bytes),
    "game_versions": game_versions,
    "release_type": "release",
    "changelog_type": "html",
    "url": f"https://www.curseforge.com/minecraft/mc-mods/towns-kingdoms-wildshape/files/{file_id}",
}
(ROOT / "curseforge-upload-result.json").write_text(json.dumps(receipt, indent=2) + "\n")
print(json.dumps(receipt, indent=2))
with open(os.environ["GITHUB_STEP_SUMMARY"], "a", encoding="utf-8") as summary:
    summary.write(f"Uploaded verified **{JAR.name}** with its HTML changelog.\n\n")
    summary.write(f"[CurseForge file]({receipt['url']})\n\nSHA-256: `{digest}`\n")
