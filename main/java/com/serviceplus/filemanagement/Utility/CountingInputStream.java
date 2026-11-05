package com.serviceplus.filemanagement.Utility;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class CountingInputStream  extends FilterInputStream {

    private long bytesRead = 0;

    public CountingInputStream(InputStream in) {
        super(in);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int n = super.read(b, off, len);
        if (n > 0) bytesRead += n;
        return n;
    }

    @Override
    public int read() throws IOException {
        int n = super.read();
        if (n != -1) bytesRead++;
        return n;
    }

    public long getBytesRead() {
        return bytesRead;
    }

}
