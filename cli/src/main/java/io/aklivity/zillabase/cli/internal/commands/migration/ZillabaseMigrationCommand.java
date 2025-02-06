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
package io.aklivity.zillabase.cli.internal.commands.migration;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.stream.Stream;

import com.github.rvesse.airline.annotations.Command;

import io.aklivity.zillabase.cli.config.ZillabaseConfig;
import io.aklivity.zillabase.cli.internal.commands.ZillabaseCommand;
import io.aklivity.zillabase.cli.internal.migrations.ZillabaseMigrationsApplyHelper;
import io.aklivity.zillabase.cli.internal.migrations.ZillabaseMigrationsDiffHelper;
import io.aklivity.zillabase.cli.internal.migrations.model.ZillabaseMigrationFile;

public abstract class ZillabaseMigrationCommand extends ZillabaseCommand
{
    protected static final Path MIGRATIONS_PATH = ZillabaseMigrationsDiffHelper.MIGRATIONS_PATH;

    protected static final String MIGRATION_FILE_FORMAT = ZillabaseMigrationsDiffHelper.MIGRATION_FILE_FORMAT;

    protected final Matcher matcher;

    protected final ZillabaseMigrationsDiffHelper migrationDiff;
    protected final ZillabaseMigrationsApplyHelper migrationApply;

    protected ZillabaseMigrationCommand()
    {
        ZillabaseConfig config = new ZillabaseConfig();
        String dbName = config.risingwave.db;
        this.migrationDiff = new ZillabaseMigrationsDiffHelper(dbName);
        this.migrationApply = new ZillabaseMigrationsApplyHelper(migrationDiff, dbName);
        this.matcher = migrationDiff.fileMatcher;
    }

    protected final Stream<String> listMigrations() throws IOException
    {
        return migrationDiff.allMigrationFiles().stream().map(ZillabaseMigrationFile::scriptName);
    }

    protected String saveNewMigrationFile(
        String name,
        String content) throws IOException
    {
        Optional<String> latest = listMigrations().reduce((n1, n2) -> n2);

        int next = latest.isPresent() && matcher.reset(latest.get()).matches()
            ? Integer.parseInt(matcher.group("number")) + 1
            : 0;

        String filename = MIGRATION_FILE_FORMAT.formatted(next, name);
        Path newMigration = MIGRATIONS_PATH.resolve(filename);

        Files.createDirectories(MIGRATIONS_PATH);
        Path path = Files.createFile(newMigration);

        if (content != null)
        {
            Files.writeString(path, content, UTF_8, CREATE, TRUNCATE_EXISTING);
        }

        return filename;
    }

    @Command(
        name = "help",
        hidden = true)
    public static final class Help<T> extends com.github.rvesse.airline.help.Help<T>
    {
        public Help()
        {
            this.command.add("migration");
        }
    }
}
