package com.ecommerce.app.module.product;

import java.sql.Timestamp;
import java.util.*;

import com.ecommerce.app.module.review.Review;
import com.fasterxml.jackson.annotation.JsonAlias;

public class Product{
   @JsonAlias("productId")
   private UUID id;
    private String name;
    private String description;
    private String category;
    private String ingredients;
    // Optional Tamil-script copy of the corresponding English fields.
    // We keep the English field as the primary value used by search +
    // schema.org JSON-LD (so Google/AI engines can summarise the page),
    // and render the Tamil string as a small subtitle / secondary block
    // for trust + cultural authenticity. Tamil fields are nullable on
    // the DB to keep existing rows working with no migration of data.
    private String nameTamil;
    private String descriptionTamil;
    private String ingredientsTamil;
    private double price;
    private int stockQuantity;
    private boolean isActive;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private List<ProductImage> images;
    // URL of the is_primary=true row in product_images. Hydrated everywhere
    // we return product info (list, detail, search, recommendations, admin)
    // so the frontend can render a thumbnail without a second round-trip
    // or having to walk the full `images` list.
    private String primaryImageUrl;
    // Review fields - populated only when fetching a single product
    // (averageRating + reviewCount on the list view would mean an N+1 query).
    private Double averageRating;
    private Integer reviewCount;
    private List<Review> reviews;
    // Purchasable variants (e.g. "250g", "500g", "5 pcs"). Hydrated only
    // by the detail endpoint - the list view shows the base price/stock
    // and lets the user pick a size on the detail page. Empty list when
    // the product ships in a single size; the frontend then falls back
    // to product.price + product.stockQuantity.
    private List<ProductVariant> variants;
    // Free-form JSON (string) for storefront metadata the schema has no column for
    // (catalogue code, badge, tags, featured flag, MRPs...). Opaque to the backend.
    private String attributes;
    // First-class tags attached via the product_tags join table. Populated
    // on detail + list-view reads so clients can render chips without a
    // separate /api/tag?productId= round-trip. Writes to this field via
    // the product endpoints are IGNORED — tags are managed exclusively
    // through /api/tag (set replace / attach / detach) so a stray product
    // PUT cannot accidentally wipe them.
    private List<Tag> productTags;
    public UUID getId() {
        return id;
    }
    public void setId(UUID id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getCategory() {
        return category;
    }
    public void setCategory(String category) {
        this.category = category;
    }
    public String getIngredients() {
        return ingredients;
    }
    public void setIngredients(String ingredients) {
        this.ingredients = ingredients;
    }
    public String getNameTamil() { return nameTamil; }
    public void setNameTamil(String nameTamil) { this.nameTamil = nameTamil; }
    public String getDescriptionTamil() { return descriptionTamil; }
    public void setDescriptionTamil(String descriptionTamil) { this.descriptionTamil = descriptionTamil; }
    public String getIngredientsTamil() { return ingredientsTamil; }
    public void setIngredientsTamil(String ingredientsTamil) { this.ingredientsTamil = ingredientsTamil; }
    public double getPrice() {
        return price;
    }
    public void setPrice(double price) {
        this.price = price;
    }
    public int getStockQuantity() {
        return stockQuantity;
    }
    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }
    public boolean isActive() {
        return isActive;
    }
    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }
    public Timestamp getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
    public Timestamp getUpdatedAt() {
        return updatedAt;
    }
    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
    public List<ProductImage> getImages() {
        return images;
    }
    public void setImages(List<ProductImage> images) {
        this.images = images;
    }

    public String getPrimaryImageUrl() { return primaryImageUrl; }
    public void setPrimaryImageUrl(String primaryImageUrl) { this.primaryImageUrl = primaryImageUrl; }

    public Double getAverageRating() { return averageRating; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }

    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }

    public List<Review> getReviews() { return reviews; }
    public void setReviews(List<Review> reviews) { this.reviews = reviews; }

    public String getAttributes() { return attributes; }

    /**
     * Tag slugs attached to this product, exposed as a convenience read-only
     * field. Prefers the first-class {@code product_tags} join table (hydrated
     * by {@link ProductService}); falls back to the legacy JSON-packed list in
     * {@code attributes.tags} for records written before the join table existed.
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "tags", access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    public List<String> getTags() {
        List<String> out = new ArrayList<>();
        if (productTags != null && !productTags.isEmpty()) {
            for (Tag t : productTags) {
                String slug = t.getSlug();
                if (slug != null && !slug.isBlank()) out.add(slug);
            }
            if (!out.isEmpty()) return out;
        }
        if (attributes == null || attributes.isBlank()) return out;
        try {
            com.fasterxml.jackson.databind.JsonNode t = new com.fasterxml.jackson.databind.ObjectMapper().readTree(attributes).get("tags");
            if (t != null && t.isArray()) t.forEach(n -> { if (n.isTextual() && !n.asText().isBlank()) out.add(n.asText()); });
        } catch (Exception ignored) { }
        return out;
    }
    public void setAttributes(String attributes) { this.attributes = attributes; }

    /**
     * Full tag objects (id, name, slug, description). Serialised on reads;
     * ignored on writes so product PUTs cannot clobber the tag set — manage
     * tags through {@code /api/tag} instead.
     */
    @com.fasterxml.jackson.annotation.JsonProperty(value = "productTags", access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    public List<Tag> getProductTags() { return productTags; }
    public void setProductTags(List<Tag> productTags) { this.productTags = productTags; }

    public List<ProductVariant> getVariants() { return variants; }
    public void setVariants(List<ProductVariant> variants) { this.variants = variants; }

}