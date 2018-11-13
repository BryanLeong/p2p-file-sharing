import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
        Common.sendChunkList(peerMap, peer, chunkList);
    }
}
