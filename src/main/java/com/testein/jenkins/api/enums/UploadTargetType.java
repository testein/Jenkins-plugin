package com.testein.jenkins.api.enums;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

public enum UploadTargetType {
    Js(0), Jar(1);

    private final int value;

    UploadTargetType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static com.testein.jenkins.api.models.TaskStatus fromKey(int key) {
        for(com.testein.jenkins.api.models.TaskStatus type : com.testein.jenkins.api.models.TaskStatus.values()) {
            if(type.getValue() == key) {
                return type;
            }
        }
        return null;
    }

    public class TaskStatusDeserializer implements JsonDeserializer<com.testein.jenkins.api.models.TaskStatus> {
        @Override
        public com.testein.jenkins.api.models.TaskStatus deserialize(JsonElement element, Type arg1, JsonDeserializationContext arg2) throws JsonParseException {
            int key = element.getAsInt();
            return com.testein.jenkins.api.models.TaskStatus.fromKey(key);
        }
    }
}
