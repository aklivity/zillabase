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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.postgresql.jdbc.PreferQueryMode;

import io.aklivity.zillabase.cli.internal.migrations.model.ZillabaseDatabaseSchema;
import io.aklivity.zillabase.cli.internal.migrations.model.ZillabaseMaterializedView;
import io.aklivity.zillabase.cli.internal.migrations.model.ZillabaseTable;
import io.aklivity.zillabase.cli.internal.migrations.model.ZillabaseZfunction;
import io.aklivity.zillabase.cli.internal.migrations.model.ZillabaseZtable;
import io.aklivity.zillabase.cli.internal.migrations.model.ZillabaseZview;

public final class DatabaseSchemaRepository
{
    private final String url;
    private final Properties props;

    public DatabaseSchemaRepository(
        String dbName)
    {
        this.url = "jdbc:postgresql://localhost:4567/%s".formatted(dbName);
        this.props = new Properties();
        this.props.setProperty("user", "postgres");
        this.props.setProperty("preferQueryMode", PreferQueryMode.SIMPLE.value());
    }

    public ZillabaseDatabaseSchema loadActualSchema()
    {
        try (Connection connection = DriverManager.getConnection(url, props))
        {
            ZillabaseDatabaseSchema schema = new ZillabaseDatabaseSchema();

            // load ztables
            loadZtables(connection, schema);
            loadZviews(connection, schema);
            loadZfunctions(connection, schema);

            // load normal tables, materialized views, standard views
            loadTables(connection, schema);
            loadMaterializedViews(connection, schema);
            loadViews(connection, schema);

            return schema;
        }
        catch (SQLException e)
        {
            return new ZillabaseDatabaseSchema();
        }
    }

    private void loadZtables(
        Connection connection,
        ZillabaseDatabaseSchema schema) throws SQLException
    {
        String query = """
            SELECT name, sql
            FROM zb_catalog.ztables
            """;
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query))
        {
            while (rs.next())
            {
                ZillabaseZtable metadata = new ZillabaseZtable(
                    rs.getString("name"),
                    rs.getString("sql")
                );
                schema.addZtable(metadata);
            }
        }
    }

    private void loadZviews(
        Connection connection,
        ZillabaseDatabaseSchema schema) throws SQLException
    {
        String query = """
            SELECT name, sql
            FROM zb_catalog.zviews
            WHERE name NOT IN ('zcatalogs')
            """;
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query))
        {
            while (rs.next())
            {
                ZillabaseZview metadata = new ZillabaseZview(
                    rs.getString("name"),
                    rs.getString("sql")
                );
                schema.addZview(metadata);
            }
        }
    }

    private void loadZfunctions(
        Connection connection,
        ZillabaseDatabaseSchema schema) throws SQLException
    {
        String query = """
            SELECT name, sql
            FROM zb_catalog.zfunctions
            """;
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query))
        {
            while (rs.next())
            {
                ZillabaseZfunction metadata = new ZillabaseZfunction(
                    rs.getString("name"),
                    rs.getString("sql")
                );
                schema.addZfunction(metadata);
            }
        }
    }

    private void loadTables(
        Connection connection,
        ZillabaseDatabaseSchema schema) throws SQLException
    {
        List<String> tables = showObjects(connection, "TABLES");
        if (!tables.isEmpty())
        {
            String includes = tables.stream()
                .map(name -> "'" + name + "'")
                .collect(Collectors.joining(", "));
            String query = "SELECT name, definition FROM rw_tables WHERE name IN (%s)".formatted(includes);

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(query))
            {
                while (rs.next())
                {
                    ZillabaseTable metadata = new ZillabaseTable(
                        rs.getString("name"),
                        rs.getString("definition")
                    );
                    schema.addTable(metadata);
                }
            }
        }
    }

    private void loadMaterializedViews(
        Connection connection,
        ZillabaseDatabaseSchema schema) throws SQLException
    {
        List<String> mviews = showObjects(connection, "MATERIALIZED VIEWS");

        String includes = mviews.stream()
            .filter(m -> !"zcatalogs".equals(m))
            .map(name -> "'" + name + "'")
            .collect(Collectors.joining(", "));
        String query = "SELECT name, definition FROM rw_materialized_views WHERE name IN (%s)".formatted(includes);

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query))
        {
            while (rs.next())
            {
                ZillabaseMaterializedView mv = new ZillabaseMaterializedView(
                    rs.getString("name"),
                    rs.getString("definition")
                );
                schema.addMaterializedView(mv);
            }
        }
    }

    private void loadViews(
        Connection connection,
        ZillabaseDatabaseSchema schema) throws SQLException
    {
        List<String> views = showObjects(connection, "VIEWS");
        if (!views.isEmpty())
        {
            String includes = views.stream()
                .map(name -> "'" + name + "'")
                .collect(Collectors.joining(", "));
            String query = "SELECT name, definition FROM rw_views WHERE name IN (%s)".formatted(includes);

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(query))
            {
                while (rs.next())
                {
                    ZillabaseZview view = new ZillabaseZview(
                        rs.getString("name"),
                        rs.getString("definition")
                    );
                    schema.addView(view);
                }
            }
        }
    }

    private List<String> showObjects(
        Connection connection,
        String type) throws SQLException
    {
        String query = "SHOW %s".formatted(type);
        List<String> result = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query))
        {
            while (rs.next())
            {
                result.add(rs.getString("name"));
            }
        }

        return result;
    }
}
