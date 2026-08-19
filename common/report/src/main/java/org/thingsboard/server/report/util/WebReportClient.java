/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.JsonNode
 *  com.fasterxml.jackson.databind.node.ObjectNode
 *  io.netty.channel.EventLoopGroup
 *  io.netty.channel.nio.NioEventLoopGroup
 *  io.netty.handler.ssl.SslContextBuilder
 *  jakarta.annotation.PostConstruct
 *  jakarta.annotation.PreDestroy
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.http.HttpHeaders
 *  org.springframework.http.HttpStatus
 *  org.springframework.http.HttpStatusCode
 *  org.springframework.http.MediaType
 *  org.springframework.http.ResponseEntity
 *  org.springframework.http.client.reactive.ClientHttpConnector
 *  org.springframework.http.client.reactive.ReactorClientHttpConnector
 *  org.springframework.stereotype.Component
 *  org.springframework.web.client.RestClientResponseException
 *  org.springframework.web.reactive.function.client.ExchangeFilterFunctions
 *  org.springframework.web.reactive.function.client.ExchangeStrategies
 *  org.springframework.web.reactive.function.client.WebClient
 *  org.springframework.web.reactive.function.client.WebClient$RequestBodySpec
 *  org.springframework.web.reactive.function.client.WebClientResponseException
 *  org.thingsboard.common.util.JacksonUtil
 *  org.thingsboard.server.common.data.StringUtils
 *  org.thingsboard.server.common.data.dashboardreport.DashboardReportConfig
 *  org.thingsboard.server.common.data.dashboardreport.DashboardReportData
 *  org.thingsboard.server.common.data.exception.ThingsboardErrorCode
 *  org.thingsboard.server.common.data.exception.ThingsboardException
 *  reactor.netty.http.client.HttpClient
 */
package org.thingsboard.server.report.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.ssl.SslContextBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.SSLException;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.dashboardreport.DashboardReportConfig;
import org.thingsboard.server.common.data.dashboardreport.DashboardReportData;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.report.util.ReportUtils;
import reactor.netty.http.client.HttpClient;

@Component
public class WebReportClient {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(WebReportClient.class);
    public static final Pattern CONTENT_DISPOSITION_REGEX = Pattern.compile("(?i)^.*filename=\"?([^\"]+)\"?.*filename\\*=UTF-8''([^\"]+).*$");
    @Value(value="${reports.web_report.base_url}")
    private String webReportServerBaseUrl;
    @Value(value="${reports.web_report.max_response_size:52428800}")
    private int maxResponseSize;
    private EventLoopGroup eventLoopGroup;
    private WebClient webClient;

    @PostConstruct
    public void init() {
        try {
            this.eventLoopGroup = new NioEventLoopGroup();
            HttpClient httpClient = ((HttpClient)HttpClient.create().runOn(this.eventLoopGroup)).secure(t -> {
                try {
                    t.sslContext(SslContextBuilder.forClient().build());
                }
                catch (SSLException e) {
                    throw new RuntimeException(e);
                }
            });
            this.webClient = WebClient.builder().filter(ExchangeFilterFunctions.limitResponseSize((long)this.maxResponseSize)).clientConnector((ClientHttpConnector)new ReactorClientHttpConnector(httpClient)).exchangeStrategies(ExchangeStrategies.builder().codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(this.maxResponseSize)).build()).build();
        }
        catch (Exception e) {
            log.error("Can't initialize dashboard report service due to {}", (Object)e.getMessage(), (Object)e);
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (this.eventLoopGroup != null) {
            this.eventLoopGroup.shutdownGracefully(0L, 5L, TimeUnit.SECONDS);
        }
    }

    public void requestDashboardReport(DashboardReportConfig reportConfig, String reportsServerEndpointUrl, String accessToken, long accessTokenExpiration, Consumer<DashboardReportData> onSuccess, Consumer<Throwable> onFailure) {
        JsonNode dashboardReportRequest = this.createDashboardReportRequest(reportConfig, accessToken, accessTokenExpiration, null);
        this.requestDashboardReport(dashboardReportRequest, reportsServerEndpointUrl, onSuccess, onFailure);
    }

    public void requestDashboardReport(DashboardReportConfig reportConfig, String reportsServerEndpointUrl, String accessToken, long accessTokenExpiration, Integer pageWidth, Consumer<DashboardReportData> onSuccess, Consumer<Throwable> onFailure) {
        JsonNode dashboardReportRequest = this.createDashboardReportRequest(reportConfig, accessToken, accessTokenExpiration, pageWidth);
        this.requestDashboardReport(dashboardReportRequest, reportsServerEndpointUrl, onSuccess, onFailure);
    }

    public void requestDashboardReport(JsonNode dashboardReportRequest, String reportsServerEndpointUrl, Consumer<DashboardReportData> onSuccess, Consumer<Throwable> onFailure) {
        if (StringUtils.isEmpty((String)reportsServerEndpointUrl)) {
            reportsServerEndpointUrl = this.webReportServerBaseUrl;
        }
        String endpointUrl = reportsServerEndpointUrl + "/dashboardReport";
        byte[] requestBody = JacksonUtil.writeValueAsBytes((Object)dashboardReportRequest);
        ((WebClient.RequestBodySpec)((WebClient.RequestBodySpec)this.webClient.post().uri(endpointUrl, new Object[0])).headers(headers -> this.prepareHeaders((HttpHeaders)headers, requestBody))).bodyValue((Object)requestBody).retrieve().toEntity(byte[].class).subscribe(responseEntity -> {
            try {
                onSuccess.accept(this.extractResponse((ResponseEntity<byte[]>)responseEntity));
            }
            catch (Throwable t) {
                this.processError(onFailure, t);
            }
        }, t -> this.processError(onFailure, (Throwable)t));
    }

    private JsonNode createDashboardReportRequest(DashboardReportConfig reportConfig, String accessToken, long accessTokenExpiration, Integer pageWidth) {
        String reportName = ReportUtils.prepareReportName(reportConfig.getNamePattern(), new Date(), reportConfig.getTimezone());
        ObjectNode dashboardReportRequest = JacksonUtil.newObjectNode();
        dashboardReportRequest.put("baseUrl", reportConfig.getBaseUrl());
        dashboardReportRequest.put("dashboardId", reportConfig.getDashboardId());
        dashboardReportRequest.put("token", accessToken);
        dashboardReportRequest.put("expiration", accessTokenExpiration);
        dashboardReportRequest.put("name", reportName);
        dashboardReportRequest.set("reportParams", this.createReportParams(reportConfig, pageWidth));
        return dashboardReportRequest;
    }

    private JsonNode createReportParams(DashboardReportConfig reportConfig, Integer pageWidth) {
        ObjectNode reportParams = JacksonUtil.newObjectNode();
        reportParams.put("type", reportConfig.getType());
        reportParams.put("state", reportConfig.getState());
        if (!reportConfig.isUseDashboardTimewindow()) {
            reportParams.set("timewindow", reportConfig.getTimewindow());
        }
        reportParams.put("timezone", reportConfig.getTimezone());
        if (pageWidth != null) {
            reportParams.put("pageWidth", pageWidth);
        }
        return reportParams;
    }

    private void processError(Consumer<Throwable> onFailure, Throwable t) {
        if (t instanceof RestClientResponseException) {
            onFailure.accept((Throwable)new ThingsboardException(((RestClientResponseException)t).getStatusText(), ThingsboardErrorCode.GENERAL));
        } else if (t instanceof WebClientResponseException) {
            WebClientResponseException webClientResponseException = (WebClientResponseException)t;
            String error = webClientResponseException.getResponseBodyAsString();
            if (StringUtils.isBlank((String)error)) {
                error = webClientResponseException.getStatusText();
            }
            HttpStatusCode httpStatusCode = webClientResponseException.getStatusCode();
            HttpStatus httpStatus = HttpStatus.resolve((int)httpStatusCode.value());
            ThingsboardErrorCode errorCode = ThingsboardErrorCode.GENERAL;
            if (HttpStatus.BAD_REQUEST.equals((Object)httpStatus)) {
                errorCode = ThingsboardErrorCode.BAD_REQUEST_PARAMS;
            }
            onFailure.accept((Throwable)new ThingsboardException(error, errorCode));
        } else {
            onFailure.accept(t);
        }
    }

    private DashboardReportData extractResponse(ResponseEntity<byte[]> responseEntity) throws UnsupportedEncodingException {
        DashboardReportData reportData = new DashboardReportData();
        reportData.setData((byte[])responseEntity.getBody());
        reportData.setContentType(responseEntity.getHeaders().getContentType().toString());
        String disposition = responseEntity.getHeaders().getFirst("Content-Disposition");
        Matcher matcher = CONTENT_DISPOSITION_REGEX.matcher(disposition);
        if (matcher.find()) {
            String utf8FileName = matcher.group(2);
            if (!utf8FileName.isBlank()) {
                reportData.setName(URLDecoder.decode(utf8FileName, StandardCharsets.UTF_8));
            } else {
                String fileName = matcher.group(1);
                reportData.setName(URLDecoder.decode(fileName, "ISO_8859_1"));
            }
        }
        return reportData;
    }

    private void prepareHeaders(HttpHeaders headers, byte[] json) {
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentLength((long)json.length);
        headers.setConnection("keep-alive");
    }
}

