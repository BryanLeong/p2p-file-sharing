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
        int batchSize = 5;
        RequestAlgo calculator = new RequestAlgo(peerMap);
        String fileName = "MidTermReview.pdf";
        int no_of_chunks = 0;

        // Send 'query' message to broadcast address on startup to discover peers and ask for their list of chunks
        Common.sendQuery(socket, "255.255.255.255");
        
        while (true) { // This while line should be changed to see if any peers have replied and have break condition?
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

                batch = new HashSet<String>();
                // then loop through the sorted chunkList and add their available chunk to the batch

                Set<String> calculatedBatch = calculator.rarestChunks(peer, batchSize);
                batch.addAll(calculatedBatch);

//                for (int i = 1; i <= batchSize; i++) {
//                    for (String chunkId : peerChunks) {
//                        if (!chunkMap.containsKey(chunkId)) {
//                            batch.add(chunkId);
//                            requestedChunks.add(chunkId);
//                        }
//                    }
//                }

                // We then send the batch containing the chunks we want.
                batchMap.put(peer, batch);

                // send 'request' message to peer to initiate file transfer
                Common.sendRequest(socket, peer, batch);
            }
        }
    }
}
