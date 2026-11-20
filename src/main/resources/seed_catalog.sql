-- Seed: the original dummy storefront catalogue, mapped to real product UUIDs.
-- Idempotent: products already present in product_legacy_map are skipped.
-- Requires db_migrations.sql (attributes column + product_legacy_map). Run once per environment.

DO $$ DECLARE pid UUID; BEGIN
  IF NOT EXISTS (SELECT 1 FROM product_legacy_map WHERE slug = 'neon-tokyo') THEN
    INSERT INTO products (product_id, name, description, category, price, stock_quantity, is_active, attributes)
    VALUES (gen_random_uuid(), 'Neon Tokyo', 'Rain on asphalt, signage bleeding into the puddles. Printed on 300 GSM matte art paper so the blacks stay deep and the neon stays soft.', 'Movies', 499, 100, true, '{"code": "WS-MOV-01", "alt": "Cars on a rain-slick road between high-rise buildings lit by neon signs at night", "badge": "bestseller", "orientation": "portrait"}') RETURNING product_id INTO pid;
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, 'A4', 499, 100, true, 0);
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, 'A3', 799, 100, true, 1);
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, 'A2', 1199, 100, true, 2);
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, '12×18', 719, 100, true, 3);
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, '18×24', 1069, 100, true, 4);
    INSERT INTO product_images (id, product_id, image_url, object_key, is_primary) VALUES (gen_random_uuid(), pid, 'https://images.unsplash.com/photo-1601042879364-f3947d3f9c16?w=900&h=1200&fit=crop&auto=format&q=80', 'external/neon-tokyo', true);
    INSERT INTO product_legacy_map (slug, code, product_id) VALUES ('neon-tokyo', 'WS-MOV-01', pid);
  END IF;
END $$;

DO $$ DECLARE pid UUID; BEGIN
  IF NOT EXISTS (SELECT 1 FROM product_legacy_map WHERE slug = 'crossing') THEN
    INSERT INTO products (product_id, name, description, category, price, stock_quantity, is_active, attributes)
    VALUES (gen_random_uuid(), 'The Crossing', 'One figure, one intersection, one held breath. A quiet cinematic frame for the wall above a desk.', 'Movies', 449, 100, true, '{"code": "WS-MOV-02", "alt": "A person in a grey raincoat crossing a neon-lit intersection beside an orange car", "orientation": "portrait"}') RETURNING product_id INTO pid;
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, 'A4', 449, 100, true, 0);
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, 'A3', 719, 100, true, 1);
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, '12×18', 649, 100, true, 2);
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, '18×24', 969, 100, true, 3);
    INSERT INTO product_images (id, product_id, image_url, object_key, is_primary) VALUES (gen_random_uuid(), pid, 'https://images.unsplash.com/photo-1541702467897-41915a07d3a7?w=900&h=1200&fit=crop&auto=format&q=80', 'external/crossing', true);
    INSERT INTO product_legacy_map (slug, code, product_id) VALUES ('crossing', 'WS-MOV-02', pid);
  END IF;
END $$;

DO $$ DECLARE pid UUID; BEGIN
  IF NOT EXISTS (SELECT 1 FROM product_legacy_map WHERE slug = 'golden-fields') THEN
    INSERT INTO products (product_id, name, description, category, price, stock_quantity, is_active, attributes)
    VALUES (gen_random_uuid(), 'Golden Fields', 'Sun-bleached earth meeting an unbroken sky. Warm enough to sit beside cane furniture, calm enough for a bedroom.', 'Nature', 469, 100, true, '{"code": "WS-NAT-01", "alt": "A wide brown desert field stretching under a clear blue sky", "orientation": "portrait"}') RETURNING product_id INTO pid;
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, 'A4', 469, 100, true, 0);
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, 'A3', 749, 100, true, 1);
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, 'A2', 1129, 100, true, 2);
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, '18×24', 1009, 100, true, 3);
    INSERT INTO product_images (id, product_id, image_url, object_key, is_primary) VALUES (gen_random_uuid(), pid, 'https://images.unsplash.com/photo-1613854680145-f04b67cd7fa1?w=900&h=1200&fit=crop&auto=format&q=80', 'external/golden-fields', true);
    INSERT INTO product_legacy_map (slug, code, product_id) VALUES ('golden-fields', 'WS-NAT-01', pid);
  END IF;
END $$;

DO $$ DECLARE pid UUID; BEGIN
  IF NOT EXISTS (SELECT 1 FROM product_legacy_map WHERE slug = 'dune-shadow') THEN
    INSERT INTO products (product_id, name, description, category, price, stock_quantity, is_active, attributes)
    VALUES (gen_random_uuid(), 'Dune Shadow', 'Light doing geometry on sand. The tonal range holds up beautifully at A2 — this one is worth going large on.', 'Nature', 519, 100, true, '{"code": "WS-NAT-02", "alt": "Curving sand dunes cast in alternating shadow and light", "badge": "few-left", "orientation": "portrait"}') RETURNING product_id INTO pid;
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, 'A4', 519, 100, true, 0);
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, 'A3', 829, 100, true, 1);
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, 'A2', 1249, 100, true, 2);
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, '12×18', 749, 100, true, 3);
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, '18×24', 1119, 100, true, 4);
    INSERT INTO product_images (id, product_id, image_url, object_key, is_primary) VALUES (gen_random_uuid(), pid, 'https://images.unsplash.com/photo-1750859876218-7c66b60afa02?w=900&h=1200&fit=crop&auto=format&q=80', 'external/dune-shadow', true);
    INSERT INTO product_legacy_map (slug, code, product_id) VALUES ('dune-shadow', 'WS-NAT-02', pid);
  END IF;
END $$;

DO $$ DECLARE pid UUID; BEGIN
  IF NOT EXISTS (SELECT 1 FROM product_legacy_map WHERE slug = 'fog-pines') THEN
    INSERT INTO products (product_id, name, description, category, price, stock_quantity, is_active, attributes)
    VALUES (gen_random_uuid(), 'Fog & Pines', 'Almost monochrome, almost silent. Pairs well with the sage and cream rooms it usually ends up in.', 'Nature', 429, 100, true, '{"code": "WS-NAT-03", "alt": "Green pine trees surrounded by thick morning fog", "orientation": "portrait"}') RETURNING product_id INTO pid;
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, 'A4', 429, 100, true, 0);
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, 'A3', 689, 100, true, 1);
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, '12×18', 619, 100, true, 2);
    INSERT INTO product_images (id, product_id, image_url, object_key, is_primary) VALUES (gen_random_uuid(), pid, 'https://images.unsplash.com/photo-1529333110658-26b4cf759641?w=900&h=1200&fit=crop&auto=format&q=80', 'external/fog-pines', true);
    INSERT INTO product_legacy_map (slug, code, product_id) VALUES ('fog-pines', 'WS-NAT-03', pid);
  END IF;
END $$;

DO $$ DECLARE pid UUID; BEGIN
  IF NOT EXISTS (SELECT 1 FROM product_legacy_map WHERE slug = 'first-press') THEN
    INSERT INTO products (product_id, name, description, category, price, stock_quantity, is_active, attributes)
    VALUES (gen_random_uuid(), 'First Press', 'For the people who still flip the record halfway through dinner. Deep blues against warm wood.', 'Music', 439, 100, true, '{"code": "WS-MUS-01", "alt": "A black and blue turntable resting on a brown wooden side table", "orientation": "portrait"}') RETURNING product_id INTO pid;
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, 'A4', 439, 100, true, 0);
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, 'A3', 699, 100, true, 1);
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, 'A2', 1049, 100, true, 2);
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, '12×18', 639, 100, true, 3);
    INSERT INTO product_images (id, product_id, image_url, object_key, is_primary) VALUES (gen_random_uuid(), pid, 'https://images.unsplash.com/photo-1485726696757-76885c99f0f5?w=900&h=1200&fit=crop&auto=format&q=80', 'external/first-press', true);
    INSERT INTO product_legacy_map (slug, code, product_id) VALUES ('first-press', 'WS-MUS-01', pid);
  END IF;
END $$;

DO $$ DECLARE pid UUID; BEGIN
  IF NOT EXISTS (SELECT 1 FROM product_legacy_map WHERE slug = 'side-b') THEN
    INSERT INTO products (product_id, name, description, category, price, stock_quantity, is_active, attributes)
    VALUES (gen_random_uuid(), 'Side B', 'A top-down study of a tonearm mid-track. Reads as a graphic print from across the room.', 'Music', 459, 100, true, '{"code": "WS-MUS-02", "alt": "A black turntable photographed from above on a brown wooden table", "orientation": "portrait"}') RETURNING product_id INTO pid;
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, 'A4', 459, 100, true, 0);
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, 'A3', 729, 100, true, 1);
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, '18×24', 989, 100, true, 2);
    INSERT INTO product_images (id, product_id, image_url, object_key, is_primary) VALUES (gen_random_uuid(), pid, 'https://images.unsplash.com/photo-1526394931762-90052e97b376?w=900&h=1200&fit=crop&auto=format&q=80', 'external/side-b', true);
    INSERT INTO product_legacy_map (slug, code, product_id) VALUES ('side-b', 'WS-MUS-02', pid);
  END IF;
END $$;

DO $$ DECLARE pid UUID; BEGIN
  IF NOT EXISTS (SELECT 1 FROM product_legacy_map WHERE slug = 'soft-machine') THEN
    INSERT INTO products (product_id, name, description, category, price, stock_quantity, is_active, attributes)
    VALUES (gen_random_uuid(), 'Soft Machine', 'Molten oranges folding into each other. The one print in the catalogue that changes colour with the light in the room.', 'Abstract', 489, 100, true, '{"code": "WS-ABS-01", "alt": "A close-up of stacked orange and yellow curved forms", "orientation": "portrait"}') RETURNING product_id INTO pid;
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, 'A4', 489, 100, true, 0);
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, 'A3', 779, 100, true, 1);
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, 'A2', 1169, 100, true, 2);
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, '18×24', 1049, 100, true, 3);
    INSERT INTO product_images (id, product_id, image_url, object_key, is_primary) VALUES (gen_random_uuid(), pid, 'https://images.unsplash.com/photo-1738937444519-f0a7084c955e?w=900&h=1200&fit=crop&auto=format&q=80', 'external/soft-machine', true);
    INSERT INTO product_legacy_map (slug, code, product_id) VALUES ('soft-machine', 'WS-ABS-01', pid);
  END IF;
END $$;

DO $$ DECLARE pid UUID; BEGIN
  IF NOT EXISTS (SELECT 1 FROM product_legacy_map WHERE slug = 'blue-torsion') THEN
    INSERT INTO products (product_id, name, description, category, price, stock_quantity, is_active, attributes)
    VALUES (gen_random_uuid(), 'Blue Torsion', 'Hard geometry that somehow reads as fabric. A cool counterweight if the rest of your wall runs warm.', 'Abstract', 479, 100, true, '{"code": "WS-ABS-02", "alt": "Blue geometric shapes twisting into an abstract woven pattern", "badge": "new", "orientation": "portrait"}') RETURNING product_id INTO pid;
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, 'A4', 479, 100, true, 0);
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, 'A3', 769, 100, true, 1);
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, 'A2', 1149, 100, true, 2);
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, '12×18', 689, 100, true, 3);
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, '18×24', 1029, 100, true, 4);
    INSERT INTO product_images (id, product_id, image_url, object_key, is_primary) VALUES (gen_random_uuid(), pid, 'https://images.unsplash.com/photo-1774360502057-a934d7d0ce60?w=900&h=1200&fit=crop&auto=format&q=80', 'external/blue-torsion', true);
    INSERT INTO product_legacy_map (slug, code, product_id) VALUES ('blue-torsion', 'WS-ABS-02', pid);
  END IF;
END $$;

DO $$ DECLARE pid UUID; BEGIN
  IF NOT EXISTS (SELECT 1 FROM product_legacy_map WHERE slug = 'quiet-planet') THEN
    INSERT INTO products (product_id, name, description, category, price, stock_quantity, is_active, attributes)
    VALUES (gen_random_uuid(), 'Quiet Planet', 'A single grain-heavy sphere with a hairline caption set below it. Understated, and the easiest thing here to hang anywhere.', 'Typography', 409, 100, true, '{"code": "WS-TYP-01", "alt": "A black and white grainy rendering of a sphere against a dark field", "orientation": "portrait"}') RETURNING product_id INTO pid;
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, 'A4', 409, 100, true, 0);
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, 'A3', 649, 100, true, 1);
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, '12×18', 589, 100, true, 2);
    INSERT INTO product_images (id, product_id, image_url, object_key, is_primary) VALUES (gen_random_uuid(), pid, 'https://images.unsplash.com/photo-1652074847108-0b4294408ca1?w=900&h=1200&fit=crop&auto=format&q=80', 'external/quiet-planet', true);
    INSERT INTO product_legacy_map (slug, code, product_id) VALUES ('quiet-planet', 'WS-TYP-01', pid);
  END IF;
END $$;

DO $$ DECLARE pid UUID; BEGIN
  IF NOT EXISTS (SELECT 1 FROM product_legacy_map WHERE slug = 'lubitel') THEN
    INSERT INTO products (product_id, name, description, category, price, stock_quantity, is_active, attributes)
    VALUES (gen_random_uuid(), 'Lubitel 2', 'A twin-lens reflex, shot the way it deserves. Every dial and screw is sharp right up to A2.', 'Vintage', 449, 100, true, '{"code": "WS-VIN-01", "alt": "A vintage Lubitel 2 twin lens reflex camera photographed against a plain backdrop", "orientation": "portrait"}') RETURNING product_id INTO pid;
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, 'A4', 449, 100, true, 0);
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, 'A3', 719, 100, true, 1);
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, 'A2', 1079, 100, true, 2);
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, '12×18', 649, 100, true, 3);
    INSERT INTO product_images (id, product_id, image_url, object_key, is_primary) VALUES (gen_random_uuid(), pid, 'https://images.unsplash.com/photo-1759995929231-9380d247b004?w=900&h=1200&fit=crop&auto=format&q=80', 'external/lubitel', true);
    INSERT INTO product_legacy_map (slug, code, product_id) VALUES ('lubitel', 'WS-VIN-01', pid);
  END IF;
END $$;

DO $$ DECLARE pid UUID; BEGIN
  IF NOT EXISTS (SELECT 1 FROM product_legacy_map WHERE slug = 'court-light') THEN
    INSERT INTO products (product_id, name, description, category, price, stock_quantity, is_active, attributes)
    VALUES (gen_random_uuid(), 'Court Light', 'The last ball on the court after everyone has gone home. Grainy, warm, and quieter than most sports prints.', 'Sports', 439, 100, true, '{"code": "WS-SPR-01", "alt": "A brown basketball resting on a grey concrete outdoor court floor", "orientation": "portrait"}') RETURNING product_id INTO pid;
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, 'A4', 439, 100, true, 0);
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, 'A3', 699, 100, true, 1);
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, '18×24', 939, 100, true, 2);
    INSERT INTO product_images (id, product_id, image_url, object_key, is_primary) VALUES (gen_random_uuid(), pid, 'https://images.unsplash.com/photo-1595795279832-13f0df36fbb9?w=900&h=1200&fit=crop&auto=format&q=80', 'external/court-light', true);
    INSERT INTO product_legacy_map (slug, code, product_id) VALUES ('court-light', 'WS-SPR-01', pid);
  END IF;
END $$;

DO $$ DECLARE pid UUID; BEGIN
  IF NOT EXISTS (SELECT 1 FROM product_legacy_map WHERE slug = 'shelf-life') THEN
    INSERT INTO products (product_id, name, description, category, price, stock_quantity, is_active, attributes)
    VALUES (gen_random_uuid(), 'Shelf Life', 'Two old bodies on a dark shelf, lit like a still life. Deep shadows — best on the matte stock.', 'Vintage', 419, 100, true, '{"code": "WS-VIN-02", "alt": "Two vintage cameras displayed side by side on a dark wooden shelf", "orientation": "portrait"}') RETURNING product_id INTO pid;
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, 'A4', 419, 100, true, 0);
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, 'A3', 669, 100, true, 1);
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, '12×18', 609, 100, true, 2);
    INSERT INTO product_images (id, product_id, image_url, object_key, is_primary) VALUES (gen_random_uuid(), pid, 'https://images.unsplash.com/photo-1784316395297-c1f0b835242b?w=900&h=1200&fit=crop&auto=format&q=80', 'external/shelf-life', true);
    INSERT INTO product_legacy_map (slug, code, product_id) VALUES ('shelf-life', 'WS-VIN-02', pid);
  END IF;
END $$;

DO $$ DECLARE pid UUID; BEGIN
  IF NOT EXISTS (SELECT 1 FROM product_legacy_map WHERE slug = 'monstera-study') THEN
    INSERT INTO products (product_id, name, description, category, price, stock_quantity, is_active, attributes)
    VALUES (gen_random_uuid(), 'Monstera Study', 'One leaf, edge-lit, nothing else. The print people buy in pairs and hang either side of a doorway.', 'Botanical', 399, 100, true, '{"code": "WS-BOT-01", "alt": "A single green leaf photographed against a pale background", "badge": "new", "orientation": "portrait"}') RETURNING product_id INTO pid;
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, 'A4', 399, 100, true, 0);
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, 'A3', 639, 100, true, 1);
    INSERT INTO product_variants (product_id, label, price, stock_quantity, is_active, sort_order) VALUES (pid, '12×18', 579, 100, true, 2);
    INSERT INTO product_images (id, product_id, image_url, object_key, is_primary) VALUES (gen_random_uuid(), pid, 'https://images.unsplash.com/photo-1533038590840-1cde6e668a91?w=900&h=1200&fit=crop&auto=format&q=80', 'external/monstera-study', true);
    INSERT INTO product_legacy_map (slug, code, product_id) VALUES ('monstera-study', 'WS-BOT-01', pid);
  END IF;
END $$;
