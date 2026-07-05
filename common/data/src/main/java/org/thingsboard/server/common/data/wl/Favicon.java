package org.thingsboard.server.common.data.wl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Generated;

@Schema
@JsonIgnoreProperties(ignoreUnknown=true)
public class Favicon {
    private String url;

    public Favicon() {
    }

    public Favicon(String url) {
        this.url = url;
    }

    @Generated
    public String getUrl() {
        return this.url;
    }

    @Generated
    public void setUrl(String url) {
        this.url = url;
    }

    @Generated
    public String toString() {
        return "Favicon(url=" + this.getUrl() + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Favicon)) {
            return false;
        }
        Favicon other = (Favicon)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$url = this.getUrl();
        String other$url = other.getUrl();
        return !(this$url == null ? other$url != null : !this$url.equals(other$url));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof Favicon;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $url = this.getUrl();
        result = result * 59 + ($url == null ? 43 : $url.hashCode());
        return result;
    }
}

