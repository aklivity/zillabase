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
package io.aklivity.zillabase.cli.internal.commands.migration.add;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import com.github.rvesse.airline.annotations.Arguments;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.restrictions.Required;

import io.aklivity.zillabase.cli.internal.commands.migration.ZillabaseMigrationCommand;

@Command(
    name = "add",
    description = "Creates a new migration file locally")
public final class ZillabaseMigrationAddCommand extends ZillabaseMigrationCommand
{
    @Arguments(title = { "name" })
    @Required
    public List<String> args;

    @Override
    protected void invoke()
    {
        try
        {
            String name = args.get(0);

            Optional<String> latest = listMigrations().reduce((n1, n2) -> n2);

            int next = latest.isPresent() && matcher.reset(latest.get()).matches()
                ? Integer.parseInt(matcher.group("number")) + 1
                : 0;

            String filename = MIGRATION_FILE_FORMAT.formatted(next, name);
            Path newMigration = MIGRATIONS_PATH.resolve(filename);

            Files.createDirectories(MIGRATIONS_PATH);
            Files.createFile(newMigration);

            System.out.printf("Created migration %s\n", filename);
        }
        catch (IOException ex)
        {
            ex.printStackTrace(System.err);
        }
    }
}
