package android.os;

/** Headless benchmark only: Android trace markers do not participate in lookup work. */
public final class Trace {
    private Trace() {}
    public static void beginSection(String name) {}
    public static void endSection() {}
}
