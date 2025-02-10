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
package io.aklivity.zillabase.cli.internal.migrations.comparator;

import java.util.ArrayList;
import java.util.List;

import io.aklivity.zillabase.cli.internal.migrations.model.ZillabaseDatabaseSchema;
import io.aklivity.zillabase.cli.internal.migrations.model.ZillabaseSchemaDiff;

public final class ZillabaseSchemaComparator
{
    @FunctionalInterface
    private interface SchemaComparisonStrategy
    {
        void compare(
            ZillabaseDatabaseSchema actual,
            ZillabaseDatabaseSchema expected,
            ZillabaseSchemaDiff diff);
    }

    private final List<SchemaComparisonStrategy> strategies;
    {
        List<SchemaComparisonStrategy> strategies = new ArrayList<>();
        strategies.add(this::compareZtables);
        strategies.add(this::compareZfunctions);
        strategies.add(this::compareZviews);
        strategies.add(this::compareTables);
        strategies.add(this::compareMaterializedViews);
        strategies.add(this::compareViews);
        this.strategies = strategies;
    }

    public ZillabaseSchemaDiff compareSchemas(
        ZillabaseDatabaseSchema actual,
        ZillabaseDatabaseSchema expected)
    {
        ZillabaseSchemaDiff diff = new ZillabaseSchemaDiff();

        strategies.forEach(strategy -> strategy.compare(actual, expected, diff));
        strategies.forEach(strategy -> strategy.compare(expected, actual, diff));

        return diff;
    }

    private void compareZtables(
        ZillabaseDatabaseSchema from,
        ZillabaseDatabaseSchema to,
        ZillabaseSchemaDiff diff)
    {
        from.ztables().stream()
            .filter(z -> to.ztables().stream().noneMatch(e -> e.name().equals(z.name())))
            .forEach(z -> diff.addDifference(z.sql()));
    }

    private void compareZviews(
        ZillabaseDatabaseSchema from,
        ZillabaseDatabaseSchema to,
        ZillabaseSchemaDiff diff)
    {
        from.zviews().stream()
            .filter(z -> to.zviews().stream().noneMatch(e -> e.name().equals(z.name())))
            .forEach(z -> diff.addDifference(z.sql()));
    }

    private void compareZfunctions(
        ZillabaseDatabaseSchema from,
        ZillabaseDatabaseSchema to,
        ZillabaseSchemaDiff diff)
    {
        from.zfunctions().stream()
            .filter(z -> to.zfunctions().stream().noneMatch(e -> e.name().equals(z.name())))
            .forEach(z -> diff.addDifference(z.sql()));
    }

    private void compareTables(
        ZillabaseDatabaseSchema from,
        ZillabaseDatabaseSchema to,
        ZillabaseSchemaDiff diff)
    {
        from.tables().stream()
            .filter(t -> from.ztables().stream().noneMatch(z -> z.name().equals(t.name())))
            .filter(t -> to.tables().stream().noneMatch(e -> e.name().equals(t.name())))
            .forEach(t -> diff.addDifference(t.sql()));
    }

    private void compareMaterializedViews(
        ZillabaseDatabaseSchema from,
        ZillabaseDatabaseSchema to,
        ZillabaseSchemaDiff diff)
    {
        from.materializedViews().stream()
            .filter(mv -> from.zviews().stream().noneMatch(z -> z.name().equals(mv.name())))
            .filter(mv -> to.materializedViews().stream().noneMatch(e -> e.name().equals(mv.name())))
            .forEach(mv -> diff.addDifference(mv.sql()));
    }

    private void compareViews(
        ZillabaseDatabaseSchema from,
        ZillabaseDatabaseSchema to,
        ZillabaseSchemaDiff diff)
    {
        from.views().stream()
            .filter(v -> to.views().stream().noneMatch(e -> e.name().equals(v.name())))
            .forEach(v -> diff.addDifference(v.sql()));
    }
}

