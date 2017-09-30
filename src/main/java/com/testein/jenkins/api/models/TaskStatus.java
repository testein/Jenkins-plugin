package com.testein.jenkins.api.models;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

public enum TaskStatus {
    New(0), Queued(1), Started(2), InProgress(3), Canceling(4), Canceled(5), Failed(6), Success(7);

    private final int value;

    TaskStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static TaskStatus fromKey(int key) {
        for(TaskStatus type : TaskStatus.values()) {
            if(type.getValue() == key) {
                return type;
            }
        }
        return null;
    }

    public class TaskStatusDeserializer implements JsonDeserializer<TaskStatus> {
        @Override
        public TaskStatus deserialize(JsonElement element, Type arg1, JsonDeserializationContext arg2) throws JsonParseException {
            int key = element.getAsInt();
            return TaskStatus.fromKey(key);
        }
    }
}
