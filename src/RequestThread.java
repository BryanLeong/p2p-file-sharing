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
                }
                calculator.updatePeer(peer, peerChunks);

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

                Set<String> calculatedBatch = calculator.rarestChunks(peerChunks, batchSize);
                batch.addAll(calculatedBatch);

//                for (int i = 1; i <= batchSize; i++) {
//                    for (String chunkId : peerChunks) {
//                        if (!chunkMap.containsKey(chunkId)) {
//                            batch.add(chunkId);
//                            requestedChunks.add(chunkId);
//                        }
//                    }
//                }

//                // Remove chunks in chunkList that are already in chunkMap (the chunks we already have)
//                // We assume that chunks in the chunkMap are in a good format, e.g. "filename/no_of_chunks/chunk_no"
//                Enumeration<String> chunkIter = chunkMap.keys();
//                while (chunkIter.hasMoreElements()) {
//                    String current = chunkIter.nextElement();
//                    String completedChunk = Common.unpackChunk(current, fileName);
//                    if (completedChunk != null){
//                        chunkList.remove(Integer.valueOf(completedChunk));
//                        // .remove still works if value is not in the list.
//                        // .remove is overloaded with (int index) and (Object o), but in this case we are removing
//                        //  an object and Java is smart enough to know that
//                        if (no_of_chunks == 0){
//                            no_of_chunks = Integer.valueOf(current.split("/")[2]);
//                        }
//                    }
//
//                }
//
//                chunkIter = batchMap.keys();
//                while (chunkIter.hasMoreElements()){
//                    String current = chunkIter.nextElement();
//                    String batchChunk = Common.unpackChunk(current, fileName);
//                    if (batchChunk != null){
//                        int chunkIndex = chunkList.indexOf(Integer.valueOf(batchChunk));
//                        if (chunkIndex == -1){
//                            continue;
//                        }
//                        int batchedChunk = chunkList.remove(chunkIndex);
//                        chunkList.add(batchedChunk);
//                    }
//                }

                // We then send the batch containing the chunks we want and add the chunkIds to requestedChunks
                batchMap.put(peer, batch);
                requestedChunks.addAll(batch);

                // send 'request' message to peer to initiate file transfer
                Common.sendRequest(socket, peer, batch);
            }
        }
    }
}
