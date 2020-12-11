package com.whz.gecko;

import com.taobao.gecko.core.buffer.IoBuffer;
import com.taobao.gecko.core.core.CodecFactory;
import com.taobao.gecko.core.core.Session;
import com.whz.gecko.command.WhzRequest;
import com.whz.serializlable.CodeUtil;

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

        /**
         * 将消息对象进行编码（序列化），获取对应的字节缓冲区
         *
         * @param message
         * @param session
         * @return
         */
        public IoBuffer encode(Object message, Session session) {
            byte[] bytes = CodeUtil.objectToByteArray(message);
            IoBuffer buffer = IoBuffer.allocate(bytes.length).setAutoExpand(true);
            buffer.put(bytes);
            buffer.flip();

            return buffer;
        }
    }

    class WhzDecoder implements Decoder {

        /**
         * 对buffer进行解码（反序列化），并返回对应的对象
         *
         * @param buffer
         * @param session
         * @return
         */
        public Object decode(IoBuffer buffer, Session session) {
            if (!buffer.hasRemaining()) {
                return null;
            }

            byte[] bytes = new byte[buffer.limit()];
            buffer.get(bytes);
            return CodeUtil.byteArrayToObject(bytes);
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
