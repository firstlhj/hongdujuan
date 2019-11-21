package cn.withive.wxpay.config;

import cn.withive.wxpay.model.ResModel;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class JacksonConfig {

    static class EnumSerializer extends JsonSerializer<Enum> {
        @Override
        public void serialize(Enum value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value != null) {
                gen.writeNumber(value.ordinal());
            }
        }
    }
    @Bean
    public SimpleModule simpleModule() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(ResModel.StatusEnum.class, new EnumSerializer());
        return module;
    }
}
