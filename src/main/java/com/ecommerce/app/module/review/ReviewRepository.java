package com.ecommerce.app.module.review;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ecommerce.app.config.DBConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ReviewRepository {
    private static final Logger LOG = LoggerFactory.getLogger(ReviewRepository.class);


    /**
     * Insert a new review, or update the existing one when the same user
     * reviews the same product (uq_user_product_review). Returns the
     * persisted row.
     */
    public Review upsert(Review review) {
        // ON CONFLICT updates everything so a resubmission can also swap the
        // photo / city. photo_* columns default to NULL on first write.
        String sql = """
                INSERT INTO reviews (review_id, user_id, product_id, rating, comment,
                                     photo_url, photo_object_key, city)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (user_id, product_id)
                DO UPDATE SET rating           = EXCLUDED.rating,
                              comment          = EXCLUDED.comment,
                              photo_url        = COALESCE(EXCLUDED.photo_url, reviews.photo_url),
                              photo_object_key = COALESCE(EXCLUDED.photo_object_key, reviews.photo_object_key),
                              city             = COALESCE(EXCLUDED.city, reviews.city),
                              created_at       = CURRENT_TIMESTAMP
                RETURNING review_id, user_id, product_id, rating, comment, created_at,
                          photo_url, photo_object_key, city
                """;

        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            UUID id = review.getReviewId() != null ? review.getReviewId() : UUID.randomUUID();
            ps.setObject(1, id);
            ps.setObject(2, review.getUserId());
            ps.setObject(3, review.getProductId());
            ps.setInt(4, review.getRating());
            ps.setString(5, review.getComment());
            ps.setString(6, review.getPhotoUrl());
            ps.setString(7, review.getPhotoObjectKey());
            ps.setString(8, review.getCity());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            LOG.error("sql exception at review upsert ", e);
        }
        return null;
    }

    /** Reviews for a product, newest first, with reviewer email joined. */
    public List<Review> findByProductId(UUID productId, int limit, int offset) {
        String sql = """
                SELECT r.review_id, r.user_id, r.product_id, r.rating, r.comment, r.created_at,
                       r.photo_url, r.photo_object_key, r.city,
                       u.email AS user_email
                FROM reviews r
                LEFT JOIN users u ON u.user_id = r.user_id
                WHERE r.product_id = ?
                ORDER BY r.created_at DESC
                LIMIT ? OFFSET ?
                """;
        List<Review> out = new ArrayList<>();
        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, productId);
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Review r = mapRow(rs);
                    r.setUserEmail(rs.getString("user_email"));
                    out.add(r);
                }
            }
        } catch (SQLException e) {
            LOG.error("sql exception at findByProductId reviews ", e);
        }
        return out;
    }

    public Review findById(UUID reviewId) {
        String sql = "SELECT review_id, user_id, product_id, rating, comment, created_at, "
                   + "photo_url, photo_object_key, city FROM reviews WHERE review_id = ?";
        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, reviewId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            LOG.error("sql exception at findById review ", e);
        }
        return null;
    }

    public boolean delete(UUID reviewId, UUID userId) {
        String sql = "DELETE FROM reviews WHERE review_id = ? AND user_id = ?";
        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, reviewId);
            ps.setObject(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.error("sql exception at delete review ", e);
        }
        return false;
    }

    public ReviewSummary summaryForProduct(UUID productId) {
        String sql = "SELECT COALESCE(AVG(rating),0) AS avg_rating, COUNT(*) AS cnt FROM reviews WHERE product_id = ?";
        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double avg = rs.getDouble("avg_rating");
                    int cnt = rs.getInt("cnt");
                    // round avg to 2 decimals
                    avg = Math.round(avg * 100.0) / 100.0;
                    return new ReviewSummary(avg, cnt);
                }
            }
        } catch (SQLException e) {
            LOG.error("sql exception at summaryForProduct ", e);
        }
        return new ReviewSummary(0.0, 0);
    }

    /**
     * Batch version of {@link #summaryForProduct(UUID)}: takes a list of
     * product ids and returns a single map of {@code productId -> summary}.
     *
     * <p>Used by catalog / search hydration to avoid an N+1 query loop.
     * For 10 products on Neon serverless this saves ~9 extra round-trips
     * (≈2.5s on a cold connection).</p>
     *
     * <p>Products with no rows in {@code reviews} are absent from the
     * returned map; callers should treat that as "0 reviews, avg 0".</p>
     */
    public java.util.Map<UUID, ReviewSummary> summariesForProducts(java.util.Collection<UUID> productIds) {
        java.util.Map<UUID, ReviewSummary> out = new java.util.HashMap<>();
        if (productIds == null || productIds.isEmpty()) return out;

        // Build "?, ?, ?" placeholder list. UUIDs are bound via setObject,
        // so this is safe from SQL injection.
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < productIds.size(); i++) {
            if (i > 0) placeholders.append(',');
            placeholders.append('?');
        }
        String sql = "SELECT product_id, COALESCE(AVG(rating),0) AS avg_rating, COUNT(*) AS cnt "
                + "FROM reviews WHERE product_id IN (" + placeholders + ") "
                + "GROUP BY product_id";

        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            int idx = 1;
            for (UUID id : productIds) {
                ps.setObject(idx++, id);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID pid = (UUID) rs.getObject("product_id");
                    double avg = rs.getDouble("avg_rating");
                    int cnt = rs.getInt("cnt");
                    avg = Math.round(avg * 100.0) / 100.0;
                    out.put(pid, new ReviewSummary(avg, cnt));
                }
            }
        } catch (SQLException e) {
            LOG.error("sql exception at summariesForProducts ", e);
        }
        return out;
    }

    private Review mapRow(ResultSet rs) throws SQLException {
        Review r = new Review();
        r.setReviewId((UUID) rs.getObject("review_id"));
        r.setUserId((UUID) rs.getObject("user_id"));
        r.setProductId((UUID) rs.getObject("product_id"));
        r.setRating(rs.getInt("rating"));
        r.setComment(rs.getString("comment"));
        r.setCreatedAt(rs.getTimestamp("created_at"));
        // photo_* and city are nullable; mapRow tolerates callers that
        // didn't SELECT them by swallowing SQLException("column not found").
        try { r.setPhotoUrl(rs.getString("photo_url")); }                 catch (SQLException ignored) {}
        try { r.setPhotoObjectKey(rs.getString("photo_object_key")); }    catch (SQLException ignored) {}
        try { r.setCity(rs.getString("city")); }                          catch (SQLException ignored) {}
        return r;
    }

    /**
     * Featured reviews for the home page testimonial carousel.
     *
     * Filters:
     *   - rating &gt;= 4 (only positive testimonials, by design)
     *   - non-empty comment (silent 5-star ratings make poor testimonials)
     *   - newest first
     *
     * Joins the users table for display name fallback and the products
     * table so the UI can deep-link the testimonial to the SKU.
     */
    public List<Review> findFeatured(int limit) {
        if (limit <= 0 || limit > 50) limit = 12;
        String sql = """
                SELECT r.review_id, r.user_id, r.product_id, r.rating, r.comment, r.created_at,
                       r.photo_url, r.photo_object_key, r.city,
                       u.email AS user_email,
                       p.name  AS product_name
                FROM reviews r
                LEFT JOIN users    u ON u.user_id    = r.user_id
                LEFT JOIN products p ON p.product_id = r.product_id
                WHERE r.rating >= 4
                  AND r.comment IS NOT NULL
                  AND length(trim(r.comment)) > 0
                ORDER BY r.created_at DESC
                LIMIT ?
                """;
        List<Review> out = new ArrayList<>();
        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Review r = mapRow(rs);
                    r.setUserEmail(rs.getString("user_email"));
                    r.setProductName(rs.getString("product_name"));
                    out.add(r);
                }
            }
        } catch (SQLException e) {
            LOG.error("sql exception at findFeatured reviews ", e);
        }
        return out;
    }

    /** Admin moderation feed: every review, newest first. */
    public List<Review> findAll(int limit, int offset) {
        String sql = """
                SELECT r.review_id, r.user_id, r.product_id, r.rating, r.comment, r.created_at,
                       r.photo_url, r.photo_object_key, r.city,
                       u.email AS user_email,
                       p.name  AS product_name
                FROM reviews r
                LEFT JOIN users    u ON u.user_id    = r.user_id
                LEFT JOIN products p ON p.product_id = r.product_id
                ORDER BY r.created_at DESC
                LIMIT ? OFFSET ?
                """;
        List<Review> out = new ArrayList<>();
        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Review r = mapRow(rs);
                    r.setUserEmail(rs.getString("user_email"));
                    r.setProductName(rs.getString("product_name"));
                    out.add(r);
                }
            }
        } catch (SQLException e) {
            LOG.error("sql exception at findAll reviews ", e);
        }
        return out;
    }

    public boolean deleteAny(UUID reviewId) {
        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM reviews WHERE review_id = ?")) {
            ps.setObject(1, reviewId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOG.error("sql exception at deleteAny review ", e);
        }
        return false;
    }

    /**
     * Reviews that carry a wall photo — powers the storefront
     * #photo-reviews gallery. Highest-rated, newest first so the strip
     * always leads with a 5★ testimonial.
     */
    public List<Review> findWithPhoto(int limit) {
        if (limit <= 0 || limit > 60) limit = 24;
        String sql = """
                SELECT r.review_id, r.user_id, r.product_id, r.rating, r.comment, r.created_at,
                       r.photo_url, r.photo_object_key, r.city,
                       u.email AS user_email,
                       p.name  AS product_name
                FROM reviews r
                LEFT JOIN users    u ON u.user_id    = r.user_id
                LEFT JOIN products p ON p.product_id = r.product_id
                WHERE r.photo_url IS NOT NULL
                  AND length(trim(r.photo_url)) > 0
                ORDER BY r.rating DESC, r.created_at DESC
                LIMIT ?
                """;
        List<Review> out = new ArrayList<>();
        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Review r = mapRow(rs);
                    r.setUserEmail(rs.getString("user_email"));
                    r.setProductName(rs.getString("product_name"));
                    out.add(r);
                }
            }
        } catch (SQLException e) {
            LOG.error("sql exception at findWithPhoto reviews ", e);
        }
        return out;
    }
}
