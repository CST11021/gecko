package com.whz.gecko.heardbeat;

import com.taobao.gecko.core.buffer.IoBuffer;
import com.taobao.gecko.core.command.CommandHeader;
import com.taobao.gecko.core.command.kernel.HeartBeatRequestCommand;
import com.whz.gecko.WhzCommand;

import java.io.Serializable;

/**
 * @Author: wanghz
 * @Date: 2020/11/19 7:33 PM
 */
public class WhzHeartBeatRequestCommand implements HeartBeatRequestCommand, WhzCommand, Serializable {

    @Override
    public CommandHeader getRequestHeader() {
        return this;
    }

    @Override
    public Integer getOpaque() {
        return 0;
    }

    @Override
    public Object decode(IoBuffer buffer) {
        return heartBeat_request;
    }

    @Override
    public IoBuffer encode() {
        return heartBeat_request;
    }
}
