package com.ecommerce.app.module.settings;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.ecommerce.app.module.iam.models.ApiResponse;
import com.ecommerce.app.module.iam.security.AuthContext;
import com.ecommerce.app.module.iam.security.AuthUser;
import com.ecommerce.app.module.iam.util.SendResponseUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Storefront settings document.
 *
 *   GET /api/settings[?key=site]                    -> public; data is null until first publish
 *   PUT /api/settings[?key=site]  (Admin only)      -> body { data: {...}, expectedRevision: n }
 *
 * The admin check is done here (not via annotation) because the authorization
 * filter only resolves method-level annotations on public methods.
 */
@WebServlet("/api/settings")
public class SettingsController extends HttpServlet {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DEFAULT_KEY = "site";
    private static final int MAX_BYTES = 256 * 1024;

    private final SettingsRepository repo = new SettingsRepository();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SettingsRepository.Row row = repo.find(key(request));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("data", row == null ? null : MAPPER.readTree(row.data));
        payload.put("revision", row == null ? 0 : row.revision);
        SendResponseUtil.sendResponse(new ApiResponse(true, "settings fetched", payload, 200), response);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        AuthUser user = AuthContext.get();
        if (user == null || !user.hasRole("Admin")) {
            SendResponseUtil.sendResponse(new ApiResponse(false, "admin role required", null, 403), response);
            return;
        }
        byte[] raw = request.getInputStream().readNBytes(MAX_BYTES + 1);
        if (raw.length > MAX_BYTES) {
            SendResponseUtil.sendResponse(new ApiResponse(false, "settings too large", null, 413), response);
            return;
        }
        JsonNode body = MAPPER.readTree(raw);
        if (body == null || !body.hasNonNull("data") || !body.get("data").isObject()) {
            SendResponseUtil.sendResponse(new ApiResponse(false, "data object is required", null, 400), response);
            return;
        }
        long expected = body.hasNonNull("expectedRevision") ? body.get("expectedRevision").asLong(0) : 0;
        long saved = repo.save(key(request), MAPPER.writeValueAsString(body.get("data")), expected);
        if (saved < 0) {
            SendResponseUtil.sendResponse(
                    new ApiResponse(false, "settings changed elsewhere; reload and retry", null, 409), response);
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("revision", saved);
        SendResponseUtil.sendResponse(new ApiResponse(true, "settings saved", payload, 200), response);
    }

    private static String key(HttpServletRequest request) {
        String k = request.getParameter("key");
        return k != null && k.matches("[a-z0-9_-]{1,50}") ? k : DEFAULT_KEY;
    }
}
