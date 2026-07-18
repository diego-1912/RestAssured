package utils;

import config.ConfigManager;
import config.EnvironmentConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Optional cross-check layer: verifies API responses against persisted state via JDBC.
 * Connection details come from the {@code db} section of the environment config; the
 * username/password are resolved from environment variables, never stored in YAML.
 */
public class DbVerifier implements AutoCloseable {

    private final Connection connection;

    public DbVerifier() {
        EnvironmentConfig.DbConfig db = ConfigManager.getInstance().db();
        String username = ConfigManager.resolveSecret(db.usernameEnvVar, false);
        String password = ConfigManager.resolveSecret(db.passwordEnvVar, false);
        try {
            this.connection = DriverManager.getConnection(db.url, username, password);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to connect to verification DB: " + db.url, e);
        }
    }

    /** Runs a read-only query and returns rows as column-name -> value maps. */
    public List<Map<String, Object>> query(String sql) {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            int columnCount = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(rs.getMetaData().getColumnLabel(i), rs.getObject(i));
                }
                rows.add(row);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Query failed: " + sql, e);
        }
        return rows;
    }

    public int executeUpdate(String sql) {
        try (Statement statement = connection.createStatement()) {
            return statement.executeUpdate(sql);
        } catch (SQLException e) {
            throw new IllegalStateException("Update failed: " + sql, e);
        }
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to close DB connection", e);
        }
    }
}
