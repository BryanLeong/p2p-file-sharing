import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

class App {
    public static void main(String[] args) {
        ConcurrentHashMap<String, byte[]> chunkMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, Set<String>> peerMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, Set<String>> batchMap = new ConcurrentHashMap<>();
        CopyOnWriteArrayList<String> requestedChunks = new CopyOnWriteArrayList<>();
        int chunkSize = 1024;

        (new FileIOThread(chunkSize, chunkMap)).start();
        (new ListenThread(chunkMap, peerMap)).start();
        (new DownloadThread(chunkSize, chunkMap, batchMap, requestedChunks)).start();
        (new RequestThread(chunkMap, peerMap, batchMap, requestedChunks)).start();
    }
}
