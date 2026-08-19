/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.swagger.v3.oas.annotations.media.Schema
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.components;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Generated;
import org.thingsboard.server.common.data.report.configuration.components.AbstractImageComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.image.ImageSourceType;

@Schema
public class ImageComponent
extends AbstractImageComponent {
    private ImageSourceType sourceType;
    private String imageUrl;

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.IMAGE;
    }

    @Generated
    public ImageSourceType getSourceType() {
        return this.sourceType;
    }

    @Generated
    public String getImageUrl() {
        return this.imageUrl;
    }

    @Generated
    public void setSourceType(ImageSourceType sourceType) {
        this.sourceType = sourceType;
    }

    @Generated
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @Override
    @Generated
    public String toString() {
        return "ImageComponent(sourceType=" + String.valueOf((Object)this.getSourceType()) + ", imageUrl=" + this.getImageUrl() + ")";
    }

    @Override
    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ImageComponent)) {
            return false;
        }
        ImageComponent other = (ImageComponent)o;
        if (!other.canEqual(this)) {
            return false;
        }
        ImageSourceType this$sourceType = this.getSourceType();
        ImageSourceType other$sourceType = other.getSourceType();
        if (this$sourceType == null ? other$sourceType != null : !((Object)((Object)this$sourceType)).equals((Object)other$sourceType)) {
            return false;
        }
        String this$imageUrl = this.getImageUrl();
        String other$imageUrl = other.getImageUrl();
        return !(this$imageUrl == null ? other$imageUrl != null : !this$imageUrl.equals(other$imageUrl));
    }

    @Override
    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof ImageComponent;
    }

    @Override
    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        ImageSourceType $sourceType = this.getSourceType();
        result = result * 59 + ($sourceType == null ? 43 : ((Object)((Object)$sourceType)).hashCode());
        String $imageUrl = this.getImageUrl();
        result = result * 59 + ($imageUrl == null ? 43 : $imageUrl.hashCode());
        return result;
    }

    @Generated
    public ImageComponent() {
    }
}

