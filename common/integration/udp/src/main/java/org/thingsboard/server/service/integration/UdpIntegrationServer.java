/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.integration;

import com.fasterxml.jackson.databind.JsonNode;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * In-process UDP server for a UDP integration: listens on the configured port and feeds each
 * received datagram to the uplink processing chain. Configuration JSON: {port, soRcvBuf?,
 * soBroadcast?, handlerConfiguration?: {handlerType, charsetName?, maxFrameLength?}}. PE has no
 * in-process UDP integration at all (its UDP type is executed only by a separate remote
 * integration-executor process, absent from this port) — this mirrors CE's own TCP integration
 * pattern instead, treating each UDP packet as one uplink message.
 */
@Slf4j
public class UdpIntegrationServer implements IntegrationRuntime {

    private final Integration integration;
    private final IntegrationUplinkSink uplinkSink;
    @Getter
    private final TenantId tenantId;

    private EventLoopGroup group;
    private Channel serverChannel;
    private volatile String handlerType = "BINARY";
    private volatile String charsetName = "UTF-8";
    private volatile int maxFrameLength = 128;

    public UdpIntegrationServer(Integration integration, IntegrationUplinkSink uplinkSink) {
        this.integration = integration;
        this.uplinkSink = uplinkSink;
        this.tenantId = integration.getTenantId();
    }

    @Override
    public void start() throws Exception {
        JsonNode cfg = integration.getConfiguration();
        if (cfg == null || !cfg.has("port")) {
            throw new IllegalArgumentException("UDP port is not configured!");
        }
        int port = cfg.get("port").asInt();
        // PE's tb-udp-integration-form exposes these two; the UI carried them with no effect before.
        // soRcvBuf is entered in KB, matching the TCP form's units and PE's own label.
        int soRcvBufBytes = (cfg.has("soRcvBuf") ? cfg.get("soRcvBuf").asInt() : 64) * 1024;
        boolean soBroadcast = cfg.has("soBroadcast") && cfg.get("soBroadcast").asBoolean();
        // PE nests these under `handlerConfiguration` (tb-udp-integration-form). Unlike TCP's, this
        // is NOT framing — a datagram is already one message — it selects how the payload is
        // presented to the uplink converter.
        JsonNode handlerCfg = cfg.get("handlerConfiguration");
        this.handlerType = handlerCfg != null && handlerCfg.hasNonNull("handlerType")
                ? handlerCfg.get("handlerType").asText("BINARY") : "BINARY";
        this.charsetName = handlerCfg != null && handlerCfg.hasNonNull("charsetName")
                ? handlerCfg.get("charsetName").asText("UTF-8") : "UTF-8";
        this.maxFrameLength = handlerCfg != null && handlerCfg.hasNonNull("maxFrameLength")
                ? handlerCfg.get("maxFrameLength").asInt(128) : 128;

        group = new NioEventLoopGroup(1, new DefaultThreadFactory("udp-integration"));
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST, soBroadcast)
                    .option(ChannelOption.SO_RCVBUF, soRcvBufBytes)
                    // Netty sizes the per-read buffer independently of SO_RCVBUF; without this a
                    // datagram larger than the 2048-byte default is silently truncated, which would
                    // make a raised soRcvBuf look like it had no effect.
                    .option(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(soRcvBufBytes))
                    .handler(new SimpleChannelInboundHandler<DatagramPacket>() {
                        @Override
                        protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
                            byte[] data = new byte[packet.content().readableBytes()];
                            packet.content().readBytes(data);
                            Map<String, String> metadata = new HashMap<>();
                            metadata.put("remoteAddress", String.valueOf(packet.sender()));
                            metadata.put("handlerType", handlerType);
                            try {
                                uplinkSink.processUplink(integration, applyHandler(data), metadata);
                            } catch (Exception e) {
                                log.warn("[{}][{}] Failed to process UDP uplink: {}", tenantId, integration.getName(), e.getMessage());
                            }
                        }

                        @Override
                        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                            log.debug("[{}] UDP integration channel error: {}", integration.getId(), cause.getMessage());
                        }
                    });
            serverChannel = bootstrap.bind(port).sync().channel();
            log.info("[{}][{}] UDP integration listening on port {}", tenantId, integration.getName(), port);
        } catch (Exception e) {
            stop();
            throw e;
        }
    }

    /**
     * PE's handler types, as driven by tb-udp-integration-form: HEX enables maxFrameLength, TEXT
     * enables charsetName, BINARY (the default) and JSON take neither. The uplink sink deals in
     * bytes, so TEXT/HEX transcode to UTF-8 bytes — that is what a converter's decodeToString()
     * then sees.
     */
    private byte[] applyHandler(byte[] data) {
        switch (handlerType) {
            case "TEXT":
                try {
                    return new String(data, Charset.forName(charsetName)).getBytes(StandardCharsets.UTF_8);
                } catch (Exception e) {
                    log.warn("[{}][{}] Unsupported charset [{}], passing the datagram through unchanged",
                            tenantId, integration.getName(), charsetName);
                    return data;
                }
            case "HEX": {
                int len = Math.min(data.length, Math.max(maxFrameLength, 0));
                StringBuilder sb = new StringBuilder(len * 2);
                for (int i = 0; i < len; i++) {
                    sb.append(String.format("%02X", data[i]));
                }
                return sb.toString().getBytes(StandardCharsets.UTF_8);
            }
            case "JSON":
            case "BINARY":
            default:
                return data;
        }
    }

    @Override
    public void stop() {
        if (serverChannel != null) {
            serverChannel.close();
            serverChannel = null;
        }
        if (group != null) {
            group.shutdownGracefully();
            group = null;
        }
    }

}
