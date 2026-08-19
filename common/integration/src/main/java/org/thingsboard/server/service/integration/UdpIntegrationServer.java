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
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;

import java.util.HashMap;
import java.util.Map;

/**
 * In-process UDP server for a UDP integration: listens on the configured port and feeds each
 * received datagram to the uplink processing chain. Configuration JSON: {port}. PE has no
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

        group = new NioEventLoopGroup(1, new DefaultThreadFactory("udp-integration"));
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST, false)
                    .handler(new SimpleChannelInboundHandler<DatagramPacket>() {
                        @Override
                        protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
                            byte[] data = new byte[packet.content().readableBytes()];
                            packet.content().readBytes(data);
                            Map<String, String> metadata = new HashMap<>();
                            metadata.put("remoteAddress", String.valueOf(packet.sender()));
                            try {
                                uplinkSink.processUplink(integration, data, metadata);
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
