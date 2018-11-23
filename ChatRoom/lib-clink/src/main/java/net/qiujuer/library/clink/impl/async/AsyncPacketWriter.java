package net.qiujuer.library.clink.impl.async;

import net.qiujuer.library.clink.core.IoArgs;
import net.qiujuer.library.clink.core.ReceivePacket;

import java.io.Closeable;
import java.io.IOException;

/**
 * 写数据到Packet中
 */
class AsyncPacketWriter implements Closeable {
    private final PacketProvider provider;

    AsyncPacketWriter(PacketProvider provider) {
        this.provider = provider;
    }


    /**
     * 构建一份数据容纳封装
     * 当前帧如果没有则返回至少6字节长度的IoArgs，
     * 如果当前帧有，则返回当前帧未消费完成的区间
     *
     * @return IoArgs
     */
    IoArgs takeIoArgs() {
        return null;
    }

    /**
     * 消费IoArgs中的数据
     *
     * @param args IoArgs
     */
    void consumeIoArgs(IoArgs args) {

    }

    /**
     * 关闭操作，关闭时若当前还有正在接收的Packet，则尝试停止对应的Packet接收
     */
    @Override
    public void close() throws IOException {

    }

    /**
     * Packet提供者
     */
    interface PacketProvider {

        /**
         * 拿Packet操作
         *
         * @param type       Packet类型
         * @param length     Packet长度
         * @param headerInfo Packet headerInfo
         * @return 通过类型，长度，描述等信息得到一份接收Packet
         */
        ReceivePacket takePacket(byte type, long length, byte[] headerInfo);

        /**
         * 结束一份Packet
         *
         * @param packet    接收包
         * @param isSucceed 是否成功接收完成
         */
        void completedPacket(ReceivePacket packet, boolean isSucceed);
    }
}
