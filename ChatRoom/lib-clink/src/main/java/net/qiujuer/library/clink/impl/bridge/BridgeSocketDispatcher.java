package net.qiujuer.library.clink.impl.bridge;

import net.qiujuer.library.clink.core.*;
import net.qiujuer.library.clink.impl.exceptions.EmptyIoArgsException;
import net.qiujuer.library.clink.utils.plugin.CircularByteBuffer;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 桥接调度器实现
 * 当前调度器同时实现了发送者与接受者调度逻辑
 * 核心思想为：把接受者接收到的数据全部转发给发送者
 */
public class BridgeSocketDispatcher implements ReceiveDispatcher, SendDispatcher {
    /**
     * 数据暂存的缓冲区
     */
    private final CircularByteBuffer mBuffer = new CircularByteBuffer(512, true);
    // 根据缓冲区得到的读取、写入通道
    private final ReadableByteChannel readableByteChannel = Channels.newChannel(mBuffer.getInputStream());
    private final WritableByteChannel writableByteChannel = Channels.newChannel(mBuffer.getOutputStream());

    // 有数据则接收，无数据不强求填满，有多少返回多少
    private final IoArgs receiveIoArgs = new IoArgs(256, false);
    private final Receiver receiver;

    // 当前是否处于发送中
    private final AtomicBoolean isSending = new AtomicBoolean();
    // 用以发送的IoArgs，默认全部发送数据
    private final IoArgs sendIoArgs = new IoArgs();
    private volatile Sender sender;

    public BridgeSocketDispatcher(Receiver receiver) {
        this.receiver = receiver;
    }

    /**
     * 绑定一个新的发送者，在绑定时，将老的发送者对应的调度设置为null
     *
     * @param sender 新的发送者
     */
    public void bindSender(Sender sender) {
        // 清理老的发送者回调
        final Sender oldSender = this.sender;
        if (oldSender != null) {
            oldSender.setSendListener(null);
        }

        // 清理操作
        synchronized (isSending) {
            isSending.set(false);
        }
        mBuffer.clear();

        // 设置新的发送者
        this.sender = sender;
        if (sender != null) {
            sender.setSendListener(senderEventProcessor);
            requestSend();
        }
    }

    /**
     * 外部初始化好了桥接调度器后需要调用start方法开始
     */
    @Override
    public void start() {
        // nothing
        receiver.setReceiveListener(receiverEventProcessor);
        registerReceive();
    }

    @Override
    public void stop() {
        // nothing
        receiver.setReceiveListener(null);
    }

    @Override
    public void send(SendPacket packet) {
        // nothing
    }

    @Override
    public void sendHeartbeat() {
        // nothing
    }

    @Override
    public void cancel(SendPacket packet) {
        // nothing
    }

    @Override
    public void close() {
        // nothing
    }

    /**
     * 请求网络进行数据接收
     */
    private void registerReceive() {
        try {
            receiver.postReceiveAsync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 请求网络进行数据发送
     */
    private void requestSend() {
        synchronized (isSending) {
            final AtomicBoolean isRegisterSending = isSending;
            final Sender sender = this.sender;
            if (isRegisterSending.get() || sender == null) {
                return;
            }

            // 返回True代表当前有数据需要发送
            if (mBuffer.getAvailable() > 0) {
                try {
                    isRegisterSending.set(true);
                    sender.postSendAsync();
                } catch (Exception e) {
                    e.printStackTrace();
                    isRegisterSending.set(false);
                }
            } else {
                isRegisterSending.set(false);
            }
        }
    }

    /**
     * 接受者回调
     */
    private final IoArgs.IoArgsEventProcessor receiverEventProcessor = new IoArgs.IoArgsEventProcessor() {
        @Override
        public IoArgs provideIoArgs() {
            receiveIoArgs.resetLimit();
            // 一份新的IoArgs需要调用一次开始写入数据的操作
            receiveIoArgs.startWriting();
            return receiveIoArgs;
        }

        @Override
        public boolean onConsumeFailed(Throwable e) {
            // args 不可能为null，错误信息直接打印，并且关闭流程
            new RuntimeException(e).printStackTrace();
            return true;
        }

        @Override
        public boolean onConsumeCompleted(IoArgs args) {
            args.finishWriting();
            try {
                args.writeTo(writableByteChannel);
                // 接收数据后请求发送数据
                requestSend();
                // 继续接收数据
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                // 不再接收
                return false;
            }
        }
    };


    /**
     * 发送者回调
     */
    private final IoArgs.IoArgsEventProcessor senderEventProcessor = new IoArgs.IoArgsEventProcessor() {
        @Override
        public IoArgs provideIoArgs() {
            try {
                int available = mBuffer.getAvailable();
                IoArgs args = BridgeSocketDispatcher.this.sendIoArgs;
                if (available > 0) {
                    args.limit(available);
                    args.startWriting();
                    args.readFrom(readableByteChannel);
                    args.finishWriting();
                    return args;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @SuppressWarnings("Duplicates")
        @Override
        public boolean onConsumeFailed(Throwable e) {
            if (e instanceof EmptyIoArgsException) {
                // 设置当前发送状态
                synchronized (isSending) {
                    isSending.set(false);
                    // 继续请求发送当前的数据
                    requestSend();
                }
                // 无需关闭链接
                return false;
            } else {
                // 关闭链接
                return true;
            }
        }

        @Override
        public boolean onConsumeCompleted(IoArgs args) {
            if (mBuffer.getAvailable() > 0) {
                return true;
            } else {
                // 设置当前发送状态
                synchronized (isSending) {
                    isSending.set(false);
                    // 继续请求发送当前的数据
                    requestSend();
                }
                return false;
            }
        }
    };
}
