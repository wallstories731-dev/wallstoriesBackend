package com.ecommerce.app.module.review;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ecommerce.app.module.iam.repository.EmailVerificationRepository;
import com.ecommerce.app.module.product.image.StorageFactory;
import com.ecommerce.app.module.product.image.StorageService;

import jakarta.servlet.http.Part;

public class ReviewService {
    private static final Logger LOG = LoggerFactory.getLogger(ReviewService.class);

    private final ReviewRepository repo = new ReviewRepository();
    private final EmailVerificationRepository emailVerificationRepository = new EmailVerificationRepository();
    // Lazy-fetched: a dev running without R2/MinIO env vars can still submit
    // text-only reviews — the storage backend is only touched on upload.
    private final StorageService storageService = StorageFactory.get();

    public Review submit(UUID userId, UUID productId, int rating, String comment) {
        return submit(userId, productId, rating, comment, null, null);
    }

    /**
     * Full submit with optional wall photo + city. Photo is uploaded first;
     * if the DB write fails the already-uploaded object is cleaned up so we
     * don't leak storage. Everything else mirrors the text-only path.
     */
    public Review submit(UUID userId, UUID productId, int rating, String comment,
                         Part photoPart, String city) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("rating must be between 1 and 5");
        }
        if (productId == null) {
            throw new IllegalArgumentException("productId is required");
        }
        if (userId == null) {
            throw new IllegalArgumentException("unauthenticated");
        }
        if (!emailVerificationRepository.isUserVerified(userId)) {
            throw new IllegalArgumentException(
                    "please verify your email before posting a review");
        }
        if (city != null) {
            String trimmed = city.trim();
            city = trimmed.isEmpty() ? null : trimmed.substring(0, Math.min(100, trimmed.length()));
        }

        String photoUrl = null;
        String photoObjectKey = null;
        if (photoPart != null && photoPart.getSize() > 0) {
            validatePhotoPart(photoPart);
            String fileName = "review-" + UUID.randomUUID() + extensionFor(photoPart.getContentType());
            try (InputStream in = photoPart.getInputStream()) {
                // "reviews/" prefix keeps review artwork from colliding with
                // product images in the same bucket and makes it easy to
                // lifecycle-prune abandoned uploads out of band.
                photoObjectKey = storageService.upload("reviews/" + fileName, in,
                        photoPart.getSize(), photoPart.getContentType());
                photoUrl = storageService.getFileUrl(photoObjectKey);
            } catch (Exception e) {
                LOG.error("review photo upload failed", e);
                throw new IllegalArgumentException("photo upload failed, please try again");
            }
        }

        Review r = new Review();
        r.setUserId(userId);
        r.setProductId(productId);
        r.setRating(rating);
        r.setComment(comment);
        r.setPhotoUrl(photoUrl);
        r.setPhotoObjectKey(photoObjectKey);
        r.setCity(city);
        Review saved = repo.upsert(r);
        if (saved == null && photoObjectKey != null) {
            // DB write failed after the object landed: clean up so we
            // never leave orphan uploads in R2/MinIO.
            try { storageService.delete(photoObjectKey); }
            catch (Exception e) { LOG.warn("review photo cleanup failed for {}: {}", photoObjectKey, e.getMessage()); }
        }
        return saved;
    }

    private static final long MAX_REVIEW_PHOTO_BYTES = 5L * 1024L * 1024L;

    private static void validatePhotoPart(Part part) {
        if (part.getSize() > MAX_REVIEW_PHOTO_BYTES) {
            throw new IllegalArgumentException("photo must be 5 MB or smaller");
        }
        String ct = part.getContentType();
        if (ct == null
                || !(ct.equalsIgnoreCase("image/jpeg")
                  || ct.equalsIgnoreCase("image/png")
                  || ct.equalsIgnoreCase("image/webp"))) {
            throw new IllegalArgumentException("photo must be a JPEG, PNG or WebP image");
        }
    }

    private static String extensionFor(String contentType) {
        if (contentType == null) return "";
        switch (contentType.toLowerCase()) {
            case "image/jpeg": return ".jpg";
            case "image/png":  return ".png";
            case "image/webp": return ".webp";
            default:           return "";
        }
    }

    public List<Review> getForProduct(UUID productId, int limit, int offset) {
        return repo.findByProductId(productId, limit, offset);
    }

    /** Recent high-rated reviews across all products, for the home page. */
    public List<Review> getFeatured(int limit) {
        return repo.findFeatured(limit);
    }

    public ReviewSummary getSummary(UUID productId) {
        return repo.summaryForProduct(productId);
    }

    /** Photo-review gallery for the home / #photo-reviews storefront section. */
    public List<Review> getPhotoReviews(int limit) {
        return repo.findWithPhoto(limit);
    }

    public boolean deleteOwn(UUID reviewId, UUID userId) {
        Review existing = repo.findById(reviewId);
        if (existing == null || existing.getUserId() == null
                || !existing.getUserId().equals(userId)) {
            return false;
        }
        boolean ok = repo.delete(reviewId, userId);
        if (ok) deletePhotoAsset(existing);
        return ok;
    }

    public List<Review> getAll(int limit, int offset) {
        return repo.findAll(limit, offset);
    }

    public boolean deleteAny(UUID reviewId) {
        Review existing = repo.findById(reviewId);
        boolean ok = repo.deleteAny(reviewId);
        if (ok) deletePhotoAsset(existing);
        return ok;
    }

    public Review getById(UUID reviewId) {
        return repo.findById(reviewId);
    }

    /**
     * Best-effort cleanup of the storage object behind a review's photo.
     * Storage failures are logged but never block the DB delete — the
     * admin's intent is to remove the review; a stale object is recoverable.
     */
    private void deletePhotoAsset(Review review) {
        if (review == null || review.getPhotoObjectKey() == null
                || review.getPhotoObjectKey().isBlank()) {
            return;
        }
        try { storageService.delete(review.getPhotoObjectKey()); }
        catch (Exception e) {
            LOG.warn("review photo delete failed for {}: {}",
                    review.getPhotoObjectKey(), e.getMessage());
        }
    }
}
