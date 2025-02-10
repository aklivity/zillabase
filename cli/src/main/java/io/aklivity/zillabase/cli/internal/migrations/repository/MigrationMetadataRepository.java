/*
 * Copyright 2024 Aklivity Inc
 *
 * Licensed under the Aklivity Community License (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 *
 *   https://www.aklivity.io/aklivity-community-license/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.aklivity.zillabase.cli.internal.migrations.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.postgresql.jdbc.PreferQueryMode;

import io.aklivity.zillabase.cli.internal.migrations.model.ZillabaseMigrationFile;
import io.aklivity.zillabase.cli.internal.migrations.model.ZillabaseMigrationMetadata;

public final class MigrationMetadataRepository
{
    private final String url;
    private final Properties props;

    public MigrationMetadataRepository(
        String dbName)
    {
        this.url = "jdbc:postgresql://localhost:4567/%s".formatted(dbName);
        this.props = new Properties();
        this.props.setProperty("user", "postgres");
        this.props.setProperty("preferQueryMode", PreferQueryMode.SIMPLE.value());
    }

    public void recordMigration(
        ZillabaseMigrationFile migration)
    {
        String insertSql = """
            INSERT INTO zb_catalog.schema_version
                   (version, description, script_name, checksum, applied_on)
            VALUES (?, ?, ?, ?, now())
            """;

        try (Connection connection = DriverManager.getConnection(url, props);
             PreparedStatement ps = connection.prepareStatement(insertSql))
        {
            ps.setString(1, migration.version());
            ps.setString(2, migration.description());
            ps.setString(3, migration.scriptName());
            ps.setString(4, migration.checksum());
            ps.executeUpdate();
        }
        catch (SQLException ex)
        {
            System.err.format("Failed to record migration: %s\n", ex.getMessage());
        }
    }

    public List<ZillabaseMigrationMetadata> loadMetadata()
    {
        List<ZillabaseMigrationMetadata> migrations = new ArrayList<>();
        String query = """
            SELECT version, description, script_name, checksum, applied_on
            FROM zb_catalog.schema_version
            ORDER BY applied_on ASC
            """;

        try (Connection connection = DriverManager.getConnection(url, props);
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query))
        {
            while (rs.next())
            {
                ZillabaseMigrationMetadata metadata = new ZillabaseMigrationMetadata(
                    rs.getString("version"),
                    rs.getString("description"),
                    rs.getString("script_name"),
                    rs.getString("checksum"),
                    rs.getTimestamp("applied_on")
                );
                migrations.add(metadata);
            }
        }
        catch (SQLException e)
        {
            System.err.format("Failed to load migration metadata: %s\n", e.getMessage());
        }

        return migrations;
    }

    public boolean isVersionApplied(
        String version)
    {
        boolean isApplied = false;

        String query = "SELECT version FROM zb_catalog.schema_version WHERE version = ?";

        applied:
        try (Connection connection = DriverManager.getConnection(url, props);
             PreparedStatement ps = connection.prepareStatement(query))
        {
            ps.setString(1, version);
            try (ResultSet rs = ps.executeQuery())
            {
                isApplied = rs.next();
                break applied;
            }
        }
        catch (SQLException ex)
        {
            System.err.format("Failed to check migration: %s\n", ex.getMessage());
        }

        return isApplied;
    }
}
