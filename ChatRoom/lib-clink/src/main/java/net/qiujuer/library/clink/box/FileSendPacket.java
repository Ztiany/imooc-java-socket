package net.qiujuer.library.clink.box;

import net.qiujuer.library.clink.core.SendPacket;

import java.io.File;
import java.io.FileInputStream;

public class FileSendPacket extends SendPacket<FileInputStream> {

    public FileSendPacket(File file) {
        this.length = file.length();
    }

    @Override
    protected FileInputStream createStream() {
        return null;
    }

}
