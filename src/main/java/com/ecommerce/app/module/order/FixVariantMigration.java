package com.ecommerce.app.module.order;

import com.ecommerce.app.config.DBConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * One-time migration: backfill variant_label on order_items where it's NULL
 * but a variant_id exists (or the product has only one variant).
 */
public class FixVariantMigration {
    private static final Logger LOG = LoggerFactory.getLogger(FixVariantMigration.class);

    public static void run() {
        try (Connection conn = DBConfig.getConnection()) {
            migrate(conn);
        } catch (SQLException e) {
            throw new IllegalStateException("Variant migration failed", e);
        }
    }

    // Package-private for testing without initializing the production connection pool.
    static void migrate(Connection conn) throws SQLException {
        conn.setAutoCommit(false);
        try {
            fixCartUniqueConstraint(conn);
            ensureTagTables(conn);
            backfillVariantLabels(conn);
            conn.commit();
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException rollbackFailure) {
                e.addSuppressed(rollbackFailure);
            }
            throw e;
        }
    }

    private static void ensureTagTables(Connection conn) throws SQLException {
        LOG.info("[MIGRATION] Ensuring tags + product_tags tables exist...");
        // Mirrors db_migrations.sql §10. Idempotent so repeated boots are
        // cheap and safe. Kept here (not in a dedicated migration runner)
        // because this file is already our startup bootstrapper.
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS tags ("
                    + "tag_id UUID PRIMARY KEY DEFAULT gen_random_uuid(), "
                    + "name VARCHAR(80) NOT NULL, "
                    + "slug VARCHAR(80) NOT NULL UNIQUE, "
                    + "description TEXT, "
                    + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            st.execute("CREATE TABLE IF NOT EXISTS product_tags ("
                    + "product_id UUID NOT NULL REFERENCES products(product_id) ON DELETE CASCADE, "
                    + "tag_id UUID NOT NULL REFERENCES tags(tag_id) ON DELETE CASCADE, "
                    + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "PRIMARY KEY (product_id, tag_id))");
            st.execute("CREATE INDEX IF NOT EXISTS idx_product_tags_tag_id ON product_tags(tag_id)");
            LOG.info("[MIGRATION] Tag tables ready");
        }
    }

    private static void fixCartUniqueConstraint(Connection conn) throws SQLException {
        LOG.info("[MIGRATION] Fixing cart unique constraint to allow multiple variants...");
        String dropOld = "ALTER TABLE cart_items DROP CONSTRAINT IF EXISTS uq_cart_product";
        String createNew = """
            CREATE UNIQUE INDEX IF NOT EXISTS uq_cart_product_variant
            ON cart_items (cart_id, product_id, COALESCE(variant_id, '00000000-0000-0000-0000-000000000000'::uuid))
            """;
        String dropOldOrder = "ALTER TABLE order_items DROP CONSTRAINT IF EXISTS uq_order_product";
        String createNewOrder = """
            CREATE UNIQUE INDEX IF NOT EXISTS uq_order_product_variant
            ON order_items (order_id, product_id, COALESCE(variant_id, '00000000-0000-0000-0000-000000000000'::uuid))
            """;
        try (Statement st = conn.createStatement()) {
            st.execute(dropOld);
            st.execute(createNew);
            st.execute(dropOldOrder);
            st.execute(createNewOrder);
            LOG.info("[MIGRATION] Cart + order_items constraints fixed: now allows multiple variants per product");
        }
    }

    private static void backfillVariantLabels(Connection conn) throws SQLException {
        LOG.info("[MIGRATION] Backfilling variant_label on order_items...");
        String sql = """
            UPDATE order_items oi
            SET variant_id = pv.variant_id,
                variant_label = pv.label
            FROM (
                SELECT pv2.variant_id, pv2.label, pv2.product_id, pv2.is_active,
                       COUNT(*) FILTER (WHERE pv2.is_active = true)
                           OVER (PARTITION BY pv2.product_id) AS active_count
                FROM product_variants pv2
            ) pv
            WHERE oi.variant_label IS NULL
              AND oi.product_id = pv.product_id
              AND (oi.variant_id = pv.variant_id
                   OR (oi.variant_id IS NULL AND pv.is_active = true AND pv.active_count = 1))
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int updated = ps.executeUpdate();
            LOG.info("[MIGRATION] Updated {} order_items with variant labels", updated);
        }
    }
}
