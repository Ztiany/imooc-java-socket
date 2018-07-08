import java.io.IOException;
import java.net.*;

/**
 * UDP 搜索者，用于搜索服务支持方
 */
public class UDPSearcher {
    public static void main(String[] args) throws IOException {
        System.out.println("UDPSearcher Started.");

        // 发送者无需指定发送端口，系统随机分配
        DatagramSocket ds = new DatagramSocket();

        // 构建发送包
        String requestData = "HelloWord!";
        final byte[] requestDataBytes = requestData.getBytes();
        DatagramPacket requestPack = new DatagramPacket(requestDataBytes, requestDataBytes.length);
        // DatagramPacket 需包含接收者的目标地址信息：127.0.0.1-20000
        requestPack.setAddress(InetAddress.getLocalHost());
        requestPack.setPort(20000);

        // 发送
        ds.send(requestPack);


        // 构建接收回送消息实体
        final byte[] buf = new byte[512];
        DatagramPacket receivePack = new DatagramPacket(buf, buf.length);

        // 接收
        ds.receive(receivePack);

        String ip = receivePack.getAddress().getHostAddress();
        int port = receivePack.getPort();
        int dataLen = receivePack.getLength();
        String data = new String(receivePack.getData(), 0, dataLen);
        System.out.println("UDPSearcher receive from IP:" + ip + "\tPort:" + port + "\tData:" + data);

        System.out.println("UDPSearcher Finished.");
        ds.close();
    }
}
