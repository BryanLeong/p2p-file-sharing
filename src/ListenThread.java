import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

class ListenThread extends Thread {
    private DatagramSocket socket;
    private InetAddress localAddress;
    private ConcurrentHashMap<String, byte[]> chunkMap;
    private ConcurrentHashMap<String, Set<String>> peerMap;
    private ConcurrentHashMap<String, Date> peerUpdateMap;

    public ListenThread(InetAddress localAddress,
                        ConcurrentHashMap<String, byte[]> chunkMap,
                        ConcurrentHashMap<String, Set<String>> peerMap,
                        ConcurrentHashMap<String, Date> peerUpdateMap) {
        this.chunkMap = chunkMap;
        this.peerMap = peerMap;
        this.peerUpdateMap = peerUpdateMap;
        this.localAddress = localAddress;
        try {
            socket = new DatagramSocket(8001, localAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        String peer;
        String[] data;
        DatagramPacket packet;

        while (true) {
            byte[] buf = new byte[2048];
            packet = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packet);
                if (packet.getAddress().equals(localAddress)) {
                    continue;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            peer = packet.getAddress().getHostAddress();
            data = (new String(packet.getData())).trim().split(",", 2);

            // Record time of last update from peer
            if (peerUpdateMap.containsKey(peer)) {
                peerUpdateMap.replace(peer, new Date());
            } else {
                peerUpdateMap.put(peer, new Date());
            }

            System.out.println("Received packet from: " + peer);
            System.out.println("Packet type: " + data[0]);
            System.out.println("Data: " + data[1] + "\n");

            switch (data[0]) {
                case "query":
                    // New peer detected: ask for peer's chunk list
                    peerMap.put(peer, new HashSet<>());
                    Common.replyQuery(socket, peer);
                    // Reply with list of available chunks
                    Common.sendChunkList(socket, peer, chunkMap.keySet());
                    break;
                case "hello":
                    // Peer responded to our query, send list of available chunks
                    peerMap.put(peer, new HashSet<>());
                    Common.sendChunkList(socket, peer, chunkMap.keySet());
                    break;
                case "request":
                    // Get batch of requested chunks and start new UploadThread to send chunks to requester
                    if (!data[1].isEmpty()) {
                        Thread uploadThread = new UploadThread(localAddress, chunkMap, peer, data[1].split(","));
                        uploadThread.start();
                    }
                    break;
                case "list":
                    // Add to peer's list of chunks
                    if (!data[1].isEmpty()) {
                        peerMap.get(peer).addAll(Arrays.asList(data[1].split(",")));
                    }
                    break;
                default:
                    break;
            }
        }
    }
}