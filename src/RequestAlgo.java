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
            }

        } else {
            peerChunks.put(peer, chunks);
            buildRarityMap(peer, chunks);
        }
    }

    public Set<String> rarestChunks(Set<String> peerChunks, int batchSize, Set<String> requestedChunks) {
        int cutoff = Integer.MAX_VALUE;
        // cutoff is the gatekeeper for rarest chunks, it takes the rarity of the chunk most recently eliminated.

        Set<String> rareChunks = new HashSet<>();
        for (String chunk : peerChunks) {
            if (rareChunks.size() < batchSize) {
                // rareChunks not filled yet
                rareChunks.add(chunk);
            } else {
                if (rarity(chunk, requestedChunks) < cutoff) {
                    // Contender for rarechunks

                    // We add the contender to rareChunks then remove the max (most common).
                    String maxChunk = "";
                    int maxRarity = 0;
                    rareChunks.add(chunk);
                    for (String rareChunk : rareChunks) {
                        if (rarity(rareChunk, requestedChunks) > maxRarity) {
                            maxRarity = rarity(rareChunk, requestedChunks);
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

    private int rarity(String chunk, Set<String> requestedChunks) {
        if (requestedChunks.contains(chunk)){
            return Integer.MAX_VALUE;
        }
        return rarityMap.get(chunk).size();
    }
}