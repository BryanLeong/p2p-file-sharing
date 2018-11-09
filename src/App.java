import java.net.InetAddress;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

class App {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw new Exception("Please specify the IP address of the interface as an argument");
        }
        InetAddress localAddress = InetAddress.getByName(args[0]);
        ConcurrentHashMap<String, byte[]> chunkMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, Set<String>> peerMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, Date> peerUpdateMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, Set<String>> batchMap = new ConcurrentHashMap<>();
        CopyOnWriteArrayList<String> requestedChunks = new CopyOnWriteArrayList<>();
        CopyOnWriteArrayList<String> newChunks = new CopyOnWriteArrayList<>();
        int chunkSize = 1024;

        Thread updateThread = new UpdateThread(peerMap, newChunks);
        updateThread.start();
        (new FileIOThread(chunkSize, updateThread, chunkMap, newChunks)).start();
        // wait for FileIOThread to populate chunkMap
        // to be replaced with some sort of synchronization barrier
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        (new ListenThread(chunkMap, peerMap, peerUpdateMap)).start();
        (new PeerTrackerThread(peerUpdateMap, peerMap, batchMap, requestedChunks)).start();
        (new DownloadThread(chunkSize, updateThread, chunkMap, batchMap, requestedChunks, newChunks)).start();
        (new RequestThread(localAddress, chunkMap, peerMap, batchMap, requestedChunks)).start();
    }
}
