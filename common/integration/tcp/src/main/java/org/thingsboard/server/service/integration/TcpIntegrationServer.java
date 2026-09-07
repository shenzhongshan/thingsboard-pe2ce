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
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.json.JsonObjectDecoder;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;

import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * In-process TCP server for a TCP integration: listens on the configured port and feeds each
 * decoded frame to the uplink processing chain. Configuration JSON: {port, soBacklogOption,
 * soRcvBuf, soSndBuf (KB), soKeepaliveOption, tcpNoDelay, handlerType: 'TEXT'|'JSON'|'BINARY',
 * maxFrameLength, charsetName; TEXT-only: stripDelimiter, messageSeparator:
 * 'SYSTEM_LINE_SEPARATOR'|'NUL'|'CUSTOM', customSeparatorRawValue; BINARY-only: lengthFieldOffset,
 * lengthFieldLength, lengthAdjustment, initialBytesToStrip, byteOrder: 'BIG_ENDIAN'|'LITTLE_ENDIAN',
 * failFast}. Field names and defaults match PE's real tb-tcp-integration-form/TcpIntegrationServer
 * — TEXT uses Netty's DelimiterBasedFrameDecoder, JSON uses JsonObjectDecoder (self-delimiting on
 * balanced braces/brackets, no extra config needed), BINARY uses LengthFieldBasedFrameDecoder.
 */
@Slf4j
public class TcpIntegrationServer implements IntegrationRuntime {

    private final Integration integration;
    private final IntegrationUplinkSink uplinkSink;
    @Getter
    private final TenantId tenantId;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public TcpIntegrationServer(Integration integration, IntegrationUplinkSink uplinkSink) {
        this.integration = integration;
        this.uplinkSink = uplinkSink;
        this.tenantId = integration.getTenantId();
    }

    @Override
    public void start() throws Exception {
        JsonNode cfg = integration.getConfiguration();
        if (cfg == null || !cfg.has("port")) {
            throw new IllegalArgumentException("TCP port is not configured!");
        }
        int port = cfg.get("port").asInt();
        String handlerType = cfg.has("handlerType") ? cfg.get("handlerType").asText() : "TEXT";
        int maxFrameLength = cfg.has("maxFrameLength") ? cfg.get("maxFrameLength").asInt() : 65535;
        Charset charset = cfg.has("charsetName") && StringUtils.isNotEmpty(cfg.get("charsetName").asText())
                ? Charset.forName(cfg.get("charsetName").asText()) : StandardCharsets.UTF_8;

        int soBacklog = cfg.has("soBacklogOption") ? cfg.get("soBacklogOption").asInt() : 128;
        int soRcvBufBytes = (cfg.has("soRcvBuf") ? cfg.get("soRcvBuf").asInt() : 64) * 1024;
        int soSndBufBytes = (cfg.has("soSndBuf") ? cfg.get("soSndBuf").asInt() : 64) * 1024;
        boolean soKeepAlive = !cfg.has("soKeepaliveOption") || cfg.get("soKeepaliveOption").asBoolean(true);
        boolean tcpNoDelay = cfg.has("tcpNoDelay") && cfg.get("tcpNoDelay").asBoolean();

        bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("tcp-integration-boss"));
        workerGroup = new NioEventLoopGroup(new DefaultThreadFactory("tcp-integration-worker"));
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, soBacklog)
                    .childOption(ChannelOption.SO_KEEPALIVE, soKeepAlive)
                    .childOption(ChannelOption.TCP_NODELAY, tcpNoDelay)
                    .childOption(ChannelOption.SO_RCVBUF, soRcvBufBytes)
                    .childOption(ChannelOption.SO_SNDBUF, soSndBufBytes)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            switch (handlerType) {
                                case "BINARY":
                                    ch.pipeline().addLast(binaryFrameDecoder(cfg, maxFrameLength));
                                    break;
                                case "JSON":
                                    ch.pipeline().addLast(new JsonObjectDecoder(maxFrameLength));
                                    break;
                                default:
                                    ch.pipeline().addLast(textFrameDecoder(cfg, maxFrameLength));
                            }
                            ch.pipeline().addLast(new UplinkHandler(charset));
                        }
                    });
            serverChannel = bootstrap.bind(port).sync().channel();
            log.info("[{}][{}] TCP integration listening on port {}", tenantId, integration.getName(), port);
        } catch (Exception e) {
            stop();
            throw e;
        }
    }

    private LengthFieldBasedFrameDecoder binaryFrameDecoder(JsonNode cfg, int maxFrameLength) {
        int lengthFieldOffset = cfg.has("lengthFieldOffset") ? cfg.get("lengthFieldOffset").asInt() : 0;
        int lengthFieldLength = cfg.has("lengthFieldLength") ? cfg.get("lengthFieldLength").asInt() : 2;
        int lengthAdjustment = cfg.has("lengthAdjustment") ? cfg.get("lengthAdjustment").asInt() : 0;
        int initialBytesToStrip = cfg.has("initialBytesToStrip") ? cfg.get("initialBytesToStrip").asInt() : 0;
        boolean failFast = !cfg.has("failFast") || cfg.get("failFast").asBoolean(true);
        ByteOrder byteOrder = cfg.has("byteOrder") && "LITTLE_ENDIAN".equals(cfg.get("byteOrder").asText())
                ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        return new LengthFieldBasedFrameDecoder(byteOrder, maxFrameLength, lengthFieldOffset, lengthFieldLength,
                lengthAdjustment, initialBytesToStrip, failFast);
    }

    private DelimiterBasedFrameDecoder textFrameDecoder(JsonNode cfg, int maxFrameLength) {
        boolean stripDelimiter = !cfg.has("stripDelimiter") || cfg.get("stripDelimiter").asBoolean(true);
        String separatorType = cfg.has("messageSeparator") ? cfg.get("messageSeparator").asText() : "SYSTEM_LINE_SEPARATOR";
        ByteBuf[] delimiter;
        switch (separatorType) {
            case "NUL":
                delimiter = Delimiters.nulDelimiter();
                break;
            case "CUSTOM":
                if (cfg.has("customSeparatorRawValue") && StringUtils.isNotEmpty(cfg.get("customSeparatorRawValue").asText())) {
                    String d = cfg.get("customSeparatorRawValue").asText().replace("\\n", "\n").replace("\\r", "\r");
                    delimiter = new ByteBuf[]{io.netty.buffer.Unpooled.copiedBuffer(d, StandardCharsets.UTF_8)};
                } else {
                    delimiter = Delimiters.lineDelimiter();
                }
                break;
            default:
                delimiter = Delimiters.lineDelimiter();
        }
        return new DelimiterBasedFrameDecoder(maxFrameLength, stripDelimiter, delimiter);
    }

    @Override
    public void stop() {
        if (serverChannel != null) {
            serverChannel.close();
            serverChannel = null;
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
            bossGroup = null;
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
            workerGroup = null;
        }
    }

    private class UplinkHandler extends SimpleChannelInboundHandler<ByteBuf> {

        private final Charset charset;

        UplinkHandler(Charset charset) {
            this.charset = charset;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
            byte[] data = new byte[msg.readableBytes()];
            msg.readBytes(data);
            Map<String, String> metadata = new HashMap<>();
            metadata.put("remoteAddress", String.valueOf(ctx.channel().remoteAddress()));
            metadata.put("charset", charset.name());
            try {
                uplinkSink.processUplink(integration, data, metadata);
            } catch (Exception e) {
                log.warn("[{}][{}] Failed to process TCP uplink: {}", tenantId, integration.getName(), e.getMessage());
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.debug("[{}] TCP integration channel error: {}", integration.getId(), cause.getMessage());
            ctx.close();
        }
    }

}
