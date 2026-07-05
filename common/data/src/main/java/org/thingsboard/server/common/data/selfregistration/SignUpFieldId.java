package org.thingsboard.server.common.data.selfregistration;

import lombok.Generated;

public enum SignUpFieldId {
    EMAIL(true),
    PASSWORD(true),
    REPEAT_PASSWORD(false),
    FIRST_NAME(true),
    LAST_NAME(true),
    PHONE(true),
    COUNTRY(true),
    CITY(true),
    STATE(true),
    ZIP(true),
    ADDRESS(true),
    ADDRESS2(true);

    private final boolean validate;

    private SignUpFieldId(boolean needCheck) {
        this.validate = needCheck;
    }

    @Generated
    public boolean isValidate() {
        return this.validate;
    }
}

