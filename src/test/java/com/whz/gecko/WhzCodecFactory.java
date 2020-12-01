package com.whz.gecko;

import com.taobao.gecko.core.buffer.IoBuffer;
import com.taobao.gecko.core.core.CodecFactory;
import com.taobao.gecko.core.core.Session;
import com.whz.gecko.client.WhzRequest;
import com.whz.gecko.code.CodeUtil;

/**
 * @Author: wanghz
 * @Date: 2020/11/19 5:06 PM
 */
public class WhzCodecFactory implements CodecFactory {

    @Override
    public Encoder getEncoder() {
        return new WhzEncoder();
    }

    @Override
    public Decoder getDecoder() {
        return new WhzDecoder();
    }

    class WhzEncoder implements Encoder {

        @Override
        public IoBuffer encode(Object message, Session session) {

            // if (message instanceof WhzCommand) {
            //     WhzRequest request = (WhzRequest) message;
            //     return request.encode();
            // } else {
            //     throw new RuntimeException("编码异常");
            // }

            return IoBuffer.wrap(CodeUtil.objectToByteArray(message));
        }

    }

    class WhzDecoder implements Decoder {

        @Override
        public Object decode(IoBuffer buff, Session session) {

            // WhzCommand command = (WhzCommand) session.getAttribute(WhzWireFormatType.PARAM_TYPE);
            // if (command != null) {
            //     return command.decode(buff);
            // }
            //
            // if (buff.equals(WhzCommand.heartBeat_request)) {
            //     return new WhzHeartBeatRequestCommand();
            // }
            //
            // if (buff.equals(WhzCommand.heartBeat_response)) {
            //     return new WhzBooleanAckCommand();
            // }
            //
            // throw new RuntimeException("解码异常");

            return CodeUtil.byteArrayToObject(buff.array());

        }

    }

    public static void main(String[] args) throws Exception {
        WhzRequest request = new WhzRequest("hello");
        WhzCodecFactory factory = new WhzCodecFactory();
        IoBuffer buffer = factory.getEncoder().encode(request, null);
        // System.out.println(buffer.getString(Charset.forName("UTF-8").newDecoder()));

        Object object = factory.getDecoder().decode(buffer, null);

        WhzRequest request1 = (WhzRequest) object;
        System.out.println(request1.getMessage());
    }

}
