import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

class UpdateThread extends Thread{
    private DatagramSocket socket;
    private ConcurrentHashMap<String, Set<String>> peerMap;
    private CopyOnWriteArrayList<String> newChunks;

    public UpdateThread(InetAddress localAddress,
                        ConcurrentHashMap<String, Set<String>> peerMap,
                        CopyOnWriteArrayList<String> newChunks) {
        this.peerMap = peerMap;
        this.newChunks = newChunks;
        try {
            socket = new DatagramSocket(8003, localAddress);
        } catch (SocketException e) {
            e.printStackTrace();
        }
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