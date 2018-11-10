import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
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
            byte[] data = String.format("%s,%s", chunkId, new String(chunk)).getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, address, 8001);
            try {
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}