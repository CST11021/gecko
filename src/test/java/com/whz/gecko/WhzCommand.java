package com.whz.gecko;

import com.taobao.gecko.core.buffer.IoBuffer;

/**
 * @Author: wanghz
 * @Date: 2020/11/19 5:30 PM
 */
public interface WhzCommand {

    public static final IoBuffer heartBeat_request = IoBuffer.wrap("heartBeat_request".getBytes());
    public static final IoBuffer heartBeat_response = IoBuffer.wrap("heartBeat_response".getBytes());

    public Object decode(IoBuffer buffer);

    public IoBuffer encode();

}
