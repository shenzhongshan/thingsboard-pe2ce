package org.thingsboard.server.common.data.signup;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public enum SignUpResult {
    SUCCESS,
    INACTIVE_USER_EXISTS;

}

