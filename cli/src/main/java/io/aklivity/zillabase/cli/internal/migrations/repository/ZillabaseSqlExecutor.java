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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.postgresql.jdbc.PreferQueryMode;

public final class ZillabaseSqlExecutor
{
    private final String url;
    private final Properties props;

    public ZillabaseSqlExecutor(
        String dbName)
    {
        this.url = "jdbc:postgresql://localhost:4567/%s".formatted(dbName);

        this.props = new Properties();
        this.props.setProperty("user", "zillabase");
        this.props.setProperty("preferQueryMode", PreferQueryMode.SIMPLE.value());
    }

    public void execute(
        String rawSql) throws SQLException
    {
        try (Connection conn = DriverManager.getConnection(url, props))
        {
            String noCommentsSQL = removeComments(rawSql);

            List<String> statements = splitSQL(noCommentsSQL);

            try (Statement stmt = conn.createStatement())
            {
                // set timeout if desired
                stmt.setQueryTimeout(30);

                for (String command : statements)
                {
                    String trimmed = command.trim();
                    if (!trimmed.isEmpty())
                    {
                        System.out.println("Executing: " + trimmed);
                        stmt.executeUpdate(trimmed);
                    }
                }
            }
        }
    }

    private String removeComments(
        String sql)
    {
        return sql.replaceAll("(?s)/\\*.*?\\*/", "")
                  .replaceAll("--.*?(\\r?\\n)", "");
    }

    private List<String> splitSQL(
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
