package io.aklivity.zillabase.service.api.gen.internal.asyncapi.zilla;

import java.util.Map;
import java.util.function.Function;

import io.aklivity.zillabase.service.api.gen.internal.asyncapi.Builder;

public class ZillaAsyncApiConfigBuilder<T> extends Builder<T, ZillaAsyncApiConfigBuilder<T>>
{
    private final Function<ZillaAsyncApiConfig, T> mapper;

    private String name;
    private Map<String, ZillaCatalogConfig> catalogs;
    private Map<String, ZillaGuardConfig> guards;
    private Map<String, ZillaBindingConfig> bindings;
    private Map<String, Object> telemetry;


    public ZillaAsyncApiConfigBuilder(Function<ZillaAsyncApiConfig, T> mapper)
    {
        this.mapper = mapper;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<ZillaAsyncApiConfigBuilder<T>> thisType()
    {
        return (Class<ZillaAsyncApiConfigBuilder<T>>) getClass();
    }

    public ZillaAsyncApiConfigBuilder<T> name(
        String name)
    {
        this.name = name;
        return this;
    }

    public ZillaAsyncApiConfigBuilder<T> catalogs(
        Map<String, ZillaCatalogConfig> catalogs)
    {
        this.catalogs = catalogs;
        return this;
    }

    public ZillaAsyncApiConfigBuilder<T> guards(
        Map<String, ZillaGuardConfig> guards)
    {
        this.guards = guards;
        return this;
    }

    public ZillaAsyncApiConfigBuilder<T> bindings(
        Map<String, ZillaBindingConfig> bindings)
    {
        this.bindings = bindings;
        return this;
    }

    public ZillaAsyncApiConfigBuilder<T> telemetry(
        Map<String, Object> telemetry)
    {
        this.telemetry = telemetry;
        return this;
    }

    public ZillaAsyncApiConfigBuilder<T> addCatalog(
        String name,
        ZillaCatalogConfig catalog)
    {
        if (catalogs == null)
        {
            catalogs = Map.of(name, catalog);
        }
        else
        {
            catalogs.put(name, catalog);
        }
        return this;
    }

    @Override
    public T build()
    {
        ZillaAsyncApiConfig zilla = new ZillaAsyncApiConfig(
            name,
            catalogs,
            guards,
            bindings,
            telemetry);

        return mapper.apply(zilla);
    }
}
