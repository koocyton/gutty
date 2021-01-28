package com.doopp.gutty.framework.json;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;

public class GsonMessageConverter implements MessageConverter {

    private Gson gson;

    public GsonMessageConverter() {
        this.gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            // .serializeNulls()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .setLongSerializationPolicy(LongSerializationPolicy.STRING)
            .create();
    }

    public GsonMessageConverter(Gson gson) {
        assert gson!=null : "A Gson instance is required";
        this.gson = gson;
    }

    public void setGson(Gson gson) {
        assert gson!=null : "A Gson instance is required";
        this.gson = gson;
    }

    public Gson getGson() {
        return this.gson;
    }

    @Override
    public String toJson(Object object) {
        return this.gson.toJson(object);
    }

    @Override
    public <T> T fromJson(String json, Class<T> clazz) {
        return this.gson.fromJson(json, clazz);
    }
}

