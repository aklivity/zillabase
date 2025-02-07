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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.postgresql.jdbc.PreferQueryMode;

import io.aklivity.zillabase.cli.internal.commands.ZillabaseCommand;
import io.aklivity.zillabase.cli.internal.migrations.model.ZillabaseCreateZfunction;
import io.aklivity.zillabase.cli.internal.migrations.model.ZillabaseCreateZtable;
import io.aklivity.zillabase.cli.internal.migrations.model.ZillabaseCreateZview;
import io.aklivity.zillabase.cli.internal.migrations.model.ZillabaseMigrationFile;
import io.aklivity.zillabase.cli.internal.migrations.model.ZillabaseMigrationMetadata;

public final class ZillabaseMigrationsDiffHelper
{
    public static final Path MIGRATIONS_PATH = ZillabaseCommand.ZILLABASE_PATH.resolve("migrations");
    public static final String MIGRATION_FILE_FORMAT = "%06d__%s.sql";

    private static final Pattern MIGRATION_FILE_PATTERN = Pattern.compile("(?<number>\\d{6})__(?<description>.+)\\.sql");
    private static final Pattern CREATE_QUERY_PATTERN = Pattern.compile(
          "(?i)^\\s*CREATE\\s+" +
          "(?:(?:OR|REPLACE|TEMP|UNLOGGED|GLOBAL|LOCAL)\\s+)*" +
          "((?:MATERIALIZED\\s+VIEW)|ZTABLE|TABLE|VIEW|FUNCTION|ZVIEW|ZFUNCTION)" +
          "\\s+" +
          "(?:IF\\s+NOT\\s+EXISTS\\s+)?" +
          "([^\\s(]+)");

    public final Matcher fileMatcher = MIGRATION_FILE_PATTERN.matcher("");
    public final Matcher createMatcher = CREATE_QUERY_PATTERN.matcher("");

    private final String url;
    private final Properties props;

    private Connection connection;

    private final List<SchemaComparisonStrategy> strategies;
    {
        List<SchemaComparisonStrategy> strategies = new ArrayList<>();
        strategies.add(this::compareCreateZtable);
        strategies.add(this::compareCreateZfunction);
        strategies.add(this::compareCreateZview);
        this.strategies = strategies;
    }

    private final List<DatabaseObjectApplier> appliers;
    {
        List<DatabaseObjectApplier> appliers = new ArrayList<>();
        appliers.add(this::applyZtables);
        appliers.add(this::applyZfunctions);
        appliers.add(this::applyZviews);
        appliers.add(this::applyTables);
        this.appliers = appliers;
    }

    public ZillabaseMigrationsDiffHelper(
         String db)
    {
        this.url = "jdbc:postgresql://localhost:4567/%s".formatted(db);
        this.props = new Properties();
        this.props.setProperty("user", "postgres");
        this.props.setProperty("preferQueryMode", PreferQueryMode.SIMPLE.value());
    }

    public List<ZillabaseMigrationMetadata> appliedMigrationsMetadata()
    {
        List<ZillabaseMigrationMetadata> migrations = new ArrayList<>();
        String query = """
            SELECT version, description, script_name, checksum, applied_on
            FROM zb_catalog.schema_version
            ORDER BY applied_on ASC
            """;

        connect();

        try (Statement stmt = connection.createStatement();
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
            System.out.println("Failed to record migration " + e.getMessage());
        }

        return migrations;
    }

    public List<ZillabaseMigrationFile> allMigrationFiles()
    {
        List<ZillabaseMigrationFile> migrations;

        try
        {
            migrations = Files.list(MIGRATIONS_PATH)
                .sorted()
                .filter(Files::isRegularFile)
                .filter(n -> fileMatcher.reset(n.getFileName().toString()).matches())
                .map(this::parseFile)
                .collect(Collectors.toList());
        }
        catch (IOException ex)
        {
            migrations = emptyList();
        }

        return migrations;
    }

    public List<ZillabaseMigrationFile> unappliedFiles()
    {
        List<ZillabaseMigrationFile> migrations;

        try
        {
            migrations = Files.list(MIGRATIONS_PATH)
                .sorted()
                .filter(Files::isRegularFile)
                .filter(n -> fileMatcher.reset(n.getFileName().toString()).matches())
                .map(this::parseFile)
                .filter(f -> !isVersionApplied(f.version()))
                .collect(Collectors.toList());
        }
        catch (IOException ex)
        {
            migrations = emptyList();
        }

        return migrations;
    }

    public List<ZillabaseMigrationFile> filesDiff()
    {
        List<ZillabaseMigrationFile> pending = new ArrayList<>();
        List<String> appliedVersions = new ArrayList<>();

        List<ZillabaseMigrationMetadata> appliedMigrations = appliedMigrationsMetadata();
        List<ZillabaseMigrationFile> localMigrations = allMigrationFiles();

        for (ZillabaseMigrationMetadata metadata : appliedMigrations)
        {
            appliedVersions.add(metadata.version());
        }

        for (ZillabaseMigrationFile file : localMigrations)
        {
            if (!appliedVersions.contains(file.version()))
            {
                pending.add(file);
            }
        }

        return pending;
    }

    public String databaseDiff()
    {
        ZillabaseDatabaseSchema actual = readSchemaFromDatabase();
        ZillabaseDatabaseSchema expected = readSchemaFromFiles();

        ZillabaseSchemaDiff schemaDiff = compareSchemas(actual, expected);

        return schemaDiff.generatePatchScript();
    }

    @FunctionalInterface
    private interface SchemaComparisonStrategy
    {
        void compare(
            ZillabaseDatabaseSchema actual,
            ZillabaseDatabaseSchema expected,
            ZillabaseSchemaDiff diff);
    }

    @FunctionalInterface
    private interface DatabaseObjectApplier
    {
        void apply(
            ZillabaseDatabaseSchema schema);
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

    private ZillabaseDatabaseSchema readSchemaFromDatabase()
    {
        ZillabaseDatabaseSchema schema = new ZillabaseDatabaseSchema();
        appliers.forEach(a -> a.apply(schema));

        return schema;
    }

    private ZillabaseDatabaseSchema readSchemaFromFiles()
    {
        ZillabaseDatabaseSchema schema = new ZillabaseDatabaseSchema();
        allMigrationFiles().stream()
            .filter(f -> isVersionApplied(f.version()))
            .map(ZillabaseMigrationFile::sqlContents)
            .map(ZillabaseMigrationsDiffHelper::splitSQL)
            .flatMap(List::stream)
            .forEach(s -> parserCreate(schema, s));

        return schema;
    }

    private ZillabaseSchemaDiff compareSchemas(
        ZillabaseDatabaseSchema actual,
        ZillabaseDatabaseSchema expected)
    {
        ZillabaseSchemaDiff diff = new ZillabaseSchemaDiff();

        strategies.forEach(strategy -> strategy.compare(actual, expected, diff));
        strategies.forEach(strategy -> strategy.compare(expected, actual, diff));

        return diff;
    }

    private void parserCreate(
        ZillabaseDatabaseSchema schema,
        String sql)
    {
        createMatcher.reset(sql);

        if (createMatcher.find())
        {
            String type = createMatcher.group(1);
            String name = createMatcher.group(2);

            switch (type)
            {
            case "ZFUNCTION":
                schema.addZfunction(new ZillabaseCreateZfunction(name, sql));
                break;
            case "ZTABLE":
                schema.addZtable(new ZillabaseCreateZtable(name, sql));
                break;
            case "ZVIEW":
                schema.addZview(new ZillabaseCreateZview(name, sql));
                break;
            }
        }
    }

    private boolean isVersionApplied(
        String version)
    {
        boolean applied = false;

        String query = """
            SELECT version FROM zb_catalog.schema_version WHERE version = ?""";

        connect();

        try (Connection conn = DriverManager.getConnection(url, props);
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

    private void applyZtables(
        ZillabaseDatabaseSchema schema)
    {
        String query = """
            SELECT name, sql
            FROM zb_catalog.ztables
            """;

        connect();

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query))
        {
            connection.getMetaData();
            while (rs.next())
            {
                ZillabaseCreateZtable metadata = new ZillabaseCreateZtable(
                    rs.getString("name"),
                    rs.getString("sql")
                );
                schema.addZtable(metadata);
            }
        }
        catch (SQLException e)
        {
            System.out.println("Failed to fetch ztables " + e.getMessage());
        }
    }

    private void applyZviews(
        ZillabaseDatabaseSchema schema)
    {
        String query = """
            SELECT name, sql
            FROM zb_catalog.zviews
            """;

        connect();

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query))
        {
            while (rs.next())
            {
                ZillabaseCreateZview metadata = new ZillabaseCreateZview(
                    rs.getString("name"),
                    rs.getString("sql")
                );
                schema.addZview(metadata);
            }
        }
        catch (SQLException e)
        {
            System.out.println("Failed to fetch zviews " + e.getMessage());
        }
    }

    private void applyZfunctions(
        ZillabaseDatabaseSchema schema)
    {
        String query = """
            SELECT name, sql
            FROM zb_catalog.zfunctions
            """;

        connect();

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query))
        {
            while (rs.next())
            {
                ZillabaseCreateZfunction metadata = new ZillabaseCreateZfunction(
                    rs.getString("name"),
                    rs.getString("sql")
                );
                schema.addZfunction(metadata);
            }
        }
        catch (SQLException e)
        {
            System.out.println("Failed to fetch zfunctions " + e.getMessage());
        }
    }

    private void applyTables(
        ZillabaseDatabaseSchema schema)
    {
        try
        {
            DatabaseMetaData meta = connection.getMetaData();
            ResultSet tables = meta.getTables(null, null, "%", new String[] {"TABLE"});

            while (tables.next())
            {
                String tableName = tables.getString("TABLE_NAME");

                System.out.println(tableName);
            }
        }
        catch (SQLException e)
        {
            System.out.println("Failed to fetch tables " + e.getMessage());
        }
    }

    private void compareCreateZtable(
        ZillabaseDatabaseSchema actual,
        ZillabaseDatabaseSchema expected,
        ZillabaseSchemaDiff diff)
    {
        actual.ztables().stream()
            .filter(z -> expected.ztables().stream().noneMatch(e -> e.name().equals(z.name())))
            .forEach(z -> diff.addDifference(z.sql()));
    }

    private void compareCreateZview(
        ZillabaseDatabaseSchema actual,
        ZillabaseDatabaseSchema expected,
        ZillabaseSchemaDiff diff)
    {
        actual.zviews().stream()
            .filter(z -> expected.zviews().stream().noneMatch(e -> e.name().equals(z.name())))
            .forEach(z -> diff.addDifference(z.sql()));
    }

    private void compareCreateZfunction(
        ZillabaseDatabaseSchema actual,
        ZillabaseDatabaseSchema expected,
        ZillabaseSchemaDiff diff)
    {
        actual.zfunctions().stream()
            .filter(z -> expected.zfunctions().stream().noneMatch(e -> e.name().equals(z.name())))
            .forEach(z -> diff.addDifference(z.sql()));
    }

    private ZillabaseMigrationFile parseFile(
        Path filePath)
    {
        String fileName = filePath.getFileName().toString();

        String version = fileMatcher.reset(fileName).matches()
            ? fileMatcher.group("number")
            : "000000";
        String description = fileMatcher.group("description");

        String sqlContent = "";
        String checksum = "";

        try
        {
            sqlContent = Files.readString(filePath, UTF_8);
            checksum = computeSHA256Checksum(filePath);
        }
        catch (Exception e)
        {
            System.out.println("Failed to read migration " + e.getMessage());
        }

        return new ZillabaseMigrationFile(version, description, fileName, sqlContent, checksum);
    }

    private static List<String> splitSQL(
        String sql)
    {
        List<String> result = new ArrayList<>();
        StringBuilder command = new StringBuilder();
        boolean insideDollarBlock = false;

        sql = removeSqlComments(sql);

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

    private static String removeSqlComments(
        String sql)
    {
        return sql.replaceAll("(?s)/\\*.*?\\*/", "")
            .replaceAll("--.*?(?=\\r?\\n|$)", "")
            .trim();
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
}
