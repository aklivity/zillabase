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

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import io.aklivity.zillabase.cli.internal.commands.ZillabaseCommand;
import io.aklivity.zillabase.cli.internal.migrations.comparator.ZillabaseSchemaComparator;
import io.aklivity.zillabase.cli.internal.migrations.model.ZillabaseDatabaseSchema;
import io.aklivity.zillabase.cli.internal.migrations.model.ZillabaseMigrationFile;
import io.aklivity.zillabase.cli.internal.migrations.model.ZillabaseMigrationMetadata;
import io.aklivity.zillabase.cli.internal.migrations.model.ZillabaseSchemaDiff;
import io.aklivity.zillabase.cli.internal.migrations.parser.SchemaParser;
import io.aklivity.zillabase.cli.internal.migrations.repository.ZillabaseDatabaseSchemaRepository;
import io.aklivity.zillabase.cli.internal.migrations.repository.ZillabaseMigrationFileRepository;
import io.aklivity.zillabase.cli.internal.migrations.repository.ZillabaseMigrationMetadataRepository;

public final class ZillabaseMigrationService
{
    private static final Path MIGRATIONS_PATH = ZillabaseCommand.ZILLABASE_PATH.resolve("migrations");

    private final ZillabaseDatabaseSchemaRepository zillabaseDatabaseSchemaRepository;
    private final ZillabaseMigrationMetadataRepository metadataRepository;
    private final ZillabaseMigrationFileRepository fileRepository;
    private final SchemaParser schemaParser;
    private final ZillabaseSchemaComparator schemaComparator;

    public ZillabaseMigrationService(
        int port,
        String db)
    {
        this.zillabaseDatabaseSchemaRepository = new ZillabaseDatabaseSchemaRepository(port, db);
        this.metadataRepository = new ZillabaseMigrationMetadataRepository(port, db);

        this.fileRepository = new ZillabaseMigrationFileRepository(MIGRATIONS_PATH);
        this.schemaParser = new SchemaParser();
        this.schemaComparator = new ZillabaseSchemaComparator();
    }

    public String newEmptyMigrationFile(
        String name) throws IOException
    {
        return saveNewMigrationFile(name, null);
    }

    public String saveNewMigrationFile(
        String name,
        String content) throws IOException
    {
        int nextVersion = fileRepository.findNextMigrationVersion();

        String versionStr = String.format("%06d", nextVersion);
        String safeName = sanitizeName(name);
        String fileName = versionStr + "__" + safeName + ".sql";

        String actualContent;
        if (content == null || content.isBlank())
        {
            actualContent = "-- Write your SQL here\n";
        }
        else
        {
            actualContent = content;
        }

        return fileRepository.createNewMigrationFile(fileName, actualContent);
    }

    public List<ZillabaseMigrationMetadata> loadMigrationsMetadata()
    {
        return metadataRepository.loadMetadata();
    }

    public List<ZillabaseMigrationFile> allMigrationFiles()
    {
        return fileRepository.listAllMigrationFiles();
    }

    public List<ZillabaseMigrationFile> unappliedFiles()
    {
        return allMigrationFiles().stream()
            .filter(f -> !metadataRepository.isVersionApplied(f.version()))
            .collect(Collectors.toList());
    }

    public String databaseDiff()
    {
        ZillabaseDatabaseSchema from = zillabaseDatabaseSchemaRepository.loadActualSchema();
        ZillabaseDatabaseSchema to = buildExpectedSchema();

        ZillabaseSchemaDiff diff = schemaComparator.compareSchemas(from, to);

        return diff.generatePatchScript();
    }

    private ZillabaseDatabaseSchema buildExpectedSchema()
    {
        ZillabaseDatabaseSchema expectedSchema = new ZillabaseDatabaseSchema();
        List<ZillabaseMigrationFile> allFiles = allMigrationFiles();
        for (ZillabaseMigrationFile file : allFiles)
        {
            if (metadataRepository.isVersionApplied(file.version()))
            {
                schemaParser.parseCreateStatements(expectedSchema, file.sqlContents());
            }
        }
        return expectedSchema;
    }

    private String sanitizeName(
        String name)
    {
        return name.trim().replaceAll("[^a-zA-Z0-9_\\-]+", "_");
    }
}
