import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * UDP 提供者，用于提供服务
 */
public class UDPProvider {

    public static void main(String[] args) throws IOException {
        System.out.println("UDPProvider Started.");

        // 作为接受者，需要指定接收数据的端口
        DatagramSocket ds = new DatagramSocket(20000);

        // 构建接收实体
        final byte[] buf = new byte[512];
        DatagramPacket receivePack = new DatagramPacket(buf, buf.length);

        // 开始接收
        ds.receive(receivePack);

        // 打印接收到的信息与发送者信息
        String ip = receivePack.getAddress().getHostAddress();
        int port = receivePack.getPort();
        int dataLen = receivePack.getLength();
        String data = new String(receivePack.getData(), 0, dataLen);
        System.out.println("UDPProvider receive from IP:" + ip + "\tPort:" + port + "\tData:" + data);

        // 构建一份回送数据
        String responseData = "Receive data with len:" + dataLen;
        byte[] responseDataBytes = responseData.getBytes();
        // 直接回送到发送者上，发送者的ip与端口
        DatagramPacket responsePack = new DatagramPacket(responseDataBytes,
                responseDataBytes.length,
                receivePack.getAddress(),
                receivePack.getPort());

        // 回送
        ds.send(responsePack);

        // 结束
        System.out.println("UDPProvider Finished.");
        ds.close();
    }
}
