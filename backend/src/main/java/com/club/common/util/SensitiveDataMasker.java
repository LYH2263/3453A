package com.club.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SensitiveDataMasker {

    private static final Set<String> SENSITIVE_KEYS = new HashSet<>(Arrays.asList(
            "password", "token", "authorization", "resume_text"
    ));

    private static final String MASK_VALUE = "***";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String maskParams(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        try {
            List<Object> maskedArgs = new ArrayList<>();
            for (Object arg : args) {
                maskedArgs.add(maskObject(arg));
            }
            return Arrays.toString(maskedArgs.toArray());
        } catch (Exception e) {
            return Arrays.toString(args);
        }
    }

    @SuppressWarnings("unchecked")
    public static Object maskObject(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Map) {
            return maskMap((Map<String, Object>) obj);
        }
        if (obj instanceof List) {
            return maskList((List<Object>) obj);
        }
        if (obj instanceof String) {
            String str = (String) obj;
            if (isJsonString(str)) {
                try {
                    Object parsed = parseJson(str);
                    if (parsed instanceof Map) {
                        return objectMapper.writeValueAsString(maskMap((Map<String, Object>) parsed));
                    }
                    if (parsed instanceof List) {
                        return objectMapper.writeValueAsString(maskList((List<Object>) parsed));
                    }
                } catch (Exception e) {
                    return maskStringByPattern(str);
                }
            }
            return maskStringByPattern(str);
        }
        return obj;
    }

    private static Map<String, Object> maskMap(Map<String, Object> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (isSensitiveKey(key)) {
                result.put(key, MASK_VALUE);
            } else {
                result.put(key, maskObject(value));
            }
        }
        return result;
    }

    private static List<Object> maskList(List<Object> list) {
        List<Object> result = new ArrayList<>();
        for (Object item : list) {
            result.add(maskObject(item));
        }
        return result;
    }

    private static boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        String lowerKey = key.toLowerCase().replace("-", "_");
        for (String sensitive : SENSITIVE_KEYS) {
            if (lowerKey.contains(sensitive)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isJsonString(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        String trimmed = str.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}"))
                || (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }

    private static Object parseJson(String str) throws Exception {
        String trimmed = str.trim();
        if (trimmed.startsWith("{")) {
            return objectMapper.readValue(str, new TypeReference<Map<String, Object>>() {});
        }
        if (trimmed.startsWith("[")) {
            return objectMapper.readValue(str, new TypeReference<List<Object>>() {});
        }
        return str;
    }

    private static final Pattern SENSITIVE_FIELD_PATTERN = Pattern.compile(
            "(\"(?:password|token|authorization|resume_text)\"\\s*:\\s*\")([^\"]*)(\")",
            Pattern.CASE_INSENSITIVE
    );

    private static String maskStringByPattern(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        Matcher matcher = SENSITIVE_FIELD_PATTERN.matcher(str);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(1) + MASK_VALUE + matcher.group(3)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static String truncateStackTrace(String stackTrace, int maxLength) {
        if (stackTrace == null) {
            return null;
        }
        if (stackTrace.length() <= maxLength) {
            return stackTrace;
        }
        return stackTrace.substring(0, maxLength) + "... [truncated]";
    }
}
