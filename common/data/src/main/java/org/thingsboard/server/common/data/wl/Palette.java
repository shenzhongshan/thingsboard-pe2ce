package org.thingsboard.server.common.data.wl;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.Generated;

public class Palette {
    @Schema(description="Name of the pre-defined palette, or 'custom'", example="custom", requiredMode=Schema.RequiredMode.REQUIRED)
    private String type;
    @JsonProperty(value="extends")
    @Schema(description="Pre-defined palette name that the custom palette extends", example="purple")
    private String extendsPalette;
    @Schema(description="Mapping of hue identifier number to the rgb(a) color code")
    private Map<String, String> colors;

    @Generated
    public Palette() {
    }

    @Generated
    public String getType() {
        return this.type;
    }

    @Generated
    public String getExtendsPalette() {
        return this.extendsPalette;
    }

    @Generated
    public Map<String, String> getColors() {
        return this.colors;
    }

    @Generated
    public void setType(String type) {
        this.type = type;
    }

    @Generated
    public void setExtendsPalette(String extendsPalette) {
        this.extendsPalette = extendsPalette;
    }

    @Generated
    public void setColors(Map<String, String> colors) {
        this.colors = colors;
    }

    @Generated
    public String toString() {
        return "Palette(type=" + this.getType() + ", extendsPalette=" + this.getExtendsPalette() + ", colors=" + String.valueOf(this.getColors()) + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Palette)) {
            return false;
        }
        Palette other = (Palette)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$type = this.getType();
        String other$type = other.getType();
        if (this$type == null ? other$type != null : !this$type.equals(other$type)) {
            return false;
        }
        String this$extendsPalette = this.getExtendsPalette();
        String other$extendsPalette = other.getExtendsPalette();
        if (this$extendsPalette == null ? other$extendsPalette != null : !this$extendsPalette.equals(other$extendsPalette)) {
            return false;
        }
        Map<String, String> this$colors = this.getColors();
        Map<String, String> other$colors = other.getColors();
        return !(this$colors == null ? other$colors != null : !((Object)this$colors).equals(other$colors));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof Palette;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $type = this.getType();
        result = result * 59 + ($type == null ? 43 : $type.hashCode());
        String $extendsPalette = this.getExtendsPalette();
        result = result * 59 + ($extendsPalette == null ? 43 : $extendsPalette.hashCode());
        Map<String, String> $colors = this.getColors();
        result = result * 59 + ($colors == null ? 43 : ((Object)$colors).hashCode());
        return result;
    }
}

