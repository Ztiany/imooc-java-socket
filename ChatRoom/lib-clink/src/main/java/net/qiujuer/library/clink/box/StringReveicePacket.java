package net.qiujuer.library.clink.box;

import net.qiujuer.library.clink.core.ReceivePacket;

public class StringReveicePacket extends ReceivePacket {
    private byte[] buffer;
    private int position;

    public StringReveicePacket(int len) {
        buffer = new byte[len];
        length = len;
    }

    @Override
    public void save(byte[] bytes, int count) {
        System.arraycopy(bytes, 0, buffer, position, count);
        position += count;
    }

    public String string() {
        return new String(buffer);
    }
}
