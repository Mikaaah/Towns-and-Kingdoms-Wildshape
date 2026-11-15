"""Installed-server SkyLands upgrade fixture; downloads optional mods, never bundles them."""
import argparse
import csv
import hashlib
import io
import json
import os
from pathlib import Path
import shutil
import socket
import struct
import subprocess
import time
import urllib.request
import zipfile
import zlib
from concurrent.futures import ThreadPoolExecutor


def download(url, path, expected=None):
    if not path.exists():
        path.parent.mkdir(parents=True, exist_ok=True)
        request = urllib.request.Request(url, headers={"User-Agent": "Wildshape-Skylands-compatibility-test"})
        with urllib.request.urlopen(request, timeout=90) as response, path.open("wb") as output:
            shutil.copyfileobj(response, output)
    digest = hashlib.sha256(path.read_bytes()).hexdigest()
    if expected and digest != expected:
        raise RuntimeError(f"Unexpected SHA-256 for {path.name}: {digest}")
    return {"file": path.name, "bytes": path.stat().st_size, "sha256": digest}


def prepare(root):
    dependencies = root / "dependencies"
    wanted = {"lithostitched", "geckolib", "terrablender", "glitchcore",
              "natures_spirit", "biomesoplenty", "regions_unexplored"}
    rows = [r for r in csv.DictReader(open("docs/audit/beta-1.0/verified-dependencies.csv"))
            if any(mod.split("@")[0] in wanted for mod in r["mods"].split(";"))]
    assert len(rows) == len(wanted), rows

    def pack_dependency(row):
        _, project, file = row["jar"].removesuffix(".jar").split("-")
        url = f"https://www.cursemaven.com/curse/maven/fixture-{project}/{file}/fixture-{project}-{file}.jar"
        return download(url, dependencies / row["jar"], row["sha256"])

    with ThreadPoolExecutor(max_workers=4) as executor:
        inspected = list(executor.map(pack_dependency, rows))
    inspected.append(download(
        "https://www.cursemaven.com/curse/maven/skylands-neoforge-port-1735016/9583195/skylands-neoforge-port-1735016-9583195.jar",
        dependencies / "skylands-0.9.4.jar",
        "457a67022d025992b25739a25eaec77a66c4abe1dce766d0f0df1173c41c752e"))
    with zipfile.ZipFile(dependencies / "skylands-0.9.4.jar") as jar:
        dimension = json.loads(jar.read("data/minecraft/dimension_type/overworld.json"))
        assert dimension["min_y"] + dimension["height"] == 528, dimension
        assert b'"min_inclusive": 290' in jar.read("data/skylands/lithostitched/biome_injector/sky_climate_layout.json")
        inspected[-1]["nativeOverworldDimension"] = dimension
    (root / "dependency-inspection.json").write_text(json.dumps(inspected, indent=2))
    return dependencies


def nbt_value(stream, tag):
    def number(fmt):
        size = struct.calcsize(fmt)
        return struct.unpack(fmt, stream.read(size))[0]
    def string():
        return stream.read(number(">H")).decode("utf8")
    if tag in (1, 2, 3, 4, 5, 6):
        return number({1: ">b", 2: ">h", 3: ">i", 4: ">q", 5: ">f", 6: ">d"}[tag])
    if tag == 7:
        return stream.read(number(">i"))
    if tag == 8:
        return string()
    if tag == 9:
        kind, count = number(">B"), number(">i")
        return [nbt_value(stream, kind) for _ in range(count)]
    if tag == 10:
        result = {}
        while (kind := number(">B")) != 0:
            name = string()
            result[name] = nbt_value(stream, kind)
        return result
    if tag in (11, 12):
        return [number(">i" if tag == 11 else ">q") for _ in range(number(">i"))]
    raise ValueError(f"Unsupported NBT tag {tag}")


def chunk_nbt(world, x, z):
    region = world / "region" / f"r.{x // 32}.{z // 32}.mca"
    with region.open("rb") as file:
        file.seek(4 * ((x % 32) + (z % 32) * 32))
        address = int.from_bytes(file.read(4), "big") >> 8
        assert address, (x, z, "chunk not saved")
        file.seek(address * 4096)
        count = int.from_bytes(file.read(4), "big")
        kind = file.read(1)[0]
        raw = file.read(count - 1)
    assert kind == 2, (x, z, kind)
    stream = io.BytesIO(zlib.decompress(raw))
    assert stream.read(1) == b"\x0a"
    stream.read(int.from_bytes(stream.read(2), "big"))
    return nbt_value(stream, 10)


def palette_digest(container, count, minimum_bits):
    palette = container["palette"]
    encoded = [json.dumps(item, sort_keys=True, separators=(",", ":")).encode() + b"\n" for item in palette]
    if len(palette) == 1:
        return hashlib.sha256(encoded[0] * count).hexdigest()
    bits = max(minimum_bits, (len(palette) - 1).bit_length())
    per_long, mask = 64 // bits, (1 << bits) - 1
    data = container["data"]
    digest = hashlib.sha256()
    for index in range(count):
        value = (data[index // per_long] & ((1 << 64) - 1)) >> ((index % per_long) * bits)
        digest.update(encoded[value & mask])
    return digest.hexdigest()


def snapshot(world, x, z, max_section):
    data = chunk_nbt(world, x, z)
    assert data["Status"] == "minecraft:full", data["Status"]
    sections = {}
    for section in data["sections"]:
        if section["Y"] >= max_section or "block_states" not in section:
            continue
        sections[str(section["Y"])] = {
            "blocks": palette_digest(section["block_states"], 4096, 4),
            "biomes": palette_digest(section["biomes"], 64, 1),
        }
    return {"chunk": [x, z], "status": data["Status"], "sections": sections}


def rcon(command):
    def packet(sock, request_id, kind, payload):
        body = struct.pack("<ii", request_id, kind) + payload.encode() + b"\0\0"
        sock.sendall(struct.pack("<i", len(body)) + body)
        def read(count):
            result = b""
            while len(result) < count:
                chunk = sock.recv(count - len(result))
                if not chunk:
                    raise ConnectionError("RCON closed")
                result += chunk
            return result
        size = struct.unpack("<i", read(4))[0]
        response = read(size)
        assert struct.unpack("<i", response[:4])[0] != -1, "RCON auth failed"
        return response[8:-2].decode()
    with socket.create_connection(("127.0.0.1", 25585), timeout=10) as sock:
        packet(sock, 1, 3, "wildshape-test-only")
        return packet(sock, 2, 2, command)


def properties(server, world):
    (server / "server.properties").write_text(f"""level-name={world}
level-type=wildshape:wildshape
level-seed=2031102026
online-mode=false
server-ip=127.0.0.1
server-port=25586
enable-rcon=true
rcon.port=25585
rcon.password=wildshape-test-only
view-distance=2
simulation-distance=2
max-tick-time=-1
spawn-protection=0
""")


def run_server(root, server, mode=None):
    phase = mode or "baseline-1.0.2"
    log = root / f"{phase}.log"
    env = os.environ.copy()
    if mode:
        env["JAVA_TOOL_OPTIONS"] = env.get("JAVA_TOOL_OPTIONS", "") + f" -Dwildshape.skylandsSmoke={mode}"
    with log.open("w") as output:
        process = subprocess.Popen(["bash", "run.sh", "nogui"], cwd=server, env=env,
                                   stdin=subprocess.PIPE, stdout=output, stderr=subprocess.STDOUT)
        try:
            if mode is None:
                deadline = time.monotonic() + 300
                while time.monotonic() < deadline:
                    assert process.poll() is None, f"Baseline server exited; see {log}"
                    try:
                        if "time" in rcon("time query daytime").lower():
                            break
                    except (OSError, AssertionError):
                        pass
                    time.sleep(2)
                else:
                    raise TimeoutError("Baseline startup")
                for command in ["gamerule spawnChunkRadius 0", "gamerule randomTickSpeed 0",
                                "gamerule doMobSpawning false", "gamerule doWeatherCycle false",
                                "gamerule doDaylightCycle false", "forceload add 0 0"]:
                    rcon(command)
                deadline = time.monotonic() + 180
                while "Test passed" not in rcon("execute if loaded 8 90 8"):
                    if time.monotonic() >= deadline:
                        raise TimeoutError("Existing baseline chunk")
                    time.sleep(1)
                rcon("setblock 8 90 8 minecraft:diamond_block")
                assert "Test passed" in rcon("execute if block 8 90 8 minecraft:diamond_block")
                rcon("save-all flush")
                rcon("stop")
                process.wait(timeout=90)
            else:
                process.wait(timeout=480)
            assert process.returncode == 0, f"Server {phase} exit {process.returncode}; see {log}"
            if mode:
                report_file = server / ("skylands-generate.json" if mode == "generate" else f"skylands-{mode}.json")
                report = json.loads(report_file.read_text())
                shutil.copyfile(report_file, root / report_file.name)
                assert report["passed"], report
                return report
        finally:
            if process.poll() is None:
                process.terminate()
                try:
                    process.wait(timeout=20)
                except subprocess.TimeoutExpired:
                    process.kill()
                    process.wait()
            print(f"{phase}: exit={process.returncode}", flush=True)
            if process.returncode:
                print(log.read_text(errors="replace")[-16000:])


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--artifact", type=Path)
    parser.add_argument("--work", type=Path, default=Path("skylands-smoke"))
    parser.add_argument("--prepare-only", action="store_true")
    args = parser.parse_args()
    root = args.work.resolve()
    root.mkdir(parents=True, exist_ok=True)
    deps = prepare(root)
    if args.prepare_only:
        return
    assert args.artifact and args.artifact.is_file(), "Pass the verified new JAR with --artifact"
    baseline = root / "wildshape-1.0.2.jar"
    download("https://raw.githubusercontent.com/Mikaaah/Towns-and-Kingdoms-Wildshape/publish/curseforge-1.0.2/release-assets/1.0.2/wildshape-1.0.2-neoforge-1.21.1.jar",
             baseline, "d26e23ec016b9d6f94a700bdc4d09ebf3bd64027b8b46295a450cb8add49339d")
    installer = root / "neoforge-installer.jar"
    download("https://maven.neoforged.net/releases/net/neoforged/neoforge/21.1.266/neoforge-21.1.266-installer.jar", installer)
    server = root / "server"
    server.mkdir(exist_ok=True)
    with (root / "installer.log").open("w") as output:
        subprocess.run(["java", "-jar", str(installer), "--installServer"], cwd=server,
                       stdout=output, stderr=subprocess.STDOUT, check=True)
    (server / "eula.txt").write_text("eula=true\n")
    (server / "user_jvm_args.txt").write_text("-Xms512M\n-Xmx3G\n-XX:ActiveProcessorCount=4\n")
    mods = server / "mods"
    mods.mkdir(exist_ok=True)
    shutil.copyfile(baseline, mods / "wildshape.jar")
    properties(server, "upgrade-world")
    run_server(root, server)
    world = server / "upgrade-world"
    preset_file = world / "datapacks/wildshape-preset.zip"
    preset_hash = hashlib.sha256(preset_file.read_bytes()).hexdigest()
    with zipfile.ZipFile(preset_file) as pack:
        dimension = json.loads(pack.read("data/minecraft/dimension_type/overworld.json"))
        noise = json.loads(pack.read("data/minecraft/worldgen/noise_settings/overworld.json"))
    baseline_info = {"version": "1.0.2", "noiseHeight": noise["noise"]["height"],
                     "minY": dimension["min_y"], "maxYExclusive": dimension["min_y"] + dimension["height"]}
    (server / "skylands-baseline.json").write_text(json.dumps(baseline_info, indent=2))
    (root / "skylands-baseline.json").write_text(json.dumps(baseline_info, indent=2))
    before = snapshot(world, 0, 0, baseline_info["maxYExclusive"] // 16)
    (root / "existing-chunk-before.json").write_text(json.dumps(before, indent=2))
    shutil.copyfile(args.artifact, mods / "wildshape.jar")
    for jar in deps.glob("*.jar"):
        shutil.copyfile(jar, mods / jar.name)
    generated = run_server(root, server, "generate")
    after = snapshot(world, 0, 0, baseline_info["maxYExclusive"] // 16)
    (root / "existing-chunk-after.json").write_text(json.dumps(after, indent=2))
    assert before == after, "Existing chunk blocks or biomes changed during upgrade"
    assert hashlib.sha256(preset_file.read_bytes()).hexdigest() == preset_hash, "Saved terrain preset was rewritten"
    x, z = generated["chunk"]
    island_before = snapshot(world, x, z, 33)
    (root / "island-before-reload.json").write_text(json.dumps(island_before, indent=2))
    reloaded = run_server(root, server, "reload")
    island_after = snapshot(world, x, z, 33)
    (root / "island-after-reload.json").write_text(json.dumps(island_after, indent=2))
    assert island_before == island_after, "Saved island blocks or biomes changed after reload"
    assert generated["skyBlocksInSkyLayer"] == reloaded["skyBlocksInSkyLayer"]
    for jar in mods.glob("*.jar"):
        if jar.name != "wildshape.jar":
            jar.unlink()
    properties(server, "absent-world")
    absent = run_server(root, server, "absent")
    assert generated["otherDimensions"] == absent["otherDimensions"], "Other dimension heights changed"
    summary = {"passed": True, "baselineVersion": "1.0.2", "runtimeNeoForge": "21.1.266",
               "skylands": "0.9.4", "existingChunkBlocksAndBiomesPreserved": True,
               "savedPresetUnchanged": True, "savedPresetSha256": preset_hash,
               "islandBlocksAndBiomesSurviveReload": True, "withoutOptionalMods": True,
               "otherDimensionsUnchanged": True, "artifactSha256": hashlib.sha256(args.artifact.read_bytes()).hexdigest()}
    (root / "summary.json").write_text(json.dumps(summary, indent=2))
    print(json.dumps(summary, indent=2))


if __name__ == "__main__":
    main()
