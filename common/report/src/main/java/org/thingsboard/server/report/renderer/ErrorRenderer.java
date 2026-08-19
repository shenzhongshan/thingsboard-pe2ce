/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.stereotype.Component
 *  org.thingsboard.server.common.data.exception.ThingsboardException
 *  org.thingsboard.server.common.data.report.configuration.components.ErrorComponent
 *  org.thingsboard.server.common.data.report.configuration.components.ReportComponentType
 */
package org.thingsboard.server.report.renderer;

import java.util.HashMap;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.report.configuration.components.ErrorComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.renderer.PdfReportComponentRenderer;
import org.thingsboard.server.report.util.ThymeleafUtil;

@Component
public class ErrorRenderer
implements PdfReportComponentRenderer<ErrorComponent> {
    @Override
    public String render(ErrorComponent errorComponent, ComponentData reportDataSource) {
        HashMap<String, Object> componentVariables = new HashMap<String, Object>();
        componentVariables.put("errorMessage", errorComponent.getErrorMessage());
        Exception exception = this.extractRootException(errorComponent.getException());
        if (exception != null) {
            componentVariables.put("exception", this.formatExceptionMessage(exception));
        }
        return ThymeleafUtil.renderFromHtmlTemplate("html/components/error-template", componentVariables);
    }

    private Exception extractRootException(Exception exception) {
        RuntimeException runtimeException;
        Throwable throwable;
        if (exception instanceof RuntimeException && (throwable = (runtimeException = (RuntimeException)exception).getCause()) instanceof Exception) {
            Exception cause = (Exception)throwable;
            return cause;
        }
        return exception;
    }

    private String formatExceptionMessage(Exception exception) {
        if (exception instanceof ThingsboardException) {
            ThingsboardException tbException = (ThingsboardException)exception;
            return "[" + tbException.getErrorCode().name() + "] " + tbException.getMessage();
        }
        return exception.getMessage();
    }

    @Override
    public ReportComponentType getType() {
        return ReportComponentType.ERROR;
    }
}

