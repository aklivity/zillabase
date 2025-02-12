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
package io.aklivity.zillabase.service.api.gen.internal.builder;

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
