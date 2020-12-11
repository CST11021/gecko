package com.whz.gecko.example;

import com.taobao.gecko.service.RemotingClient;
import com.taobao.gecko.service.RemotingFactory;
import com.taobao.gecko.service.config.ClientConfig;
import com.whz.gecko.WhzWireFormatType;
import com.whz.gecko.command.WhzRequest;
import com.whz.gecko.command.WhzResponse;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * @Author: wanghz
 * @Date: 2020/11/18 5:48 PM
 */
public class WhzClientTest {

    private static String uri = "test://localhost:8080";


    public static void main(String[] args) throws Exception {
        final ClientConfig clientConfig = new ClientConfig();
        clientConfig.setWireFormatType(new WhzWireFormatType());

        RemotingClient remotingClient = RemotingFactory.newRemotingClient(clientConfig);
        remotingClient.start();

        remotingClient.connect(uri);
        remotingClient.awaitReadyInterrupt(uri);

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            String message = br.readLine();

            WhzRequest request = new WhzRequest(message);
            WhzResponse response = (WhzResponse) remotingClient.invokeToGroup(uri, request);

            System.out.println(response.getResult());
        }

    }


}
