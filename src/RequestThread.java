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
    // then we sum up all the availabilities of all peers.
    List<Integer> chunkRarity(String fileName){

        int[] occurrences = null;
        // occurrences is an array of size=no_of_chunks, where the index corresponds to the chunk number and
        //  the value corresponds to the availability (number of hosts who own it) of that chunk.

        for (Map.Entry<String, Set<String>> entry : peerMap.entrySet()) {
            Set<String> peerFiles = entry.getValue();
            for (String file : peerFiles) {
                String[] parts = file.split("/"); //parts should have length of 3.
                if(!fileName.equals(parts[0]))
                    continue;
                if (occurrences == null){
                    // Assigns the number of chunks to be the size of the array
                    // Can throw index out of range or wrong type errors
                    occurrences = new int[Integer.valueOf(parts[1])];
                }

                String[] chunks = parts[2].split(",");
                for (String chunk : chunks){
                    int chunkNo = Integer.valueOf(chunk);
                    occurrences[chunkNo]++;
                }
            }
        }
        // Shouldn't happen
        if(occurrences == null) return null;

        // To return, we are converting the list from size=no_of_chunks and value = availability to
        //  List[List] where the index of the outer list refers to the availability and
        //  the inner list represents the chunks.
        List<ArrayList<Integer>> availabilityList = new ArrayList<ArrayList<Integer>>();

        // Array initialisation
        for (int i = 0; i <= peerMap.size(); i++) {
            availabilityList.add(new ArrayList<>());
//            System.out.println(output.size());
        }

        // We add all chunks sorted by their occurrences
        int i = 0;
        while (occurrences.length > i){
//            System.out.println(occurrences[i]);
            try {
                availabilityList.get(occurrences[i]).add(i);
//                System.out.println(output.get(occurrences[i]));
            } catch (IndexOutOfBoundsException e){
                availabilityList.get(availabilityList.size()-1).add(i);
            }
            i++;
        }

        // We concatenate all the lists together to form a list of all the chunks sorted by their rarity.
        List<Integer> sortedList = new ArrayList<>();
        for (ArrayList<Integer> smallList : availabilityList){
            sortedList.addAll(smallList);
        }

//        System.out.println(new ArrayList<>(Arrays.asList(occurrences)));
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
        RequestThread test = new RequestThread(null,null, peerMap, null, null);
        List<Integer> result = test.chunkRarity("a");
        System.out.println(result);
    }

    public void run() {
        String msg;
        InetAddress address;
        DatagramPacket packet;
        Set<String> batch;

        // Send 'query' message to broadcast address on startup to discover peers
        msg = "query," + chunkMap.keySet().toString().replaceAll("\\[|\\]", "");
        byte[] msgBytes = msg.getBytes();
        try {
            address = InetAddress.getByName("255.255.255.255");
            packet = new DatagramPacket(msgBytes, msgBytes.length, address, 8000);
            socket.send(packet);
            Thread.sleep(2000);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        while (true) { // This while line should be changed to see if any peers have replied and have break condition?
            // If not currently downloading from any peer,
            for (Map.Entry<String, Set<String>> entry : peerMap.entrySet()) {
                String peer = entry.getKey();
                if (batchMap.containsKey(peer) || chunkMap.keySet().containsAll(entry.getValue())) {
                    continue;
                }
                
                // check which chunks peer has but we do not and are not currently downloading

                // create batch of chunks based on scarcest-first algo
                batch = new HashSet<String>();

                // send 'request' message to peer to initiate file transfer
                String request = "request," + String.join(",", batch);
                byte[] requestBytes = request.getBytes();
                try {
                    address = InetAddress.getByName(peer);
                    packet = new DatagramPacket(requestBytes, requestBytes.length, address, 8000);
                    socket.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
