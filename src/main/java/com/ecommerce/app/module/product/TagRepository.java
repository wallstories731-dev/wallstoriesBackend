package com.ecommerce.app.module.product;

import com.ecommerce.app.config.DBConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * JDBC repository for {@link Tag} and the {@code product_tags} join table.
 *
 * <p>Schema (created by {@code db_migrations.sql} §10):</p>
 * <pre>
 * tags(tag_id uuid PK, name varchar(80), slug varchar(80) UNIQUE,
 *      description text, created_at timestamp)
 * product_tags(product_id uuid, tag_id uuid, created_at timestamp,
 *              PK(product_id, tag_id))
 * </pre>
 *
 * <p>The slug is the business key that external callers quote
 * ({@code #ironman}), so every lookup here routes through {@link Tag#toSlug}
 * for consistent normalisation.</p>
 */
public class TagRepository {
    private static final Logger LOG = LoggerFactory.getLogger(TagRepository.class);

    // ------------------------------------------------------------------
    // Tag CRUD
    // ------------------------------------------------------------------

    /**
     * Upsert a tag by its normalised slug. Returns the persisted row,
     * incrementing nothing on the product_tags side (that's the caller's job).
     * The ON CONFLICT clause makes this safe to call from multiple admin
     * sessions racing to create the same tag.
     */
    public Tag upsertBySlug(String name, String description) {
        String slug = Tag.toSlug(name);
        if (slug == null) return null;
        String cleanName = Tag.normalizeName(name);
        if (cleanName == null) cleanName = slug;

        String sql = """
                INSERT INTO tags (tag_id, name, slug, description)
                VALUES (gen_random_uuid(), ?, ?, ?)
                ON CONFLICT (slug) DO UPDATE
                    SET name = COALESCE(EXCLUDED.name, tags.name),
                        description = COALESCE(EXCLUDED.description, tags.description)
                RETURNING tag_id, name, slug, description, created_at
                """;

        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, cleanName);
            ps.setString(2, slug);
            if (description == null || description.isBlank()) {
                ps.setNull(3, java.sql.Types.VARCHAR);
            } else {
                ps.setString(3, description.trim());
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            LOG.error("sql exception at upsertBySlug tag ", e);
        }
        return null;
    }

    public Tag findBySlug(String slug) {
        String normalised = Tag.toSlug(slug);
        if (normalised == null) return null;
        String sql = "SELECT tag_id, name, slug, description, created_at FROM tags WHERE slug = ?";
        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, normalised);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            LOG.error("sql exception at findBySlug tag ", e);
        }
        return null;
    }

    public Tag findById(UUID tagId) {
        if (tagId == null) return null;
        String sql = "SELECT tag_id, name, slug, description, created_at FROM tags WHERE tag_id = ?";
        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, tagId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            LOG.error("sql exception at findById tag ", e);
        }
        return null;
    }

    /**
     * Rename / re-describe an existing tag. The slug is intentionally not
     * mutable here — changing the slug would break any external URL that
     * references it. To "rename" the slug, callers must delete + recreate.
     */
    public Tag update(UUID tagId, String name, String description) {
        if (tagId == null) return null;
        String cleanName = Tag.normalizeName(name);
        if (cleanName == null) return null;
        String sql = """
                UPDATE tags SET name = ?, description = ? WHERE tag_id = ?
                RETURNING tag_id, name, slug, description, created_at
                """;
        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, cleanName);
            if (description == null || description.isBlank()) {
                ps.setNull(2, java.sql.Types.VARCHAR);
            } else {
                ps.setString(2, description.trim());
            }
            ps.setObject(3, tagId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            LOG.error("sql exception at update tag ", e);
        }
        return null;
    }

    public boolean delete(UUID tagId) {
        if (tagId == null) return false;
        // product_tags has ON DELETE CASCADE so the join rows go with it.
        String sql = "DELETE FROM tags WHERE tag_id = ?";
        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, tagId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            LOG.error("sql exception at delete tag ", e);
        }
        return false;
    }

    /**
     * Full catalogue tag list with usage counts, ordered by most-used
     * first then alphabetical. Only counts active products so the public
     * filter sidebar doesn't advertise tags whose products are all drafts.
     */
    public List<Tag> listWithCounts() {
        String sql = """
                SELECT t.tag_id, t.name, t.slug, t.description, t.created_at,
                       COUNT(pt.product_id) FILTER (WHERE p.is_active = true) AS product_count
                FROM tags t
                LEFT JOIN product_tags pt ON pt.tag_id = t.tag_id
                LEFT JOIN products p ON p.product_id = pt.product_id
                GROUP BY t.tag_id
                ORDER BY product_count DESC, t.name ASC
                """;
        List<Tag> out = new ArrayList<>();
        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Tag t = mapRow(rs);
                t.setProductCount(rs.getInt("product_count"));
                out.add(t);
            }
        } catch (SQLException e) {
            LOG.error("sql exception at listWithCounts tag ", e);
        }
        return out;
    }

    // ------------------------------------------------------------------
    // product_tags join table
    // ------------------------------------------------------------------

    /** Tags attached to one product, in insertion order. */
    public List<Tag> findByProductId(UUID productId) {
        if (productId == null) return List.of();
        String sql = """
                SELECT t.tag_id, t.name, t.slug, t.description, t.created_at
                FROM product_tags pt
                JOIN tags t ON t.tag_id = pt.tag_id
                WHERE pt.product_id = ?
                ORDER BY pt.created_at, t.name
                """;
        List<Tag> out = new ArrayList<>();
        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LOG.error("sql exception at findByProductId tag ", e);
        }
        return out;
    }

    /**
     * Batch lookup: {productId -> List<Tag>} for a set of products. Used
     * to hydrate list-view endpoints without the per-row N+1.
     */
    public Map<UUID, List<Tag>> findByProductIds(Collection<UUID> productIds) {
        Map<UUID, List<Tag>> out = new HashMap<>();
        if (productIds == null || productIds.isEmpty()) return out;
        String sql = """
                SELECT pt.product_id, t.tag_id, t.name, t.slug, t.description, t.created_at
                FROM product_tags pt
                JOIN tags t ON t.tag_id = pt.tag_id
                WHERE pt.product_id = ANY(?)
                ORDER BY pt.created_at, t.name
                """;
        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setArray(1, c.createArrayOf("uuid", productIds.toArray()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID pid = rs.getObject("product_id", java.util.UUID.class);
                    Tag t = mapRow(rs);
                    out.computeIfAbsent(pid, k -> new ArrayList<>()).add(t);
                }
            }
        } catch (SQLException e) {
            LOG.error("sql exception at findByProductIds tag ", e);
        }
        return out;
    }

    /**
     * Replace the full tag set on a product in a single transaction.
     * {@code tagNames} is a free-form list of user strings — each is
     * normalised via {@link Tag#toSlug}, upserted into {@code tags}, and
     * linked via {@code product_tags}. Rows absent from {@code tagNames}
     * are detached. Duplicates in the input are collapsed by slug.
     *
     * @return the final tags attached to the product (persisted).
     */
    public List<Tag> replaceProductTags(UUID productId, List<String> tagNames) {
        if (productId == null) return List.of();
        // De-dupe by slug, preserve insertion order.
        Map<String, String> desired = new LinkedHashMap<>();
        if (tagNames != null) {
            for (String raw : tagNames) {
                String slug = Tag.toSlug(raw);
                if (slug == null) continue;
                String name = Tag.normalizeName(raw);
                desired.putIfAbsent(slug, name != null ? name : slug);
            }
        }

        try (Connection c = DBConfig.getConnection()) {
            boolean prev = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                // Resolve / create each desired tag.
                List<UUID> desiredIds = new ArrayList<>(desired.size());
                for (Map.Entry<String, String> e : desired.entrySet()) {
                    UUID tagId = upsertBySlugTx(c, e.getValue(), e.getKey());
                    if (tagId != null) desiredIds.add(tagId);
                }

                // Nuke existing links not in the desired set, then insert
                // the missing ones. Doing both inside the transaction so
                // readers never see a product with no tags mid-swap.
                try (PreparedStatement del = c.prepareStatement(
                        "DELETE FROM product_tags WHERE product_id = ? AND NOT (tag_id = ANY(?))")) {
                    del.setObject(1, productId);
                    del.setArray(2, c.createArrayOf("uuid", desiredIds.toArray()));
                    del.executeUpdate();
                }
                if (desiredIds.isEmpty()) {
                    // No tags at all: still clear any that existed (handled above).
                } else {
                    try (PreparedStatement ins = c.prepareStatement(
                            "INSERT INTO product_tags (product_id, tag_id) VALUES (?, ?) "
                            + "ON CONFLICT (product_id, tag_id) DO NOTHING")) {
                        for (UUID tagId : desiredIds) {
                            ins.setObject(1, productId);
                            ins.setObject(2, tagId);
                            ins.addBatch();
                        }
                        ins.executeBatch();
                    }
                }
                c.commit();
            } catch (Exception ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(prev);
            }
        } catch (Exception e) {
            LOG.error("exception at replaceProductTags ", e);
            return List.of();
        }
        return findByProductId(productId);
    }

    /** Transactional variant of upsertBySlug used by {@link #replaceProductTags}. */
    private UUID upsertBySlugTx(Connection c, String name, String slug) throws SQLException {
        String sql = """
                INSERT INTO tags (tag_id, name, slug)
                VALUES (gen_random_uuid(), ?, ?)
                ON CONFLICT (slug) DO UPDATE SET name = COALESCE(EXCLUDED.name, tags.name)
                RETURNING tag_id
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, slug);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getObject(1, java.util.UUID.class);
            }
        }
        return null;
    }

    /**
     * Attach a single tag to a product. Idempotent — safe to call for a
     * tag already linked. Returns true when a NEW link was inserted.
     */
    public boolean attach(UUID productId, UUID tagId) {
        if (productId == null || tagId == null) return false;
        String sql = "INSERT INTO product_tags (product_id, tag_id) VALUES (?, ?) "
                + "ON CONFLICT (product_id, tag_id) DO NOTHING";
        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, productId);
            ps.setObject(2, tagId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            LOG.error("sql exception at attach tag ", e);
        }
        return false;
    }

    public boolean detach(UUID productId, UUID tagId) {
        if (productId == null || tagId == null) return false;
        String sql = "DELETE FROM product_tags WHERE product_id = ? AND tag_id = ?";
        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, productId);
            ps.setObject(2, tagId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            LOG.error("sql exception at detach tag ", e);
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Product lookup by tag (powers the storefront filter sidebar)
    // ------------------------------------------------------------------

    /**
     * Find product ids that carry ANY of the given tag slugs. Used by
     * {@link ProductService#searchProducts} when the request includes a
     * {@code ?tag=} filter. Returns an empty list when no slug matches.
     *
     * <p>We return just the ids here so the caller can compose them with
     * other filters (category, price, inStock) using the existing search
     * machinery instead of duplicating that SQL.</p>
     */
    public List<UUID> findActiveProductIdsByTagSlugs(Collection<String> slugs) {
        List<UUID> out = new ArrayList<>();
        if (slugs == null || slugs.isEmpty()) return out;
        List<String> cleaned = new ArrayList<>();
        for (String raw : slugs) {
            String s = Tag.toSlug(raw);
            if (s != null) cleaned.add(s);
        }
        if (cleaned.isEmpty()) return out;

        String sql = """
                SELECT DISTINCT pt.product_id
                FROM product_tags pt
                JOIN tags t ON t.tag_id = pt.tag_id
                JOIN products p ON p.product_id = pt.product_id
                WHERE t.slug = ANY(?) AND p.is_active = true
                """;
        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setArray(1, c.createArrayOf("varchar", cleaned.toArray()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(rs.getObject(1, java.util.UUID.class));
            }
        } catch (SQLException e) {
            LOG.error("sql exception at findActiveProductIdsByTagSlugs ", e);
        }
        return out;
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private static Tag mapRow(ResultSet rs) throws SQLException {
        Tag t = new Tag();
        t.setTagId(rs.getObject("tag_id", java.util.UUID.class));
        t.setName(rs.getString("name"));
        t.setSlug(rs.getString("slug"));
        t.setDescription(rs.getString("description"));
        t.setCreatedAt(rs.getTimestamp("created_at"));
        return t;
    }
}
