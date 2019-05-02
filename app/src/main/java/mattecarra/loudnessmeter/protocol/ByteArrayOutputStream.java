package mattecarra.loudnessmeter.protocol;

public class ByteArrayOutputStream extends java.io.ByteArrayOutputStream {
    public ByteArrayOutputStream(byte[] buf) {
        this.buf = buf;
        this.reset();
    }
}
