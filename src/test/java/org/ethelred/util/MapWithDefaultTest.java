package org.ethelred.util;

import java.util.Map;

import com.google.common.base.Supplier;
import com.google.common.collect.Maps;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

public class MapWithDefaultTest
{
    @Test
    public void basicTest()
    {
        Map<String, String> ms = Maps.newHashMap();
        assertThat(ms.get("key")).isNull();
        Supplier<String> def = () -> "def";
        ms = MapWithDefault.wrap(ms, def);
        assertThat(ms.containsKey("key")).isFalse();
        assertThat(ms.get("key")).isEqualTo("def");
        assertThat(ms.containsKey("key")).isTrue();
    }
}
