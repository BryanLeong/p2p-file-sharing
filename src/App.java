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

        // bind the UDP sockets to be created to the IP address of the specified interface
        InetAddress localAddress = InetAddress.getByName(args[0]);

        // create appropriate data structures for storing the requred data
        ConcurrentHashMap<String, byte[]> chunkMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, Set<String>> peerMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, Date> peerUpdateMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, Set<String>> batchMap = new ConcurrentHashMap<>();
        CopyOnWriteArrayList<String> requestedChunks = new CopyOnWriteArrayList<>();
        CopyOnWriteArrayList<String> newChunks = new CopyOnWriteArrayList<>();

        // start FileIOThread and wait for it to finish populating chunkMap with our local files before starting the
        // other threads
        CountDownLatch cdl = new CountDownLatch(1);
        Thread updateThread = new UpdateThread(peerMap, newChunks);
        updateThread.start();
        (new FileIOThread(cdl, updateThread, chunkMap, newChunks)).start();
        cdl.await();

        // start the rest of the threads
        (new ListenThread(localAddress, chunkMap, peerMap, peerUpdateMap)).start();
        (new PeerTrackerThread(peerUpdateMap, peerMap, batchMap, requestedChunks)).start();
        (new DownloadThread(localAddress, updateThread, chunkMap, batchMap, requestedChunks, newChunks)).start();
        (new RequestThread(localAddress, chunkMap, peerMap, batchMap, requestedChunks)).start();
    }
}
