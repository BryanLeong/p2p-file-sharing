import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

// RequestThread is responsible for asking other peers for the chunks that we need
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
                }
                calculator.updatePeer(peer, peerChunks);

                // Skip this peer if we are already downloading from it or we have all the chunks it has
                if (batchMap.containsKey(peer) || chunkMap.keySet().containsAll(peerChunks)) {
                    continue;
                }

                // We remove chunks that are already completed
                Enumeration<String> chunkIter = chunkMap.keys();
                while(chunkIter.hasMoreElements()){
                    peerChunks.remove(chunkIter.nextElement());
                }

                // And we remove chunks that are already requested for
                chunkIter = batchMap.keys();
                while(chunkIter.hasMoreElements()){
                    Set<String> batchSet = batchMap.get(chunkIter.nextElement());
                    Common.removeRepeats(peerChunks, batchSet);
                }

                batch = new HashSet<String>();
                // then loop through the sorted chunkList and add their available chunk to the batch
                Set<String> requestedSet;
                synchronized (requestedChunks) {
                    requestedSet = new HashSet<>(requestedChunks);
                }
                Set<String> calculatedBatch = calculator.rarestChunks(peerChunks, batchSize, requestedSet);
                batch.addAll(calculatedBatch);

                // We then send the batch containing the chunks we want and add the chunkIds to requestedChunks
                batchMap.put(peer, batch);
                requestedChunks.addAll(batch);

                // send 'request' message to peer to initiate file transfer
                Common.sendRequest(socket, peer, batch);
            }
        }
    }
}
