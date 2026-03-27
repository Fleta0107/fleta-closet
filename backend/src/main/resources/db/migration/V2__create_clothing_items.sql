CREATE TABLE clothing_items (
    id          BIGSERIAL       PRIMARY KEY,
    user_id     BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category    VARCHAR(50)     NOT NULL,
    brand       VARCHAR(100),
    color       VARCHAR(50),
    image_path  VARCHAR(500),
    memo        TEXT,
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE TABLE clothing_item_tags (
    clothing_item_id BIGINT          NOT NULL REFERENCES clothing_items(id) ON DELETE CASCADE,
    tag              VARCHAR(100)    NOT NULL
);

CREATE INDEX idx_clothing_items_user_id ON clothing_items(user_id);
CREATE INDEX idx_clothing_items_category ON clothing_items(category);