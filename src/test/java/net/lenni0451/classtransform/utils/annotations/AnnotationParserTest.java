package net.lenni0451.classtransform.utils.annotations;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import net.lenni0451.classtransform.utils.tree.ClassTree;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnnotationParserTest {

    @Test
    @DisplayName("Convert list to map")
    public void convertListToMap() {
        List<Object> list = new ArrayList<>();
        list.add("test1");
        list.add(123);
        list.add("test2");
        list.add(true);

        Map<String, Object> map = AnnotationUtils.listToMap(list);
        assertTrue(map.containsKey("test1"));
        assertTrue(map.containsKey("test2"));
        assertEquals(123, map.get("test1"));
        assertEquals(true, map.get("test2"));
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface TestAnnotation {
        int[] ints() default {};

        long[] longs() default {};

        String[] strings() default {};

        int single() default 0;
    }

    @Test
    @DisplayName("Parse annotation with primitive array members")
    public void parsePrimitiveArrayMembers() {
        //ASM decodes int[] as List<Integer> — previously VerifyError
        //in the generated AnnotationWrapper (ANEWARRAY/AASTORE on ints)
        Map<String, Object> values = new HashMap<>();
        values.put("ints", Arrays.asList(1, 2, 3));
        values.put("longs", Arrays.asList(4L, 5L));
        values.put("strings", Arrays.asList("a", "b"));
        values.put("single", 42);

        TestAnnotation annotation = AnnotationParser.parse(
                TestAnnotation.class,
                new ClassTree(),
                new BasicClassProvider(),
                values);

        assertTrue(Arrays.equals(new int[]{1, 2, 3}, annotation.ints()));
        assertTrue(Arrays.equals(new long[]{4L, 5L}, annotation.longs()));
        assertTrue(Arrays.equals(new String[]{"a", "b"}, annotation.strings()));
        assertEquals(42, annotation.single());
    }

    @Test
    @DisplayName("Convert map to list")
    public void convertMapToList() {
        Map<String, Object> map = new HashMap<>();
        map.put("test1", 123);
        map.put("test2", true);

        List<Object> list = AnnotationUtils.mapToList(map);
        assertTrue(list.contains("test1"));
        assertTrue(list.contains("test2"));
        assertTrue(list.contains(123));
        assertTrue(list.contains(true));
    }

}
