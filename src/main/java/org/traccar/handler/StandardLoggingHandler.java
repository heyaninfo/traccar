/*
 * Copyright 2019 - 2023 Anton Tananaev (anton@traccar.org)
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
package org.traccar.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.NetworkMessage;
import org.traccar.helper.NetworkUtil;
import org.traccar.model.LogRecord;
import org.traccar.session.ConnectionManager;

import java.net.InetSocketAddress;

public class StandardLoggingHandler extends ChannelDuplexHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(StandardLoggingHandler.class);

    private final String protocol;
    private ConnectionManager connectionManager;

    public StandardLoggingHandler(String protocol) {
        this.protocol = protocol;
    }

    @Inject
    public void setConnectionManager(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        LogRecord record = createLogRecord(msg);
        log(ctx, false, record);
        super.channelRead(ctx, msg);
        if (record != null) {
            connectionManager.updateLog(record);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        log(ctx, true, createLogRecord(msg));
        super.write(ctx, msg, promise);
    }

    private LogRecord createLogRecord(Object msg) {
        if (msg instanceof NetworkMessage) {
            NetworkMessage networkMessage = (NetworkMessage) msg;
            if (networkMessage.getMessage() instanceof ByteBuf) {
                LogRecord record = new LogRecord();
                record.setAddress((InetSocketAddress) networkMessage.getRemoteAddress());
                record.setData(ByteBufUtil.hexDump((ByteBuf) networkMessage.getMessage()));
                return record;
            }
        }
        return null;
    }

    private void log(ChannelHandlerContext ctx, boolean downstream, LogRecord record) {
        if (record != null) {
            StringBuilder message = new StringBuilder();
            message.append("[").append(NetworkUtil.session(ctx.channel())).append(": ");
            message.append(protocol);
            message.append(downstream ? " > " : " < ");
            message.append(record.getAddress().getHostString());
            message.append("] ");
            message.append(record.getData());
            LOGGER.info(message.toString());
        }
    }

}
