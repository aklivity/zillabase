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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import io.aklivity.zillabase.cli.internal.commands.ZillabaseCommand;

public final class ZillabaseMigrationsHelper
{
    public static final Path MIGRATIONS_PATH = ZillabaseCommand.ZILLABASE_PATH.resolve("migrations");

    public static final Pattern MIGRATION_FILE_PATTERN = Pattern.compile("(?<number>\\d{6})__.+\\.sql");
    public static final String MIGRATION_FILE_FORMAT = "%06d__%s.sql";

    public final Matcher matcher = MIGRATION_FILE_PATTERN.matcher("");

    public Stream<Path> list()
    {
        Stream<Path> migrations;

        try
        {
            migrations = Files.list(MIGRATIONS_PATH)
                .sorted()
                .filter(Files::isRegularFile)
                .filter(n -> matcher.reset(n.getFileName().toString()).matches());
        }
        catch (IOException ex)
        {
            migrations = Stream.empty();
        }

        return migrations;
    }
}
