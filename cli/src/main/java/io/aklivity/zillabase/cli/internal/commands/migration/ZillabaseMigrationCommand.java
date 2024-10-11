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
import java.util.regex.Pattern;
import java.util.stream.Stream;

import io.aklivity.zillabase.cli.internal.commands.ZillabaseCommand;
import io.aklivity.zillabase.cli.internal.migrations.ZillabaseMigrationsHelper;

public abstract class ZillabaseMigrationCommand extends ZillabaseCommand
{
    protected static final Path MIGRATIONS_PATH = ZillabaseMigrationsHelper.MIGRATIONS_PATH;

    protected static final Pattern MIGRATION_FILE_PATTERN = ZillabaseMigrationsHelper.MIGRATION_FILE_PATTERN;
    protected static final String MIGRATION_FILE_FORMAT = ZillabaseMigrationsHelper.MIGRATION_FILE_FORMAT;

    protected final Matcher matcher;

    private final ZillabaseMigrationsHelper helper;

    protected ZillabaseMigrationCommand()
    {
        this.helper = new ZillabaseMigrationsHelper();
        this.matcher = helper.matcher;
    }

    protected final Stream<String> listMigrations() throws IOException
    {
        return helper.list()
            .map(p -> p.getFileName().toString());
    }
}
