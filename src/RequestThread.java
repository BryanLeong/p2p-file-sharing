import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

class RequestThread extends Thread {
    private DatagramSocket socket;
    private ConcurrentHashMap<String, byte[]> chunkMap;
    private ConcurrentHashMap<String, Set<String>> peerMap;
    private ConcurrentHashMap<String, Set<String>> batchMap;
    private CopyOnWriteArrayList<String> requestedChunks;


    public RequestThread(InetAddress localAddress,
                         ConcurrentHashMap<String, byte[]> chunkMap,
                         ConcurrentHashMap<String, Set<String>> peerMap,
                         ConcurrentHashMap<String, Set<String>> batchMap,
                         CopyOnWriteArrayList<String> requestedChunks) {
        this.chunkMap = chunkMap;
        this.peerMap = peerMap;
        this.batchMap = batchMap;
        this.requestedChunks = requestedChunks;
        try {
            socket = new DatagramSocket(8002, localAddress);
            socket.setBroadcast(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
    }

    public void run() {
        Set<String> batch;
        int batchSize = 10;
        RequestAlgo calculator = new RequestAlgo(peerMap);

        // Send 'query' message to broadcast address on startup to discover peers and ask for their list of chunks
        Common.sendQuery(socket, "255.255.255.255");
        
        while (true) {
            // If not currently downloading from any peer,
            for (Map.Entry<String, Set<String>> entry : peerMap.entrySet()) {
                String peer = entry.getKey();
                Set<String> peerChunks;
                synchronized (entry.getValue()) {
                    peerChunks = new HashSet<>(entry.getValue());
                    calculator.updatePeer(peer, peerChunks);
                }
                if (batchMap.containsKey(peer) || chunkMap.keySet().containsAll(peerChunks)) {
                    continue;
                }

                // generate the batch by taking the rarest chunks first
                batch = calculator.rarestChunks(peer, batchSize);

                // We then send the batch containing the chunks we want and add the chunkIds to requestedChunks
                batchMap.put(peer, batch);
                requestedChunks.addAll(batch);

                // send 'request' message to peer to initiate file transfer
                Common.sendRequest(socket, peer, batch);
            }
        }
    }
}
