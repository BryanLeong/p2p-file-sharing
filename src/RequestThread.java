import java.io.IOException;
import java.net.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

class RequestThread extends Thread {
    private DatagramSocket socket;
    private ConcurrentHashMap<String, byte[]> chunkMap;
    private ConcurrentHashMap<String, Set<String>> peerMap;
    private ConcurrentHashMap<String, Set<String>> batchMap;
    private CopyOnWriteArrayList<String> requestedChunks;


    public RequestThread(ConcurrentHashMap<String, byte[]> chunkMap,
                         ConcurrentHashMap<String, Set<String>> peerMap,
                         ConcurrentHashMap<String, Set<String>> batchMap,
                         CopyOnWriteArrayList<String> requestedChunks) {
        this.chunkMap = chunkMap;
        this.peerMap = peerMap;
        this.batchMap = batchMap;
        this.requestedChunks = requestedChunks;
        try {
            socket = new DatagramSocket();
            socket.setBroadcast(true);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        String msg;
        InetAddress address;
        DatagramPacket packet;
        Set<String> batch;

        // Send 'query' message to broadcast address on startup to discover peers
        msg = "query," + chunkMap.keySet().toString().replaceAll("\\[|\\]", "");
        byte[] msgBytes = msg.getBytes();
        try {
            address = InetAddress.getByName("255.255.255.255");
            packet = new DatagramPacket(msgBytes, msgBytes.length, address, 8000);
            socket.send(packet);
            Thread.sleep(2000);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        while (true) {
            // If not currently downloading from any peer,
            for (Map.Entry<String, Set<String>> entry : peerMap.entrySet()) {
                String peer = entry.getKey();
                if (batchMap.containsKey(peer)) {
                    continue;
                }
                // check which chunks peer has but we do not and are not currently downloading

                // create batch of chunks based on scarcest-first algo
                batch = new HashSet<String>();

                // send 'request' message to peer to initiate file transfer
                String request = "request," + batch.toString().replaceAll("\\[|\\]", "");
                byte[] requestBytes = request.getBytes();
                try {
                    address = InetAddress.getByName(peer);
                    packet = new DatagramPacket(requestBytes, requestBytes.length, address, 8000);
                    socket.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
