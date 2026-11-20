-- Seed: dummy customer reviews, including photo reviews for the storefront
-- /#photo-reviews gallery. Idempotent — safe to re-run.
--
-- Requires:
--   * db_migrations.sql  §11 (photo_url / photo_object_key / city columns)
--   * seed_catalog.sql            (populates product_legacy_map slug → product_id)
--
-- Reviewers are created once (per email) with is_admin=false, email_verified=true.
-- Each review is upserted on the (user_id, product_id) uniqueness constraint
-- `uq_user_product_review`, so re-running just refreshes rating/comment/photo.

BEGIN;

-- 1. Reviewer accounts. password_hash is a sentinel so no one can log in as them.
INSERT INTO users (user_id, email, password_hash, first_name, last_name, status, is_admin, email_verified, email_verified_at)
VALUES
  (gen_random_uuid(), 'priya.menon@seed.wallstories.test',  'seed:disabled', 'Priya',   'Menon',  'ACTIVE', false, true, NOW()),
  (gen_random_uuid(), 'arjun.rao@seed.wallstories.test',    'seed:disabled', 'Arjun',   'Rao',    'ACTIVE', false, true, NOW()),
  (gen_random_uuid(), 'neha.iyer@seed.wallstories.test',    'seed:disabled', 'Neha',    'Iyer',   'ACTIVE', false, true, NOW()),
  (gen_random_uuid(), 'kabir.shah@seed.wallstories.test',   'seed:disabled', 'Kabir',   'Shah',   'ACTIVE', false, true, NOW()),
  (gen_random_uuid(), 'meera.nair@seed.wallstories.test',   'seed:disabled', 'Meera',   'Nair',   'ACTIVE', false, true, NOW()),
  (gen_random_uuid(), 'rohan.gupta@seed.wallstories.test',  'seed:disabled', 'Rohan',   'Gupta',  'ACTIVE', false, true, NOW()),
  (gen_random_uuid(), 'ishita.verma@seed.wallstories.test', 'seed:disabled', 'Ishita',  'Verma',  'ACTIVE', false, true, NOW()),
  (gen_random_uuid(), 'dev.pillai@seed.wallstories.test',   'seed:disabled', 'Dev',     'Pillai', 'ACTIVE', false, true, NOW())
ON CONFLICT (email) DO NOTHING;

-- 2. Reviews. Each block looks up (reviewer, product) then upserts the row.
--    photo_url uses Unsplash so the gallery renders without an R2 upload.
--    photo_object_key is NULL for photo-less rows so the storage cleanup path
--    in ReviewService.deleteAny() becomes a no-op.

DO $$
DECLARE
  uid UUID;
  pid UUID;
BEGIN
  --- Neon Tokyo — photo review ---------------------------------------------
  SELECT user_id INTO uid FROM users          WHERE email = 'priya.menon@seed.wallstories.test';
  SELECT product_id INTO pid FROM product_legacy_map WHERE slug = 'neon-tokyo';
  IF uid IS NOT NULL AND pid IS NOT NULL THEN
    INSERT INTO reviews (review_id, user_id, product_id, rating, comment, photo_url, photo_object_key, city, created_at)
    VALUES (gen_random_uuid(), uid, pid, 5,
            'Hung this above my desk and the whole corner finally feels intentional. Matte finish kills glare from the window.',
            'https://images.unsplash.com/photo-1513519245088-0e12902e5a38?w=900&h=1200&fit=crop&auto=format&q=80',
            NULL, 'Chennai', NOW() - INTERVAL '3 days')
    ON CONFLICT (user_id, product_id) DO UPDATE
      SET rating           = EXCLUDED.rating,
          comment          = EXCLUDED.comment,
          photo_url        = EXCLUDED.photo_url,
          photo_object_key = EXCLUDED.photo_object_key,
          city             = EXCLUDED.city;
  END IF;

  --- The Crossing — text-only --------------------------------------------
  SELECT user_id INTO uid FROM users          WHERE email = 'arjun.rao@seed.wallstories.test';
  SELECT product_id INTO pid FROM product_legacy_map WHERE slug = 'crossing';
  IF uid IS NOT NULL AND pid IS NOT NULL THEN
    INSERT INTO reviews (review_id, user_id, product_id, rating, comment, photo_url, photo_object_key, city, created_at)
    VALUES (gen_random_uuid(), uid, pid, 4,
            'Lovely moody print, packed really well. Shipped in a sturdy tube and arrived in Bangalore in three days.',
            NULL, NULL, 'Bengaluru', NOW() - INTERVAL '6 days')
    ON CONFLICT (user_id, product_id) DO UPDATE
      SET rating  = EXCLUDED.rating,
          comment = EXCLUDED.comment,
          city    = EXCLUDED.city;
  END IF;

  --- Golden Fields — photo review ----------------------------------------
  SELECT user_id INTO uid FROM users          WHERE email = 'neha.iyer@seed.wallstories.test';
  SELECT product_id INTO pid FROM product_legacy_map WHERE slug = 'golden-fields';
  IF uid IS NOT NULL AND pid IS NOT NULL THEN
    INSERT INTO reviews (review_id, user_id, product_id, rating, comment, photo_url, photo_object_key, city, created_at)
    VALUES (gen_random_uuid(), uid, pid, 5,
            'Warm desert tones against our cane sofa — it just works. A2 was the right call, the detail holds up beautifully.',
            'https://images.unsplash.com/photo-1616486338812-3dadae4b4ace?w=900&h=1200&fit=crop&auto=format&q=80',
            NULL, 'Pune', NOW() - INTERVAL '9 days')
    ON CONFLICT (user_id, product_id) DO UPDATE
      SET rating           = EXCLUDED.rating,
          comment          = EXCLUDED.comment,
          photo_url        = EXCLUDED.photo_url,
          photo_object_key = EXCLUDED.photo_object_key,
          city             = EXCLUDED.city;
  END IF;

  --- Dune Shadow — photo review ------------------------------------------
  SELECT user_id INTO uid FROM users          WHERE email = 'kabir.shah@seed.wallstories.test';
  SELECT product_id INTO pid FROM product_legacy_map WHERE slug = 'dune-shadow';
  IF uid IS NOT NULL AND pid IS NOT NULL THEN
    INSERT INTO reviews (review_id, user_id, product_id, rating, comment, photo_url, photo_object_key, city, created_at)
    VALUES (gen_random_uuid(), uid, pid, 5,
            'The tonal range on this is unreal. Printed at A2 on the matte stock and the shadows stay deep without crushing.',
            'https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=900&h=1200&fit=crop&auto=format&q=80',
            NULL, 'Mumbai', NOW() - INTERVAL '12 days')
    ON CONFLICT (user_id, product_id) DO UPDATE
      SET rating           = EXCLUDED.rating,
          comment          = EXCLUDED.comment,
          photo_url        = EXCLUDED.photo_url,
          photo_object_key = EXCLUDED.photo_object_key,
          city             = EXCLUDED.city;
  END IF;

  --- Fog & Pines — text-only ---------------------------------------------
  SELECT user_id INTO uid FROM users          WHERE email = 'meera.nair@seed.wallstories.test';
  SELECT product_id INTO pid FROM product_legacy_map WHERE slug = 'fog-pines';
  IF uid IS NOT NULL AND pid IS NOT NULL THEN
    INSERT INTO reviews (review_id, user_id, product_id, rating, comment, photo_url, photo_object_key, city, created_at)
    VALUES (gen_random_uuid(), uid, pid, 4,
            'Quiet, calming piece — sits well in the bedroom. Would have loved an A2 option, hopefully soon.',
            NULL, NULL, 'Kochi', NOW() - INTERVAL '15 days')
    ON CONFLICT (user_id, product_id) DO UPDATE
      SET rating  = EXCLUDED.rating,
          comment = EXCLUDED.comment,
          city    = EXCLUDED.city;
  END IF;

  --- First Press — photo review ------------------------------------------
  SELECT user_id INTO uid FROM users          WHERE email = 'rohan.gupta@seed.wallstories.test';
  SELECT product_id INTO pid FROM product_legacy_map WHERE slug = 'first-press';
  IF uid IS NOT NULL AND pid IS NOT NULL THEN
    INSERT INTO reviews (review_id, user_id, product_id, rating, comment, photo_url, photo_object_key, city, created_at)
    VALUES (gen_random_uuid(), uid, pid, 5,
            'Perfect over the record shelf. The blues really pop against the oak and the frame feels premium.',
            'https://images.unsplash.com/photo-1487215078519-e21cc028cb29?w=900&h=1200&fit=crop&auto=format&q=80',
            NULL, 'Hyderabad', NOW() - INTERVAL '18 days')
    ON CONFLICT (user_id, product_id) DO UPDATE
      SET rating           = EXCLUDED.rating,
          comment          = EXCLUDED.comment,
          photo_url        = EXCLUDED.photo_url,
          photo_object_key = EXCLUDED.photo_object_key,
          city             = EXCLUDED.city;
  END IF;

  --- Neon Tokyo — second review (different user) -------------------------
  SELECT user_id INTO uid FROM users          WHERE email = 'ishita.verma@seed.wallstories.test';
  SELECT product_id INTO pid FROM product_legacy_map WHERE slug = 'neon-tokyo';
  IF uid IS NOT NULL AND pid IS NOT NULL THEN
    INSERT INTO reviews (review_id, user_id, product_id, rating, comment, photo_url, photo_object_key, city, created_at)
    VALUES (gen_random_uuid(), uid, pid, 4,
            'Loved the colours. One tiny crease on arrival but support sent a replacement the same week — great service.',
            NULL, NULL, 'Delhi', NOW() - INTERVAL '21 days')
    ON CONFLICT (user_id, product_id) DO UPDATE
      SET rating  = EXCLUDED.rating,
          comment = EXCLUDED.comment,
          city    = EXCLUDED.city;
  END IF;

  --- Dune Shadow — second review (text) ----------------------------------
  SELECT user_id INTO uid FROM users          WHERE email = 'dev.pillai@seed.wallstories.test';
  SELECT product_id INTO pid FROM product_legacy_map WHERE slug = 'dune-shadow';
  IF uid IS NOT NULL AND pid IS NOT NULL THEN
    INSERT INTO reviews (review_id, user_id, product_id, rating, comment, photo_url, photo_object_key, city, created_at)
    VALUES (gen_random_uuid(), uid, pid, 5,
            'Exactly the vibe I wanted for the reading nook. Paper stock feels thick and the colours are spot on.',
            NULL, NULL, 'Thiruvananthapuram', NOW() - INTERVAL '24 days')
    ON CONFLICT (user_id, product_id) DO UPDATE
      SET rating  = EXCLUDED.rating,
          comment = EXCLUDED.comment,
          city    = EXCLUDED.city;
  END IF;
END $$;

COMMIT;

-- Quick sanity check — uncomment to verify from psql.
-- SELECT r.rating, r.city, u.first_name, p.name, (r.photo_url IS NOT NULL) AS has_photo
-- FROM reviews r
-- JOIN users u    ON u.user_id    = r.user_id
-- JOIN products p ON p.product_id = r.product_id
-- WHERE u.email LIKE '%@seed.wallstories.test'
-- ORDER BY r.created_at DESC;
