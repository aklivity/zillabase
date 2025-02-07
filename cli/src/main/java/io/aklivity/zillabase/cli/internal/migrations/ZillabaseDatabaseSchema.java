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

import java.util.ArrayList;
import java.util.List;

import io.aklivity.zillabase.cli.internal.migrations.model.ZillabaseMaterializedView;
import io.aklivity.zillabase.cli.internal.migrations.model.ZillabaseTable;
import io.aklivity.zillabase.cli.internal.migrations.model.ZillabaseZfunction;
import io.aklivity.zillabase.cli.internal.migrations.model.ZillabaseZtable;
import io.aklivity.zillabase.cli.internal.migrations.model.ZillabaseZview;

public class ZillabaseDatabaseSchema
{
    private final List<ZillabaseZtable> ztables;
    private final List<ZillabaseZfunction> zfunctions;
    private final List<ZillabaseZview> zviews;
    private final List<ZillabaseTable> tables;
    private final List<ZillabaseMaterializedView> materializedViews;
    private final List<ZillabaseZview> views;

    public ZillabaseDatabaseSchema()
    {
        this.ztables = new ArrayList<>();
        this.zfunctions = new ArrayList<>();
        this.zviews = new ArrayList<>();
        this.tables = new ArrayList<>();
        this.materializedViews = new ArrayList<>();
        this.views = new ArrayList<>();
    }

    public void addZtable(
        ZillabaseZtable ztable)
    {
        ztables.add(ztable);
    }

    public void addZfunction(
        ZillabaseZfunction zfunction)
    {
        zfunctions.add(zfunction);
    }

    public void addZview(
        ZillabaseZview zview)
    {
        zviews.add(zview);
    }

    public void addTable(
        ZillabaseTable table)
    {
        tables.add(table);
    }

    public void addMaterializedView(
        ZillabaseMaterializedView mview)
    {
        materializedViews.add(mview);
    }

    public void addView(
        ZillabaseZview view)
    {
        views.add(view);
    }

    public List<ZillabaseZtable> ztables()
    {
        return ztables;
    }

    public List<ZillabaseZfunction> zfunctions()
    {
        return zfunctions;
    }

    public List<ZillabaseZview> zviews()
    {
        return zviews;
    }

    public List<ZillabaseTable> tables()
    {
        return tables;
    }

    public List<ZillabaseMaterializedView> materializedViews()
    {
        return materializedViews;
    }

    public List<ZillabaseZview> views()
    {
        return views;
    }
}
