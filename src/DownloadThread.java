import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

class DownloadThread extends Thread {
    private DatagramSocket socket;
    private ConcurrentHashMap<String, byte[]> chunkMap;
    private ConcurrentHashMap<String, Set<String>> batchMap;
    private CopyOnWriteArrayList<String> requestedChunks;
    private int chunkSize;

    public DownloadThread(int chunkSize,
                          ConcurrentHashMap<String, byte[]> chunkMap,
                          ConcurrentHashMap<String, Set<String>> batchMap,
                          CopyOnWriteArrayList<String> requestedChunks) {
        this.chunkSize = chunkSize;
        this.chunkMap = chunkMap;
        this.batchMap = batchMap;
        this.requestedChunks = requestedChunks;
        try {
            socket = new DatagramSocket(8001);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        String peer;
        String[] data;
        DatagramPacket packet;

        while (true) {
            byte[] buf = new byte[chunkSize];
            packet = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }

            peer = packet.getAddress().getHostAddress();
            data = (new String(packet.getData())).trim().split(",", 2);

            chunkMap.putIfAbsent(data[0], data[1].getBytes());
            batchMap.get(peer).remove(data[0]);
            if (batchMap.get(peer).isEmpty()) {
                batchMap.remove(peer);
                // send all peers updated list of chunks
            }
            requestedChunks.remove(data[0]);
        }
    }
}