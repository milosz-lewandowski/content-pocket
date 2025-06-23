package pl.mewash.contentlaundry.utils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class JsonMapperConfig {

    public static ObjectMapper getPrettyMapper() {
        JsonFactory jsonFactory = JsonFactory.builder()
                .configure(JsonWriteFeature.ESCAPE_NON_ASCII, false)
                .build();

        return new ObjectMapper(jsonFactory)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .registerModule(new JavaTimeModule());
    }
}
