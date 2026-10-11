# ADR-0002 — One tutor agent with tools, flow controlled by code

- **Status:** Accepted
- **Date:** 2026-10-11

## Context
The tutor needs to look up curriculum content, read the learner's history, and record mistakes and new words.
This could be built as several cooperating agents or as one agent with tools.

## Decision
A single agent (Spring AI `ChatClient`) with five tools. Java code controls the flow around it (load settings,
retrieve context, call the agent, validate output, save). Tools validate their arguments, writes are
idempotent, and there's a cap of 4 tool calls per message. Every call is logged and shown in the UI.

## Alternatives considered
- **Multiple agents (tutor, evaluator, planner) talking to each other** — more LLM calls means more latency and
  cost, and it's harder to debug, with no clear quality gain for this use case.
- **No tools (everything stuffed into the prompt)** — larger prompts, and the model couldn't save progress.

## Consequences
- Positive: predictable, cheap, easy to test and to explain; tool calls are auditable.
- Negative: less autonomy; adding new capabilities means adding tools explicitly.
