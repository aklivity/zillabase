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

import io.aklivity.zillabase.cli.internal.migrations.model.ZillabaseCreateZfunction;
import io.aklivity.zillabase.cli.internal.migrations.model.ZillabaseCreateZtable;
import io.aklivity.zillabase.cli.internal.migrations.model.ZillabaseCreateZview;

public class ZillabaseDatabaseSchema
{
    private final List<ZillabaseCreateZtable> ztables;
    private final List<ZillabaseCreateZfunction> zfunctions;
    private final List<ZillabaseCreateZview> zviews;

    public ZillabaseDatabaseSchema()
    {
        this.ztables = new ArrayList<>();
        this.zfunctions = new ArrayList<>();
        this.zviews = new ArrayList<>();
    }

    public void addZtable(
        ZillabaseCreateZtable ztable)
    {
        ztables.add(ztable);
    }

    public void addZfunction(
        ZillabaseCreateZfunction zfunction)
    {
        zfunctions.add(zfunction);
    }

    public void addZview(
        ZillabaseCreateZview zview)
    {
        zviews.add(zview);
    }

    public List<ZillabaseCreateZtable> ztables()
    {
        return ztables;
    }

    public List<ZillabaseCreateZfunction> zfunctions()
    {
        return zfunctions;
    }

    public List<ZillabaseCreateZview> zviews()
    {
        return zviews;
    }

}
