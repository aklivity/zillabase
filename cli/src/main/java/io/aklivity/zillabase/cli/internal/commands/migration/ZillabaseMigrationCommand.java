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

import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.stream.Stream;

import com.github.rvesse.airline.annotations.Command;

import io.aklivity.zillabase.cli.config.ZillabaseConfig;
import io.aklivity.zillabase.cli.internal.commands.ZillabaseCommand;
import io.aklivity.zillabase.cli.internal.migrations.ZillabaseMigrationsDiffHelper;
import io.aklivity.zillabase.cli.internal.migrations.model.ZillabaseMigrationFile;

public abstract class ZillabaseMigrationCommand extends ZillabaseCommand
{
    protected static final Path MIGRATIONS_PATH = ZillabaseMigrationsDiffHelper.MIGRATIONS_PATH;

    protected static final String MIGRATION_FILE_FORMAT = ZillabaseMigrationsDiffHelper.MIGRATION_FILE_FORMAT;

    protected final Matcher matcher;

    protected final ZillabaseMigrationsDiffHelper migrationDiff;

    protected ZillabaseMigrationCommand()
    {
        ZillabaseConfig config = new ZillabaseConfig();
        this.migrationDiff = new ZillabaseMigrationsDiffHelper(config.risingwave.db);
        this.matcher = migrationDiff.fileMatcher;
    }

    protected final Stream<String> listMigrations() throws IOException
    {
        return migrationDiff.allMigrationFiles().stream().map(ZillabaseMigrationFile::scriptName);
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
