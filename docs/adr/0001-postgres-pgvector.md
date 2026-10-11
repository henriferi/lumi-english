# ADR-0001 — PostgreSQL + pgvector for both app data and vectors

- **Status:** Accepted
- **Date:** 2026-10-11

## Context
The app stores conversations, mistakes and vocabulary, and needs vector search over a small curriculum
(a few hundred chunks).

## Decision
Use one PostgreSQL database with the pgvector extension, through Spring AI's PgVectorStore.

## Alternatives considered
- **Dedicated vector DB (Qdrant, Pinecone, Weaviate)** — one more service to run, no benefit at this size.
- **In-memory vector store** — re-embedding on every restart costs time and tokens, and it can't be queried with SQL.

## Consequences
- Positive: one container, one backup, metadata filters in SQL, simple local setup.
- Negative: at a much larger scale a dedicated engine could offer better tuning.
