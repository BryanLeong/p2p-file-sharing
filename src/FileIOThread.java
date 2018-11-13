import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

class FileIOThread extends Thread {
    private ConcurrentHashMap<String, byte[]> chunkMap;
    private CopyOnWriteArrayList<String> newChunks;
    private File folder;
    private List<String> fileNameList;
    private List<File> fileList;
    private Thread updateThread;
    private final int chunkSize = 10000;

    public FileIOThread(CountDownLatch cdl,
                        Thread updateThread,
                        ConcurrentHashMap<String, byte[]> chunkMap,
                        CopyOnWriteArrayList<String> newChunks) {
        this.updateThread = updateThread;
        this.chunkMap = chunkMap;
        this.newChunks = newChunks;
        folder = new File("files");
        fileNameList = Arrays.asList(folder.list());
        fileList = Arrays.asList(folder.listFiles());
        populateChunkMap();
        cdl.countDown();
    }

    private Set<String> populateChunkMap() {
        Set<String> newChunks = new HashSet<>();
        for (File file : fileList) {
            String fileName = file.getName();
            if (fileName.matches("(.+\\.tmp)$")) {
                try {
                    FileInputStream fis = new FileInputStream(file);
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    Map<String, byte[]> fileChunks = (HashMap<String, byte[]>) ois.readObject();
                    ois.close();
                    fis.close();
                    for (Map.Entry<String, byte[]> entry : fileChunks.entrySet()) {
                        String chunkId = entry.getKey();
                        if (!chunkMap.containsKey(chunkId)) {
                            chunkMap.put(chunkId, entry.getValue());
                            newChunks.add(chunkId);
                        }
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    byte[] fileBytes = Files.readAllBytes(file.toPath());
                    int totalChunks = (int) Math.ceil(1.0 * fileBytes.length / chunkSize);
                    int start = 0, end;
                    for (int i = 1; i <= totalChunks; i++) {
                        if (start + chunkSize < fileBytes.length) {
                            end = start + chunkSize;
                        } else {
                            end = fileBytes.length;
                        }
                        String chunkId = String.format("%s/%d/%d", fileName, totalChunks, i);
                        byte[] chunk = Arrays.copyOfRange(fileBytes, start, end);
                        if (!chunkMap.containsKey(chunkId)) {
                            chunkMap.put(chunkId, chunk);
                            newChunks.add(chunkId);
                        }
                        start += chunkSize;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return newChunks;
    }

    public void run() {
        while (true) {
            // update chunkList if new files are added
            List<File> newFileList = Arrays.asList(folder.listFiles());
            if (!fileList.containsAll(newFileList)) {
                fileNameList = Arrays.asList(folder.list());
                fileList = newFileList;
                Set<String> result = populateChunkMap();
                synchronized (newChunks) {
                    newChunks.addAll(result);
                }
                if (!updateThread.isInterrupted()) {
                    updateThread.interrupt();
                }
            }

            // if we have all chunks of a file, write it to disk
            // else store chunks in a temp file on disk
            List<String> keyList = new ArrayList<>(chunkMap.keySet());
            keyList.replaceAll(s -> s.split("/\\d+$")[0]);
            for (String s : new HashSet<>(keyList)) {
                String[] fnc = s.split("/");

                if (!fileNameList.contains(fnc[0])) {
                    Map<String, byte[]> fileChunks = chunkMap.entrySet().stream()
                            .filter(entry -> entry.getKey().contains(fnc[0]))
                            .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));

                    int numChunks = Integer.valueOf(fnc[1]);
                    String fp = "files/" + fnc[0];
                    System.out.println(fileChunks.size());
                    if (fileChunks.size() == numChunks) {
                        try {
                            if (fileNameList.contains(fnc[0] + ".tmp")) {
                                File tmpFile = new File(fp + ".tmp");
                                tmpFile.delete();
                            }
                            FileOutputStream fos = new FileOutputStream(fp, true);
                            for (int i = 1; i <= numChunks; i++) {
                                fos.write(fileChunks.get(s + "/" + i));
                            }
                            fos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            FileOutputStream fos = new FileOutputStream(fp + ".tmp");
                            ObjectOutputStream oos = new ObjectOutputStream(fos);
                            oos.writeObject(fileChunks);
                            oos.close();
                            fos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
