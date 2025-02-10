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

import java.util.List;

import io.aklivity.zillabase.cli.internal.migrations.model.ZillabaseMigrationFile;
import io.aklivity.zillabase.cli.internal.migrations.repository.MigrationMetadataRepository;
import io.aklivity.zillabase.cli.internal.migrations.repository.ZillabaseSqlExecutor;

public final class ZillabaseMigrationApplier
{
    private final ZillabaseSqlExecutor sqlExecutor;
    private final MigrationMetadataRepository metadataRepository;

    public ZillabaseMigrationApplier(
        String dbName)
    {
        this.sqlExecutor = new ZillabaseSqlExecutor(dbName);
        this.metadataRepository = new MigrationMetadataRepository(dbName);
    }

    public void apply(
        List<ZillabaseMigrationFile> unappliedFiles)
    {
        for (ZillabaseMigrationFile file : unappliedFiles)
        {
            apply(file);
        }
    }

    private void apply(
        ZillabaseMigrationFile migration)
    {
        try
        {
            sqlExecutor.execute(migration.sqlContents());
            metadataRepository.recordMigration(migration);
        }
        catch (Exception ex)
        {
            System.err.format("Failed to apply migration %s: %s\n",
                migration.scriptName(), ex.getMessage());
        }
    }
}
