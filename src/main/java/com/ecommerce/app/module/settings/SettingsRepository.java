package com.ecommerce.app.module.settings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ecommerce.app.config.DBConfig;

/**
 * Key/value store for storefront settings (shop name, hero, FAQ, offers...).
 * The JSON document is opaque to the backend; the frontend owns its shape.
 * Writes are optimistic: callers pass the revision they read.
 */
public class SettingsRepository {
    private static final Logger LOG = LoggerFactory.getLogger(SettingsRepository.class);

    public static final class Row {
        public final String data;
        public final long revision;

        Row(String data, long revision) {
            this.data = data;
            this.revision = revision;
        }
    }

    public Row find(String key) {
        String sql = "SELECT data::text, revision FROM site_settings WHERE setting_key = ?";
        try (Connection c = DBConfig.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new Row(rs.getString(1), rs.getLong(2));
            }
        } catch (SQLException e) {
            LOG.error("sql exception at settings find ", e);
        }
        return null;
    }

    /** Returns the new revision, or -1 when expectedRevision is stale. */
    public long save(String key, String json, long expectedRevision) {
        String upsert = """
                INSERT INTO site_settings (setting_key, data, revision)
                VALUES (?, ?::jsonb, 1)
                ON CONFLICT (setting_key) DO UPDATE
                   SET data = EXCLUDED.data, revision = site_settings.revision + 1, updated_at = now()
                 WHERE site_settings.revision = ?
                RETURNING revision
                """;
        try (Connection c = DBConfig.getConnection(); PreparedStatement ps = c.prepareStatement(upsert)) {
            ps.setString(1, key);
            ps.setString(2, json);
            ps.setLong(3, expectedRevision);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {
            LOG.error("sql exception at settings save ", e);
        }
        return -1;
    }
}
