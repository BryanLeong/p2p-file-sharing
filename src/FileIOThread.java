import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

class FileIOThread extends Thread {
    private ConcurrentHashMap<String, byte[]> chunkMap;
    private CopyOnWriteArrayList<String> newChunks;
    private File folder;
    private List<File> fileList;
    private int chunkSize;
    private Thread updateThread;

    public FileIOThread(int chunkSize,
                        CountDownLatch cdl,
                        Thread updateThread,
                        ConcurrentHashMap<String, byte[]> chunkMap,
                        CopyOnWriteArrayList<String> newChunks) {
        this.chunkSize = chunkSize;
        this.updateThread = updateThread;
        this.chunkMap = chunkMap;
        this.newChunks = newChunks;
        folder = new File("files");
        fileList = Arrays.asList(folder.listFiles());
        populateChunkMap();
        cdl.countDown();
    }

    private Set<String> populateChunkMap() {
        Set<String> newChunks = new HashSet<>();
        for (File file : fileList) {
            String fileName = file.getName();
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
        return newChunks;
    }

    public void run() {
        while (true) {
            // update chunkList if new files are added
            List<File> newFileList = Arrays.asList(folder.listFiles());
            if (!fileList.containsAll(newFileList)) {
                fileList = newFileList;
                newChunks.addAll(populateChunkMap());
                if (!updateThread.isInterrupted()) {
                    updateThread.interrupt();
                }
            }

            // if we have all chunks of a file, write it to disk
            // else store chunks in a temp file on disk (update populateChunkMap() to read these as chunks as well)

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
