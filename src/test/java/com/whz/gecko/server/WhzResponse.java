package com.whz.gecko.server;

import com.taobao.gecko.core.buffer.IoBuffer;
import com.taobao.gecko.core.command.ResponseCommand;
import com.taobao.gecko.core.command.ResponseStatus;
import com.taobao.gecko.core.command.kernel.BooleanAckCommand;
import com.whz.gecko.WhzCommand;
import com.whz.gecko.code.CodeUtil;

import java.io.Serializable;
import java.net.InetSocketAddress;

/**
 * @Author: wanghz
 * @Date: 2020/11/18 5:55 PM
 */
public class WhzResponse implements ResponseCommand, BooleanAckCommand, WhzCommand, Serializable {

    private static final long serialVersionUID = -4601915156778195626L;

    private String result;
    private Integer opaque;
    private InetSocketAddress responseHost;
    private ResponseStatus responseStatus;
    private long responseTime;
    private String errorMsg;


    public WhzResponse(String result, Integer opaque, ResponseStatus responseStatus) {
        this.result = result;
        this.opaque = opaque;
        this.responseStatus = responseStatus;
        this.responseTime = System.nanoTime();
    }

    public String getResult() {
        return result;
    }
    public void setResult(String result) {
        this.result = result;
    }


    @Override
    public ResponseStatus getResponseStatus() {
        return responseStatus;
    }

    @Override
    public void setResponseStatus(ResponseStatus responseStatus) {
        this.responseStatus = responseStatus;
    }

    /**
     * 是否为BooleanAckCommand
     *
     * @return
     */
    @Override
    public boolean isBoolean() {
        return true;
    }

    @Override
    public InetSocketAddress getResponseHost() {
        return this.responseHost;
    }

    @Override
    public void setResponseHost(InetSocketAddress address) {
        this.responseHost = address;
    }

    @Override
    public long getResponseTime() {
        return this.responseTime;
    }
    @Override
    public void setResponseTime(long time) {
        this.responseTime = time;
    }

    @Override
    public void setOpaque(Integer opaque) {
        this.opaque = opaque;
    }
    @Override
    public Integer getOpaque() {
        return opaque;
    }

    @Override
    public String getErrorMsg() {
        return errorMsg;
    }

    @Override
    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    @Override
    public Object decode(IoBuffer buffer) {
        // String result;
        // try {
        //     result = buffer.getString(Charset.forName("UTF-8").newDecoder());
        // } catch (CharacterCodingException e) {
        //     throw new RuntimeException(e);
        // }
        // return new WhzRequest(result);

        return CodeUtil.byteArrayToObject(buffer.array());
    }

    @Override
    public IoBuffer encode() {
        // final IoBuffer buffer = IoBuffer.allocate(1024);
        // buffer.setAutoExpand(true);
        // buffer.put(result.getBytes());
        //
        // buffer.flip();
        // return buffer;

        return IoBuffer.wrap(CodeUtil.objectToByteArray(this));
    }
}
