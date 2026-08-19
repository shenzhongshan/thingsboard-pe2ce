/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.swagger.v3.oas.annotations.media.Schema
 *  lombok.Generated
 */
package org.thingsboard.server.common.data.report.configuration.components;

import io.swagger.v3.oas.annotations.media.Schema;
import java.beans.ConstructorProperties;
import lombok.Generated;
import org.thingsboard.server.common.data.report.configuration.components.AbstractReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;

@Schema
public class ErrorComponent
extends AbstractReportComponent {
    private String errorMessage;
    private Exception exception;

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.ERROR;
    }

    @Generated
    public String getErrorMessage() {
        return this.errorMessage;
    }

    @Generated
    public Exception getException() {
        return this.exception;
    }

    @Generated
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Generated
    public void setException(Exception exception) {
        this.exception = exception;
    }

    @Override
    @Generated
    public String toString() {
        return "ErrorComponent(errorMessage=" + this.getErrorMessage() + ", exception=" + String.valueOf(this.getException()) + ")";
    }

    @Override
    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ErrorComponent)) {
            return false;
        }
        ErrorComponent other = (ErrorComponent)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$errorMessage = this.getErrorMessage();
        String other$errorMessage = other.getErrorMessage();
        if (this$errorMessage == null ? other$errorMessage != null : !this$errorMessage.equals(other$errorMessage)) {
            return false;
        }
        Exception this$exception = this.getException();
        Exception other$exception = other.getException();
        return !(this$exception == null ? other$exception != null : !this$exception.equals(other$exception));
    }

    @Override
    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof ErrorComponent;
    }

    @Override
    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $errorMessage = this.getErrorMessage();
        result = result * 59 + ($errorMessage == null ? 43 : $errorMessage.hashCode());
        Exception $exception = this.getException();
        result = result * 59 + ($exception == null ? 43 : $exception.hashCode());
        return result;
    }

    @Generated
    public ErrorComponent() {
    }

    @ConstructorProperties(value={"errorMessage", "exception"})
    @Generated
    public ErrorComponent(String errorMessage, Exception exception) {
        this.errorMessage = errorMessage;
        this.exception = exception;
    }
}

