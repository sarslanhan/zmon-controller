package org.zalando.zmon.transformer;

import java.util.Map;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.zalando.zmon.domain.Parameter;
import org.zalando.zmon.transformer.JsonTransformer;

import com.google.common.collect.ImmutableMap;

public class JsonTransformerTest {

    private final JsonTransformer jsonTransformer = new JsonTransformer();

    @Test
    public void testUnmarshalNull() throws Exception {
        MatcherAssert.assertThat(jsonTransformer.unmarshalFromDb(null), Matchers.nullValue());
    }

    @Test
    public void testUnmarshalFromDB() throws Exception {
        final String hstore =
            "\"param0\"=>\"{\\\"value\\\":0,\\\"comment\\\":\\\"desc 0\\\",\\\"type\\\":\\\"int\\\"}\", "
                + "\"param1\"=>\"{\\\"value\\\":1,\\\"comment\\\":\\\"desc 1\\\",\\\"type\\\":\\\"float\\\"}\"";

        final Map<String, Parameter> expected = ImmutableMap.of("param0", new Parameter(0, "desc 0", "int"), "param1",
                new Parameter(1, "desc 1", "float"));

        MatcherAssert.assertThat(jsonTransformer.unmarshalFromDb(hstore), Matchers.is(expected));
    }

    @Test
    public void testMarshalNull() throws Exception {
        MatcherAssert.assertThat(jsonTransformer.marshalToDb(null), Matchers.nullValue());
    }

    @Test
    public void testMarshalToDb() throws Exception {

        final Map<String, Parameter> input = ImmutableMap.of("param0", new Parameter(0, "desc 0", "int"), "param1",
                new Parameter(1, "desc 1", "float"));

        final String result0 =
            "\"param0\"=>\"{\\\"value\\\":0,\\\"comment\\\":\\\"desc 0\\\",\\\"type\\\":\\\"int\\\"}\","
                + "\"param1\"=>\"{\\\"value\\\":1,\\\"comment\\\":\\\"desc 1\\\",\\\"type\\\":\\\"float\\\"}\"";

        final String result1 =
            "\"param1\"=>\"{\\\"value\\\":1,\\\"comment\\\":\\\"desc 1\\\",\\\"type\\\":\\\"float\\\"}\","
                + "\"param0\"=>\"{\\\"value\\\":0,\\\"comment\\\":\\\"desc 0\\\",\\\"type\\\":\\\"int\\\"}\"";

        MatcherAssert.assertThat(jsonTransformer.marshalToDb(input),
            Matchers.anyOf(Matchers.is(result0), Matchers.is(result1)));
    }
}
