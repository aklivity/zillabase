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
package io.aklivity.zillabase.cli.internal.commands.migration.list;

import java.io.IOException;
import java.util.stream.Stream;

import com.github.rvesse.airline.annotations.Command;

import io.aklivity.zillabase.cli.internal.commands.migration.ZillabaseMigrationCommand;

@Command(
    name = "list",
    description = "Lists local migrations")
public final class ZillabaseMigrationListCommand extends ZillabaseMigrationCommand
{
    @Override
    protected void invoke()
    {
        try
        {
            Stream<String> migrations = listMigrations();

            migrations.forEach(System.out::println);
        }
        catch (IOException ex)
        {
            ex.printStackTrace(System.err);
        }
    }
}
