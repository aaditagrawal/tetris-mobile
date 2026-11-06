"""Measure the pinned baseline adapter and the Activity's cached-provider replacement."""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import platform
import re
import statistics
import subprocess
import tempfile
import tomllib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
FIXTURE = Path(__file__).resolve().parent
ACTIVITY = "app/src/main/java/com/maverick/tetris_mobile/MainActivity.kt"
MODELS = (
    "RefreshRateViewModel",
    "PaletteViewModel",
    "GameViewModel",
    "ScoreSaveViewModel",
    "GameSessionViewModel",
)


def git(*args: str) -> str:
    return subprocess.check_output(["git", *args], cwd=ROOT, text=True).strip()


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base", default="7d866326e601758790b98440a654089a8c76ad43")
    parser.add_argument(
        "--output",
        type=Path,
        default=Path(tempfile.gettempdir()) / "tetris-viewmodel-benchmark.json",
    )
    args = parser.parse_args()
    baseline_bytes = subprocess.check_output(
        ["git", "show", f"{args.base}:{ACTIVITY}"], cwd=ROOT
    )
    baseline = baseline_bytes.decode()
    candidate = (ROOT / ACTIVITY).read_text()
    for model in MODELS:
        if not re.search(rf"val \w+: {model} = viewModel\(\)", baseline):
            raise SystemExit(f"Baseline no longer matches fixture: {model}")
        if f"remember {{ models[{model}::class.java] }}" not in candidate:
            raise SystemExit(f"Candidate no longer matches fixture: {model}")
    versions = tomllib.loads(git("show", f"{args.base}:gradle/libs.versions.toml"))
    version = versions["versions"]["lifecycleViewmodelCompose"]
    if version != "2.11.0":
        raise SystemExit("Benchmark adapter pin differs from the target snapshot")
    command = [
        str(ROOT / "gradlew"),
        "testDebugUnitTest",
        "--tests",
        "*ViewModelLookupBenchmark",
        "-I",
        str(FIXTURE / "benchmark.init.gradle"),
        f"-PviewModelBenchmarkDir={FIXTURE}",
        "--offline",
        "--no-daemon",
        "--max-workers=2",
        "--no-parallel",
        "--no-configuration-cache",
        "--warning-mode=fail",
        "--console=plain",
    ]
    args.output.parent.mkdir(parents=True, exist_ok=True)
    log = args.output.with_suffix(".log")
    with log.open("w") as output:
        subprocess.run(
            command, cwd=ROOT, stdout=output, stderr=subprocess.STDOUT, check=True
        )
    samples: dict[str, dict[str, list[float]]] = {}
    pattern = r"VIEWMODEL_BENCH workload=(\w+) variant=(baseline|candidate) pair=(\d+) ns=([\d.]+)"
    for workload, variant, pair, nanos in re.findall(pattern, log.read_text()):
        values = samples.setdefault(workload, {}).setdefault(variant, [])
        if int(pair) != len(values):
            raise SystemExit(f"Unexpected sample sequence: {workload}/{variant}/{pair}")
        values.append(float(nanos))
    result = {
        "baseline": git("rev-parse", args.base),
        "head": git("rev-parse", "HEAD"),
        "dirty": bool(git("status", "--porcelain")),
        "activity_sha256": {
            "baseline": hashlib.sha256(baseline_bytes).hexdigest(),
            "candidate": hashlib.sha256(candidate.encode()).hexdigest(),
        },
        "adapter_version": version,
        "platform": platform.platform(),
        "test_jvm": re.findall(r"VIEWMODEL_JVM (.+)", log.read_text()),
        "command": command,
        "log": str(log),
        "workloads": {},
        "scope": "Headless JVM adapter/Compose composition cost for five models. No device, UI frame, app startup or disk latency claim. Trace markers are no-ops in this opt-in fixture only.",
    }
    for workload in ("first", "recreation", "recomposition"):
        result["workloads"][workload] = {}
        for variant in ("baseline", "candidate"):
            values = samples.get(workload, {}).get(variant, [])
            if len(values) != 12:
                raise SystemExit(
                    f"Expected 12 samples for {workload}/{variant}; got {len(values)}"
                )
            ordered = sorted(values)
            result["workloads"][workload][variant] = {
                "n": len(values),
                "median_ns": statistics.median(values),
                "p95_ns": ordered[math.ceil(0.95 * len(values)) - 1],
                "min_ns": min(values),
                "max_ns": max(values),
                "samples_ns": values,
            }
        stats = result["workloads"][workload]
        stats["median_reduction_percent"] = 100 * (
            1 - stats["candidate"]["median_ns"] / stats["baseline"]["median_ns"]
        )
    args.output.write_text(json.dumps(result, indent=2) + "\n")
    print(args.output)
    for workload, stats in result["workloads"].items():
        print(
            f"{workload}: {stats['baseline']['median_ns']:.1f} -> {stats['candidate']['median_ns']:.1f} ns, {stats['median_reduction_percent']:.1f}% lower median"
        )


if __name__ == "__main__":
    main()
