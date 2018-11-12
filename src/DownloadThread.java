import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

class DownloadThread extends Thread {
    private DatagramSocket socket;
    private ConcurrentHashMap<String, byte[]> chunkMap;
    private ConcurrentHashMap<String, Set<String>> batchMap;
    private CopyOnWriteArrayList<String> requestedChunks;
    private CopyOnWriteArrayList<String> newChunks;
    private Thread updateThread;

    public DownloadThread(InetAddress localAddress,
                          Thread updateThread,
                          ConcurrentHashMap<String, byte[]> chunkMap,
                          ConcurrentHashMap<String, Set<String>> batchMap,
                          CopyOnWriteArrayList<String> requestedChunks,
                          CopyOnWriteArrayList<String> newChunks) {
        this.updateThread = updateThread;
        this.chunkMap = chunkMap;
        this.batchMap = batchMap;
        this.requestedChunks = requestedChunks;
        this.newChunks = newChunks;
        try {
            socket = new DatagramSocket(8000, localAddress);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        String peer;
        DatagramPacket packet;

        while (true) {
            byte[] buf = new byte[20000];
            packet = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }

            peer = packet.getAddress().getHostAddress();
            ByteBuffer bb = ByteBuffer.wrap(packet.getData());
            int chunkIdLength = bb.getInt();
            int dataLength = bb.getInt();
            byte[] chunkIdBytes = new byte[chunkIdLength];
            byte[] data = new byte[dataLength];
            bb.get(chunkIdBytes, 0, chunkIdLength);
            bb.get(data, 0, dataLength);
            String chunkId = new String(chunkIdBytes);

            chunkMap.putIfAbsent(chunkId, data);
            batchMap.get(peer).remove(chunkId);
            if (batchMap.get(peer).isEmpty()) {
                batchMap.remove(peer);
            }
            requestedChunks.remove(chunkId);
            synchronized (newChunks) {
                newChunks.add(chunkId);
            }

            if (newChunks.size() >= 10) {
                if (!updateThread.isInterrupted())
                updateThread.interrupt();
            }
        }
    }
}