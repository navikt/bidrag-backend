package no.nav.bidrag.dokument.journalpost.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import no.nav.bidrag.transport.felles.JsonUtilsKt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonMapper {
  private static final Logger LOGGER = LoggerFactory.getLogger(JsonMapper.class);


  public static <T> T fromJsonString(String jsonString, Class<T> valueType){
    try {
      return JsonUtilsKt.getCommonObjectmapper().readValue(jsonString, valueType);
    } catch (Exception e){
      LOGGER.warn("Det skjedde en feil ved deserialisering json string", e);
      return null;
    }
  }

  public static <T> T fromJsonString(String jsonString, TypeReference<T> valueType){
    try {
      return JsonUtilsKt.getCommonObjectmapper().readValue(jsonString, valueType);
    } catch (Exception e){
      LOGGER.warn("Det skjedde en feil ved deserialisering av json string", e);
      return null;
    }
  }

  public static String toJsonString(Object object){
    try {
      return JsonUtilsKt.getCommonObjectmapper().writeValueAsString(object);
    } catch (Exception e){
      LOGGER.warn("Det skjedde en feil ved serialisering av json objekt", e);
      throw new RuntimeException("Det skjedde en feil ved serialisering av json objekt", e);
    }
  }

}
