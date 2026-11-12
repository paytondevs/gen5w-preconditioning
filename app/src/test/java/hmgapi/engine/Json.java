package hmgapi.engine;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Test-only stand-in for the installed HMG-API JSON reader. */
public final class Json {
    public static final Map<String,Map<String,Object>> objects = new HashMap<>();
    public static Map<String,Object> object(String text) {
        Map<String,Object> value = objects.get(text);
        if (value == null) throw new IllegalArgumentException("fixture not registered");
        return new LinkedHashMap<>(value);
    }
}
