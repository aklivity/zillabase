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
package io.aklivity.zillabase.cli.internal.migrations;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.postgresql.jdbc.PreferQueryMode;

import io.aklivity.zillabase.cli.internal.migrations.model.ZillabaseMigrationFile;

public final class ZillabaseMigrationsApplyHelper
{
    private final ZillabaseMigrationsDiffHelper diffHelper;
    private final String url;
    private final Properties props;

    private Connection connection;

    public ZillabaseMigrationsApplyHelper(
        ZillabaseMigrationsDiffHelper diffHelper,
        String db)
    {
        this.diffHelper = diffHelper;
        this.url = "jdbc:postgresql://localhost:4567/%s".formatted(db);
        this.props = new Properties();
        this.props.setProperty("user", "zillabase");
        this.props.setProperty("preferQueryMode", PreferQueryMode.SIMPLE.value());
    }

    public void apply()
    {
        diffHelper.unappliedFiles().forEach(this::processFile);
    }

    private void connect()
    {
        if (connection == null)
        {
            try
            {
                connection = DriverManager.getConnection(url, props);
            }
            catch (SQLException e)
            {
                System.out.println("Failed to connect to " + url);
            }
        }
    }

    private void processFile(
        ZillabaseMigrationFile migration)
    {
        try
        {
            String content = migration.sqlContents();
            if (content != null)
            {
                connect();

                Statement stmt = connection.createStatement();
                // Set the timeout in seconds (for example, 30 seconds)
                stmt.setQueryTimeout(30);
                String noCommentsSQL = content.replaceAll("(?s)/\\*.*?\\*/", "")
                        .replaceAll("--.*?(\\r?\\n)", "");

                List<String> splitCommands = splitSQL(noCommentsSQL);

                for (String command : splitCommands)
                {
                    if (!command.trim().isEmpty())
                    {
                        command = command.trim().replaceAll("[\\n\\r]+$", "");
                        System.out.println("Executing command: " + command);
                        stmt.executeUpdate(command);
                    }
                }
            }

            diffHelper.record(migration);

        }
        catch (Exception ex)
        {
            System.out.format("Failed to process %s. ex: %s\n", migration.scriptName(), ex.getMessage());
        }
    }

    private static List<String> splitSQL(
        String sql)
    {
        List<String> result = new ArrayList<>();
        StringBuilder command = new StringBuilder();
        boolean insideDollarBlock = false;

        String[] lines = sql.split("\\r?\\n");

        for (String line : lines)
        {
            if (line.contains("$$"))
            {
                insideDollarBlock = !insideDollarBlock;
            }

            command.append(line).append("\n");

            if (!insideDollarBlock && line.trim().endsWith(";"))
            {
                result.add(command.toString().trim());
                command.setLength(0);
            }
        }

        if (!command.isEmpty())
        {
            result.add(command.toString().trim());
        }

        return result;
    }
}
