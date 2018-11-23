package net.qiujuer.lesson.sample.server;

import net.qiujuer.lesson.sample.server.handle.ClientHandler;
import net.qiujuer.library.clink.utils.CloseUtils;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServer implements ClientHandler.ClientHandlerCallback,
        ServerAcceptor.AcceptListener {
    private final int port;
    private final File cachePath;
    private final ExecutorService forwardingThreadPoolExecutor;
    private List<ClientHandler> clientHandlerList = new ArrayList<>();
    private ServerAcceptor acceptor;
    private ServerSocketChannel server;

    private long receiveSize;
    private long sendSize;

    TCPServer(int port, File cachePath) {
        this.port = port;
        this.cachePath = cachePath;
        // 转发线程池
        this.forwardingThreadPoolExecutor = Executors.newSingleThreadExecutor();
    }

    boolean start() {
        try {
            ServerAcceptor acceptor = new ServerAcceptor(this);

            ServerSocketChannel server = ServerSocketChannel.open();
            // 设置为非阻塞
            server.configureBlocking(false);
            // 绑定本地端口
            server.socket().bind(new InetSocketAddress(port));
            // 注册客户端连接到达监听
            server.register(acceptor.getSelector(), SelectionKey.OP_ACCEPT);

            this.server = server;
            this.acceptor = acceptor;

            // 线程需要启动
            acceptor.start();
            if (acceptor.awaitRunning()) {
                System.out.println("服务器准备就绪～");
                System.out.println("服务器信息：" + server.getLocalAddress().toString());
                return true;
            } else {
                System.out.println("启动异常！");
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    void stop() {
        if (acceptor != null) {
            acceptor.exit();
        }

        CloseUtils.close(server);

        synchronized (TCPServer.this) {
            for (ClientHandler clientHandler : clientHandlerList) {
                clientHandler.exit();
            }

            clientHandlerList.clear();
        }

        // 停止线程池
        forwardingThreadPoolExecutor.shutdownNow();
    }

    public synchronized void broadcast(String str) {
        for (ClientHandler clientHandler : clientHandlerList) {
            clientHandler.send(str);
        }
        // 发送数量增加
        sendSize += clientHandlerList.size();
    }

    @Override
    public synchronized void onSelfClosed(ClientHandler handler) {
        clientHandlerList.remove(handler);
    }

    @Override
    public void onNewMessageArrived(final ClientHandler handler, final String msg) {
        // 接收数量加一
        receiveSize++;

        // 异步提交转发任务
        forwardingThreadPoolExecutor.execute(() -> {
            synchronized (TCPServer.this) {
                for (ClientHandler clientHandler : clientHandlerList) {
                    if (clientHandler.equals(handler)) {
                        // 跳过自己
                        continue;
                    }
                    // 对其他客户端发送消息
                    clientHandler.send(msg);
                    // 发送数量加一
                    sendSize++;
                }
            }
        });
    }

    /**
     * 获取当前的状态信息
     */
    Object[] getStatusString() {
        return new String[]{
                "客户端数量：" + clientHandlerList.size(),
                "发送数量：" + sendSize,
                "接收数量：" + receiveSize
        };
    }

    @Override
    public void onNewSocketArrived(SocketChannel channel) {
        try {
            ClientHandler clientHandler = new ClientHandler(channel, this, cachePath);
            System.out.println(clientHandler.getClientInfo() + ":Connected!");
            synchronized (TCPServer.this) {
                clientHandlerList.add(clientHandler);
                System.out.println("当前客户端数量：" + clientHandlerList.size());
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("客户端链接异常：" + e.getMessage());
        }

    }
}
