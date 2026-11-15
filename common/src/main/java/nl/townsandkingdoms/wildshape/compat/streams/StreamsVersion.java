package nl.townsandkingdoms.wildshape.compat.streams;

/** Exact inspected releases; an unknown release must never fall through to the legacy ABI. */
public enum StreamsVersion {
    LEGACY_215, MODERN_216, MODERN_217, UNSUPPORTED;

    public static StreamsVersion of(String version) {
        if (matches(version, "2.15.2")) return LEGACY_215;
        if (matches(version, "2.16.2")) return MODERN_216;
        if (matches(version, "2.17.1") || matches(version, "2.17.3") || matches(version, "2.17.4"))
            return MODERN_217;
        return UNSUPPORTED;
    }

    public boolean modern() { return this == MODERN_216 || this == MODERN_217; }

    private static boolean matches(String version, String release) {
        return version != null && (version.equals(release) || version.startsWith(release + "+"));
    }
}
