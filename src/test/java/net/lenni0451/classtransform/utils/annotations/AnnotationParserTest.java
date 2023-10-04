package net.lenni0451.classtransform.utils.annotations;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
