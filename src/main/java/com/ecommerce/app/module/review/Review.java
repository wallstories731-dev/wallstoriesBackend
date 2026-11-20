package com.ecommerce.app.module.review;

import java.sql.Timestamp;
import java.util.UUID;

public class Review {
    private UUID reviewId;
    private UUID userId;
    private UUID productId;
    private int rating;
    private String comment;
    private Timestamp createdAt;
    // Display-only, joined from users table when present.
    private String userEmail;
    // Display-only, joined from products table when present (featured feed).
    private String productName;
    // Optional wall-photo attached by the customer at submission time.
    // photoUrl is the public URL served to the frontend; photoObjectKey is
    // the backing storage key (R2/MinIO) used when the admin deletes the
    // review and we need to also drop the file.
    private String photoUrl;
    private String photoObjectKey;
    // Free-text city so testimonials can be credited without a users lookup.
    private String city;

    public UUID getReviewId() { return reviewId; }
    public void setReviewId(UUID reviewId) { this.reviewId = reviewId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getPhotoObjectKey() { return photoObjectKey; }
    public void setPhotoObjectKey(String photoObjectKey) { this.photoObjectKey = photoObjectKey; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
}
