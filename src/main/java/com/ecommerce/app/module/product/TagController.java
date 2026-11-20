package com.ecommerce.app.module.product;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ecommerce.app.common.ApiResponse;
import com.ecommerce.app.module.iam.security.RequiresRole;
import com.ecommerce.app.util.SendResponseUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Tag catalogue + product ↔ tag linkage.
 *
 * <pre>
 *   GET    /api/tag                             -> list all tags + usage counts (public)
 *   GET    /api/tag?slug=ironman                -> one tag by slug (public)
 *   GET    /api/tag?productId=&lt;uuid&gt;             -> tags attached to a product (public)
 *   POST   /api/tag           body {name,description?}                                (Admin)
 *   PUT    /api/tag?tagId=&lt;uuid&gt;  body {name?, description?}                          (Admin)
 *   DELETE /api/tag?tagId=&lt;uuid&gt;                                                      (Admin)
 *
 *   -- product ↔ tag link management --
 *   POST   /api/tag?productId=&lt;uuid&gt;  body {tags: ["ironman", "marvel"]}              (Admin)
 *       Replaces the full tag set of the product (set-based).
 *   POST   /api/tag?productId=&lt;uuid&gt;&amp;tagId=&lt;uuid&gt;                                    (Admin)
 *       Attaches one tag. Use this for single-add from the admin UI.
 *   DELETE /api/tag?productId=&lt;uuid&gt;&amp;tagId=&lt;uuid&gt;                                    (Admin)
 *       Detaches one tag.
 * </pre>
 *
 * <p>Tag names are user-facing strings (e.g. {@code "Iron Man"}); the
 * backend canonicalises them to lowercase slugs ({@code "iron-man"}) which
 * are the unique business key. Callers may pass either the slug or the
 * display name — both are accepted and normalised identically.</p>
 */
@WebServlet("/api/tag")
public class TagController extends HttpServlet {
    private static final Logger LOG = LoggerFactory.getLogger(TagController.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ProductService productService = new ProductService();
    private final TagRepository tagRepository = new TagRepository();

    // ==================================================================
    // Reads (public)
    // ==================================================================

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            UUID productId = parseUuid(request.getParameter("productId"));
            if (productId != null) {
                // Tags attached to a single product.
                List<Tag> tags = tagRepository.findByProductId(productId);
                SendResponseUtil.sendResponse(
                        new ApiResponse(true, "product tags fetched", tags, 200), response);
                return;
            }

            String slug = request.getParameter("slug");
            if (slug != null && !slug.isBlank()) {
                Tag t = tagRepository.findBySlug(slug);
                if (t == null) {
                    SendResponseUtil.sendResponse(
                            new ApiResponse(false, "tag not found", null, 404), response);
                    return;
                }
                SendResponseUtil.sendResponse(
                        new ApiResponse(true, "tag fetched", t, 200), response);
                return;
            }

            // Default: full catalogue with usage counts.
            // Also expose the legacy {tag, count} rows harvested from
            // products.attributes so storefronts migrating off the JSON
            // tags still see something. Prefer the table when both exist.
            List<Tag> catalogue = tagRepository.listWithCounts();
            if (!catalogue.isEmpty()) {
                SendResponseUtil.sendResponse(
                        new ApiResponse(true, "tags fetched", catalogue, 200), response);
                return;
            }

            List<Map<String, Object>> legacy = new ArrayList<>();
            productService.getTagCounts().forEach((tag, count) -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("name", tag);
                row.put("slug", Tag.toSlug(tag));
                row.put("productCount", count);
                legacy.add(row);
            });
            SendResponseUtil.sendResponse(
                    new ApiResponse(true, "tags fetched", legacy, 200), response);
        } catch (Exception e) {
            LOG.error("tag list failed", e);
            SendResponseUtil.sendResponse(
                    new ApiResponse(false, "could not fetch tags", null, 500), response);
        }
    }

    // ==================================================================
    // Writes (admin)
    // ==================================================================

    /**
     * Two forms:
     * <ul>
     *   <li>No {@code productId} query — create (or upsert) a tag by name.</li>
     *   <li>With {@code productId} query — attach tags to that product.
     *       Either pass {@code tagId} as a query param to attach one,
     *       or send {@code {tags: [...]}} as the JSON body to replace
     *       the full set.</li>
     * </ul>
     */
    @RequiresRole("Admin")
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            UUID productId = parseUuid(request.getParameter("productId"));

            if (productId == null) {
                // -- create / upsert a tag --
                JsonNode body = readBody(request);
                String name = body.hasNonNull("name") ? body.get("name").asText() : null;
                if (name == null || name.isBlank()) {
                    SendResponseUtil.sendResponse(
                            new ApiResponse(false, "name is required", null, 400), response);
                    return;
                }
                String description = body.hasNonNull("description")
                        ? body.get("description").asText() : null;
                Tag saved = tagRepository.upsertBySlug(name, description);
                if (saved == null) {
                    SendResponseUtil.sendResponse(
                            new ApiResponse(false, "invalid tag name", null, 400), response);
                    return;
                }
                SendResponseUtil.sendResponse(
                        new ApiResponse(true, "tag saved", saved, 200), response);
                return;
            }

            // -- product ↔ tag linkage --
            UUID tagId = parseUuid(request.getParameter("tagId"));
            if (tagId != null) {
                boolean inserted = tagRepository.attach(productId, tagId);
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("attached", true);
                out.put("inserted", inserted);
                SendResponseUtil.sendResponse(
                        new ApiResponse(true, inserted ? "tag attached" : "tag already attached", out, 200),
                        response);
                return;
            }

            // Full set replacement: {"tags": ["ironman", "marvel"]}.
            // This is the hot path called by the admin catalog editor when
            // the poster is saved; it collapses N add+remove calls into one.
            JsonNode body = readBody(request);
            JsonNode arr = body.get("tags");
            List<String> names = new ArrayList<>();
            if (arr != null && arr.isArray()) {
                arr.forEach(n -> { if (n.isTextual()) names.add(n.asText()); });
            }
            List<Tag> result = tagRepository.replaceProductTags(productId, names);
            SendResponseUtil.sendResponse(
                    new ApiResponse(true, "product tags replaced", result, 200), response);
        } catch (Exception e) {
            LOG.error("tag write failed", e);
            SendResponseUtil.sendResponse(
                    new ApiResponse(false, "could not save tag", null, 500), response);
        }
    }

    @RequiresRole("Admin")
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            UUID tagId = parseUuid(request.getParameter("tagId"));
            if (tagId == null) {
                SendResponseUtil.sendResponse(
                        new ApiResponse(false, "tagId is required", null, 400), response);
                return;
            }
            JsonNode body = readBody(request);
            String name = body.hasNonNull("name") ? body.get("name").asText() : null;
            String description = body.hasNonNull("description") ? body.get("description").asText() : null;
            if (name == null && description == null) {
                SendResponseUtil.sendResponse(
                        new ApiResponse(false, "name or description required", null, 400), response);
                return;
            }
            // If name is omitted, keep the current one (fetch first).
            Tag current = tagRepository.findById(tagId);
            if (current == null) {
                SendResponseUtil.sendResponse(
                        new ApiResponse(false, "tag not found", null, 404), response);
                return;
            }
            Tag saved = tagRepository.update(
                    tagId,
                    name != null ? name : current.getName(),
                    description != null ? description : current.getDescription());
            if (saved == null) {
                SendResponseUtil.sendResponse(
                        new ApiResponse(false, "could not update tag", null, 500), response);
                return;
            }
            SendResponseUtil.sendResponse(
                    new ApiResponse(true, "tag updated", saved, 200), response);
        } catch (Exception e) {
            LOG.error("tag update failed", e);
            SendResponseUtil.sendResponse(
                    new ApiResponse(false, "could not update tag", null, 500), response);
        }
    }

    /**
     * Either deletes a single product↔tag link (when both productId and
     * tagId are given) or deletes the tag entirely (tagId only).
     */
    @RequiresRole("Admin")
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            UUID tagId = parseUuid(request.getParameter("tagId"));
            UUID productId = parseUuid(request.getParameter("productId"));
            if (tagId == null) {
                SendResponseUtil.sendResponse(
                        new ApiResponse(false, "tagId is required", null, 400), response);
                return;
            }
            if (productId != null) {
                boolean ok = tagRepository.detach(productId, tagId);
                if (!ok) {
                    SendResponseUtil.sendResponse(
                            new ApiResponse(false, "link not found", null, 404), response);
                    return;
                }
                SendResponseUtil.sendResponse(
                        new ApiResponse(true, "tag detached", null, 200), response);
                return;
            }
            boolean ok = tagRepository.delete(tagId);
            if (!ok) {
                SendResponseUtil.sendResponse(
                        new ApiResponse(false, "tag not found", null, 404), response);
                return;
            }
            SendResponseUtil.sendResponse(
                    new ApiResponse(true, "tag deleted", null, 200), response);
        } catch (Exception e) {
            LOG.error("tag delete failed", e);
            SendResponseUtil.sendResponse(
                    new ApiResponse(false, "could not delete tag", null, 500), response);
        }
    }

    // ==================================================================
    // helpers
    // ==================================================================

    private static UUID parseUuid(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return UUID.fromString(s.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static JsonNode readBody(HttpServletRequest request) throws IOException {
        try {
            JsonNode node = MAPPER.readTree(request.getInputStream());
            return node == null ? MAPPER.createObjectNode() : node;
        } catch (Exception e) {
            return MAPPER.createObjectNode();
        }
    }
}
