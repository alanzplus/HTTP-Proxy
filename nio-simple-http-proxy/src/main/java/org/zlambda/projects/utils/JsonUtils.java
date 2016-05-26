package org.zlambda.projects.utils;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;

public enum JsonUtils {
  ;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
      .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
      .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
      .setSerializationInclusion(JsonInclude.Include.NON_NULL)
      .configure(SerializationFeature.INDENT_OUTPUT, true)
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  public static String serialize(Object obj) throws Exception {
    return OBJECT_MAPPER.writeValueAsString(obj);
  }

  public static <T> T deserialize(String json, Class<T> type) throws IOException {
    return OBJECT_MAPPER.readValue(json, type);
  }
}
