import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// This thread is spawned when we need to send our chunk list to other peers
class SendChunkListThread extends Thread {
    ConcurrentHashMap<String, Set<String>> peerMap;
    String peer;
    List<String> chunkList;

    public SendChunkListThread(ConcurrentHashMap<String, Set<String>> peerMap, String peer, Set<String> chunkSet) {
        this.peerMap = peerMap;
        this.peer = peer;
        this.chunkList = new ArrayList<>(chunkSet);
    }

    public void run() {
        // send our chunk list, making sure that the peer we send to ACKs the list before sending the next one
        Common.sendChunkList(peerMap, peer, chunkList);
    }
}