import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

class RequestAlgo {

    // keys of rarityMap is chunkId, value is set of peers
    // to compare rarity we can use .length on the set.
    HashMap<String, Set<String>> rarityMap;

    // keys of peerChunks is peer, value is set of chunks
    HashMap<String, Set<String>> peerChunks;

    RequestAlgo(ConcurrentHashMap<String, Set<String>> peerMap) {
        rarityMap = new HashMap<>();
        peerChunks = new HashMap<>();

        // peerChunks has a copy of peerMap
        Enumeration<String> peerEnum = peerMap.keys();
        while (peerEnum.hasMoreElements()) {
            String mapKey = peerEnum.nextElement();
            synchronized (peerMap.get(mapKey)) {
                peerChunks.put(mapKey, new HashSet<>(peerMap.get(mapKey)));
            }
        }

        buildRarityMap(peerMap);
    }

    private void buildRarityMap(String peer, Set<String> chunks) {
        for (String chunk : chunks) {
            if (rarityMap.containsKey(chunk)) {
                rarityMap.get(chunk).add(peer);
            } else {
                Set<String> newSet = new HashSet<>();
                newSet.add(peer);
                rarityMap.put(chunk, newSet);

            }
        }
    }

    private void buildRarityMap(ConcurrentHashMap<String, Set<String>> peerMap) {
        // loop to run buildRarityMap for all peers in peerMap.
        Iterator<String> peers = peerMap.keySet().iterator();
        while (peers.hasNext()) {
            String currentPeer = peers.next();
            Set<String> peerSetCopy = new HashSet<>();
            synchronized (peerMap.get(currentPeer)) {
                peerSetCopy.addAll(peerMap.get(currentPeer));
            }
            buildRarityMap(currentPeer, peerSetCopy);
        }
    }

    // To be used when peers have updated their map.
    public void updatePeer(String peer, Set<String> chunksInput) {
        Set<String> chunks = new HashSet<>(chunksInput);
        // This should be a deep enough copy because Strings are immutable.
        Set<String> lostChunks = new HashSet<>();

        if (peerChunks.containsKey(peer)) {
            for (String localChunk : peerChunks.get(peer)) {
                if (!chunks.remove(localChunk)) {
                    // It's a lost chunk
                    lostChunks.add(localChunk);
                    rarityMap.get(localChunk).remove(peer);
                }
            }
            if (lostChunks.size() > 0) {
                System.out.println("Lost chunks: ");
                System.out.println(lostChunks);
            }

            peerChunks.get(peer).removeAll(lostChunks);

            for (String newChunk : chunks) {
                // new chunks the peer has
                peerChunks.get(peer).add(newChunk);
                Set<String> temp = new HashSet<>();
                temp.add(peer);
                if (rarityMap.putIfAbsent(newChunk, temp) != null) {
                    rarityMap.get(newChunk).add(peer);
                }
                System.out.println("New Chunk: " + newChunk);
            }

        } else {
            peerChunks.put(peer, chunks);
            buildRarityMap(peer, chunks);
        }
    }

    public Set<String> rarestChunks(String peer) {
        return rarestChunks(peer, 5);
    }

    public Set<String> rarestChunks(String peer, int batchSize) {
        int cutoff = Integer.MAX_VALUE;
        // cutoff is the gatekeeper for rarest chunks, it takes the rarity of the chunk most recently eliminated.

        Set<String> rareChunks = new HashSet<>();
        for (String chunk : peerChunks.get(peer)) {
            if (rareChunks.size() < batchSize) {
                // rareChunks not filled yet
                rareChunks.add(chunk);
            } else {
                if (rarity(chunk) < cutoff) {
                    // Contender for rarechunks

                    // We add the contender to rareChunks then remove the max (most common).
                    String maxChunk = "";
                    int maxRarity = 0;
                    rareChunks.add(chunk);
                    for (String rareChunk : rareChunks) {
                        if (rarity(rareChunk) > maxRarity) {
                            maxRarity = rarity(rareChunk);
                            maxChunk = rareChunk;
                        }
                    }
                    rareChunks.remove(maxChunk);
                    cutoff = maxRarity;
                }
            }
        }
        return rareChunks;
    }

    private int rarity(String chunk) {
        return rarityMap.get(chunk).size();
    }

    public static void main(String[] args) {
        ConcurrentHashMap<String, Set<String>> peerMap = new ConcurrentHashMap<>();
        Set<String> testSet = new HashSet<>();
        Set<String> testSet2 = new HashSet<>();
        testSet.add("a/8/1");
        testSet.add("a/8/2");
        testSet.add("a/8/3");
        testSet.add("a/8/6");
        testSet.add("a/8/7");
        testSet.add("a/8/8");

        testSet2.add("a/8/2");
        testSet2.add("a/8/3");
        testSet2.add("a/8/4");
        testSet2.add("a/8/5");
        testSet2.add("a/8/6");
        testSet2.add("a/8/7");
        peerMap.put("123", testSet);
        peerMap.put("321", testSet2);

        RequestAlgo testalgo = new RequestAlgo(peerMap);
//        System.out.println(testalgo.rarestChunks("123"));
//        System.out.println(testalgo.rarestChunks("321"));
//        System.out.println(testSet);
        testalgo.updatePeer("123", testSet2);
//        System.out.println(testSet);
        testalgo.updatePeer("321", testSet);
        System.out.println(testalgo.peerChunks.get("321"));
//        System.out.println(testSet);
//        System.out.println(testalgo.rarestChunks("123"));
//        System.out.println(testalgo.rarestChunks("321"));
    }

}
