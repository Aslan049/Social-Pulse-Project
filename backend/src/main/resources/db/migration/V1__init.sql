CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(150) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE upload_batches (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    platform          VARCHAR(20)  NOT NULL CHECK (platform IN ('INSTAGRAM', 'X')),
    original_filename VARCHAR(255) NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'PROCESSING'
                      CHECK (status IN ('PROCESSING', 'COMPLETED', 'FAILED')),
    total_rows        INTEGER      NOT NULL DEFAULT 0,
    imported_rows     INTEGER      NOT NULL DEFAULT 0,
    rejected_rows     INTEGER      NOT NULL DEFAULT 0,
    uploaded_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE upload_errors (
    id         BIGSERIAL PRIMARY KEY,
    batch_id   BIGINT       NOT NULL REFERENCES upload_batches(id) ON DELETE CASCADE,
    row_number INTEGER      NOT NULL,
    field      VARCHAR(100),
    message    VARCHAR(500) NOT NULL,
    raw_line   TEXT
);

CREATE TABLE posts (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    batch_id        BIGINT       NOT NULL REFERENCES upload_batches(id) ON DELETE CASCADE,
    platform        VARCHAR(20)  NOT NULL CHECK (platform IN ('INSTAGRAM', 'X')),
    external_id     VARCHAR(100) NOT NULL,
    published_at    TIMESTAMPTZ  NOT NULL,
    content_preview VARCHAR(200),
    post_type       VARCHAR(20)  NOT NULL DEFAULT 'OTHER'
                    CHECK (post_type IN ('IMAGE', 'VIDEO', 'TEXT', 'REEL', 'OTHER')),
    impressions     INTEGER CHECK (impressions >= 0),
    reach           INTEGER CHECK (reach >= 0),
    likes           INTEGER CHECK (likes >= 0),
    comments        INTEGER CHECK (comments >= 0),
    shares          INTEGER CHECK (shares >= 0),
    saves           INTEGER CHECK (saves >= 0),
    clicks          INTEGER CHECK (clicks >= 0),
    engagement_total INTEGER      NOT NULL DEFAULT 0,
    engagement_rate  NUMERIC(8,4),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_posts_user_platform_external UNIQUE (user_id, platform, external_id)
);

CREATE INDEX idx_posts_user_published ON posts (user_id, published_at);
CREATE INDEX idx_posts_user_platform  ON posts (user_id, platform);
CREATE INDEX idx_batches_user         ON upload_batches (user_id);
CREATE INDEX idx_errors_batch         ON upload_errors (batch_id);

CREATE TABLE reports (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type         VARCHAR(10) NOT NULL CHECK (type IN ('EXCEL', 'PDF')),
    filters_json TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);