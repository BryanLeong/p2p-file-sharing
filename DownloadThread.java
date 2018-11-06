import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;

class DownloadThread extends Thread {
    private DatagramSocket socket;
    private int chunkSize = 1024;

    public DownloadThread() {
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

            byte[] data = packet.getData();
            int chunkNum = ByteBuffer.wrap(data).getInt();
            data = Arrays.copyOfRange(data, 4, data.length);

            // Store received chunkNum and data in some form of shared memory
        }
    }
}