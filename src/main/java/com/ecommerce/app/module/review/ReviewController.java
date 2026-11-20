package com.ecommerce.app.module.review;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.ecommerce.app.module.iam.models.ApiResponse;
import com.ecommerce.app.module.iam.security.AuthContext;
import com.ecommerce.app.module.iam.security.AuthUser;
import com.ecommerce.app.module.iam.util.SendResponseUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

/**
 * Product reviews.
 *
 *   GET    /api/review?productId=<uuid>           -> list reviews + summary (public)
 *   GET    /api/review?featured=true&limit=       -> featured testimonials (public)
 *   GET    /api/review?photos=true&limit=         -> photo-review gallery (public)
 *   POST   /api/review                            -> create/update own review (auth)
 *                                                    JSON body: { productId, rating (1-5), comment }
 *                                                    multipart/form-data: adds parts
 *                                                        productId / rating / comment / city
 *                                                        photo (JPEG/PNG/WebP, 5 MB max)
 *   DELETE /api/review?reviewId=<uuid>            -> delete own review (auth)
 *
 * Multipart is detected by Content-Type so existing JSON callers keep working
 * unchanged; the photo-upload UI simply sends FormData instead.
 */
@WebServlet("/api/review")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = 5L * 1024L * 1024L,
        maxRequestSize = 10L * 1024L * 1024L
)
public class ReviewController extends HttpServlet {
    private static final Logger LOG = LoggerFactory.getLogger(ReviewController.class);


    private final ReviewService service = new ReviewService();
    private static final ObjectMapper mapper = new ObjectMapper();

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Featured testimonials feed for the home page.
        // GET /api/review?featured=true&limit=12   -> public, no productId required.
        boolean featured = "true".equalsIgnoreCase(request.getParameter("featured"));
        if (featured) {
            int limit = parseIntOrDefault(request.getParameter("limit"), 12);
            if (limit <= 0 || limit > 50) limit = 12;
            List<Review> featuredReviews = service.getFeatured(limit);
            Map<String, Object> payload = new HashMap<>();
            payload.put("reviews", featuredReviews);
            SendResponseUtil.sendResponse(
                    new ApiResponse(true, "featured reviews fetched", payload, 200), response);
            return;
        }

        // Wall-photo gallery for the storefront #photo-reviews section.
        // GET /api/review?photos=true&limit=24 -> public, review.photo_url is not null.
        if ("true".equalsIgnoreCase(request.getParameter("photos"))) {
            int limit = parseIntOrDefault(request.getParameter("limit"), 24);
            if (limit <= 0 || limit > 60) limit = 24;
            Map<String, Object> payload = new HashMap<>();
            payload.put("reviews", service.getPhotoReviews(limit));
            SendResponseUtil.sendResponse(
                    new ApiResponse(true, "photo reviews fetched", payload, 200), response);
            return;
        }

        String productIdParam = request.getParameter("productId");
        // Admin moderation: GET /api/review?all=true&limit&offset
        if ("true".equalsIgnoreCase(request.getParameter("all"))) {
            AuthUser admin = AuthContext.get();
            if (admin == null || !admin.hasRole("Admin")) {
                SendResponseUtil.sendResponse(new ApiResponse(false, "admin role required", null, 403), response);
                return;
            }
            int limit = parseIntOrDefault(request.getParameter("limit"), 100);
            int offset = parseIntOrDefault(request.getParameter("offset"), 0);
            if (limit <= 0 || limit > 200) limit = 100;
            if (offset < 0) offset = 0;
            Map<String, Object> payload = new HashMap<>();
            payload.put("reviews", service.getAll(limit, offset));
            SendResponseUtil.sendResponse(new ApiResponse(true, "reviews fetched", payload, 200), response);
            return;
        }
        if (productIdParam == null || productIdParam.isBlank()) {
            SendResponseUtil.sendResponse(new ApiResponse(false, "productId is required", null, 400), response);
            return;
        }
        UUID productId;
        try {
            productId = UUID.fromString(productIdParam);
        } catch (IllegalArgumentException e) {
            SendResponseUtil.sendResponse(new ApiResponse(false, "invalid productId", null, 400), response);
            return;
        }
        int limit = parseIntOrDefault(request.getParameter("limit"), 20);
        int offset = parseIntOrDefault(request.getParameter("offset"), 0);
        if (limit <= 0 || limit > 100) limit = 20;
        if (offset < 0) offset = 0;

        List<Review> reviews = service.getForProduct(productId, limit, offset);
        ReviewSummary summary = service.getSummary(productId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("summary", summary);
        payload.put("reviews", reviews);
        SendResponseUtil.sendResponse(new ApiResponse(true, "reviews fetched", payload, 200), response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AuthUser user = AuthContext.get();
        if (user == null || user.getUserId() == null) {
            SendResponseUtil.sendResponse(new ApiResponse(false, "missing userid", null, 401), response);
            return;
        }
        try {
            // Branch on Content-Type: multipart carries an optional photo
            // + text fields, JSON is the legacy text-only body. Everything
            // downstream (ReviewService.submit) handles either path.
            String contentType = request.getContentType();
            boolean multipart = contentType != null
                    && contentType.toLowerCase().startsWith("multipart/");
            UUID productId;
            int rating;
            String comment;
            String city = null;
            Part photoPart = null;

            if (multipart) {
                String productIdParam = request.getParameter("productId");
                String ratingParam    = request.getParameter("rating");
                if (productIdParam == null || productIdParam.isBlank()
                        || ratingParam == null || ratingParam.isBlank()) {
                    SendResponseUtil.sendResponse(
                            new ApiResponse(false, "productId and rating are required", null, 400), response);
                    return;
                }
                try {
                    productId = UUID.fromString(productIdParam.trim());
                } catch (IllegalArgumentException e) {
                    SendResponseUtil.sendResponse(new ApiResponse(false, "invalid productId", null, 400), response);
                    return;
                }
                try {
                    rating = Integer.parseInt(ratingParam.trim());
                } catch (NumberFormatException e) {
                    SendResponseUtil.sendResponse(new ApiResponse(false, "invalid rating", null, 400), response);
                    return;
                }
                comment = request.getParameter("comment");
                city    = request.getParameter("city");
                // Accept either "photo" or "image" to match the admin
                // product-image shape; we take the first non-empty file.
                for (Part part : request.getParts()) {
                    if (("photo".equals(part.getName()) || "image".equals(part.getName()))
                            && part.getSize() > 0) {
                        photoPart = part;
                        break;
                    }
                }
            } else {
                JsonNode body = mapper.readTree(request.getInputStream());
                if (!body.hasNonNull("productId") || !body.hasNonNull("rating")) {
                    SendResponseUtil.sendResponse(
                            new ApiResponse(false, "productId and rating are required", null, 400), response);
                    return;
                }
                try {
                    productId = UUID.fromString(body.get("productId").asText());
                } catch (IllegalArgumentException e) {
                    SendResponseUtil.sendResponse(new ApiResponse(false, "invalid productId", null, 400), response);
                    return;
                }
                rating = body.get("rating").asInt();
                comment = body.hasNonNull("comment") ? body.get("comment").asText() : null;
                city    = body.hasNonNull("city")    ? body.get("city").asText()    : null;
            }

            Review saved = service.submit(user.getUserId(), productId, rating, comment, photoPart, city);
            if (saved == null) {
                SendResponseUtil.sendResponse(new ApiResponse(false, "could not save review", null, 500), response);
                return;
            }
            SendResponseUtil.sendResponse(new ApiResponse(true, "review saved", saved, 200), response);
        } catch (IllegalArgumentException e) {
            SendResponseUtil.sendResponse(new ApiResponse(false, e.getMessage(), null, 400), response);
        } catch (Exception e) {
            LOG.error("exception at review controller doPost ", e);
            SendResponseUtil.sendResponse(new ApiResponse(false, "could not save review", null, 500), response);
        }
    }

    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AuthUser user = AuthContext.get();
        if (user == null || user.getUserId() == null) {
            SendResponseUtil.sendResponse(new ApiResponse(false, "missing userid", null, 401), response);
            return;
        }
        String reviewIdParam = request.getParameter("reviewId");
        if (reviewIdParam == null || reviewIdParam.isBlank()) {
            SendResponseUtil.sendResponse(new ApiResponse(false, "reviewId is required", null, 400), response);
            return;
        }
        UUID reviewId;
        try {
            reviewId = UUID.fromString(reviewIdParam);
        } catch (IllegalArgumentException e) {
            SendResponseUtil.sendResponse(new ApiResponse(false, "invalid reviewId", null, 400), response);
            return;
        }
        boolean deleted = user.hasRole("Admin")
                ? service.deleteAny(reviewId)
                : service.deleteOwn(reviewId, user.getUserId());
        if (!deleted) {
            SendResponseUtil.sendResponse(
                    new ApiResponse(false, "review not found or not yours", null, 404), response);
            return;
        }
        SendResponseUtil.sendResponse(new ApiResponse(true, "review deleted", null, 200), response);
    }

    private static int parseIntOrDefault(String raw, int fallback) {
        if (raw == null || raw.isEmpty()) return fallback;
        try { return Integer.parseInt(raw); } catch (NumberFormatException e) { return fallback; }
    }
}
