-- Initial schema (docs/PROJETO.md, section 7).
-- The vector_store table is created by Spring AI's PgVectorStore, not here.

CREATE EXTENSION IF NOT EXISTS vector;

-- Single learner, no login: exactly one row with id = 1.
CREATE TABLE learner_settings (
    id       SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    name     VARCHAR(50) NOT NULL,
    age_band VARCHAR(10) NOT NULL CHECK (age_band IN ('KID', 'TEEN', 'ADULT')),
    level    VARCHAR(10) NOT NULL CHECK (level IN ('PRE_A1', 'A1', 'A2'))
);

INSERT INTO learner_settings (id, name, age_band, level) VALUES (1, 'Learner', 'ADULT', 'PRE_A1');

CREATE TABLE conversation (
    id         BIGSERIAL PRIMARY KEY,
    started_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE message (
    id              BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT      NOT NULL REFERENCES conversation (id) ON DELETE CASCADE,
    role            VARCHAR(10) NOT NULL CHECK (role IN ('USER', 'TUTOR')),
    text            TEXT        NOT NULL,
    reply_json      JSONB,
    trace_json      JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_message_conversation ON message (conversation_id, created_at);

-- Skill catalog, filled from the curriculum front matter.
CREATE TABLE skill (
    tag     VARCHAR(50)  PRIMARY KEY,
    name_pt VARCHAR(100) NOT NULL,
    level   VARCHAR(10)  NOT NULL CHECK (level IN ('PRE_A1', 'A1', 'A2'))
);

-- Same mistake in the same message is counted once (idempotent tool calls).
CREATE TABLE mistake (
    id         BIGSERIAL PRIMARY KEY,
    message_id BIGINT       NOT NULL REFERENCES message (id) ON DELETE CASCADE,
    skill      VARCHAR(50)  NOT NULL REFERENCES skill (tag),
    original   VARCHAR(500) NOT NULL,
    corrected  VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_mistake_message_skill UNIQUE (message_id, skill)
);

-- The same word is saved only once.
CREATE TABLE vocab_word (
    id               BIGSERIAL PRIMARY KEY,
    word             VARCHAR(100) NOT NULL,
    translation      VARCHAR(200) NOT NULL,
    example          VARCHAR(500),
    times_reviewed   INTEGER      NOT NULL DEFAULT 0,
    last_reviewed_at TIMESTAMPTZ,
    CONSTRAINT uq_vocab_word_word UNIQUE (word)
);

-- Lets ingestion skip curriculum files that did not change.
CREATE TABLE curriculum_file (
    path         VARCHAR(255) PRIMARY KEY,
    content_hash VARCHAR(64)  NOT NULL,
    ingested_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE ai_call_log (
    id            BIGSERIAL PRIMARY KEY,
    operation     VARCHAR(50)  NOT NULL,
    model         VARCHAR(100) NOT NULL,
    input_tokens  INTEGER,
    output_tokens INTEGER,
    latency_ms    BIGINT       NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE tool_call_log (
    id         BIGSERIAL PRIMARY KEY,
    message_id BIGINT REFERENCES message (id) ON DELETE CASCADE,
    tool       VARCHAR(50) NOT NULL,
    args_json  JSONB,
    ok         BOOLEAN     NOT NULL,
    latency_ms BIGINT      NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
