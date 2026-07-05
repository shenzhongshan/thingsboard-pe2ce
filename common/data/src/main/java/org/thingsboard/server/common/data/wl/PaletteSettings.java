package org.thingsboard.server.common.data.wl;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Generated;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.wl.Palette;

@Schema
public class PaletteSettings {
    @Schema(description="Primary palette JSON", requiredMode=Schema.RequiredMode.REQUIRED)
    private Palette primaryPalette;
    @Schema(description="Accent palette JSON", requiredMode=Schema.RequiredMode.REQUIRED)
    private Palette accentPalette;

    public PaletteSettings merge(PaletteSettings otherPaletteSettings) {
        if (this.primaryPalette == null || StringUtils.isEmpty(this.primaryPalette.getType())) {
            this.primaryPalette = otherPaletteSettings.primaryPalette;
        }
        if (this.accentPalette == null || StringUtils.isEmpty(this.accentPalette.getType())) {
            this.accentPalette = otherPaletteSettings.accentPalette;
        }
        return this;
    }

    @Generated
    public PaletteSettings() {
    }

    @Generated
    public Palette getPrimaryPalette() {
        return this.primaryPalette;
    }

    @Generated
    public Palette getAccentPalette() {
        return this.accentPalette;
    }

    @Generated
    public void setPrimaryPalette(Palette primaryPalette) {
        this.primaryPalette = primaryPalette;
    }

    @Generated
    public void setAccentPalette(Palette accentPalette) {
        this.accentPalette = accentPalette;
    }

    @Generated
    public String toString() {
        return "PaletteSettings(primaryPalette=" + String.valueOf(this.getPrimaryPalette()) + ", accentPalette=" + String.valueOf(this.getAccentPalette()) + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof PaletteSettings)) {
            return false;
        }
        PaletteSettings other = (PaletteSettings)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Palette this$primaryPalette = this.getPrimaryPalette();
        Palette other$primaryPalette = other.getPrimaryPalette();
        if (this$primaryPalette == null ? other$primaryPalette != null : !((Object)this$primaryPalette).equals(other$primaryPalette)) {
            return false;
        }
        Palette this$accentPalette = this.getAccentPalette();
        Palette other$accentPalette = other.getAccentPalette();
        return !(this$accentPalette == null ? other$accentPalette != null : !((Object)this$accentPalette).equals(other$accentPalette));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof PaletteSettings;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Palette $primaryPalette = this.getPrimaryPalette();
        result = result * 59 + ($primaryPalette == null ? 43 : ((Object)$primaryPalette).hashCode());
        Palette $accentPalette = this.getAccentPalette();
        result = result * 59 + ($accentPalette == null ? 43 : ((Object)$accentPalette).hashCode());
        return result;
    }
}

