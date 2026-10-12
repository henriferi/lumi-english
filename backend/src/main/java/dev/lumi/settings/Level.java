package dev.lumi.settings;

/** CEFR-based level, from absolute beginner (PRE_A1) to elementary (A2). Declared in increasing order. */
public enum Level {
    PRE_A1,
    A1,
    A2;

    /** Numeric rank (0, 1, 2) so levels can be compared in vector store filters ("levelRank <= 1"). */
    public int rank() {
        return ordinal();
    }
}
