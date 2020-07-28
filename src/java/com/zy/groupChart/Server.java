package com.zy.groupChart;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

/**
 * @author zy
 * @creat 2020-07-{DAY}-{TIME}
 */
public class Server {

    private ServerSocketChannel servSocketChannel;
    private Selector selector;

    /**
     * 初始化服务器
     * */
    public Server(){

        try {
            servSocketChannel = ServerSocketChannel.open();
            servSocketChannel.socket().bind(new InetSocketAddress(7000));
            selector = Selector.open();
            //开启非阻塞模式
            servSocketChannel.configureBlocking(false);
            //注册serverSocketChannel
            servSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 监听连接
     * */
    public void listen()  {
        while (true) {
            Iterator<SelectionKey> keyIterator = null;
            try {
                //这里我们等待1秒，如果没有事件发生, 返回
                if(selector.select(1000) == 0) { //没有事件发生

                    continue;
                }
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                keyIterator = selectionKeys.iterator();
                if(keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    //客户端连接
                    if(key.isAcceptable()) {
                        SocketChannel socketChannel = servSocketChannel.accept();
                        socketChannel.configureBlocking(false);
                        socketChannel.register(selector, SelectionKey.OP_READ);
                        System.out.println(socketChannel.getRemoteAddress() + "上线");
                    }
                    //读已就绪
                    if(key.isReadable()) {
                        readData(key);
                    }else{
                        System.out.println("等待处理");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            //当前的key 删除，防止重复处理
            keyIterator.remove();
        }
    }
    public void readData(SelectionKey selectionKey) {
        SocketChannel channel = (SocketChannel)selectionKey.channel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        try {
            int read = channel.read(byteBuffer);
            while (read > 0) {
                String msg = new String(byteBuffer.array());
                System.out.println(msg);
                //转发
                sendInfoToOtherClients(msg, channel);
                byteBuffer.clear();
                read = channel.read(byteBuffer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendInfoToOtherClients(String msg, SocketChannel channel) {
        Set<SelectionKey> keys = selector.keys();
        for (SelectionKey key : keys) {
            SelectableChannel targetChannel = key.channel();
            if(targetChannel instanceof SocketChannel && targetChannel != channel) {
                SocketChannel socketChannel = (SocketChannel) targetChannel;
                ByteBuffer wrap = ByteBuffer.wrap(msg.getBytes());
                try {
                    socketChannel.write(wrap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.listen();
    }
}
