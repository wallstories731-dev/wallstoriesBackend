CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TYPE public.inventory_operation_enum AS ENUM ('ORDER_PLACED', 'ORDER_CANCELLED', 'ADMIN_UPDATE', 'RESTOCK');

CREATE TYPE public.order_status_enum AS ENUM ('PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED');

CREATE TYPE public.permission_action_enum AS ENUM ('create', 'read', 'update', 'delete', 'crud', 'cru', 'cr', 'c');

CREATE TYPE public.user_status_enum AS ENUM ('ACTIVE', 'INACTIVE', 'DEAD');

CREATE TABLE IF NOT EXISTS public.affiliate_attributions (
  "attribution_id" uuid DEFAULT gen_random_uuid() NOT NULL,
  "referred_user_id" uuid NOT NULL,
  "link_id" uuid NOT NULL,
  "affiliate_id" uuid NOT NULL,
  "attributed_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
  "expires_at" timestamp without time zone NOT NULL,
  "consumed" boolean DEFAULT false NOT NULL,
  "created_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
  "updated_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT "affiliate_attributions_referred_user_id_key" UNIQUE (referred_user_id),
  CONSTRAINT "affiliate_attributions_pkey" PRIMARY KEY (attribution_id)
);

CREATE TABLE IF NOT EXISTS public.affiliate_conversions (
  "conversion_id" uuid DEFAULT gen_random_uuid() NOT NULL,
  "attribution_id" uuid NOT NULL,
  "order_id" uuid NOT NULL,
  "affiliate_id" uuid NOT NULL,
  "referred_user_id" uuid NOT NULL,
  "order_total" numeric(12,2) NOT NULL,
  "points_awarded" integer NOT NULL,
  "reversed" boolean DEFAULT false NOT NULL,
  "created_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT "affiliate_conversions_order_id_key" UNIQUE (order_id),
  CONSTRAINT "affiliate_conversions_pkey" PRIMARY KEY (conversion_id)
);

CREATE TABLE IF NOT EXISTS public.affiliate_links (
  "link_id" uuid DEFAULT gen_random_uuid() NOT NULL,
  "code" character varying(16) NOT NULL,
  "affiliate_id" uuid NOT NULL,
  "product_id" uuid,
  "target_path" character varying(255) DEFAULT '/'::character varying NOT NULL,
  "clicks" integer DEFAULT 0 NOT NULL,
  "active" boolean DEFAULT true NOT NULL,
  "created_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
  "updated_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT "affiliate_links_code_key" UNIQUE (code),
  CONSTRAINT "affiliate_links_pkey" PRIMARY KEY (link_id)
);

CREATE TABLE IF NOT EXISTS public.blog_posts (
  "post_id" uuid DEFAULT gen_random_uuid() NOT NULL,
  "title" character varying(200) NOT NULL,
  "slug" character varying(220) NOT NULL,
  "excerpt" text,
  "content" text NOT NULL,
  "cover_image_url" text,
  "category" character varying(80),
  "tags" text,
  "status" character varying(20) DEFAULT 'DRAFT'::character varying NOT NULL,
  "author_id" uuid,
  "view_count" integer DEFAULT 0 NOT NULL,
  "title_tamil" text,
  "content_tamil" text,
  "published_at" timestamp without time zone,
  "created_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
  "updated_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT "blog_posts_slug_key" UNIQUE (slug),
  CONSTRAINT "blog_posts_pkey" PRIMARY KEY (post_id),
  CONSTRAINT "chk_blog_posts_status" CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'PUBLISHED'::character varying, 'ARCHIVED'::character varying])::text[])))
);

CREATE TABLE IF NOT EXISTS public.cart_items (
  "cart_item_id" uuid DEFAULT gen_random_uuid() NOT NULL,
  "cart_id" uuid NOT NULL,
  "product_id" uuid NOT NULL,
  "quantity" integer DEFAULT 1 NOT NULL,
  "created_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
  "price" numeric DEFAULT 0 NOT NULL,
  "variant_id" uuid,
  "variant_label" character varying(80),
  CONSTRAINT "cart_items_pkey" PRIMARY KEY (cart_item_id)
);

CREATE TABLE IF NOT EXISTS public.carts (
  "cart_id" uuid DEFAULT gen_random_uuid() NOT NULL,
  "user_id" uuid NOT NULL,
  "created_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
  "total_amount" numeric DEFAULT 0 NOT NULL,
  CONSTRAINT "carts_user_id_key" UNIQUE (user_id),
  CONSTRAINT "carts_pkey" PRIMARY KEY (cart_id)
);

CREATE TABLE IF NOT EXISTS public.contact_messages (
  "message_id" uuid NOT NULL,
  "name" character varying(120) NOT NULL,
  "email" character varying(255) NOT NULL,
  "phone" character varying(40),
  "subject" character varying(200),
  "message" text NOT NULL,
  "user_id" uuid,
  "status" character varying(20) DEFAULT 'NEW'::character varying NOT NULL,
  "created_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
  "updated_at" timestamp without time zone,
  CONSTRAINT "contact_messages_pkey" PRIMARY KEY (message_id)
);

CREATE TABLE IF NOT EXISTS public.email_verification_tokens (
  "token_id" uuid DEFAULT gen_random_uuid() NOT NULL,
  "user_id" uuid NOT NULL,
  "token_hash" character varying(128) NOT NULL,
  "expires_at" timestamp without time zone NOT NULL,
  "used_at" timestamp without time zone,
  "created_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT "email_verification_tokens_token_hash_key" UNIQUE (token_hash),
  CONSTRAINT "email_verification_tokens_pkey" PRIMARY KEY (token_id)
);

CREATE TABLE IF NOT EXISTS public.festival_category (
  "festival_id" uuid DEFAULT gen_random_uuid() NOT NULL,
  "name" character varying(100) NOT NULL,
  "slug" character varying(100) NOT NULL,
  "display_order" integer DEFAULT 0 NOT NULL,
  "is_active" boolean DEFAULT true NOT NULL,
  "created_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT "festival_category_name_key" UNIQUE (name),
  CONSTRAINT "festival_category_slug_key" UNIQUE (slug),
  CONSTRAINT "festival_category_pkey" PRIMARY KEY (festival_id)
);

CREATE TABLE IF NOT EXISTS public.festival_image (
  "image_id" uuid DEFAULT gen_random_uuid() NOT NULL,
  "festival_id" uuid NOT NULL,
  "image_url" character varying(1000) NOT NULL,
  "object_key" character varying(500) NOT NULL,
  "label" character varying(200),
  "display_order" integer DEFAULT 0 NOT NULL,
  "is_active" boolean DEFAULT true NOT NULL,
  "created_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
  "created_by" uuid,
  CONSTRAINT "festival_image_pkey" PRIMARY KEY (image_id)
);

CREATE TABLE IF NOT EXISTS public.festival_message (
  "message_id" uuid DEFAULT gen_random_uuid() NOT NULL,
  "festival_id" uuid NOT NULL,
  "message_text" text NOT NULL,
  "share_slug" character varying(12) NOT NULL,
  "is_active" boolean DEFAULT true NOT NULL,
  "created_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
  "created_by" uuid,
  "image_id" uuid,
  "sender_name" character varying(120),
  "recipient_name" character varying(120),
  CONSTRAINT "festival_message_share_slug_key" UNIQUE (share_slug),
  CONSTRAINT "festival_message_pkey" PRIMARY KEY (message_id)
);

CREATE TABLE IF NOT EXISTS public.instant_order_items (
  "id" uuid DEFAULT gen_random_uuid() NOT NULL,
  "instant_order_id" uuid NOT NULL,
  "menu_item_id" uuid,
  "menu_item_name" character varying(120) NOT NULL,
  "unit_price" numeric(10,2) NOT NULL,
  "quantity" integer NOT NULL,
  "line_total" numeric(10,2) NOT NULL,
  CONSTRAINT "instant_order_items_pkey" PRIMARY KEY (id),
  CONSTRAINT "instant_order_items_line_total_check" CHECK ((line_total >= (0)::numeric)),
  CONSTRAINT "instant_order_items_quantity_check" CHECK ((quantity > 0)),
  CONSTRAINT "instant_order_items_unit_price_check" CHECK ((unit_price >= (0)::numeric))
);

CREATE TABLE IF NOT EXISTS public.instant_orders (
  "id" uuid DEFAULT gen_random_uuid() NOT NULL,
  "user_id" uuid,
  "customer_name" character varying(120) NOT NULL,
  "customer_phone" character varying(20),
  "notes" text,
  "status" character varying(20) DEFAULT 'RECEIVED'::character varying NOT NULL,
  "total_amount" numeric(10,2) NOT NULL,
  "created_at" timestamp without time zone DEFAULT now() NOT NULL,
  "updated_at" timestamp without time zone DEFAULT now() NOT NULL,
  "token" character varying(10),
  CONSTRAINT "instant_orders_pkey" PRIMARY KEY (id),
  CONSTRAINT "instant_orders_status_check" CHECK (((status)::text = ANY ((ARRAY['RECEIVED'::character varying, 'PREPARING'::character varying, 'READY'::character varying, 'PICKED_UP'::character varying, 'CANCELLED'::character varying])::text[]))),
  CONSTRAINT "instant_orders_total_amount_check" CHECK ((total_amount >= (0)::numeric))
);

CREATE TABLE IF NOT EXISTS public.inventory_logs (
  "inventory_log_id" uuid DEFAULT gen_random_uuid() NOT NULL,
  "product_id" uuid NOT NULL,
  "quantity_before" integer NOT NULL,
  "quantity_changed" integer NOT NULL,
  "quantity_after" integer NOT NULL,
  "operation_type" character varying(50) NOT NULL,
  "created_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "inventory_logs_pkey" PRIMARY KEY (inventory_log_id)
);

CREATE TABLE IF NOT EXISTS public.live_menu_items (
  "id" uuid DEFAULT gen_random_uuid() NOT NULL,
  "name" character varying(120) NOT NULL,
  "description" text,
  "price" numeric(10,2) NOT NULL,
  "category" character varying(80),
  "image_url" text,
  "available" boolean DEFAULT true NOT NULL,
  "sort_order" integer DEFAULT 0 NOT NULL,
  "created_at" timestamp without time zone DEFAULT now() NOT NULL,
  "updated_at" timestamp without time zone DEFAULT now() NOT NULL,
  "stock_quantity" integer,
  CONSTRAINT "live_menu_items_pkey" PRIMARY KEY (id),
  CONSTRAINT "live_menu_items_price_check" CHECK ((price > (0)::numeric))
);

CREATE TABLE IF NOT EXISTS public.loyalty_cards (
  "card_id" uuid DEFAULT gen_random_uuid() NOT NULL,
  "user_id" uuid NOT NULL,
  "card_code" character varying(24) NOT NULL,
  "total_visits" integer DEFAULT 0 NOT NULL,
  "points" integer DEFAULT 0 NOT NULL,
  "created_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
  "updated_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT "loyalty_cards_card_code_key" UNIQUE (card_code),
  CONSTRAINT "loyalty_cards_user_id_key" UNIQUE (user_id),
  CONSTRAINT "loyalty_cards_pkey" PRIMARY KEY (card_id)
);

CREATE TABLE IF NOT EXISTS public.loyalty_checkins (
  "checkin_id" uuid DEFAULT gen_random_uuid() NOT NULL,
  "card_id" uuid NOT NULL,
  "user_id" uuid NOT NULL,
  "source" character varying(12) DEFAULT 'QR'::character varying NOT NULL,
  "status" character varying(12) DEFAULT 'PENDING'::character varying NOT NULL,
  "visit_day" date DEFAULT CURRENT_DATE NOT NULL,
  "order_id" uuid,
  "note" character varying(255),
  "approved_by" uuid,
  "approved_at" timestamp without time zone,
  "created_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
  "updated_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
  "purchase_amount" numeric(12,2),
  "points_awarded" integer,
  CONSTRAINT "loyalty_checkins_pkey" PRIMARY KEY (checkin_id)
);

CREATE TABLE IF NOT EXISTS public.loyalty_redemptions (
  "redemption_id" uuid DEFAULT gen_random_uuid() NOT NULL,
  "card_id" uuid NOT NULL,
  "user_id" uuid NOT NULL,
  "channel" character varying(10) DEFAULT 'OFFLINE'::character varying NOT NULL,
  "status" character varying(12) DEFAULT 'PENDING'::character varying NOT NULL,
  "points_used" integer NOT NULL,
  "point_value" numeric(10,2) NOT NULL,
  "cash_value" numeric(12,2) NOT NULL,
  "order_id" uuid,
  "note" character varying(255),
  "decided_by" uuid,
  "decided_at" timestamp without time zone,
  "created_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
  "updated_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT "loyalty_redemptions_pkey" PRIMARY KEY (redemption_id),
  CONSTRAINT "loyalty_redemptions_points_positive" CHECK ((points_used > 0))
);

CREATE TABLE IF NOT EXISTS public.loyalty_settings (
  "id" integer DEFAULT 1 NOT NULL,
  "point_value_inr" numeric(10,2) DEFAULT 10.00 NOT NULL,
  "updated_by" uuid,
  "updated_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
  "order_point_rate" numeric(10,4) DEFAULT 0 NOT NULL,
  "referral_point_rate" numeric(10,4) DEFAULT 0 NOT NULL,
  "referral_flat_points" integer DEFAULT 0 NOT NULL,
  "referral_window_days" integer DEFAULT 15 NOT NULL,
  CONSTRAINT "loyalty_settings_pkey" PRIMARY KEY (id),
  CONSTRAINT "loyalty_settings_singleton" CHECK ((id = 1))
);

CREATE TABLE IF NOT EXISTS public.notifications (
  "notification_id" uuid DEFAULT gen_random_uuid() NOT NULL,
  "user_id" uuid NOT NULL,
  "type" character varying(50) NOT NULL,
  "message" text NOT NULL,
  "is_read" boolean DEFAULT false,
  "created_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "notifications_pkey" PRIMARY KEY (notification_id)
);

CREATE TABLE IF NOT EXISTS public.order_items (
  "order_item_id" uuid DEFAULT gen_random_uuid() NOT NULL,
  "order_id" uuid NOT NULL,
  "product_id" uuid NOT NULL,
  "quantity" integer NOT NULL,
  "price" numeric(10,2) NOT NULL,
  "variant_id" uuid,
  "variant_label" character varying(80),
  CONSTRAINT "order_items_pkey" PRIMARY KEY (order_item_id)
);

CREATE TABLE IF NOT EXISTS public.orders (
  "order_id" uuid DEFAULT gen_random_uuid() NOT NULL,
  "user_id" uuid NOT NULL,
  "order_status" character varying(50) DEFAULT 'PENDING'::character varying,
  "total_amount" numeric(10,2) DEFAULT 0.00,
  "shipping_address" text NOT NULL,
  "phone_number" character varying(20) NOT NULL,
  "ordered_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
  "shipping_fee" double precision DEFAULT 0 NOT NULL,
  "tracking_number" character varying(50),
  "shipping_provider" character varying(30) DEFAULT 'STATIC'::character varying,
  CONSTRAINT "orders_pkey" PRIMARY KEY (order_id)
);

CREATE TABLE IF NOT EXISTS public.password_reset_tokens (
  "token_id" uuid DEFAULT gen_random_uuid() NOT NULL,
  "user_id" uuid NOT NULL,
  "token_hash" character varying(64) NOT NULL,
  "expires_at" timestamp without time zone NOT NULL,
  "used_at" timestamp without time zone,
  "created_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT "password_reset_tokens_token_hash_key" UNIQUE (token_hash),
  CONSTRAINT "password_reset_tokens_pkey" PRIMARY KEY (token_id)
);

CREATE TABLE IF NOT EXISTS public.payment_transactions (
  "payment_transaction_id" uuid NOT NULL,
  "order_id" uuid NOT NULL,
  "user_id" uuid NOT NULL,
  "payment_type" character varying(20) DEFAULT 'CART'::character varying NOT NULL,
  "razorpay_order_id" character varying(255) NOT NULL,
  "razorpay_payment_id" character varying(255),
  "razorpay_signature" text,
  "amount" numeric(12,2) NOT NULL,
  "currency" character varying(10) DEFAULT 'INR'::character varying NOT NULL,
  "payment_status" character varying(30) DEFAULT 'CREATED'::character varying NOT NULL,
  "raw_payload" text,
  "created_at" timestamp without time zone DEFAULT now() NOT NULL,
  "updated_at" timestamp without time zone DEFAULT now() NOT NULL,
  CONSTRAINT "payment_transactions_pkey" PRIMARY KEY (payment_transaction_id)
);

CREATE TABLE IF NOT EXISTS public.permission (
  "permission_id" bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  "name" character varying(100),
  "description" text,
  "resource" character varying(100),
  "action" character varying(50),
  CONSTRAINT "permission_pkey" PRIMARY KEY (permission_id)
);

CREATE TABLE IF NOT EXISTS public.product_images (
  "product_image_id" uuid DEFAULT gen_random_uuid() NOT NULL,
  "product_id" uuid NOT NULL,
  "image_url" character varying(500) NOT NULL,
  "object_key" character varying(255) NOT NULL,
  "is_primary" boolean DEFAULT false,
  "created_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "product_images_object_key_key" UNIQUE (object_key),
  CONSTRAINT "product_images_pkey" PRIMARY KEY (product_image_id)
);

CREATE TABLE IF NOT EXISTS public.product_variants (
  "variant_id" uuid DEFAULT gen_random_uuid() NOT NULL,
  "product_id" uuid NOT NULL,
  "label" character varying(80) NOT NULL,
  "price" numeric(10,2) NOT NULL,
  "stock_quantity" integer DEFAULT 0 NOT NULL,
  "is_active" boolean DEFAULT true NOT NULL,
  "sort_order" integer DEFAULT 0 NOT NULL,
  "created_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
  "updated_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT "product_variants_product_id_label_key" UNIQUE (product_id, label),
  CONSTRAINT "product_variants_pkey" PRIMARY KEY (variant_id)
);

CREATE TABLE IF NOT EXISTS public.products (
  "product_id" uuid DEFAULT gen_random_uuid() NOT NULL,
  "name" character varying(150) NOT NULL,
  "description" text,
  "category" character varying(100) NOT NULL,
  "price" numeric(10,2) NOT NULL,
  "stock_quantity" integer DEFAULT 0,
  "is_active" boolean DEFAULT true,
  "created_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
  "ingredients" text,
  "name_tamil" text,
  "description_tamil" text,
  "ingredients_tamil" text,
  CONSTRAINT "products_pkey" PRIMARY KEY (product_id)
);

CREATE TABLE IF NOT EXISTS public.push_subscriptions (
  "id" uuid DEFAULT gen_random_uuid() NOT NULL,
  "user_id" uuid NOT NULL,
  "endpoint" text NOT NULL,
  "p256dh" text NOT NULL,
  "auth" text NOT NULL,
  "user_agent" character varying(400),
  "created_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
  "last_seen" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT "push_subscriptions_endpoint_key" UNIQUE (endpoint),
  CONSTRAINT "push_subscriptions_pkey" PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS public.refresh_token (
  "refresh_token_id" bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  "token_hash" character varying(512),
  "user_id" uuid,
  "ip_address" character varying(45),
  "user_agent" character varying(200),
  "expires_at" timestamp without time zone,
  "created_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
  "is_revoked" boolean DEFAULT false,
  CONSTRAINT "refresh_token_pkey" PRIMARY KEY (refresh_token_id)
);

CREATE TABLE IF NOT EXISTS public.reviews (
  "review_id" uuid DEFAULT gen_random_uuid() NOT NULL,
  "user_id" uuid NOT NULL,
  "product_id" uuid NOT NULL,
  "rating" integer,
  "comment" text,
  "created_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "uq_user_product_review" UNIQUE (user_id, product_id),
  CONSTRAINT "reviews_pkey" PRIMARY KEY (review_id),
  CONSTRAINT "reviews_rating_check" CHECK (((rating >= 1) AND (rating <= 5)))
);

CREATE TABLE IF NOT EXISTS public.role (
  "role_id" bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
  "name" character varying(100),
  "description" text,
  CONSTRAINT "role_pkey" PRIMARY KEY (role_id)
);

CREATE TABLE IF NOT EXISTS public.role_permission (
  "role_id" bigint NOT NULL,
  "permission_id" bigint NOT NULL,
  CONSTRAINT "role_permission_pkey" PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE IF NOT EXISTS public.user_addresses (
  "address_id" uuid DEFAULT gen_random_uuid() NOT NULL,
  "user_id" uuid NOT NULL,
  "label" character varying(80),
  "full_name" character varying(120) NOT NULL,
  "phone" character varying(30) NOT NULL,
  "line1" character varying(160) NOT NULL,
  "line2" character varying(160),
  "city" character varying(80) NOT NULL,
  "state" character varying(80) NOT NULL,
  "pincode" character varying(20) NOT NULL,
  "country" character varying(60) DEFAULT 'IN'::character varying NOT NULL,
  "is_default" boolean DEFAULT false NOT NULL,
  "created_at" timestamp with time zone DEFAULT now() NOT NULL,
  "updated_at" timestamp with time zone DEFAULT now() NOT NULL,
  CONSTRAINT "user_addresses_pkey" PRIMARY KEY (address_id)
);

CREATE TABLE IF NOT EXISTS public.user_role (
  "role_id" bigint NOT NULL,
  "user_id" uuid NOT NULL,
  CONSTRAINT "user_role_pkey" PRIMARY KEY (role_id, user_id)
);

CREATE TABLE IF NOT EXISTS public.users (
  "user_id" uuid DEFAULT gen_random_uuid() NOT NULL,
  "email" character varying(250) NOT NULL,
  "password_hash" character varying(250) NOT NULL,
  "first_name" character varying(250),
  "last_name" character varying(250),
  "status" character varying(50),
  "is_admin" boolean DEFAULT false,
  "created_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
  "email_verified" boolean DEFAULT false NOT NULL,
  "email_verified_at" timestamp without time zone,
  "last_login" timestamp with time zone,
  CONSTRAINT "users_email_key" UNIQUE (email),
  CONSTRAINT "users_pkey" PRIMARY KEY (user_id)
);

CREATE TABLE IF NOT EXISTS public.wishlist_items (
  "user_id" uuid NOT NULL,
  "product_id" uuid NOT NULL,
  "created_at" timestamp without time zone DEFAULT now() NOT NULL,
  CONSTRAINT "wishlist_items_pkey" PRIMARY KEY (user_id, product_id)
);

ALTER TABLE public.affiliate_attributions ADD CONSTRAINT "affiliate_attributions_affiliate_id_fkey" FOREIGN KEY (affiliate_id) REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE public.affiliate_attributions ADD CONSTRAINT "affiliate_attributions_link_id_fkey" FOREIGN KEY (link_id) REFERENCES affiliate_links(link_id) ON DELETE CASCADE;

ALTER TABLE public.affiliate_attributions ADD CONSTRAINT "affiliate_attributions_referred_user_id_fkey" FOREIGN KEY (referred_user_id) REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE public.affiliate_conversions ADD CONSTRAINT "affiliate_conversions_affiliate_id_fkey" FOREIGN KEY (affiliate_id) REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE public.affiliate_conversions ADD CONSTRAINT "affiliate_conversions_attribution_id_fkey" FOREIGN KEY (attribution_id) REFERENCES affiliate_attributions(attribution_id) ON DELETE CASCADE;

ALTER TABLE public.affiliate_conversions ADD CONSTRAINT "affiliate_conversions_referred_user_id_fkey" FOREIGN KEY (referred_user_id) REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE public.affiliate_links ADD CONSTRAINT "affiliate_links_affiliate_id_fkey" FOREIGN KEY (affiliate_id) REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE public.affiliate_links ADD CONSTRAINT "affiliate_links_product_id_fkey" FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE;

ALTER TABLE public.blog_posts ADD CONSTRAINT "blog_posts_author_id_fkey" FOREIGN KEY (author_id) REFERENCES users(user_id) ON DELETE SET NULL;

ALTER TABLE public.cart_items ADD CONSTRAINT "cart_items_cart_id_fkey" FOREIGN KEY (cart_id) REFERENCES carts(cart_id) ON DELETE CASCADE;

ALTER TABLE public.cart_items ADD CONSTRAINT "cart_items_product_id_fkey" FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE;

ALTER TABLE public.carts ADD CONSTRAINT "carts_user_id_fkey" FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE public.email_verification_tokens ADD CONSTRAINT "email_verification_tokens_user_id_fkey" FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE public.festival_image ADD CONSTRAINT "festival_image_created_by_fkey" FOREIGN KEY (created_by) REFERENCES users(user_id) ON DELETE SET NULL;

ALTER TABLE public.festival_image ADD CONSTRAINT "festival_image_festival_id_fkey" FOREIGN KEY (festival_id) REFERENCES festival_category(festival_id) ON DELETE CASCADE;

ALTER TABLE public.festival_message ADD CONSTRAINT "festival_message_created_by_fkey" FOREIGN KEY (created_by) REFERENCES users(user_id) ON DELETE SET NULL;

ALTER TABLE public.festival_message ADD CONSTRAINT "festival_message_festival_id_fkey" FOREIGN KEY (festival_id) REFERENCES festival_category(festival_id) ON DELETE CASCADE;

ALTER TABLE public.festival_message ADD CONSTRAINT "festival_message_image_id_fkey" FOREIGN KEY (image_id) REFERENCES festival_image(image_id) ON DELETE SET NULL;

ALTER TABLE public.instant_order_items ADD CONSTRAINT "instant_order_items_instant_order_id_fkey" FOREIGN KEY (instant_order_id) REFERENCES instant_orders(id) ON DELETE CASCADE;

ALTER TABLE public.instant_order_items ADD CONSTRAINT "instant_order_items_menu_item_id_fkey" FOREIGN KEY (menu_item_id) REFERENCES live_menu_items(id) ON DELETE SET NULL;

ALTER TABLE public.instant_orders ADD CONSTRAINT "instant_orders_user_id_fkey" FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL;

ALTER TABLE public.inventory_logs ADD CONSTRAINT "inventory_logs_product_id_fkey" FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE;

ALTER TABLE public.loyalty_cards ADD CONSTRAINT "loyalty_cards_user_id_fkey" FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE public.loyalty_checkins ADD CONSTRAINT "loyalty_checkins_card_id_fkey" FOREIGN KEY (card_id) REFERENCES loyalty_cards(card_id) ON DELETE CASCADE;

ALTER TABLE public.loyalty_checkins ADD CONSTRAINT "loyalty_checkins_user_id_fkey" FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE public.loyalty_redemptions ADD CONSTRAINT "loyalty_redemptions_card_id_fkey" FOREIGN KEY (card_id) REFERENCES loyalty_cards(card_id) ON DELETE CASCADE;

ALTER TABLE public.loyalty_redemptions ADD CONSTRAINT "loyalty_redemptions_user_id_fkey" FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE public.notifications ADD CONSTRAINT "notifications_user_id_fkey" FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE public.order_items ADD CONSTRAINT "order_items_order_id_fkey" FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE;

ALTER TABLE public.order_items ADD CONSTRAINT "order_items_product_id_fkey" FOREIGN KEY (product_id) REFERENCES products(product_id);

ALTER TABLE public.orders ADD CONSTRAINT "orders_user_id_fkey" FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE public.password_reset_tokens ADD CONSTRAINT "password_reset_tokens_user_id_fkey" FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE public.product_images ADD CONSTRAINT "product_images_product_id_fkey" FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE;

ALTER TABLE public.product_variants ADD CONSTRAINT "product_variants_product_id_fkey" FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE;

ALTER TABLE public.push_subscriptions ADD CONSTRAINT "push_subscriptions_user_id_fkey" FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE public.refresh_token ADD CONSTRAINT "refresh_token_user_id_fkey" FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE public.reviews ADD CONSTRAINT "reviews_product_id_fkey" FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE;

ALTER TABLE public.reviews ADD CONSTRAINT "reviews_user_id_fkey" FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE public.role_permission ADD CONSTRAINT "role_permission_permission_id_fkey" FOREIGN KEY (permission_id) REFERENCES permission(permission_id) ON DELETE CASCADE;

ALTER TABLE public.role_permission ADD CONSTRAINT "role_permission_role_id_fkey" FOREIGN KEY (role_id) REFERENCES role(role_id) ON DELETE CASCADE;

ALTER TABLE public.user_addresses ADD CONSTRAINT "user_addresses_user_id_fkey" FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE public.user_role ADD CONSTRAINT "user_role_role_id_fkey" FOREIGN KEY (role_id) REFERENCES role(role_id) ON DELETE CASCADE;

ALTER TABLE public.user_role ADD CONSTRAINT "user_role_user_id_fkey" FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE public.wishlist_items ADD CONSTRAINT "wishlist_items_product_id_fkey" FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE;

ALTER TABLE public.wishlist_items ADD CONSTRAINT "wishlist_items_user_id_fkey" FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_orders_user ON public.orders USING btree (user_id);

CREATE INDEX IF NOT EXISTS idx_orders_status ON public.orders USING btree (order_status);

CREATE INDEX IF NOT EXISTS idx_products_category ON public.products USING btree (category);

CREATE INDEX IF NOT EXISTS idx_products_price ON public.products USING btree (price);

CREATE INDEX IF NOT EXISTS idx_notifications_user ON public.notifications USING btree (user_id);

CREATE INDEX IF NOT EXISTS idx_reviews_product ON public.reviews USING btree (product_id);

CREATE INDEX IF NOT EXISTS idx_inventory_product ON public.inventory_logs USING btree (product_id);

CREATE INDEX IF NOT EXISTS idx_product_variants_product ON public.product_variants USING btree (product_id);

CREATE INDEX IF NOT EXISTS idx_contact_messages_created_at ON public.contact_messages USING btree (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_contact_messages_status ON public.contact_messages USING btree (status);

CREATE INDEX IF NOT EXISTS idx_product_images_primary ON public.product_images USING btree (product_id) WHERE (is_primary = true);

CREATE INDEX IF NOT EXISTS idx_products_category_created ON public.products USING btree (category, created_at DESC) WHERE (is_active = true);

CREATE INDEX IF NOT EXISTS idx_wishlist_user_created ON public.wishlist_items USING btree (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_evt_user ON public.email_verification_tokens USING btree (user_id);

CREATE INDEX IF NOT EXISTS idx_products_fts ON public.products USING gin (to_tsvector('english'::regconfig, (((((((COALESCE(name, ''::character varying))::text || ' '::text) || COALESCE(description, ''::text)) || ' '::text) || (COALESCE(category, ''::character varying))::text) || ' '::text) || COALESCE(ingredients, ''::text))));

CREATE INDEX IF NOT EXISTS idx_products_name_lower_prefix ON public.products USING btree (lower((name)::text) text_pattern_ops);

CREATE INDEX IF NOT EXISTS idx_pt_order ON public.payment_transactions USING btree (order_id);

CREATE INDEX IF NOT EXISTS idx_pt_razorpay_order ON public.payment_transactions USING btree (razorpay_order_id);

CREATE INDEX IF NOT EXISTS idx_pt_user ON public.payment_transactions USING btree (user_id);

CREATE INDEX IF NOT EXISTS idx_user_addresses_user_id ON public.user_addresses USING btree (user_id);

CREATE INDEX IF NOT EXISTS idx_orders_tracking_number ON public.orders USING btree (tracking_number);

CREATE UNIQUE INDEX IF NOT EXISTS uq_cart_product_variant ON public.cart_items USING btree (cart_id, product_id, COALESCE(variant_id, '00000000-0000-0000-0000-000000000000'::uuid));

CREATE UNIQUE INDEX IF NOT EXISTS uq_order_product_variant ON public.order_items USING btree (order_id, product_id, COALESCE(variant_id, '00000000-0000-0000-0000-000000000000'::uuid));

CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_user_id ON public.password_reset_tokens USING btree (user_id);

CREATE INDEX IF NOT EXISTS idx_blog_posts_status_published ON public.blog_posts USING btree (status, published_at DESC);

CREATE INDEX IF NOT EXISTS idx_blog_posts_author ON public.blog_posts USING btree (author_id);

CREATE INDEX IF NOT EXISTS idx_blog_posts_fts ON public.blog_posts USING gin (to_tsvector('english'::regconfig, (((((COALESCE(title, ''::character varying))::text || ' '::text) || COALESCE(excerpt, ''::text)) || ' '::text) || COALESCE(content, ''::text))));

CREATE INDEX IF NOT EXISTS idx_live_menu_items_available ON public.live_menu_items USING btree (available, sort_order, name);

CREATE INDEX IF NOT EXISTS idx_instant_orders_status ON public.instant_orders USING btree (status, created_at);

CREATE INDEX IF NOT EXISTS idx_instant_orders_user_id ON public.instant_orders USING btree (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_instant_order_items_order_id ON public.instant_order_items USING btree (instant_order_id);

CREATE INDEX IF NOT EXISTS idx_instant_orders_token ON public.instant_orders USING btree (token) WHERE (token IS NOT NULL);

CREATE INDEX IF NOT EXISTS idx_fi_festival_id ON public.festival_image USING btree (festival_id);

CREATE INDEX IF NOT EXISTS idx_fi_active ON public.festival_image USING btree (is_active);

CREATE INDEX IF NOT EXISTS idx_fm_share_slug ON public.festival_message USING btree (share_slug);

CREATE INDEX IF NOT EXISTS idx_fm_festival_id ON public.festival_message USING btree (festival_id);

CREATE INDEX IF NOT EXISTS idx_fm_active ON public.festival_message USING btree (is_active);

CREATE INDEX IF NOT EXISTS idx_fm_image_id ON public.festival_message USING btree (image_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_festival_single_active ON public.festival_category USING btree ((true)) WHERE (is_active = true);

CREATE INDEX IF NOT EXISTS idx_loyalty_cards_user_id ON public.loyalty_cards USING btree (user_id);

CREATE INDEX IF NOT EXISTS idx_loyalty_checkins_card_id ON public.loyalty_checkins USING btree (card_id);

CREATE INDEX IF NOT EXISTS idx_loyalty_checkins_user_id ON public.loyalty_checkins USING btree (user_id);

CREATE INDEX IF NOT EXISTS idx_loyalty_checkins_status ON public.loyalty_checkins USING btree (status);

CREATE INDEX IF NOT EXISTS idx_loyalty_checkins_visit_day ON public.loyalty_checkins USING btree (visit_day);

CREATE UNIQUE INDEX IF NOT EXISTS uq_loyalty_one_visit_per_day ON public.loyalty_checkins USING btree (user_id, visit_day) WHERE ((status)::text = ANY ((ARRAY['PENDING'::character varying, 'APPROVED'::character varying])::text[]));

CREATE INDEX IF NOT EXISTS idx_loyalty_redemptions_user_id ON public.loyalty_redemptions USING btree (user_id);

CREATE INDEX IF NOT EXISTS idx_loyalty_redemptions_status ON public.loyalty_redemptions USING btree (status);

CREATE INDEX IF NOT EXISTS idx_loyalty_redemptions_order ON public.loyalty_redemptions USING btree (order_id);

CREATE INDEX IF NOT EXISTS idx_affiliate_links_affiliate ON public.affiliate_links USING btree (affiliate_id);

CREATE INDEX IF NOT EXISTS idx_affiliate_links_code ON public.affiliate_links USING btree (code);

CREATE INDEX IF NOT EXISTS idx_affiliate_attr_affiliate ON public.affiliate_attributions USING btree (affiliate_id);

CREATE INDEX IF NOT EXISTS idx_affiliate_attr_referred ON public.affiliate_attributions USING btree (referred_user_id);

CREATE INDEX IF NOT EXISTS idx_affiliate_conv_affiliate ON public.affiliate_conversions USING btree (affiliate_id);

CREATE INDEX IF NOT EXISTS idx_push_subs_user_id ON public.push_subscriptions USING btree (user_id);
