package com.github.kaiwinter.myatmo.rest;

import com.github.kaiwinter.myatmo.chart.rest.model.Body;
import com.github.kaiwinter.myatmo.chart.rest.model.Measurement;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class BodyTypeAdapter extends TypeAdapter<Body> {
    public void write(JsonWriter out, Body value) {
        throw new RuntimeException("not necessary");
    }

    @Override
    public Body read(JsonReader in) throws IOException {
        if (in == null) {
            return null;
        }
        Body body = new Body();
        in.beginObject();
        while (in.hasNext()) {
            if (in.peek() != JsonToken.NAME) {
                throw new IOException("Error parsing Measurement Body");
            }
            String nextName = in.nextName();
            if (in.peek() != JsonToken.BEGIN_ARRAY) {
                throw new IOException("Error parsing Measurement Body");
            }
            in.beginArray();
            double nextDouble = in.nextDouble();
            if (in.peek() != JsonToken.END_ARRAY) {
                throw new IOException("Error parsing Measurement Body");
            }
            in.endArray();
            body.measurements.add(new Measurement(Integer.parseInt(nextName), nextDouble));
        }
        in.endObject();
        return body;
    }
}