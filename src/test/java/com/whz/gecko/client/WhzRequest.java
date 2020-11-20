package com.whz.gecko.client;

import com.taobao.gecko.core.buffer.IoBuffer;
import com.taobao.gecko.core.command.CommandHeader;
import com.taobao.gecko.core.command.RequestCommand;
import com.taobao.gecko.core.command.kernel.HeartBeatRequestCommand;
import com.taobao.gecko.core.util.OpaqueGenerator;
import com.whz.gecko.WhzCommand;
import com.whz.gecko.code.CodeUtil;

import java.io.Serializable;

/**
 * @Author: wanghz
 * @Date: 2020/11/18 5:41 PM
 */
public class WhzRequest implements RequestCommand, HeartBeatRequestCommand, WhzCommand, Serializable {

    private static final long serialVersionUID = 5749226672361719157L;

    private String message;

    private int opaque;

    public WhzRequest(String message) {
        this.message = message;
        this.opaque = OpaqueGenerator.getNextOpaque();
        // this.opCode = OpCode.SEND_MESSAGE;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public CommandHeader getRequestHeader() {
        return this;
    }

    @Override
    public Integer getOpaque() {
        return opaque;
    }

    @Override
    public Object decode(IoBuffer buffer) {
        // String message;
        // try {
        //     message = buffer.getString(Charset.forName("UTF-8").newDecoder());
        // } catch (CharacterCodingException e) {
        //     throw new RuntimeException(e);
        // }
        // return new WhzRequest(message);

        return CodeUtil.byteArrayToObject(buffer.array());
    }

    @Override
    public IoBuffer encode() {
        // final IoBuffer buffer = IoBuffer.allocate(1024);
        // buffer.setAutoExpand(true);
        //
        // buffer.put(message.getBytes());
        // buffer.flip();
        //
        // return buffer;

        return IoBuffer.wrap(CodeUtil.objectToByteArray(this));
    }
}
