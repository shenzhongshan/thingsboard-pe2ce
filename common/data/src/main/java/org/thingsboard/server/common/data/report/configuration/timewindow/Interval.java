/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JacksonException
 *  com.fasterxml.jackson.core.JsonGenerator
 *  com.fasterxml.jackson.core.JsonParser
 *  com.fasterxml.jackson.core.JsonToken
 *  com.fasterxml.jackson.databind.DeserializationContext
 *  com.fasterxml.jackson.databind.JsonDeserializer
 *  com.fasterxml.jackson.databind.JsonSerializer
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.fasterxml.jackson.databind.SerializerProvider
 *  com.fasterxml.jackson.databind.annotation.JsonDeserialize
 *  com.fasterxml.jackson.databind.annotation.JsonSerialize
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.timewindow;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.IOException;
import lombok.Generated;
import org.thingsboard.server.common.data.kv.IntervalType;

@JsonDeserialize(using=Interval.IntervalDeserializer.class)
@JsonSerialize(using=Interval.IntervalSerializer.class)
public class Interval {
    private long interval;
    private IntervalType intervalType;

    public static Interval of(long intervalLong) {
        Interval interval = new Interval();
        interval.setIntervalType(IntervalType.MILLISECONDS);
        interval.setInterval(intervalLong);
        return interval;
    }

    public static Interval of(IntervalType intervalType) {
        Interval interval = new Interval();
        interval.setInterval(0L);
        interval.setIntervalType(intervalType);
        return interval;
    }

    @Generated
    public Interval() {
    }

    @Generated
    public long getInterval() {
        return this.interval;
    }

    @Generated
    public IntervalType getIntervalType() {
        return this.intervalType;
    }

    @Generated
    public void setInterval(long interval) {
        this.interval = interval;
    }

    @Generated
    public void setIntervalType(IntervalType intervalType) {
        this.intervalType = intervalType;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Interval)) {
            return false;
        }
        Interval other = (Interval)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.getInterval() != other.getInterval()) {
            return false;
        }
        IntervalType this$intervalType = this.getIntervalType();
        IntervalType other$intervalType = other.getIntervalType();
        return !(this$intervalType == null ? other$intervalType != null : !((Object)((Object)this$intervalType)).equals((Object)other$intervalType));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof Interval;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        long $interval = this.getInterval();
        result = result * 59 + (int)($interval >>> 32 ^ $interval);
        IntervalType $intervalType = this.getIntervalType();
        result = result * 59 + ($intervalType == null ? 43 : ((Object)((Object)$intervalType)).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "Interval(interval=" + this.getInterval() + ", intervalType=" + String.valueOf((Object)this.getIntervalType()) + ")";
    }

    public static class IntervalSerializer
    extends JsonSerializer<Interval> {
        public void serialize(Interval value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value.intervalType != null && !IntervalType.MILLISECONDS.equals((Object)value.intervalType)) {
                gen.writeString(value.intervalType.name());
            } else {
                gen.writeNumber(value.interval);
            }
        }
    }

    public static class IntervalDeserializer
    extends JsonDeserializer<Interval> {
        static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

        public Interval deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
            JsonToken token = jsonParser.currentToken();
            if (token == JsonToken.VALUE_STRING) {
                String value = jsonParser.getText();
                try {
                    IntervalType intervalType = IntervalType.valueOf(value);
                    return Interval.of(intervalType);
                }
                catch (IllegalArgumentException e) {
                    throw new IOException("Unknown Interval type: " + value);
                }
            }
            if (token == JsonToken.VALUE_NUMBER_INT) {
                long value = jsonParser.getLongValue();
                return Interval.of(value);
            }
            return new Interval();
        }
    }
}

