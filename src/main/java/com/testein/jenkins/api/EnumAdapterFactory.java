package com.testein.jenkins.api;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class EnumAdapterFactory implements TypeAdapterFactory {
    @Override
    public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
        Class<? super T> rawType = type.getRawType();

        if (!Enum.class.isAssignableFrom(rawType) || rawType == Enum.class)
        {
            return null;
        }

        if (!rawType.isEnum())
        {
            rawType = rawType.getSuperclass();
        }

        return (TypeAdapter<T>) new EnumTypeAdapter(rawType);
    }

    public class EnumTypeAdapter<T> extends TypeAdapter<T> {
        private final Map<Integer, T> intToConstant = new HashMap<>();
        private final Map<T, Integer> constantToInt = new HashMap<>();
        private final Class<T> classOfT;

        public EnumTypeAdapter(Class<T> classOf) {
            this.classOfT = classOf;
            try
            {
                for (T value : classOfT.getEnumConstants())
                {
                    int res = -1;
                    for (PropertyDescriptor propertyDescriptor : Introspector.getBeanInfo(value.getClass()).getPropertyDescriptors()) {
                        if (propertyDescriptor.getName().equalsIgnoreCase("value")) {
                            try {
                                res = (int) propertyDescriptor.getReadMethod().invoke(value);
                            } catch (IllegalAccessException e) {
                                //e.printStackTrace();
                            } catch (InvocationTargetException e) {
                                //e.printStackTrace();
                            }
                        }
                    }

                    intToConstant.put(res, value);
                    constantToInt.put(value, res);
                }
            } catch (IntrospectionException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void write(JsonWriter out, T value) throws IOException {
            out.value(value == null ? null : constantToInt.get(value));
        }

        public T read(JsonReader in) throws IOException {
            int v = in.nextInt();
            return intToConstant.get(v);
        }
    }
}
