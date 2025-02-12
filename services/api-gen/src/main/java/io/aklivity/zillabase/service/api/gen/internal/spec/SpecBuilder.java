package io.aklivity.zillabase.service.api.gen.internal.spec;

import java.util.function.Function;

public abstract class SpecBuilder<T, B extends SpecBuilder<T, B>>
{
    protected abstract Class<B> thisType();

    public final <R> R inject(Function<B, R> visitor)
    {
        return visitor.apply(thisType().cast(this));
    }

    public abstract T build();
}
