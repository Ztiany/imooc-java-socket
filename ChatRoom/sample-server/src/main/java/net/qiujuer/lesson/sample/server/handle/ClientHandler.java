package net.qiujuer.lesson.sample.server.handle;


import net.qiujuer.lesson.sample.foo.Foo;
import net.qiujuer.library.clink.box.StringReceivePacket;
import net.qiujuer.library.clink.core.Connector;
import net.qiujuer.library.clink.core.Packet;
import net.qiujuer.library.clink.core.ReceivePacket;
import net.qiujuer.library.clink.utils.CloseUtils;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executor;

public class ClientHandler extends Connector {
    private final File cachePath;
    private final String clientInfo;
    private final Executor deliveryPool;
    private final ConnectorCloseChain closeChain = new DefaultPrintConnectorCloseChain();
    private final ConnectorStringPacketChain stringPacketChain = new DefaultNonConnectorStringPacketChain();

    public ClientHandler(SocketChannel socketChannel, File cachePath, Executor deliveryPool) throws IOException {
        this.clientInfo = socketChannel.getRemoteAddress().toString();
        this.cachePath = cachePath;
        this.deliveryPool = deliveryPool;
        setup(socketChannel);
    }

    public String getClientInfo() {
        return clientInfo;
    }

    /**
     * 外部调用的退出操作
     */
    public void exit() {
        CloseUtils.close(this);
        closeChain.handle(this, this);
    }

    /**
     * 内部监测到链接断开的回调
     *
     * @param channel SocketChannel
     */
    @Override
    public void onChannelClosed(SocketChannel channel) {
        super.onChannelClosed(channel);
        closeChain.handle(this, this);
    }

    @Override
    protected File createNewReceiveFile() {
        return Foo.createRandomTemp(cachePath);
    }

    @Override
    protected void onReceivedPacket(ReceivePacket packet) {
        super.onReceivedPacket(packet);
        switch (packet.type()) {
            case Packet.TYPE_MEMORY_STRING: {
                deliveryStringPacket((StringReceivePacket) packet);
                break;
            }
            default: {
                System.out.println("New Packet:" + packet.type() + "-" + packet.length());
            }
        }
    }

    /**
     * 避免阻塞当前的数据读取线程调度，则单独交给另外一个调度线程进行数据调度
     *
     * @param packet StringReceivePacket
     */
    private void deliveryStringPacket(StringReceivePacket packet) {
        deliveryPool.execute(() -> stringPacketChain.handle(this, packet));
    }

    /**
     * 获取当前链接的消息处理责任链 链头
     *
     * @return ConnectorStringPacketChain
     */
    public ConnectorStringPacketChain getStringPacketChain() {
        return stringPacketChain;
    }

    /**
     * 获取当前链接的关闭链接处理责任链 链头
     *
     * @return ConnectorCloseChain
     */
    public ConnectorCloseChain getCloseChain() {
        return closeChain;
    }
}
