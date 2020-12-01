package com.taobao.gecko.example.rpc.transport;

import com.taobao.gecko.core.buffer.IoBuffer;
import com.taobao.gecko.core.command.CommandHeader;
import com.taobao.gecko.core.command.kernel.HeartBeatRequestCommand;
import com.taobao.gecko.example.rpc.command.RpcCommand;
import com.taobao.gecko.example.rpc.command.RpcRequest;

public class RpcHeartBeatCommand implements HeartBeatRequestCommand, RpcCommand {

    private RpcRequest request = new RpcRequest("heartBeat" + System.currentTimeMillis(), "heartBeat" + System.currentTimeMillis(), null);

    // 实现RequestCommand接口

    /**
     * 返回一个心跳的请求对象
     *
     * @return
     */
    public CommandHeader getRequestHeader() {
        return this.request;
    }

    // 实现CommandHeader接口

    /**
     * 获取一个命令id
     *
     * @return
     */
    public Integer getOpaque() {
        return this.request.getOpaque();
    }

    @Override
    public IoBuffer encode() {
        return request.encode();
    }

    @Override
    public boolean decode(IoBuffer buffer) {
        return request.decode(buffer);
    }

}