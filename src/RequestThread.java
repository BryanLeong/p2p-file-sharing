import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

class RequestThread extends Thread {
    private DatagramSocket socket;
    private ConcurrentHashMap<String, byte[]> chunkMap;
    private ConcurrentHashMap<String, Set<String>> peerMap;
    private ConcurrentHashMap<String, Set<String>> batchMap;
    private CopyOnWriteArrayList<String> requestedChunks;


    public RequestThread(InetAddress localAddress,
                         ConcurrentHashMap<String, byte[]> chunkMap,
                         ConcurrentHashMap<String, Set<String>> peerMap,
                         ConcurrentHashMap<String, Set<String>> batchMap,
                         CopyOnWriteArrayList<String> requestedChunks) {
        this.chunkMap = chunkMap;
        this.peerMap = peerMap;
        this.batchMap = batchMap;
        this.requestedChunks = requestedChunks;
        try {
            socket = new DatagramSocket(8002, localAddress);
            socket.setBroadcast(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // The list this method returns starts with the rarest chunk and ends with the most common.
    // Instead of taking peerMap, we can take calculate for each peer his availability of each chunk.
    //  then we sum up all the availabilities of all peers.
    // We also need to handle chunks without seeders.
    private List<Integer> chunkRarity(String fileName) {
        int[] occurrences = null;
        // occurrences is an array of size=no_of_chunks, where the index corresponds to the chunk number and
        //  the value corresponds to the availability (number of hosts who own it) of that chunk.

        for (Map.Entry<String, Set<String>> entry : peerMap.entrySet()) {
            Set<String> peerFiles = entry.getValue();
            for (String file : peerFiles) {
                String[] parts = file.split("/"); //parts should have length of 3.
                if (!fileName.equals(parts[0]))
                    continue;
                if (occurrences == null) {
                    // Assigns the number of chunks to be the size of the array
                    // Can throw index out of range or wrong type errors
                    occurrences = new int[Integer.valueOf(parts[1])];
                }

                String[] chunks = parts[2].split(",");
                for (String chunk : chunks) {
                    int chunkNo = Integer.valueOf(chunk);
                    occurrences[chunkNo]++;
                }
            }
        }
        // Shouldn't happen
        if (occurrences == null) return null;

        // To return, we are converting the list from size=no_of_chunks and value = availability to
        //  List[List] where the index of the outer list refers to the availability and
        //  the inner list represents the chunks.
        List<ArrayList<Integer>> availabilityList = new ArrayList<ArrayList<Integer>>();

        // Array initialisation
        for (int i = 0; i <= peerMap.size(); i++) {
            availabilityList.add(new ArrayList<>());
//            System.out.println(output.size());
        }

        // We add all chunks into lists sorted by their occurrences
        int i = 0;
        while (occurrences.length > i) {
            try {
                availabilityList.get(occurrences[i]).add(i);
            } catch (IndexOutOfBoundsException e) {
                availabilityList.get(availabilityList.size() - 1).add(i);
            }
            i++;
        }

        // We concatenate all the lists together to form a list of all the chunks sorted by their rarity.
        List<Integer> sortedList = new ArrayList<>();
        for (ArrayList<Integer> smallList : availabilityList) {
            sortedList.addAll(smallList);
        }

        return sortedList;
    }

    public static void main(String[] args) {
        ConcurrentHashMap<String, Set<String>> peerMap = new ConcurrentHashMap<>();
        Set<String> testSet = new HashSet<>();
        Set<String> testSet2 = new HashSet<>();
        testSet.add("a/8/1,2,3,5");
        testSet.add("b/4/4,7");
        testSet2.add("a/8/0,2,3,7");
        peerMap.put("123", testSet);
        peerMap.put("321", testSet2);
        RequestThread test = new RequestThread(null, null, peerMap, null, null);
        List<Integer> result = test.chunkRarity("a");
        System.out.println(result);
    }

    public void run() {
        Set<String> batch;
        int batchSize = 5;
        String fileName = "MidTermReview.pdf";
        int no_of_chunks = 0;

        // Send 'query' message to broadcast address on startup to discover peers and ask for their list of chunks
        Common.sendQuery(socket, "255.255.255.255");

        List<Integer> chunkList = chunkRarity(fileName);
        
        while (true) { // This while line should be changed to see if any peers have replied and have break condition?
            // If not currently downloading from any peer,
            for (Map.Entry<String, Set<String>> entry : peerMap.entrySet()) {
                String peer = new String(entry.getKey());
                Set<String> peerChunks = new HashSet<>(entry.getValue());
                if (batchMap.containsKey(peer) || chunkMap.keySet().containsAll(peerChunks)) {
                    continue;
                }

                batch = new HashSet<String>();
                // then loop through the sorted chunkList and add their available chunk to the batch
                for (int i = 1; i <= batchSize; i++) {
                    for (String chunkId : peerChunks) {
                        if (!chunkMap.containsKey(chunkId)) {
                            batch.add(chunkId);
                            requestedChunks.add(chunkId);
                        }
                    }
                }

                // We then send the batch containing the chunks we want.
                batchMap.put(peer, batch);

                // send 'request' message to peer to initiate file transfer
                Common.sendRequest(socket, peer, batch);
            }
        }
    }
}
