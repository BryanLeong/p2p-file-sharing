import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

// PeerTrackerThread is responsible for keeping track of which peers are still active on the network
class PeerTrackerThread extends Thread {
    private ConcurrentHashMap<String, Date> peerUpdateMap;
    private ConcurrentHashMap<String, Set<String>> peerMap;
    private ConcurrentHashMap<String, Set<String>> batchMap;
    private CopyOnWriteArrayList<String> requestedChunks;

    public PeerTrackerThread(ConcurrentHashMap<String, Date> peerUpdateMap,
                             ConcurrentHashMap<String, Set<String>> peerMap,
                             ConcurrentHashMap<String, Set<String>> batchMap,
                             CopyOnWriteArrayList<String> requestedChunks) {
        this.peerUpdateMap = peerUpdateMap;
        this.peerMap = peerMap;
        this.batchMap = batchMap;
        this.requestedChunks = requestedChunks;
    }

    public void run() {
        // Periodically check for dead peers and handle them
        while (true) {
            // check for peers which we have not heard from for more than 5 minutes
            Set<String> deadPeers = peerUpdateMap.entrySet().stream()
                    .filter(map -> (new Date()).getTime() - map.getValue().getTime() > 300000)
                    .collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue()))
                    .keySet();

            // remove the dead peers from our known list of peers
            for (String entry : deadPeers) {
                peerMap.remove(entry);
                requestedChunks.removeAll(batchMap.remove(entry));
            }

            try {
                Thread.sleep(300000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}