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

import static com.google.common.collect.ImmutableList.of;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.aklivity.zillabase.cli.internal.migrations.model.ZillabaseMigrationFile;

public final class MigrationFileRepository
{
    public static final Pattern MIGRATION_FILE_PATTERN =
        Pattern.compile("(?<number>\\d{6})__(?<description>.+)\\.sql");

    private final Path migrationsPath;

    public MigrationFileRepository(
        Path migrationsPath)
    {
        this.migrationsPath = migrationsPath;
    }

    public int findNextMigrationVersion() throws IOException
    {
        int nextVersion = 1;

        if (!Files.exists(migrationsPath))
        {
            Files.createDirectories(migrationsPath);
        }
        else
        {
            try (Stream<Path> stream = Files.list(migrationsPath))
            {
                Optional<Integer> maxVersion = stream
                    .filter(Files::isRegularFile)
                    .map(path -> MIGRATION_FILE_PATTERN.matcher(path.getFileName().toString()))
                    .filter(Matcher::matches)
                    .map(matcher -> Integer.parseInt(matcher.group("number")))
                    .max(Comparator.naturalOrder());

                nextVersion = maxVersion.map(v -> v + 1).orElse(1);
            }
        }

        return nextVersion;
    }

    public String createNewMigrationFile(
        String fileName,
        String sqlContent) throws IOException
    {
        if (!Files.exists(migrationsPath))
        {
            Files.createDirectories(migrationsPath);
        }

        Path newFile = migrationsPath.resolve(fileName);

        Files.writeString(newFile, sqlContent, CREATE, TRUNCATE_EXISTING);

        return newFile.toString();
    }

    public List<ZillabaseMigrationFile> listAllMigrationFiles()
    {
        List<ZillabaseMigrationFile> files = of();
        try
        {
            files = Files.list(migrationsPath)
                .sorted()
                .filter(Files::isRegularFile)
                .filter(this::matchesPattern)
                .map(this::parseFile)
                .collect(Collectors.toList());
        }
        catch (IOException ex)
        {
            System.err.println("Failed to list migration files");
        }

        return files;
    }

    private boolean matchesPattern(
        Path path)
    {
        return MIGRATION_FILE_PATTERN.matcher(path.getFileName().toString()).matches();
    }

    private ZillabaseMigrationFile parseFile(
        Path filePath)
    {
        ZillabaseMigrationFile migration = null;

        String fileName = filePath.getFileName().toString();
        Matcher matcher = MIGRATION_FILE_PATTERN.matcher(fileName);

        String version = "000000";
        String description = "";
        if (matcher.matches())
        {
            version = matcher.group("number");
            description = matcher.group("description");
        }

        try
        {
            String sqlContent = Files.readString(filePath, UTF_8);
            String checksum = computeSHA256Checksum(filePath);
            migration = new ZillabaseMigrationFile(version, description, fileName, sqlContent, checksum);
        }
        catch (Exception e)
        {
            System.err.println("Failed to parse migration file: " + filePath);
        }

        return migration;
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
