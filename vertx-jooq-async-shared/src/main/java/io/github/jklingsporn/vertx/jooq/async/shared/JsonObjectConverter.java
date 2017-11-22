package io.github.jklingsporn.vertx.jooq.async.shared;

import io.vertx.core.json.JsonObject;
import org.jooq.Converter;

/**
 * Created by jensklingsporn on 04.10.16.
 * Use this converter to convert any varchar/String column into a JsonObject.
 */
public class JsonObjectConverter implements Converter<String,JsonObject> {

    private static JsonObjectConverter INSTANCE;
    public static JsonObjectConverter getInstance() {
        return INSTANCE == null ? INSTANCE = new JsonObjectConverter() : INSTANCE;
    }

    @Override
    public JsonObject from(String databaseObject) {
        return databaseObject==null?null:new JsonObject(databaseObject);
    }

    @Override
    public String to(JsonObject userObject) {
        return userObject==null?null:userObject.encode();
    }

    @Override
    public Class<String> fromType() {
        return String.class;
    }

    @Override
    public Class<JsonObject> toType() {
        return JsonObject.class;
    }
}
