package com.whz.gecko.example;

import com.taobao.gecko.service.Connection;
import com.taobao.gecko.service.RemotingFactory;
import com.taobao.gecko.service.RemotingServer;
import com.taobao.gecko.service.RequestProcessor;
import com.taobao.gecko.service.config.ServerConfig;
import com.taobao.gecko.service.exception.NotifyRemotingException;
import com.whz.gecko.WhzWireFormatType;
import com.whz.gecko.command.WhzRequest;
import com.whz.gecko.command.WhzResponse;

import java.net.InetSocketAddress;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @Author: wanghz
 * @Date: 2020/11/18 5:18 PM
 */
public class WhzServerTest {

    private static InetSocketAddress serverAddr = new InetSocketAddress(8080);

    public static void main(String[] args) throws Exception {

        final ServerConfig serverConfig = new ServerConfig();
        serverConfig.setWireFormatType(new WhzWireFormatType());
        serverConfig.setLocalInetSocketAddress(serverAddr);

        RemotingServer remotingServer = RemotingFactory.newRemotingServer(serverConfig);
        remotingServer.registerProcessor(WhzRequest.class, new RequestProcessor<WhzRequest>() {

            @Override
            public void handleRequest(WhzRequest request, Connection conn) {
                String message = request.getMessage();
                System.out.println("接收到请求：" + message);

                try {
                    WhzResponse response = new WhzResponse(message, request.getOpaque());

                    conn.response(response);
                } catch (NotifyRemotingException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public ThreadPoolExecutor getExecutor() {
                return null;
            }

        });
        remotingServer.start();
    }

}
