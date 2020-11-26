package com.whz.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

public class Server {
    private Selector selector;
    private ByteBuffer readBuffer = ByteBuffer.allocate(1024);//调整缓存的大小可以看到打印输出的变化 
    private ByteBuffer sendBuffer = ByteBuffer.allocate(1024);//调整缓存的大小可以看到打印输出的变化 

    String str;

    public void start() throws IOException {
        // 打开服务器套接字通道 
        ServerSocketChannel ssc = ServerSocketChannel.open();
        // 服务器配置为非阻塞 
        ssc.configureBlocking(false);
        // 进行服务的绑定 
        ssc.bind(new InetSocketAddress("localhost", 8001));

        // 通过open()方法找到Selector
        selector = Selector.open();
        // 将channel注册到selector，等待连接
        ssc.register(selector, SelectionKey.OP_ACCEPT);

        while (!Thread.currentThread().isInterrupted()) {
            selector.select();
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = keys.iterator();
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                if (!key.isValid()) {
                    continue;
                }

                //
                if (key.isAcceptable()) {
                    accept(key);
                } else if (key.isReadable()) {
                    read(key);
                } else if (key.isWritable()) {
                    write(key);
                }
                keyIterator.remove(); //该事件已经处理，可以丢弃
            }
        }
    }

    /**
     * 接收客户端连接请求
     *
     * @param key
     * @throws IOException
     */
    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = ssc.accept();
        clientChannel.configureBlocking(false);

        // 建立连接后注册读事件
        clientChannel.register(selector, SelectionKey.OP_READ);
        System.out.println("a new client connected " + clientChannel.getRemoteAddress());
    }

    /**
     * 读取客户端请求
     *
     * @param key
     * @throws IOException
     */
    private void read(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        // Clear out our read buffer so it's ready for new data 
        this.readBuffer.clear();
        // readBuffer.flip();
        // Attempt to read off the channel 
        int numRead;
        try {
            // 将数据保存到缓存区，并返回缓冲区的字节大小
            numRead = socketChannel.read(this.readBuffer);
        } catch (IOException e) {
            // The remote forcibly closed the connection, cancel the selection key and close the channel.
            key.cancel();
            socketChannel.close();
            return;
        }

        str = new String(readBuffer.array(), 0, numRead);
        System.out.println(str);

        // 读取完客户端的消息后，注册写事件
        socketChannel.register(selector, SelectionKey.OP_WRITE);
    }

    /**
     * 回写客户端响应
     *
     * @param key
     * @throws IOException
     * @throws ClosedChannelException
     */
    private void write(SelectionKey key) throws IOException, ClosedChannelException {
        SocketChannel channel = (SocketChannel) key.channel();
        System.out.println("write:" + str);

        sendBuffer.clear();
        sendBuffer.put(str.getBytes());
        sendBuffer.flip();
        channel.write(sendBuffer);
        channel.register(selector, SelectionKey.OP_READ);
    }

    public static void main(String[] args) throws IOException {
        System.out.println("server started...");
        new Server().start();
    }
} 