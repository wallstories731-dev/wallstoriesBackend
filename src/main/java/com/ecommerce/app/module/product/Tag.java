package com.ecommerce.app.module.product;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * First-class catalog tag — e.g. "ironman", "minimal", "cinematic".
 *
 * <p>Tags are a parallel taxonomy to {@link Product#getCategory()}: a
 * product has exactly one category (e.g. "Movies"), but may carry many
 * tags (e.g. {@code #ironman}, {@code #marvel}). Tags are managed via
 * {@link TagController} ({@code /api/tag}) and attached to products via
 * the {@code product_tags} join table.</p>
 *
 * <p>The {@code slug} is a URL-safe lowercase form of {@code name} and
 * is unique per row — the business key callers use to look a tag up
 * without needing its UUID. Example: {@code name = "Iron Man"} ->
 * {@code slug = "iron-man"}.</p>
 */
public class Tag {

    private UUID tagId;
    private String name;
    private String slug;
    private String description;
    private Timestamp createdAt;

    /** Usage count; only populated by list endpoints that join product_tags. */
    private Integer productCount;

    public UUID getTagId() { return tagId; }
    public void setTagId(UUID tagId) { this.tagId = tagId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Integer getProductCount() { return productCount; }
    public void setProductCount(Integer productCount) { this.productCount = productCount; }

    /**
     * Canonicalise a free-text tag into the slug used as the business key.
     * - trims surrounding whitespace
     * - drops a leading {@code #} if present (users often type "#ironman")
     * - lowercases
     * - collapses any run of non-alphanumeric characters to a single hyphen
     * - strips leading/trailing hyphens
     *
     * <p>Returns {@code null} when the input is null/blank or has no
     * usable characters (e.g. {@code "###"}).</p>
     */
    public static String toSlug(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.startsWith("#")) s = s.substring(1).trim();
        if (s.isEmpty()) return null;
        s = s.toLowerCase(java.util.Locale.ROOT);
        s = s.replaceAll("[^a-z0-9]+", "-");
        s = s.replaceAll("^-+|-+$", "");
        if (s.isEmpty()) return null;
        if (s.length() > 80) s = s.substring(0, 80);
        return s;
    }

    /**
     * Normalise the human-readable name: trim, strip a leading {@code #},
     * and cap at 80 characters to match the DB column. Returns {@code null}
     * for blank input.
     */
    public static String normalizeName(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.startsWith("#")) s = s.substring(1).trim();
        if (s.isEmpty()) return null;
        if (s.length() > 80) s = s.substring(0, 80);
        return s;
    }
}
