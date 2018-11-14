import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

// UpdateThread is responsible for periodically updating other peers with any new chunks we have
class UpdateThread extends Thread{
    private ConcurrentHashMap<String, Set<String>> peerMap;
    private CopyOnWriteArrayList<String> newChunks;

    public UpdateThread(ConcurrentHashMap<String, Set<String>> peerMap,
                        CopyOnWriteArrayList<String> newChunks) {
        this.peerMap = peerMap;
        this.newChunks = newChunks;
    }

    public void run() {
        // If received interrupt or more than 5 minutes has passed since peers were last updated, update all peers
        // This tells other peers that we are still on the network even though we do not have any active transactions
        // with them.
        while (true) {
            synchronized (newChunks) {
                for (String peer : peerMap.keySet()) {
                    (new SendChunkListThread(peerMap, peer, new HashSet<>(newChunks))).start();
                }
                newChunks.clear();
            }

            try {
                Thread.sleep(300000);
            } catch (InterruptedException e) {
                // Interrupted by DownloadThread or FileIOThread
            }
        }
    }
}