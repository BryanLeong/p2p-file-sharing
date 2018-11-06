import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

class DownloadThread extends Thread {
    private DatagramSocket socket;
    private ConcurrentHashMap<String, byte[]> chunkMap;
    private int chunkSize = 1024;

    public DownloadThread(ConcurrentHashMap<String, byte[]> chunkMap) {
        this.chunkMap = chunkMap;
        try {
            socket = new DatagramSocket(8001);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        while (true) {
            byte[] buf = new byte[chunkSize];
            DatagramPacket packet = new DatagramPacket(buf, chunkSize);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }

            byte[] chunk = packet.getData();
            int chunkNum = ByteBuffer.wrap(chunk).getInt();
            chunk = Arrays.copyOfRange(chunk, 4, chunk.length);
            chunkMap.putIfAbsent(String.valueOf(chunkNum), chunk);
        }
    }
}