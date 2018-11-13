import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

class UploadThread extends Thread {
    private DatagramSocket socket;
    private InetAddress address;
    private ConcurrentHashMap<String, byte[]> chunkMap;
    private String[] chunkIds;

    public UploadThread(ConcurrentHashMap<String, byte[]> chunkMap,
                        String address, String[] chunkIds) {
        this.chunkMap = chunkMap;
        this.chunkIds = chunkIds;
        try {
            socket = new DatagramSocket();
            this.address = InetAddress.getByName(address);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        for (String chunkId : chunkIds) {
            byte[] chunk = chunkMap.get(chunkId);
            byte[] chunkIdBytes = chunkId.getBytes();

            ByteBuffer bb = ByteBuffer.allocate(8 + chunk.length + chunkIdBytes.length);
            bb.putInt(chunkIdBytes.length);
            bb.putInt(chunk.length);
            bb.put(chunkIdBytes);
            bb.put(chunk);
            byte[] data = bb.array();

            DatagramPacket packet = new DatagramPacket(data, data.length, address, 8000);
            try {
                socket.send(packet);
                Thread.sleep(200);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}