
-- 4. Delhivery shipping integration: tracking columns on orders
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS tracking_number VARCHAR(50);

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS shipping_provider VARCHAR(30) DEFAULT 'STATIC';

CREATE INDEX IF NOT EXISTS idx_orders_tracking_number ON orders(tracking_number);

-- 5. Fix cart unique constraint to allow multiple variants of the same product
-- Old constraint: UNIQUE(cart_id, product_id) — prevents adding 100g and 250g of same product
-- New constraint: UNIQUE(cart_id, product_id, variant_id) — allows different variants
ALTER TABLE cart_items DROP CONSTRAINT IF EXISTS uq_cart_product;
CREATE UNIQUE INDEX IF NOT EXISTS uq_cart_product_variant
    ON cart_items (cart_id, product_id, COALESCE(variant_id, '00000000-0000-0000-0000-000000000000'::uuid));

-- 6. Forgot-password: self-service password reset tokens.
-- Only SHA-256 hashes of the raw token are stored (never the raw token), so a
-- DB leak can't reveal active reset links. Mirrors email_verification_tokens.
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    token_id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used_at    TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_user_id
    ON password_reset_tokens(user_id);

-- 7. Storefront metadata that has no dedicated column (code, badge, tags, featured, MRPs...).
--    JSON text, opaque to the backend. See README.md in the repo root.
ALTER TABLE products ADD COLUMN IF NOT EXISTS attributes TEXT;

-- 8. Storefront settings document (shop info, hero, FAQ, offers) served by /api/settings.
CREATE TABLE IF NOT EXISTS site_settings (
    setting_key VARCHAR(50) PRIMARY KEY,
    data        JSONB     NOT NULL DEFAULT '{}'::jsonb,
    revision    BIGINT    NOT NULL DEFAULT 1,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 9. Maps the frontend's legacy dummy poster slugs/codes to real product UUIDs.
--    Seeded by src/main/resources/seed_catalog.sql.
CREATE TABLE IF NOT EXISTS product_legacy_map (
    slug       VARCHAR(100) PRIMARY KEY,
    code       VARCHAR(30)  UNIQUE NOT NULL,
    product_id UUID NOT NULL REFERENCES products(product_id) ON DELETE CASCADE
);

-- 10. First-class product tags (e.g. "ironman", "minimal", "cinematic").
--     Tags are a parallel taxonomy to products.category: a product has one
--     category but may carry many tags. Slug is the lowercase, URL-safe
--     unique business key; see Tag.toSlug() in the backend for normalisation.
--     product_tags is the many-to-many join; ON DELETE CASCADE from both
--     sides so dropping a product/tag cleans up the links automatically.
CREATE TABLE IF NOT EXISTS tags (
    tag_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(80) NOT NULL,
    slug        VARCHAR(80) NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS product_tags (
    product_id UUID NOT NULL REFERENCES products(product_id) ON DELETE CASCADE,
    tag_id     UUID NOT NULL REFERENCES tags(tag_id)         ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (product_id, tag_id)
);

-- Reverse lookup (tag -> products) used by the storefront filter sidebar.
-- The primary key already covers product -> tags.
CREATE INDEX IF NOT EXISTS idx_product_tags_tag_id ON product_tags(tag_id);

-- 11. Photo reviews: attach an optional "wall photo" to a review.
--     The storefront /#photo-reviews gallery and the admin moderation view
--     both expect an image URL on each review row. Uploads reuse the same
--     StorageService backend as product images (R2/MinIO); object keys are
--     prefixed "reviews/" so review and product artwork never collide.
--     City is captured alongside so testimonials can be credited
--     ("Priya • Chennai") without an extra users-table lookup.
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS photo_url         VARCHAR(1000);
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS photo_object_key  VARCHAR(500);
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS city              VARCHAR(100);
