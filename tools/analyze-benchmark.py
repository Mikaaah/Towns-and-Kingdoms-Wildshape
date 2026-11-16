"""Summarize completed paired Wildshape trials without treating pilot data as samples.

Example:
  python tools/analyze-benchmark.py --work ../benchmark-104-105-final --require-complete

JFR GC events are restricted to the actual measurement window. Observed after-GC
heap use is not an absolute heap peak or a full-GC retained/live-heap measurement.
"""
from __future__ import annotations

import argparse
from collections import Counter
from datetime import datetime
import json
from pathlib import Path
import re
import statistics


LABELS = {"A": "1.0.4", "B": "1.0.5-beta.1"}
EXPECTED = ["A", "B", "B", "A", "A", "B"]
MIB = 1024 ** 2


def duration_seconds(value):
    if isinstance(value, (float, int)):
        return float(value)
    match = re.fullmatch(r"P(?:(\d+(?:\.\d+)?)D)?T(?:(\d+(?:\.\d+)?)H)?(?:(\d+(?:\.\d+)?)M)?(?:(\d+(?:\.\d+)?)S)?", value)
    if not match:
        raise ValueError(f"Unrecognized JFR duration: {value}")
    days, hours, minutes, seconds = (float(v or 0) for v in match.groups())
    return days * 86400 + hours * 3600 + minutes * 60 + seconds


def timestamp_epoch(value):
    return datetime.fromisoformat(value.replace("Z", "+00:00")).timestamp()


def stat(values):
    values = [float(v) for v in values if v is not None]
    if not values:
        return {"count": 0, "median": None, "minimum": None, "maximum": None, "samples": []}
    return {"count": len(values), "median": statistics.median(values),
            "minimum": min(values), "maximum": max(values), "samples": values}


def parse_jfr(directory, phase):
    events_path = directory / f"{phase}-jfr-events.json"
    bounds_path = directory / f"{phase}-boundaries.json"
    if not events_path.exists() or not bounds_path.exists():
        return {"available": False}
    bounds = json.loads(bounds_path.read_text())
    if "measurementStartEpoch" not in bounds or "measurementEndEpoch" not in bounds:
        return {"available": False, "reason": "No completed measurement window"}
    began, ended = bounds["measurementStartEpoch"], bounds["measurementEndEpoch"]
    events = json.loads(events_path.read_text())["recording"]["events"]
    before, after, committed = [], [], []
    gc_events, overlap_excluded = [], 0
    for event in events:
        value = event["values"]
        start = timestamp_epoch(value["startTime"])
        if event["type"] == "jdk.GCHeapSummary" and began <= start <= ended:
            if value["when"] == "Before GC":
                before.append(value["heapUsed"] / MIB)
            elif value["when"] == "After GC":
                after.append(value["heapUsed"] / MIB)
            committed.append(value["heapSpace"]["committedSize"] / MIB)
        elif event["type"] == "jdk.GarbageCollection":
            duration = duration_seconds(value["duration"])
            if began <= start and start + duration <= ended:
                gc_events.append({"id": value["gcId"], "name": value["name"],
                                  "pauseSeconds": duration_seconds(value["sumOfPauses"]),
                                  "longestPauseSeconds": duration_seconds(value["longestPause"])})
            elif start < ended and start + duration > began:
                overlap_excluded += 1
    return {"available": True, "measurementWallSeconds": ended - began,
            "gcCollectionsFullyWithinWindow": len(gc_events),
            "gcEventsStraddlingBoundaryExcluded": overlap_excluded,
            "gcPauseTotalMilliseconds": sum(e["pauseSeconds"] for e in gc_events) * 1000,
            "gcLongestPauseMilliseconds": max((e["longestPauseSeconds"] for e in gc_events), default=0) * 1000,
            "gcNames": dict(Counter(e["name"] for e in gc_events)),
            "observedBeforeGcHeapMiB": stat(before),
            "observedAfterGcHeapMiB": stat(after),
            "observedCommittedHeapMiB": stat(committed),
            "note": "After-GC observations are not an absolute heap peak or full-GC retained/live heap. GC events straddling phase boundaries are conservatively excluded."}


def metric_values(trial):
    result = trial["result"]
    metrics = {key: result.get(key) for key in ("firstColdChunkSeconds", "generationSeconds", "remainingAreaSeconds", "reloadSeconds", "requestedChunksPerSecond")}
    for phase in ("generation", "reload"):
        measured = result.get(phase + "Metrics", {})
        metrics[phase + "RssPeakMiB"] = measured.get("rssPeakMiB")
        metrics[phase + "CpuSeconds"] = measured.get("cpuSeconds")
        jfr = trial["jfr"]["generate" if phase == "generation" else "reload"]
        metrics[phase + "GcPauseTotalMilliseconds"] = jfr.get("gcPauseTotalMilliseconds")
        metrics[phase + "ObservedAfterGcHeapMaximumMiB"] = jfr.get("observedAfterGcHeapMiB", {}).get("maximum")
    return metrics


def percent(a, b):
    if a is None or b is None or a <= 0:
        return None
    return {"baseline": a, "candidate": b, "candidateChangePercent": (b / a - 1) * 100,
            "candidateReductionPercent": (1 - b / a) * 100}


def analyze(work):
    environment = json.loads((work / "environment.json").read_text())
    found, failures = [], []
    for directory in sorted(p for p in work.glob("[0-9][0-9]-[AB]") if p.is_dir()):
        path = directory / "result.json"
        if not path.exists():
            continue
        result = json.loads(path.read_text())
        if not result.get("passed"):
            failures.append({"name": directory.name, "error": result.get("error")})
            continue
        version = result["version"]
        assert version in LABELS
        assert result["jarSha256"] == environment["artifacts"][version]["sha256"]
        assert result["requestedChunks"] == len(environment["requestedTargets"])
        assert result["generationSavedData"]["fullTargetChunks"] == result["requestedChunks"]
        assert result.get("savedPresetUnchanged") and result.get("savedBiomesUnchangedOnReload")
        assert result.get("generateCleanShutdown") and result.get("reloadCleanShutdown")
        assert abs(result["requestedChunksPerSecond"] - result["requestedChunks"] / result["generationSeconds"]) < 1e-10
        trial = {"name": directory.name, "version": version, "result": result,
                 "jfr": {p: parse_jfr(directory, p) for p in ("generate", "reload")}}
        for phase, elapsed_key in (("generate", "generationSeconds"), ("reload", "reloadSeconds")):
            recorded = trial["jfr"][phase]
            if recorded.get("available"):
                assert abs(recorded["measurementWallSeconds"] - result[elapsed_key]) < 0.1, \
                    f"Wall/monotonic phase boundary mismatch: {directory.name}/{phase}"
        trial["metrics"] = metric_values(trial)
        found.append(trial)
    order = [t["version"] for t in found]
    complete = len(found) == len(EXPECTED) and order == EXPECTED and not failures
    versions = {}
    keys = list(found[0]["metrics"]) if found else []
    for version in LABELS:
        trials = [t for t in found if t["version"] == version]
        versions[version] = {"label": LABELS[version], "count": len(trials),
                             "metrics": {key: stat(t["metrics"][key] for t in trials) for key in keys}}
    comparisons = {}
    for key in keys:
        a = versions["A"]["metrics"][key]
        b = versions["B"]["metrics"][key]
        if a["count"] and b["count"]:
            effect = percent(a["median"], b["median"])
            comparisons[key] = {**(effect or {}),
                "observedRangesOverlap": max(a["minimum"], b["minimum"]) <= min(a["maximum"], b["maximum"]),
                "everyCandidateBelowEveryBaseline": b["maximum"] < a["minimum"],
                "everyCandidateAboveEveryBaseline": b["minimum"] > a["maximum"]}
    pairs = []
    by_name = {t["name"]: t for t in found}
    for number, (a_name, b_name) in enumerate((("01-A", "02-B"), ("04-A", "03-B"), ("05-A", "06-B")), 1):
        if a_name in by_name and b_name in by_name:
            pairs.append({"block": number, "baselineTrial": a_name, "candidateTrial": b_name,
                          "metrics": {key: percent(by_name[a_name]["metrics"][key], by_name[b_name]["metrics"][key]) for key in keys}})
    limitations = [
        "Only three repetitions per version: medians/ranges are descriptive, not a robust confidence interval.",
        "One fixed seed, preset, area and reduced dependency stack; not complete live-pack TPS or all-seed behavior.",
        "Requested-target throughput includes FULL completion and request/poll overhead; Minecraft also generates neighbors.",
        "RSS is sampled every 100ms process memory, not an absolute peak; it includes JVM heap policy, native buffers and metaspace, and differs from committed/reserved heap or post-GC heap use.",
        "GC events spanning a measurement boundary are excluded because their individual pauses cannot be located precisely.",
        "Changed placement policy/strata can change actual generated work; this is shipped-version comparison, not an isolated erosion microbenchmark.",
        "No speed percentage should be presented as conclusive if the trial-to-trial range/order effects are comparable to it.",
    ]
    if found and all(not t["result"]["generationSavedData"].get("structureStarts") for t in found):
        limitations.append("No native structure starts are recorded in these target chunks; this area primarily measures terrain/biome/island generation rather than completed dungeon construction.")
    return {"complete": complete, "completedSuccessfulTrials": len(found), "expectedTrials": len(EXPECTED),
            "actualOrder": order, "failedTrials": failures, "environment": environment,
            "versions": versions, "medianComparisons": comparisons, "adjacentPairedBlocks": pairs,
            "trials": found, "limitations": limitations}


def format_stat(value, decimals=2):
    if not value or not value["count"]:
        return "pending"
    return f"{value['median']:.{decimals}f} ({value['minimum']:.{decimals}f}–{value['maximum']:.{decimals}f})"


def markdown(data):
    lines = ["# Wildshape 1.0.4 vs 1.0.5-beta.1 — measured fixture results", "",
             f"Status: **{'complete' if data['complete'] else 'partial'}**; {data['completedSuccessfulTrials']}/{data['expectedTrials']} successful paired-sequence trials.", "",
             "Values below are **median (minimum–maximum)**. Startup and saving are excluded from generation/load windows.", "",
             "| Metric | 1.0.4 | 1.0.5-beta.1 | Candidate change from baseline median |",
             "| --- | ---: | ---: | ---: |"]
    display = [("First cold FULL chunk (s)", "firstColdChunkSeconds", False),
               ("All 81 requested FULL chunks (s)", "generationSeconds", False),
               ("Requested chunks per second", "requestedChunksPerSecond", True),
               ("Fresh-JVM saved-area loading (s)", "reloadSeconds", False),
               ("Sampled generation RSS peak (MiB)", "generationRssPeakMiB", False),
               ("Sampled reload RSS peak (MiB)", "reloadRssPeakMiB", False),
               ("Generation process CPU time (s)", "generationCpuSeconds", False),
               ("Measured generation GC pauses (ms)", "generationGcPauseTotalMilliseconds", False),
               ("Observed after-GC heap maximum during generation (MiB)", "generationObservedAfterGcHeapMaximumMiB", False)]
    for label, key, higher in display:
        a = data["versions"]["A"]["metrics"].get(key)
        b = data["versions"]["B"]["metrics"].get(key)
        effect = data["medianComparisons"].get(key)
        change = "pending"
        if effect and "candidateChangePercent" in effect:
            number = effect["candidateChangePercent"]
            change = f"{number:+.1f}%"
            if effect["observedRangesOverlap"]:
                change += "; ranges overlap"
        lines.append(f"| {label} | {format_stat(a)} | {format_stat(b)} | {change} |")
    lines.extend(["", "A negative elapsed-time change means less time; a positive throughput change means more requested chunks per second. Those are different percentages.", "",
                  "## Per-trial measurements", "", "| Trial | Version | First cold (s) | 81-chunk total (s) | Saved load (s) | Sampled generation RSS peak (MiB) | Recorded native starts |",
                  "| --- | --- | ---: | ---: | ---: | ---: | ---: |"])
    for trial in data["trials"]:
        t = trial["result"]
        starts = sum(t["generationSavedData"].get("structureStarts", {}).values())
        lines.append(f"| {trial['name']} | {LABELS[trial['version']]} | {t['firstColdChunkSeconds']:.3f} | {t['generationSeconds']:.3f} | {t['reloadSeconds']:.3f} | {t['generationMetrics']['rssPeakMiB']:.1f} | {starts} |")
    if data["adjacentPairedBlocks"]:
        lines.extend(["", "## Adjacent paired generation comparisons", "",
                      "| Pair | Baseline trial | Candidate trial | Less elapsed time | More requested throughput |",
                      "| --- | --- | --- | ---: | ---: |"])
        for pair in data["adjacentPairedBlocks"]:
            elapsed = pair["metrics"]["generationSeconds"]
            rate = pair["metrics"]["requestedChunksPerSecond"]
            lines.append(f"| {pair['block']} | {pair['baselineTrial']} | {pair['candidateTrial']} | {elapsed['candidateReductionPercent']:+.2f}% | {rate['candidateChangePercent']:+.2f}% |")
    lines.extend(["", "## Limitations", ""])
    lines.extend("- " + note for note in data["limitations"])
    if data["failedTrials"]:
        lines.extend(["", "## Failed trials", "", "```json", json.dumps(data["failedTrials"], indent=2), "```"])
    return "\n".join(lines) + "\n"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--work", type=Path, required=True)
    parser.add_argument("--output", type=Path)
    parser.add_argument("--require-complete", action="store_true")
    args = parser.parse_args()
    work = args.work.resolve()
    output = (args.output or work).resolve()
    output.mkdir(parents=True, exist_ok=True)
    data = analyze(work)
    (output / "metrics-analysis.json").write_text(json.dumps(data, indent=2) + "\n")
    (output / "metrics-report.md").write_text(markdown(data))
    print(json.dumps({"complete": data["complete"], "successfulTrials": data["completedSuccessfulTrials"],
                      "failedTrials": data["failedTrials"], "output": str(output)}, indent=2))
    if args.require_complete and not data["complete"]:
        raise SystemExit(2)


if __name__ == "__main__":
    main()
