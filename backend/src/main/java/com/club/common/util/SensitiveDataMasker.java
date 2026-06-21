package com.club.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SensitiveDataMasker {

    private static final Set<String> SENSITIVE_KEYS = new HashSet<>(Arrays.asList(
            "password", "passwd", "pwd", "secret",
            "token", "access_token", "accesstoken", "refreshtoken", "refresh_token",
            "authorization", "auth_token", "authtoken",
            "realname", "real_name", "truename", "true_name",
            "studentid", "student_id", "stuid", "stu_id",
            "idcard", "id_card", "idnumber", "id_number", "identity",
            "phone", "mobile", "tel", "telephone", "phonenumber", "phone_number",
            "email", "mail", "e_mail",
            "qq", "qqnumber", "qq_number", "wechat", "weixin",
            "resumetext", "resume_text", "resume", "introduction", "selfintroduction",
            "newpassword", "new_password", "oldpassword", "old_password",
            "confirmpassword", "confirm_password", "repassword", "re_password"
    ));

    private static final String MASK_VALUE = "***";

    private static final int STACK_TRACE_MAX_LENGTH = 2000;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Pattern PHONE_PATTERN = Pattern.compile("(?:(?<![0-9])1[3-9]\\d{9}(?![0-9]))");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("(?:(?<![0-9])[1-9]\\d{5}(?:19|20)\\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx](?![0-9]))");

    private static final Set<Class<?>> SIMPLE_TYPES = new HashSet<>(Arrays.asList(
            String.class, Integer.class, Long.class, Short.class, Byte.class,
            Double.class, Float.class, Boolean.class, Character.class,
            int.class, long.class, short.class, byte.class,
            double.class, float.class, boolean.class, char.class
    ));

    public static String maskParams(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        try {
            List<Object> maskedList = new ArrayList<>();
            for (Object arg : args) {
                maskedList.add(maskObject(arg));
            }
            return toJsonString(maskedList);
        } catch (Exception e) {
            try {
                return maskStringByPattern(Arrays.toString(args));
            } catch (Exception ex) {
                return "[]";
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static Object maskObject(Object obj) {
        if (obj == null) {
            return null;
        }

        Class<?> clazz = obj.getClass();
        if (isSimpleType(clazz)) {
            if (obj instanceof String) {
                return maskStringValue((String) obj);
            }
            return obj;
        }

        if (obj instanceof Map) {
            return maskMap((Map<String, Object>) obj);
        }

        if (obj instanceof List) {
            return maskList((List<Object>) obj);
        }

        if (obj instanceof Collection) {
            return maskList(new ArrayList<>((Collection<?>) obj));
        }

        if (obj.getClass().isArray()) {
            return maskList(Arrays.asList((Object[]) obj));
        }

        if (isSkippableType(clazz)) {
            return "<" + clazz.getSimpleName() + ">";
        }

        try {
            Map<String, Object> map = objectMapper.convertValue(obj, new TypeReference<Map<String, Object>>() {});
            return maskMap(map);
        } catch (Exception e) {
            String str = obj.toString();
            return maskStringByPattern(str);
        }
    }

    private static boolean isSimpleType(Class<?> clazz) {
        return SIMPLE_TYPES.contains(clazz) || clazz.isEnum() || clazz.isPrimitive();
    }

    private static boolean isSkippableType(Class<?> clazz) {
        String className = clazz.getName();
        return className.startsWith("jakarta.servlet.")
                || className.startsWith("javax.servlet.")
                || className.startsWith("org.springframework.web.")
                || className.contains("HttpServletRequest")
                || className.contains("HttpServletResponse")
                || className.contains("MultipartFile");
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
        if (key == null || key.isEmpty()) {
            return false;
        }
        String normalized = key.toLowerCase().replace("-", "").replace("_", "");
        for (String sensitive : SENSITIVE_KEYS) {
            if (normalized.contains(sensitive)) {
                return true;
            }
        }
        return false;
    }

    public static String maskStringValue(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return maskContent(maskStringByPattern(str));
    }

    private static String toJsonString(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return maskStringByPattern(String.valueOf(obj));
        }
    }

    private static boolean isJsonString(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        String trimmed = str.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}"))
                || (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }

    @SuppressWarnings("unused")
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
            "(?i)(\"(?:password|passwd|pwd|token|auth[^,\\\"\\\\]*|authorization|realName|real_name|studentId|student_id|idCard|id_card|phone|mobile|tel|email|resume[^,\\\"\\\\]*|secret)\"\\s*[:=]\\s*\")([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)(\")"
    );

    private static final Pattern TOSTRING_FIELD_PATTERN = Pattern.compile(
            "(?i)((?:password|passwd|pwd|token|auth\\w*|authorization|realName|real_name|studentId|student_id|idCard|id_card|phone|mobile|tel|email|resume\\w*|secret)\\s*[=:]\\s*)([^,\\s\\)\\]]+)"
    );

    private static String maskStringByPattern(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        String result = str;
        Matcher jsonMatcher = SENSITIVE_FIELD_PATTERN.matcher(result);
        StringBuffer sb = new StringBuffer();
        while (jsonMatcher.find()) {
            jsonMatcher.appendReplacement(sb, Matcher.quoteReplacement(jsonMatcher.group(1) + MASK_VALUE + jsonMatcher.group(3)));
        }
        jsonMatcher.appendTail(sb);
        result = sb.toString();

        sb = new StringBuffer();
        Matcher toStringMatcher = TOSTRING_FIELD_PATTERN.matcher(result);
        while (toStringMatcher.find()) {
            toStringMatcher.appendReplacement(sb, Matcher.quoteReplacement(toStringMatcher.group(1) + MASK_VALUE));
        }
        toStringMatcher.appendTail(sb);
        result = sb.toString();

        return result;
    }

    private static String maskContent(String str) {
        if (str == null || str.length() < 7) {
            return str;
        }
        String result = str;
        result = PHONE_PATTERN.matcher(result).replaceAll("***");
        result = ID_CARD_PATTERN.matcher(result).replaceAll("***");
        return result;
    }

    public static String truncateStackTrace(String stackTrace) {
        return truncateStackTrace(stackTrace, STACK_TRACE_MAX_LENGTH);
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
