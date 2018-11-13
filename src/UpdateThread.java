import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

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
        while (true) {
            synchronized (newChunks) {
                for (String peer : peerMap.keySet()) {
                    Common.sendChunkList(peerMap, peer, newChunks);
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