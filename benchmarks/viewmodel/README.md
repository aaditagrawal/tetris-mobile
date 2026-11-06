# ViewModel lookup benchmark

Run from the repository root with its pinned JDK 21 and cached Gradle/Android dependencies:

```sh
uv run --no-project python benchmarks/viewmodel/run.py --output /tmp/viewmodel-results.json
```

On a shared machine, run this inside the site's exclusive benchmark scheduler. The script checks the five lookup call sites against the committed PR base, then compares its pinned `lifecycle-viewmodel-compose:2.11.0` implementation with the cached `ViewModelProvider` calls used in `MainActivity`. It adds the old adapter to **testImplementation only**, through an explicit init script. Normal builds and tests never load these fixture sources or the old adapter.

Both paths run in the same Compose runtime, compiler and JVM process with identical factories, creation extras, model classes and consumption. Twelve sample pairs alternate order after six warmup batches. Each sample contains 1,000 initial compositions or retained-store recreations, or 20,000 forced recompositions. Timed recompositions check model identity, render counts and observed state. First compositions check that both APIs forward the owner's extras and use identical keys; recreations must reuse all five retained models without invoking a new factory.

The fake models isolate lookup overhead. This measures headless composition work, not the app's frame rate, Android startup, rendering, storage or model constructor costs. Only Android's trace-marker calls are stubbed to no-ops, identically for both variants. Normal unit-test stubs remain strict. The JSON includes raw samples, medians, sample p95, source hashes, command and environment. With twelve samples, p95 is the largest batch average, not a per-operation latency percentile.
