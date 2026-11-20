package com.ecommerce.app.module.product;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ecommerce.app.common.ApiResponse;
import com.ecommerce.app.module.iam.security.AuthUser;
import com.ecommerce.app.module.iam.security.RequiresRole;
import com.ecommerce.app.util.SendResponseUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@WebServlet("/api/product")
@MultipartConfig
public class ProductController extends HttpServlet {
    private static final Logger LOG = LoggerFactory.getLogger(ProductController.class);

    private ProductService service = new ProductService();
    private RecommendationService recommendationService = new RecommendationService();

    @RequiresRole("Admin")
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            // Multipart text parts default to ISO-8859-1 decoding, which mangles
            // non-Latin (e.g. Tamil) characters into mojibake. Force UTF-8 before
            // reading any parameter so the product JSON keeps its original script.
            request.setCharacterEncoding("UTF-8");
            String json = request.getParameter("product");
            Product product = ProductConveterUtil.stringtoDto(json);
            product.setActive(true);
            List<Part> imageParts = new ArrayList<>();
            Collection<Part> parts = request.getParts();
            for (Part part : parts) {
                if (part.getName().equals("images") && part.getSize() > 0) {
                    imageParts.add(part);
                }
            }

            Product createdProduct = service.createProduct(product, imageParts);
            if (createdProduct == null) {
                SendResponseUtil.sendResponse(new ApiResponse(false, "product not created", null, 500), response);
                return;
            }
            SendResponseUtil.sendResponse(new ApiResponse(true, "product created", createdProduct, 200), response);
        } catch (Exception e) {
            LOG.error("exception", e);
            SendResponseUtil.sendResponse(new ApiResponse(false, "product not created", null, 500), response);
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String productId = request.getParameter("productId");
        try {
            // ----- Single product OR recommendations for a product -----
            if (productId != null) {
                UUID id;
                try {
                    id = UUID.fromString(productId);
                } catch (IllegalArgumentException ex) {
                    SendResponseUtil.sendResponse(
                            new ApiResponse(false, "invalid productId", null, 400), response);
                    return;
                }

                // ?productId=<uuid>&related=true  -> recommendation rails
                if ("true".equalsIgnoreCase(request.getParameter("related"))) {
                    int relLimit = parseIntOrDefault(request.getParameter("limit"), 8);
                    SendResponseUtil.sendResponse(
                            new ApiResponse(true, "recommendations fetched",
                                    recommendationService.recommendationsFor(id, relLimit), 200),
                            response);
                    return;
                }

                Product product = service.getProductById(id);
                // Drafts are visible to admins only.
                if (product != null && !product.isActive() && !AuthUser.isAdminRequest(request)) {
                    product = null;
                }
                if (product == null) {
                    SendResponseUtil.sendResponse(new ApiResponse(false, "product not found", null, 404), response);
                } else {
                    SendResponseUtil.sendResponse(new ApiResponse(true, "product found", product, 200), response);
                }
                return;
            }

            // ----- Pagination (shared by list + search) -----
            int limit;
            int offset;
            try {
                limit = parseIntOrDefault(request.getParameter("limit"), 10);
                offset = parseIntOrDefault(request.getParameter("offset"), 0);
            } catch (NumberFormatException nfe) {
                SendResponseUtil.sendResponse(
                        new ApiResponse(false, "limit and offset must be integers", null, 400), response);
                return;
            }
            // sanity clamps - avoid surprise unbounded queries
            if (limit <= 0 || limit > 100) limit = 10;
            if (offset < 0) offset = 0;

            // ----- Search detection -----
            // We treat the request as a search whenever ANY of the search
            // params is present (q, category, tag, minPrice, maxPrice, inStock, sort).
            String q = request.getParameter("q");
            String[] categoryParams = request.getParameterValues("category");
            String[] tagParams = request.getParameterValues("tag");
            String minPriceParam = request.getParameter("minPrice");
            String maxPriceParam = request.getParameter("maxPrice");
            String inStockParam = request.getParameter("inStock");
            String sort = request.getParameter("sort");

            // Flatten repeated `?category=` AND comma-separated forms into a
            // single deduped, trimmed list. Empty input -> empty list ->
            // the service treats it as "no category filter".
            List<String> categories = flattenCsv(categoryParams);
            // Same treatment for tags — users may pass `?tag=ironman&tag=marvel`
            // or `?tag=ironman,marvel`. Normalisation to slugs happens inside
            // TagRepository.findActiveProductIdsByTagSlugs.
            List<String> tagSlugs = flattenCsv(tagParams);

            boolean isSearch =
                    q != null || !categories.isEmpty() || !tagSlugs.isEmpty() ||
                    minPriceParam != null || maxPriceParam != null ||
                    inStockParam != null || sort != null;

            if (isSearch) {
                Double minPrice = null;
                Double maxPrice = null;
                try {
                    if (minPriceParam != null && !minPriceParam.isEmpty()) {
                        minPrice = Double.parseDouble(minPriceParam);
                    }
                    if (maxPriceParam != null && !maxPriceParam.isEmpty()) {
                        maxPrice = Double.parseDouble(maxPriceParam);
                    }
                } catch (NumberFormatException nfe) {
                    SendResponseUtil.sendResponse(
                            new ApiResponse(false, "minPrice and maxPrice must be numbers", null, 400), response);
                    return;
                }
                if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
                    SendResponseUtil.sendResponse(
                            new ApiResponse(false, "minPrice must be <= maxPrice", null, 400), response);
                    return;
                }
                boolean inStock = "true".equalsIgnoreCase(inStockParam);

                // Fast path: category-only filter (with optional sort).
                // Generic search() pays a 3-round-trip tax (search + image
                // batch + review batch) and uses ILIKE which cannot use
                // idx_products_category. The category-only path uses a
                // single LATERAL-join query with exact equality + cache.
                // Not taken when tags are present — the tag filter needs
                // the generic search path.
                boolean categoryOnly =
                        (q == null || q.trim().isEmpty()) &&
                        !categories.isEmpty() &&
                        tagSlugs.isEmpty() &&
                        minPrice == null && maxPrice == null && !inStock;
                if (categoryOnly) {
                    List<Product> products = service.getProductsByCategories(
                            categories, sort, limit, offset);
                    SendResponseUtil.sendResponse(
                            new ApiResponse(true, "search results", products, 200), response);
                    return;
                }

                List<Product> products = service.searchProducts(
                        q, categories, tagSlugs, minPrice, maxPrice, inStock, sort, limit, offset);
                SendResponseUtil.sendResponse(
                        new ApiResponse(true, "search results", products, 200), response);
                return;
            }

            // ----- Plain list -----
            // ?expand=true attaches images + variants (batched) so the storefront
            // needs no per-product detail request.
            List<Product> products = "true".equalsIgnoreCase(request.getParameter("expand"))
                    ? service.getAllActiveProductsExpanded(limit, offset)
                    : service.getAllActiveProducts(limit, offset);
            SendResponseUtil.sendResponse(new ApiResponse(true, "products fetched", products, 200), response);

        } catch (Exception e) {
            LOG.error("exception", e);
            SendResponseUtil.sendResponse(
                    new ApiResponse(false, "product not found", "internal server error upon data you given", 500),
                    response);
        }

    }

    private static int parseIntOrDefault(String raw, int fallback) {
        if (raw == null || raw.isEmpty()) return fallback;
        return Integer.parseInt(raw);
    }

    /**
     * Flattens a multi-value query param (both {@code ?x=a&x=b} and
     * {@code ?x=a,b} forms) into a de-duplicated, trimmed list. Used for
     * both {@code category} and {@code tag} filters so they share the
     * same input semantics.
     */
    private static List<String> flattenCsv(String[] raw) {
        List<String> out = new ArrayList<>();
        if (raw == null) return out;
        for (String r : raw) {
            if (r == null) continue;
            for (String piece : r.split(",")) {
                String t = piece.trim();
                if (!t.isEmpty() && !out.contains(t)) out.add(t);
            }
        }
        return out;
    }

    @RequiresRole("Admin")
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Product product = ProductConveterUtil.requestToDto(request);
        product = service.updateProduct(product);
        if (product != null && product.getId() != null) {
            SendResponseUtil.sendResponse(new ApiResponse(true, "product updated", product, 200), response);
        } else {
            SendResponseUtil.sendResponse(new ApiResponse(false, "product not found", null, 404), response);
        }
    }

    @RequiresRole("Admin")
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String productId = request.getParameter("productId");
        if (productId == null) {
            SendResponseUtil.sendResponse(new ApiResponse(false, "product id not found", null, 400), response);
            return;
        }
        UUID id;
        try {
            id = UUID.fromString(productId);
        } catch (IllegalArgumentException ex) {
            SendResponseUtil.sendResponse(new ApiResponse(false, "invalid productId", null, 400), response);
            return;
        }
        boolean isDeleted = service.deleteProduct(id);
        if (!isDeleted) {
            SendResponseUtil.sendResponse(new ApiResponse(false, "product not found", null, 404), response);
        } else {
            SendResponseUtil.sendResponse(new ApiResponse(true, "product deleted", null, 200), response);
        }
    }

}
