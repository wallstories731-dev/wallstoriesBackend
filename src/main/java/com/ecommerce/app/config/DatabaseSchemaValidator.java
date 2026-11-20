package com.ecommerce.app.config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Checks the configured database/search_path without creating or changing tables. */
public final class DatabaseSchemaValidator {
    private DatabaseSchemaValidator() {}

    public static void validate(Connection connection) throws SQLException {
        List<String> missing = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT c.oid FROM pg_catalog.pg_class c WHERE c.relname = ? AND pg_catalog.pg_table_is_visible(c.oid)")) {
            for (String table : List.of("products", "product_variants", "cart_items", "order_items")) {
                statement.setString(1, table);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next() || result.getString(1) == null) {
                        missing.add(table);
                    }
                }
            }
        }
        if (!missing.isEmpty()) {
            throw new SQLException("Missing application tables in the configured database/search_path: "
                    + String.join(", ", missing)
                    + ". Check DB_URL and the connection search_path (currentSchema). Restore/apply the "
                    + "backend base schema before starting; db_migrations.sql contains incremental changes "
                    + "only and does not create the base schema. Supabase frontend migrations are a separate schema.");
        }
    }
}
