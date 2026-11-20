-- Repair only records using this account's private R2 API hostname.
-- Set R2_PUBLIC_BASE_URL to the public domain below before restarting the backend.
-- Objects stay in place; image IDs, product links and primary flags are unchanged.
BEGIN;

UPDATE product_images
SET image_url = 'https://pub-9769fbcd0cd54caf8433f76b6dc9a37b.r2.dev/' || object_key
WHERE image_url LIKE 'https://7f4616d0ea838285b079d2f738af9173.r2.cloudflarestorage.com/%'
  AND object_key IS NOT NULL
  AND object_key <> ''
  AND object_key NOT LIKE 'external/%'
RETURNING product_image_id, product_id, image_url, is_primary;

UPDATE reviews
SET photo_url = 'https://pub-9769fbcd0cd54caf8433f76b6dc9a37b.r2.dev/' || photo_object_key
WHERE photo_url LIKE 'https://7f4616d0ea838285b079d2f738af9173.r2.cloudflarestorage.com/%'
  AND photo_object_key IS NOT NULL
  AND photo_object_key <> ''
RETURNING review_id, product_id, photo_url;

COMMIT;
