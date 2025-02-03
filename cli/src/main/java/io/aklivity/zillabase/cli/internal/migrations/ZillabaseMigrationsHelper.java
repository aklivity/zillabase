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

import static java.util.Collections.emptyList;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.postgresql.jdbc.PreferQueryMode;

import io.aklivity.zillabase.cli.internal.commands.ZillabaseCommand;

public final class ZillabaseMigrationsHelper
{
    public static final Path MIGRATIONS_PATH = ZillabaseCommand.ZILLABASE_PATH.resolve("migrations");

    public static final Pattern MIGRATION_FILE_PATTERN = Pattern.compile("(?<number>\\d{6})__.+\\.sql");
    public static final String MIGRATION_FILE_FORMAT = "%06d__%s.sql";

    public final Matcher matcher = MIGRATION_FILE_PATTERN.matcher("");
    private final String url;
    private final Properties postgresProps;
    private final Properties zillabaseProps;

    public ZillabaseMigrationsHelper(
         String db)
    {
        this.url = "jdbc:postgresql://localhost:4567/%s".formatted(db);
        this.postgresProps = new Properties();
        this.postgresProps.setProperty("user", "postgres");
        this.postgresProps.setProperty("preferQueryMode", PreferQueryMode.SIMPLE.value());

        this.zillabaseProps = new Properties();
        this.zillabaseProps.setProperty("user", "zillabase");
        this.zillabaseProps.setProperty("preferQueryMode", PreferQueryMode.SIMPLE.value());
    }

    public void applyMigration()
    {
        files().forEach(path ->
        {
            String filename = path.getFileName().toString();
            String version = matcher.reset(filename).matches()
                ? matcher.group("number")
                : "000000";

            if (!isVersionApplied(version))
            {
                processMigrationFile(version, path);
            }
        });
    }

    public boolean isVersionApplied(
        String version)
    {
        boolean applied = false;

        String query = """
            SELECT version FROM zb_catalog.schema_version WHERE version = ?""";
        try (Connection conn = DriverManager.getConnection(url, postgresProps);
            PreparedStatement ps = conn.prepareStatement(query))
        {
            ps.setString(1, version);

            ResultSet rs = ps.executeQuery();
            applied = rs.next();
        }
        catch (SQLException ex)
        {
            System.out.format("Failed to check version %s\n",  ex.getMessage());
        }

        return applied;
    }

    public void recordMigration(
            String version,
            String description,
            String scriptName,
            String checksum) throws SQLException
    {
        String insertSql = """
            INSERT INTO zb_catalog.schema_version
            (version, description, script_name, checksum, applied_on)
            VALUES (?, ?, ?, ?, now())
            """;
        try (Connection conn = DriverManager.getConnection(url, postgresProps);
            PreparedStatement ps = conn.prepareStatement(insertSql))
        {
            ps.setString(1, version);
            ps.setString(2, description);
            ps.setString(3, scriptName);
            ps.setString(4, checksum);
            ps.executeUpdate();
        }
    }

    public List<MigrationMetadata> allAppliedMigrations() throws SQLException
    {
        List<MigrationMetadata> migrations = new ArrayList<>();
        String query = """
            SELECT version, description, script_name, checksum, applied_on
            FROM zb_catalog.schema_version
            ORDER BY applied_on ASC
            """;

        try (Connection conn = DriverManager.getConnection(url, postgresProps);
            Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query))
        {
            while (rs.next())
            {
                MigrationMetadata metadata = new MigrationMetadata(
                    rs.getString("version"),
                    rs.getString("description"),
                    rs.getString("script_name"),
                    rs.getString("checksum"),
                    rs.getTimestamp("applied_on")
                );
                migrations.add(metadata);
            }
        }
        return migrations;
    }

    public List<Path> files()
    {
        List<Path> migrations;

        try
        {
            migrations = Files.list(MIGRATIONS_PATH)
                .sorted()
                .filter(Files::isRegularFile)
                .filter(n -> matcher.reset(n.getFileName().toString()).matches())
                .collect(Collectors.toList());
        }
        catch (IOException ex)
        {
            migrations = emptyList();
        }

        return migrations;
    }

    private String readSql(
        Path seedPath)
    {
        String content = null;
        try
        {
            if (Files.exists(seedPath) &&
                Files.size(seedPath) != 0 &&
                Files.readAllLines(seedPath).stream()
                    .anyMatch(line -> !line.trim().isEmpty() && !line.trim().startsWith("--")))
            {
                content = Files.readString(seedPath);
            }
        }
        catch (IOException ex)
        {
            ex.printStackTrace(System.err);
        }

        return content;
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

    private void processMigrationFile(
        String version,
        Path sql)
    {
        final String filename = sql.getFileName().toString();

        try
        {
            String content = readSql(sql);
            if (content != null)
            {
                Connection conn = DriverManager.getConnection(url, zillabaseProps);
                Statement stmt = conn.createStatement();
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

            String checksum = computeSHA256Checksum(sql);
            recordMigration(version, "", filename, checksum);

        }
        catch (Exception ex)
        {
            System.out.format("Failed to process %s. ex: %s\n", filename, ex.getMessage());
        }
    }

    private String computeSHA256Checksum(
        Path filePath) throws IOException, NoSuchAlgorithmException
    {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        try (InputStream is = Files.newInputStream(filePath))
        {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1)
            {
                digest.update(buffer, 0, bytesRead);
            }
        }

        byte[] hashBytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes)
        {
            sb.append(String.format("%02x", b));
        }

        return sb.toString();
    }

    private record MigrationMetadata(
        String version,
        String description,
        String scriptName,
        String checksum,
        Timestamp appliedOn)
    {
    }
}
