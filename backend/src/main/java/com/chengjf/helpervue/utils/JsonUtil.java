package de.jonashackt.springbootvuejs.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class JsonUtil {

    private static ObjectMapper objectMapper = new ObjectMapper();
    private static DefaultPrettyPrinter.Indenter indenter =
            new DefaultIndenter("\t", DefaultIndenter.SYS_LF);
    private static DefaultPrettyPrinter printer = new DefaultPrettyPrinter()
            .withArrayIndenter(indenter)
            .withObjectIndenter(indenter)
            .createInstance();

    static {
        objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);

    }

    public static String beautifyJson(String jsonStr) throws IOException {
        try {
            Object json = objectMapper.readValue(jsonStr, Object.class);
            String s = objectMapper.writer(printer).writeValueAsString(json);
            return s;
        } catch (IOException e) {
            throw e;
        }
    }

    public static void main(String[] args) throws IOException {
        String str = "{a:1,\"attributes\":[{\"nm\":\"ACCOUNT\",\"lv\":[{\"v\":{\"Id\":null,\"State\":null},\"vt\":\"java.util.Map\",\"cn\":1}],\"vt\":\"java.util.Map\",\"status\":\"SUCCESS\",\"lmd\":13585},{\"nm\":\"PROFILE\",\"lv\":[{\"v\":{\"Party\":null,\"Ads\":null},\"vt\":\"java.util.Map\",\"cn\":2}],\"vt\":\"java.util.Map\",\"status\":\"SUCCESS\",\"lmd\":41962}]}\n";
        String s = JsonUtil.beautifyJson(str);
        System.out.println(s);
    }

    public static String toJsonStr(Object object) throws JsonProcessingException {
        try {
            String s = objectMapper.writer(printer).writeValueAsString(object);
            return s;
        } catch (IOException e) {
            throw e;
        }
    }
}
