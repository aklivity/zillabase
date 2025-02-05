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
}
