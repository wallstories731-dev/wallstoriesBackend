package com.ecommerce.app.config;

import org.junit.jupiter.api.Test;
import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DatabaseSchemaValidatorTest {
    @Test
    void acceptsVisibleTables() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet result = mock(ResultSet.class);
        when(connection.prepareStatement("SELECT c.oid FROM pg_catalog.pg_class c WHERE c.relname = ? AND pg_catalog.pg_table_is_visible(c.oid)")).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(result);
        when(result.next()).thenReturn(true);
        when(result.getString(1)).thenReturn("visible_table");
        DatabaseSchemaValidator.validate(connection);
        verify(statement).setString(1, "products");
        verify(statement).setString(1, "order_items");
        verify(connection, never()).createStatement();
    }

    @Test
    void missingSchemaHasActionableError() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet result = mock(ResultSet.class);
        when(connection.prepareStatement("SELECT c.oid FROM pg_catalog.pg_class c WHERE c.relname = ? AND pg_catalog.pg_table_is_visible(c.oid)")).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(result);
        when(result.next()).thenReturn(true);
        SQLException error = assertThrows(SQLException.class,
                () -> DatabaseSchemaValidator.validate(connection));
        assertTrue(error.getMessage().contains("products, product_variants, cart_items, order_items"));
        assertTrue(error.getMessage().contains("currentSchema"));
        assertTrue(error.getMessage().contains("incremental changes only"));
    }
}
