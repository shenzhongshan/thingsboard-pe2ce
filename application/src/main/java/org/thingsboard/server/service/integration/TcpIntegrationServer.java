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
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * In-process TCP server for a TCP integration: listens on the configured port and feeds each
 * received frame to the uplink processing chain. Configuration JSON: {port, handlerType:
 * 'TEXT'|'JSON'|'BINARY', maxFrameLength?, charsetName?, delimiter?}. TEXT/JSON frames are split
 * on a delimiter (line-based by default); BINARY passes each TCP read as one frame. CE port:
 * PE's richer binary length-field framing and socket tuning options are dropped.
 */
@Slf4j
public class TcpIntegrationServer implements IntegrationRuntime {

    private final Integration integration;
    private final PlatformIntegrationService platformIntegrationService;
    @Getter
    private final TenantId tenantId;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public TcpIntegrationServer(Integration integration, PlatformIntegrationService platformIntegrationService) {
        this.integration = integration;
        this.platformIntegrationService = platformIntegrationService;
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
        boolean binary = "BINARY".equalsIgnoreCase(handlerType);

        bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("tcp-integration-boss"));
        workerGroup = new NioEventLoopGroup(new DefaultThreadFactory("tcp-integration-worker"));
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            if (!binary) {
                                ch.pipeline().addLast(new DelimiterBasedFrameDecoder(maxFrameLength, true, delimiter(cfg)));
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

    private ByteBuf[] delimiter(JsonNode cfg) {
        if (cfg.has("delimiter") && StringUtils.isNotEmpty(cfg.get("delimiter").asText())) {
            String d = cfg.get("delimiter").asText().replace("\\n", "\n").replace("\\r", "\r");
            return new ByteBuf[]{io.netty.buffer.Unpooled.copiedBuffer(d, StandardCharsets.UTF_8)};
        }
        return Delimiters.lineDelimiter();
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
                platformIntegrationService.processUplink(integration, data, metadata);
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
