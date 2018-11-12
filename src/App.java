import java.net.InetAddress;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

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
        CountDownLatch cdl = new CountDownLatch(1);

        Thread updateThread = new UpdateThread(localAddress, peerMap, newChunks);
        updateThread.start();
        (new FileIOThread(cdl, updateThread, chunkMap, newChunks)).start();
        // wait for FileIOThread to populate chunkMap
        cdl.await();
        (new ListenThread(localAddress, chunkMap, peerMap, peerUpdateMap)).start();
        (new PeerTrackerThread(peerUpdateMap, peerMap, batchMap, requestedChunks)).start();
        (new DownloadThread(localAddress, updateThread, chunkMap, batchMap, requestedChunks, newChunks)).start();
        (new RequestThread(localAddress, chunkMap, peerMap, batchMap, requestedChunks)).start();
    }
}
