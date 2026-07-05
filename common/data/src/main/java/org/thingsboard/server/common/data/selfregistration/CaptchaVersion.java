package org.thingsboard.server.common.data.selfregistration;

import lombok.Generated;

public enum CaptchaVersion {
    V_3("v3"),
    V_2("v2"),
    ENTERPRISE("enterprise");

    private final String name;

    private CaptchaVersion(String name) {
        this.name = name;
    }

    @Generated
    public String getName() {
        return this.name;
    }
}

