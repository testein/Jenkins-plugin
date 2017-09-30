package com.testein.jenkins.api.models;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

public enum  RunnerType {
    ById(0), ByLabel(1);

    private final int value;

    RunnerType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static RunnerType fromKey(int key) {
        for(RunnerType type : RunnerType.values()) {
            if(type.getValue() == key) {
                return type;
            }
        }
        return null;
    }

    public class RunnerTypeDeserializer implements JsonDeserializer<RunnerType> {
        @Override
        public RunnerType deserialize(JsonElement element, Type arg1, JsonDeserializationContext arg2) throws JsonParseException {
            int key = element.getAsInt();
            return RunnerType.fromKey(key);
        }
    }
}
