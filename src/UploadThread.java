import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;

class UploadThread extends Thread {
    private DatagramSocket socket;
    private InetAddress address;
    List<byte[]> chunks;

    public UploadThread(String address, List<byte[]> chunks) {
        this.chunks = chunks;
        try {
            socket = new DatagramSocket();
            this.address = InetAddress.getByName(address);
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        for (byte[] chunk : chunks) {
            DatagramPacket packet = new DatagramPacket(chunk, chunk.length, address, 8001);
            try {
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}