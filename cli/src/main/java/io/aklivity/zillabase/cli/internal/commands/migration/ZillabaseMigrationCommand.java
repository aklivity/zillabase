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
import java.util.stream.Stream;

import com.github.rvesse.airline.annotations.Command;

import io.aklivity.zillabase.cli.config.ZillabaseAdminConfig;
import io.aklivity.zillabase.cli.config.ZillabaseConfig;
import io.aklivity.zillabase.cli.internal.commands.ZillabaseCommand;
import io.aklivity.zillabase.cli.internal.migrations.ZillabaseMigrationApplier;
import io.aklivity.zillabase.cli.internal.migrations.ZillabaseMigrationService;
import io.aklivity.zillabase.cli.internal.migrations.model.ZillabaseMigrationFile;

public abstract class ZillabaseMigrationCommand extends ZillabaseCommand
{
    protected final ZillabaseMigrationService service;
    protected final ZillabaseMigrationApplier applier;

    protected ZillabaseMigrationCommand()
    {
        ZillabaseConfig config = new ZillabaseConfig();
        int port = ZillabaseAdminConfig.DEFAULT_ADMIN_PGSQL_PORT;
        String db = config.risingwave.db;

        this.service = new ZillabaseMigrationService(port, db);
        this.applier = new ZillabaseMigrationApplier(port, db);
    }

    protected final Stream<String> listMigrations() throws IOException
    {
        return service.allMigrationFiles().stream().map(ZillabaseMigrationFile::scriptName);
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
