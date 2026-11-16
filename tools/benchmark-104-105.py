"""Paired installed-server benchmark of existing JARs; never rebuilds either mod.

The same saved-world template, dependencies and JVM flags are used in every trial.
FULL completion is checked in-game and independently from all saved target NBTs.
Run order is ABBAAB; each trial uses a fresh generation JVM and fresh reload JVM.
"""
from __future__ import annotations

import argparse
from collections import Counter
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import re
import shutil
import socket
import statistics
import struct
import subprocess
import threading
import time
import zipfile

HERE = Path(__file__).resolve().parents[1]
WORKSPACE = HERE.parent
JDK = WORKSPACE / "toolchain/zulu21.54.109-ca-crac-jdk21.0.13.0.3-linux_x64"
TARGETS = [(x, z) for x in range(442, 451) for z in range(352, 361)]
FIRST = (446, 356)
ORDER = ["A", "B", "B", "A", "A", "B"]
RCON_SOCKET = None
BATCHES = [[(x, z) for x in range(bx, bx + 3) for z in range(bz, bz + 3)]
           for bx in range(442, 451, 3) for bz in range(352, 361, 3)]
ARTIFACTS = {
    "A": (WORKSPACE / "release/wildshape-1.0.4-neoforge-1.21.1.jar",
          "a1838c6acdeb7492f0baccf050a4d257e9875efffccd9b6ff2a61d5d2dc469c0"),
    "B": (WORKSPACE / "release/wildshape-1.0.5-beta.1-neoforge-1.21.1.jar",
          "31d0dde5d20d67fb151a40240c1f074fafdbebe3235aa6857ab4b65a1fc722c6"),
}


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def write_json(path, value):
    path.write_text(json.dumps(value, indent=2) + "\n")


def rcon(command, timeout=150):
    global RCON_SOCKET
    def packet(sock, request, kind, text):
        payload = struct.pack("<ii", request, kind) + text.encode() + b"\0\0"
        sock.sendall(struct.pack("<i", len(payload)) + payload)
        def read(n):
            result = b""
            while len(result) < n:
                part = sock.recv(n - len(result))
                if not part:
                    raise ConnectionError("RCON closed")
                result += part
            return result
        response = read(struct.unpack("<i", read(4))[0])
        assert struct.unpack("<i", response[:4])[0] != -1, "RCON authentication failed"
        return response[8:-2].decode()
    if RCON_SOCKET is None:
        RCON_SOCKET = socket.create_connection(("127.0.0.1", 25595), timeout=timeout)
        packet(RCON_SOCKET, 1, 3, "wildshape-local-benchmark")
    RCON_SOCKET.settimeout(timeout)
    return packet(RCON_SOCKET, 2, 2, command)


class Samples:
    """Process measurements, not an inference from JVM reserved heap size."""
    def __init__(self, pid):
        self.pid = pid
        self.items = []
        self.phase = "startup"
        self.stop = threading.Event()
        self.thread = threading.Thread(target=self.run, daemon=True)
        self.thread.start()

    def run(self):
        while not self.stop.is_set():
            try:
                status = Path(f"/proc/{self.pid}/status").read_text()
                fields = dict(re.findall(r"^(VmRSS|VmHWM|VmSwap):\s+(\d+)", status, re.M))
                stat = Path(f"/proc/{self.pid}/stat").read_text().rsplit(") ", 1)[1].split()
                cpu = (int(stat[11]) + int(stat[12])) / os.sysconf("SC_CLK_TCK")
                self.items.append({"epoch": time.time(), "monotonic": time.monotonic(),
                                   "phase": self.phase, "rssKiB": int(fields["VmRSS"]),
                                   "hwmKiB": int(fields["VmHWM"]), "cpuSeconds": cpu})
            except (FileNotFoundError, ProcessLookupError, KeyError, PermissionError):
                pass
            self.stop.wait(0.10)

    def finish(self, destination):
        self.stop.set()
        self.thread.join(timeout=2)
        write_json(destination, self.items)


def install_poll_pack(world):
    pack = world / "datapacks/wildshape-benchmark"
    functions = pack / "data/wbench/function"
    functions.mkdir(parents=True, exist_ok=True)
    write_json(pack / "pack.mcmeta", {"pack": {"pack_format": 48,
                  "description": "Benchmark-only FULL chunk readiness probes"}})
    for name, targets in [("first", [FIRST]), ("all", TARGETS)] + [(f"batch{i}", batch) for i, batch in enumerate(BATCHES)]:
        lines = ["scoreboard players set ready wbench 0"]
        for x, z in targets:
            lines.append(f"execute if loaded {x * 16 + 8} 80 {z * 16 + 8} run scoreboard players add ready wbench 1")
        (functions / f"{name}.mcfunction").write_text("\n".join(lines) + "\n")


def setup(root):
    root.mkdir(parents=True, exist_ok=True)
    dependencies = root / "dependencies"
    dependencies.mkdir(exist_ok=True)
    # Verified biome/sky dependencies, plus Streams and actual boss/dungeon mods.
    sources = list((WORKSPACE / "skylands-fixture/dependencies").glob("cf-*.jar"))
    sources += [WORKSPACE / "performance-104/dependencies/skylands-0.9.4.1.jar",
                WORKSPACE / "performance-104/dependencies/StreamsReflowing-1.21.1-neoforge-2.17.3.jar"]
    summary = json.loads((HERE / "docs/audit/full-compat-1.0.5/runtime/structure-summary.json").read_text())
    for extra in summary["fixtureExtraArtifacts"]:
        matches = list((WORKSPACE / "full-compat-105").rglob(extra["file"]))
        assert matches, extra["file"]
        source = matches[0]
        assert sha(source) == extra["sha256"], source
        sources.append(source)
    import tomllib
    inspected = []
    for source in sources:
        target = dependencies / source.name
        if target.exists():
            assert sha(target) == sha(source)
        else:
            shutil.copyfile(source, target)
        with zipfile.ZipFile(target) as jar:
            metadata = tomllib.loads(jar.read("META-INF/neoforge.mods.toml").decode())
        inspected.append({"file": target.name, "sha256": sha(target),
                          "mods": [{"id": m["modId"], "version": m["version"]} for m in metadata["mods"]]})
    for key, (path, expected) in ARTIFACTS.items():
        assert sha(path) == expected, path
    template = root / "template"
    if not template.exists():
        shutil.copytree(WORKSPACE / "performance-104/world-template", template)
        for folder in ("region", "entities", "poi"):
            for rx in (13, 14):
                for rz in (10, 11):
                    (template / folder / f"r.{rx}.{rz}.mca").unlink(missing_ok=True)
    install_poll_pack(template)
    manifest = {"dependencies": inspected, "artifacts": {k: {"file": v[0].name, "sha256": v[1]} for k, v in ARTIFACTS.items()},
                "worldSeed": 2031102026, "presetSha256": sha(template / "datapacks/wildshape-preset.zip"),
                "neoForge": "21.1.266", "java": str(JDK), "javaVersion": subprocess.check_output([str(JDK / "bin/java"), "-version"], stderr=subprocess.STDOUT, text=True),
                "requestedTargets": TARGETS, "order": ORDER, "c2me": False, "byePregen": False, "generatorAccelerator": False,
                "requestBatchSize": 9, "requestBatchCount": 9, "persistentRconConnection": True,
                "threadsExposedToJava": 4, "heap": "512M initial / 3G maximum", "pollIntervalSeconds": 0.05,
                "hostCpuAffinityCount": len(os.sched_getaffinity(0)), "harnessSha256": sha(Path(__file__))}
    write_json(root / "environment.json", manifest)
    return template


def ready(process, log):
    deadline = time.monotonic() + 300
    while time.monotonic() < deadline:
        assert process.poll() is None, f"Server exited {process.returncode}; inspect {log}"
        if 'Done (' in log.read_text(errors="replace"):
            response = rcon("time query daytime", timeout=10)
            assert "time" in response.lower(), response
            return
        time.sleep(0.5)
    raise TimeoutError(f"Server readiness: {log}")


def await_loaded(process, function, expected, timeout=180):
    deadline = time.monotonic() + timeout
    polls = 0
    while time.monotonic() < deadline:
        assert process.poll() is None, f"Server exited {process.returncode}"
        rcon(f"function wbench:{function}")
        answer = rcon("scoreboard players get ready wbench")
        match = re.search(r"ready has (\d+)", answer)
        assert match, answer
        polls += 1
        if int(match[1]) == expected:
            return polls
        time.sleep(0.05)
    raise TimeoutError(f"Expected {expected} FULL/loaded chunks, last response {answer}")


def process_metrics(samples, phase, began, ended):
    values = [s for s in samples.items if s["phase"] == phase and began <= s["monotonic"] <= ended]
    return {"rssPeakMiB": max((s["rssKiB"] for s in values), default=0) / 1024,
            "cpuSeconds": (values[-1]["cpuSeconds"] - values[0]["cpuSeconds"]) if len(values) > 1 else None,
            "sampleCount": len(values)}


def run_jvm(root, trial, mode):
    global RCON_SOCKET
    if RCON_SOCKET is not None:
        RCON_SOCKET.close()
        RCON_SOCKET = None
    work = root / trial["name"]
    server = work / "server"
    phase_log = work / f"{mode}.log"
    jfr_file = work / f"{mode}.jfr"
    gc_log = work / f"{mode}-gc.log"
    env = os.environ.copy()
    env.pop("JAVA_TOOL_OPTIONS", None)
    env["JAVA_HOME"] = str(JDK)
    env["PATH"] = str(JDK / "bin") + os.pathsep + env["PATH"]
    flags = ["-Xms512M", "-Xmx3G", "-XX:ActiveProcessorCount=4", "-XX:+UseG1GC",
             f"-Xlog:gc*:file={gc_log}:time,uptime,level,tags",
             f"-XX:StartFlightRecording=filename={jfr_file},settings=profile,dumponexit=true"]
    args = [str(JDK / "bin/java"), *flags, "@libraries/net/neoforged/neoforge/21.1.266/unix_args.txt", "nogui"]
    boundaries = {"startEpoch": time.time(), "jvmFlags": flags}
    with phase_log.open("w") as output:
        process = subprocess.Popen(args, cwd=server, env=env, stdin=subprocess.PIPE,
                                   stdout=output, stderr=subprocess.STDOUT)
        samples = Samples(process.pid)
        try:
            ready(process, phase_log)
            for command in ("forceload remove all", "gamerule spawnChunkRadius 0", "gamerule doMobSpawning false",
                            "gamerule randomTickSpeed 0", "gamerule doWeatherCycle false",
                            "gamerule doDaylightCycle false", "scoreboard objectives add wbench dummy"):
                rcon(command)
            # A plain command round-trip records the harness/tick overhead floor.
            overhead = []
            for _ in range(5):
                at = time.monotonic(); rcon("time query daytime"); overhead.append(time.monotonic() - at)
            boundaries["rconRoundTripMedianSeconds"] = statistics.median(overhead)
            samples.phase = "generation" if mode == "generate" else "reload"
            began = time.monotonic(); boundaries["measurementStartEpoch"] = time.time()
            if mode == "generate":
                rcon(f"forceload add {FIRST[0] * 16 + 8} {FIRST[1] * 16 + 8}")
                polls = await_loaded(process, "first", 1)
                trial["firstColdChunkSeconds"] = time.monotonic() - began
                trial["firstColdChunkPolls"] = polls
            remaining = time.monotonic()
            polls = 0
            for i, batch in enumerate(BATCHES):
                rcon(f"forceload add {batch[0][0] * 16} {batch[0][1] * 16} {batch[-1][0] * 16} {batch[-1][1] * 16}")
                polls += await_loaded(process, f"batch{i}", len(batch))
            polls += await_loaded(process, "all", len(TARGETS))
            ended = time.monotonic(); boundaries["measurementEndEpoch"] = time.time()
            phase = "generation" if mode == "generate" else "reload"
            trial[phase + "Seconds"] = ended - began
            trial[phase + "Polls"] = polls
            trial[phase + "Metrics"] = process_metrics(samples, phase, began, ended)
            if mode == "generate":
                trial["remainingAreaSeconds"] = ended - remaining
                trial["requestedChunksPerSecond"] = len(TARGETS) / (ended - began)
            samples.phase = "save"
            at = time.monotonic(); rcon("save-all flush"); trial[mode + "SaveSeconds"] = time.monotonic() - at
            rcon("forceload remove all")
            rcon("save-all flush")
            rcon("stop")
            process.wait(timeout=90)
            assert process.returncode == 0, f"Server exit: {process.returncode}"
            text = phase_log.read_text(errors="replace")
            assert "ServerHangWatchdog" not in text and "A single server tick took" not in text
            trial[mode + "CleanShutdown"] = True
        finally:
            if RCON_SOCKET is not None:
                RCON_SOCKET.close()
                RCON_SOCKET = None
            samples.finish(work / f"{mode}-process-samples.json")
            if process.poll() is None:
                try:
                    process.stdin.write(b"stop\n"); process.stdin.flush(); process.wait(timeout=20)
                except (OSError, subprocess.TimeoutExpired):
                    process.terminate()
                    try: process.wait(timeout=10)
                    except subprocess.TimeoutExpired: process.kill(); process.wait()
            boundaries["exitCode"] = process.returncode
            write_json(work / f"{mode}-boundaries.json", boundaries)
    # Parsing runs after server exit, outside every timed generation/load window.
    if jfr_file.exists():
        event_json = subprocess.check_output([str(JDK / "bin/jfr"), "print", "--json", "--events",
            "jdk.GCHeapSummary,jdk.GarbageCollection", str(jfr_file)], text=True)
        (work / f"{mode}-jfr-events.json").write_text(event_json)


def validate_saved(world, smoke):
    starts = Counter(); palette = set()
    for x, z in TARGETS:
        chunk = smoke.chunk_nbt(world, x, z)
        assert chunk["Status"] == "minecraft:full", (x, z, chunk["Status"])
        for key, start in chunk.get("structures", {}).get("starts", {}).items():
            if start.get("id", "INVALID") != "INVALID": starts[key] += 1
        for section in chunk.get("sections", []):
            for block in section.get("block_states", {}).get("palette", []):
                palette.add(block["Name"])
    assert any(n.startswith("skylands:") for n in palette), "Target needs native SkyLands generation"
    return {"fullTargetChunks": len(TARGETS), "structureStarts": dict(sorted(starts.items())),
            "nativeSkylandsBlockTypes": sorted(n for n in palette if n.startswith("skylands:"))}


def trial_run(root, template, number, version):
    name = f"{number:02d}-{version}"
    work = root / name
    assert not work.exists(), f"Refusing to reuse benchmark trial {work}"
    server = work / "server"
    server.mkdir(parents=True)
    world = server / "benchmark-world"
    shutil.copytree(template, world)
    (server / "libraries").symlink_to(WORKSPACE / "cold-skylands/server/libraries", target_is_directory=True)
    mods = server / "mods"; mods.mkdir()
    for source in (root / "dependencies").glob("*.jar"):
        (mods / source.name).symlink_to(source)
    shutil.copyfile(ARTIFACTS[version][0], mods / "wildshape.jar")
    config = server / "config/wildshape"; config.mkdir(parents=True)
    (config / "performance_internal.conf").write_text("tileSize=3\nbatchCount=6\nthreadCount=3\n")
    (server / "eula.txt").write_text("eula=true\n")
    (server / "server.properties").write_text("""level-name=benchmark-world
level-type=wildshape:wildshape
level-seed=2031102026
online-mode=false
server-ip=127.0.0.1
server-port=25596
enable-rcon=true
rcon.port=25595
rcon.password=wildshape-local-benchmark
view-distance=2
simulation-distance=2
max-tick-time=60000
spawn-protection=0
""")
    trial = {"name": name, "version": version, "jarSha256": ARTIFACTS[version][1], "requestedChunks": len(TARGETS)}
    preset = world / "datapacks/wildshape-preset.zip"
    preset_before = sha(preset)
    spec = importlib.util.spec_from_file_location("smoke", HERE / "tools/skylands-smoke.py")
    smoke = importlib.util.module_from_spec(spec); spec.loader.exec_module(smoke)
    # Prove all targets are absent before the timer starts.
    for x, z in TARGETS:
        try: smoke.chunk_nbt(world, x, z)
        except (FileNotFoundError, AssertionError): continue
        raise AssertionError(f"Target already saved in template: {x}, {z}")
    print(json.dumps({"trial": name, "event": "cold-start"}), flush=True)
    try:
        run_jvm(root, trial, "generate")
        trial["generationSavedData"] = validate_saved(world, smoke)
        snapshots = [smoke.snapshot(world, x, z, 33) for x, z in TARGETS]
        write_json(work / "generated-snapshots.json", snapshots)
        generated_biomes = []
        for x, z in TARGETS:
            chunk = smoke.chunk_nbt(world, x, z)
            generated_biomes.append({"chunk": [x, z], "biomes": {str(section["Y"]): smoke.palette_digest(section["biomes"], 64, 1) for section in chunk["sections"] if "biomes" in section}})
        write_json(work / "generated-biomes.json", generated_biomes)
        if number == 1:
            shutil.copytree(world / "region", work / "generated-region")
        print(json.dumps({"trial": name, "event": "generated", "seconds": trial["generationSeconds"],
                          "firstCold": trial["firstColdChunkSeconds"], "rssMiB": trial["generationMetrics"]["rssPeakMiB"]}), flush=True)
        run_jvm(root, trial, "reload")
        after = [smoke.snapshot(world, x, z, 33) for x, z in TARGETS]
        write_json(work / "reloaded-snapshots.json", after)
        reloaded_biomes = []
        for x, z in TARGETS:
            chunk = smoke.chunk_nbt(world, x, z)
            reloaded_biomes.append({"chunk": [x, z], "biomes": {str(section["Y"]): smoke.palette_digest(section["biomes"], 64, 1) for section in chunk["sections"] if "biomes" in section}})
        write_json(work / "reloaded-biomes.json", reloaded_biomes)
        assert generated_biomes == reloaded_biomes, "Saved biome data changed during reload"
        trial["savedBlockSnapshotsIdenticalOnReload"] = snapshots == after
        trial["chunksWithBlockSnapshotChangesOnReload"] = sum(a != b for a, b in zip(snapshots, after))
        trial["reloadSavedData"] = validate_saved(world, smoke)
        trial["savedPresetUnchanged"] = sha(preset) == preset_before
        assert trial["savedPresetUnchanged"]
        trial["savedBiomesUnchangedOnReload"] = True
        trial["passed"] = True
    except Exception as error:
        trial["passed"] = False; trial["error"] = repr(error)
        raise
    finally:
        write_json(work / "result.json", trial)
        print(json.dumps(trial), flush=True)
    return trial


def summarize(root, trials):
    metrics = ["firstColdChunkSeconds", "generationSeconds", "remainingAreaSeconds", "reloadSeconds", "requestedChunksPerSecond"]
    result = {"passed": all(t["passed"] for t in trials), "order": [t["version"] for t in trials],
              "requestedChunksPerTrial": len(TARGETS), "trialCount": len(trials), "versions": {}, "trials": trials}
    for version in ("A", "B"):
        values = [t for t in trials if t["version"] == version]
        if not values:
            continue
        stats = {}
        for key in metrics:
            a = [t[key] for t in values]
            stats[key] = {"median": statistics.median(a), "minimum": min(a), "maximum": max(a), "samples": a}
        for key in ("generation", "reload"):
            a = [t[key + "Metrics"]["rssPeakMiB"] for t in values]
            stats[key + "RssPeakMiB"] = {"median": statistics.median(a), "minimum": min(a), "maximum": max(a), "samples": a}
        result["versions"][version] = stats
    write_json(root / "comparison.json", result)
    return result


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--work", type=Path, default=WORKSPACE / "benchmark-104-105-final")
    parser.add_argument("--limit", type=int, default=6)
    args = parser.parse_args()
    root = args.work.resolve(); template = setup(root)
    trials = []
    for number, version in enumerate(ORDER[:args.limit], 1):
        result_file = root / f"{number:02d}-{version}/result.json"
        if result_file.exists():
            result = json.loads(result_file.read_text()); assert result["passed"], result
        else: result = trial_run(root, template, number, version)
        trials.append(result); summarize(root, trials)
    print(json.dumps(summarize(root, trials), indent=2), flush=True)


if __name__ == "__main__":
    main()
