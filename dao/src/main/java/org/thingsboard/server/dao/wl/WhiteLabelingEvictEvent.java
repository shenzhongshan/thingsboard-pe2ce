package org.thingsboard.server.dao.wl;

import java.beans.ConstructorProperties;
import lombok.Generated;
import org.thingsboard.server.dao.wl.WhiteLabelingCacheKey;

public class WhiteLabelingEvictEvent {
    private final WhiteLabelingCacheKey key;

    @ConstructorProperties(value={"key"})
    @Generated
    public WhiteLabelingEvictEvent(WhiteLabelingCacheKey key) {
        this.key = key;
    }

    @Generated
    public WhiteLabelingCacheKey getKey() {
        return this.key;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof WhiteLabelingEvictEvent)) {
            return false;
        }
        WhiteLabelingEvictEvent other = (WhiteLabelingEvictEvent)o;
        if (!other.canEqual(this)) {
            return false;
        }
        WhiteLabelingCacheKey this$key = this.getKey();
        WhiteLabelingCacheKey other$key = other.getKey();
        return !(this$key == null ? other$key != null : !((Object)this$key).equals(other$key));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof WhiteLabelingEvictEvent;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        WhiteLabelingCacheKey $key = this.getKey();
        result = result * 59 + ($key == null ? 43 : ((Object)$key).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "WhiteLabelingEvictEvent(key=" + String.valueOf(this.getKey()) + ")";
    }
}

