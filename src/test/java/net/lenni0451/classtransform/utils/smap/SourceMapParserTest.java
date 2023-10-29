package net.lenni0451.classtransform.utils.smap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SourceMapParserTest {

    @Test
    void parseAndCompare() {
        String[] smap = {
                "SMAP", //SMAP header
                "Hi.java",
                "Java",

                "*O Embed", //Embedded section
                "SMAP", //Embedded SMAP header
                "Hi.bar",
                "Java",
                "*S Foo",
                "*F",
                "1 Hi.foo",
                "*L",
                "1#1,5:1,2",
                "*E", //Embedded SMAP end

                "SMAP", //Embedded SMAP header
                "Incl.bar",
                "Java",
                "*S Foo",
                "*F",
                "1 Incl.foo",
                "*L",
                "1#1,2:1,2",
                "*E", //Embedded SMAP end
                "*C Embed", //Embedded section end

                "*S Foo", //Section 'Foo'
                "*F", //File section
                "+ 1 Hi.foo",
                "source.java",
                "2 Incl.foo",

                "*L", //Line section
                "1#1,1:1,1",
                "2#1,4:6,2",
                "1#2,2:2,2",

                "*S Bar", //Section 'Bar'
                "*F", //File section
                "1 Hi.bar",
                "2 Incl.bar",

                "*L", //Line section
                "1#1:1",
                "1#2,4:2",
                "3#1,8:6",

                "*V", //Vendor section
                "Vendor line 1",
                "Vendor line 2",
                "*E", //SMAP end
        };
        String joined = String.join("\n", smap);

        SourceMap sourceMap = SourceMapParser.parse(joined);
        assertNotNull(sourceMap);
        assertEquals(sourceMap.toString().trim(/*Remove the trailing newline*/), joined);
    }

}
