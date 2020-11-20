package com.whz.gecko.client;

import com.taobao.gecko.service.RemotingClient;
import com.taobao.gecko.service.RemotingFactory;
import com.taobao.gecko.service.config.ClientConfig;
import com.whz.gecko.WhzWireFormatType;
import com.whz.gecko.server.WhzResponse;

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

        WhzRequest request = new WhzRequest("测试123。。。");
        remotingClient.setAttribute(uri, WhzWireFormatType.PARAM_TYPE, WhzRequest.class);
        WhzResponse response = (WhzResponse) remotingClient.invokeToGroup(uri, request);

        System.out.println("请求结果：" + response.getResult());

        // BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        // while (true) {
        //     String message = br.readLine();
        //
        //     WhzRequest request = new WhzRequest(message);
        //     remotingClient.setAttribute(uri, WhzWireFormatType.PARAM_TYPE, WhzRequest.class);
        //     WhzResponse response = (WhzResponse) remotingClient.invokeToGroup(uri, request);
        //
        //     System.out.println(response.getResult());
        // }

    }


}
