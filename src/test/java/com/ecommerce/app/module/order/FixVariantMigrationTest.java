package com.ecommerce.app.module.order;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FixVariantMigrationTest {
    @Test
    void commitsConstraintsAndBackfillTogether() throws Exception {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        PreparedStatement backfill = mock(PreparedStatement.class);
        // Same Statement mock returned for both migrate stages (constraint
        // fix + tag-table bootstrap) so the executions aggregate on one
        // mock and the InOrder verification can count them in one shot.
        when(connection.createStatement()).thenReturn(statement);
        when(connection.prepareStatement(anyString())).thenReturn(backfill);
        FixVariantMigration.migrate(connection);
        InOrder order = inOrder(connection, statement, backfill);
        order.verify(connection).setAutoCommit(false);
        // Two createStatement() calls now: one for constraint fix, one
        // for ensureTagTables. 4 constraint DDLs + 3 tag DDLs = 7 total.
        order.verify(connection, times(2)).createStatement();
        verify(statement, times(7)).execute(anyString());
        order.verify(connection).prepareStatement(anyString());
        order.verify(backfill).executeUpdate();
        order.verify(backfill).close();
        order.verify(connection).commit();
        verify(connection, never()).rollback();
    }

    @Test
    void rollsBackAndPropagatesConstraintFailure() throws Exception {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(connection.createStatement()).thenReturn(statement);
        SQLException failure = new SQLException("missing table");
        when(statement.execute(anyString())).thenThrow(failure);
        assertSame(failure, assertThrows(SQLException.class, () -> FixVariantMigration.migrate(connection)));
        verify(connection).rollback();
        verify(connection, never()).commit();
        verify(connection, never()).prepareStatement(anyString());
    }

    @Test
    void backfillFailureAlsoRollsBackConstraintChanges() throws Exception {
        Connection connection = mock(Connection.class);
        when(connection.createStatement()).thenReturn(mock(Statement.class));
        PreparedStatement backfill = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(backfill);
        when(backfill.executeUpdate()).thenThrow(new SQLException("backfill failed"));
        assertThrows(SQLException.class, () -> FixVariantMigration.migrate(connection));
        verify(connection).rollback();
        verify(connection, never()).commit();
    }
}
