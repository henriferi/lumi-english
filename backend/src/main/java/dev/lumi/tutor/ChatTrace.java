package dev.lumi.tutor;

import java.util.List;

import dev.lumi.curriculum.RetrievedChunk;

/** What happened behind a reply, saved in message.trace_json (shown in the "Behind the answer" panel in Phase 2). */
record ChatTrace(List<RetrievedChunk> retrieved, boolean fallback) {
}
