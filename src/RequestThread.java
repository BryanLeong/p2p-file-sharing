import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
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
        while (true) {
            if (peerMap.isEmpty()) { // Check if list of peers is empty
                // Send 'query' message to broadcast address
                msg = "query," + chunkMap.keySet().toString().replaceAll("\\[|\\]", "");
                byte[] msgBytes = msg.getBytes();
                try {
                    address = InetAddress.getByName("255.255.255.255");
                    packet = new DatagramPacket(msgBytes, msgBytes.length, address, 8000);
                    socket.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                // If not currently downloading from any peer,
                for (String peer : peerMap.keySet()) {
                    if (batchMap.containsKey(peer)) {
                        continue;
                    }
                    // check which chunks peer has but we do not and are not currently downloading
                    // create batch of chunks based on scarcest-first algo
                    // send 'request' message to peer to initiate file transfer
                }
            }
        }
    }
}
