package com.ecommerce.app.module.order;

import com.ecommerce.app.config.DatabaseSchemaValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;

/** Uses session-local tables only; opt in with TEST_DATABASE_URL. */
@EnabledIfEnvironmentVariable(named = "TEST_DATABASE_URL", matches = ".+")
class VariantMigrationIntegrationTest {
    @Test
    void validatesMigratesAndRerunsWithoutChangingExistingVariants() throws Exception {
        try (Connection connection = DriverManager.getConnection(System.getenv("TEST_DATABASE_URL"));
             Statement statement = connection.createStatement()) {
            statement.execute("SET search_path TO pg_temp");
            assertThrows(SQLException.class, () -> DatabaseSchemaValidator.validate(connection));
            statement.execute("CREATE TEMP TABLE products (product_id uuid PRIMARY KEY)");
            statement.execute("CREATE TEMP TABLE product_variants (variant_id uuid, product_id uuid, label text, is_active boolean)");
            statement.execute("CREATE TEMP TABLE cart_items (cart_id uuid, product_id uuid, variant_id uuid, CONSTRAINT uq_cart_product UNIQUE(cart_id, product_id))");
            statement.execute("CREATE TEMP TABLE order_items (order_id uuid, product_id uuid, variant_id uuid, variant_label text, CONSTRAINT uq_order_product UNIQUE(order_id, product_id))");
            String product = "'00000000-0000-0000-0000-000000000001'";
            String first = "'00000000-0000-0000-0000-000000000002'";
            String second = "'00000000-0000-0000-0000-000000000003'";
            String cart = "'00000000-0000-0000-0000-000000000004'";
            statement.execute("INSERT INTO product_variants VALUES (" + first + "," + product + ",'small',true),(" + second + "," + product + ",'large',true)");
            statement.execute("INSERT INTO order_items VALUES (" + cart + "," + product + "," + second + ",NULL)");
            DatabaseSchemaValidator.validate(connection);
            FixVariantMigration.migrate(connection);
            FixVariantMigration.migrate(connection);
            connection.setAutoCommit(true);
            try (ResultSet result = statement.executeQuery("SELECT variant_id::text, variant_label FROM order_items")) {
                assertTrue(result.next());
                assertEquals(second.replace("'", ""), result.getString(1));
                assertEquals("large", result.getString(2));
            }
            for (String table : new String[]{"cart_items", "order_items"}) {
                String owner = table.equals("cart_items") ? "cart_id" : "order_id";
                // Both variants are valid for the same product and owner.
                if (table.equals("order_items")) statement.execute("DELETE FROM order_items");
                statement.execute("INSERT INTO " + table + " (" + owner + ",product_id,variant_id) VALUES (" + cart + "," + product + "," + first + "),(" + cart + "," + product + "," + second + ")");
                assertThrows(SQLException.class, () -> statement.execute("INSERT INTO " + table + " (" + owner + ",product_id,variant_id) VALUES (" + cart + "," + product + "," + first + ")"));
                statement.execute("INSERT INTO " + table + " (" + owner + ",product_id,variant_id) VALUES (" + cart + "," + product + ",NULL)");
                assertThrows(SQLException.class, () -> statement.execute("INSERT INTO " + table + " (" + owner + ",product_id,variant_id) VALUES (" + cart + "," + product + ",NULL)"));
            }
        }
    }
}
