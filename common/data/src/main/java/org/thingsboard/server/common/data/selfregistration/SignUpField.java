package org.thingsboard.server.common.data.selfregistration;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.beans.ConstructorProperties;
import lombok.Generated;
import org.thingsboard.server.common.data.selfregistration.SignUpFieldId;

@Schema
public class SignUpField {
    @Schema(description="Signup field id")
    @NotNull
    private SignUpFieldId id;
    @Schema(description="Signup field label")
    @NotNull
    private String label;
    @Schema(description="Indicates if field is required")
    private boolean required;

    @Generated
    public SignUpFieldId getId() {
        return this.id;
    }

    @Generated
    public String getLabel() {
        return this.label;
    }

    @Generated
    public boolean isRequired() {
        return this.required;
    }

    @Generated
    public void setId(SignUpFieldId id) {
        this.id = id;
    }

    @Generated
    public void setLabel(String label) {
        this.label = label;
    }

    @Generated
    public void setRequired(boolean required) {
        this.required = required;
    }

    @Generated
    public String toString() {
        return "SignUpField(id=" + String.valueOf((Object)this.getId()) + ", label=" + this.getLabel() + ", required=" + this.isRequired() + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SignUpField)) {
            return false;
        }
        SignUpField other = (SignUpField)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.isRequired() != other.isRequired()) {
            return false;
        }
        SignUpFieldId this$id = this.getId();
        SignUpFieldId other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)((Object)this$id)).equals((Object)other$id)) {
            return false;
        }
        String this$label = this.getLabel();
        String other$label = other.getLabel();
        return !(this$label == null ? other$label != null : !this$label.equals(other$label));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof SignUpField;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + (this.isRequired() ? 79 : 97);
        SignUpFieldId $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)((Object)$id)).hashCode());
        String $label = this.getLabel();
        result = result * 59 + ($label == null ? 43 : $label.hashCode());
        return result;
    }

    @ConstructorProperties(value={"id", "label", "required"})
    @Generated
    public SignUpField(SignUpFieldId id, String label, boolean required) {
        this.id = id;
        this.label = label;
        this.required = required;
    }

    @Generated
    public SignUpField() {
    }
}

