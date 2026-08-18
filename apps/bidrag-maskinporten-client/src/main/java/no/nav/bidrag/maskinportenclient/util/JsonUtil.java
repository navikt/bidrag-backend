package no.nav.bidrag.maskinportenclient.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtil {

  public static <T> String objectToJsonString(T object) {
    ObjectMapper mapper = new ObjectMapper();
    String jsonString = "";
    try {
      jsonString = mapper.writeValueAsString(object);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    return jsonString;
  }

  public static <T> T jsonStringToObject(String json, Class<T> objectType) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return mapper.readValue(json, objectType);
  }

}