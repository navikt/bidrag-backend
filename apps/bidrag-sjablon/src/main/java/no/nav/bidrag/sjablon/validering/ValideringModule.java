package no.nav.bidrag.sjablon.validering;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBase;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;

public class ValideringModule extends SimpleModule {
  public ValideringModule() {
    setDeserializerModifier(new ValideringDeserializerModifier());
  }

  private static class ValideringDeserializerModifier extends BeanDeserializerModifier {

    @Override
    public JsonDeserializer<?> modifyDeserializer(
        DeserializationConfig config, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {

      if (beanDesc.getType().isTypeOrSubTypeOf(Validerbar.class)) {
        return new BeanDeserializerWithValidation((BeanDeserializerBase) deserializer);
      }
      return super.modifyDeserializer(config, beanDesc, deserializer);
    }
  }

  public static class BeanDeserializerWithValidation extends BeanDeserializer {

    protected BeanDeserializerWithValidation(BeanDeserializerBase src) {
      super(src);
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      Object instance = super.deserialize(p, ctxt);
      if (instance instanceof Validerbar) {
        ((Validerbar) instance).valider();
      }
      return instance;
    }
  }
}
