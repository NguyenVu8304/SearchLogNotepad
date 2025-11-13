package Elasticsearch;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.*;

public class SearchLogKeywordParallel {

    private static final String LOG_DIR = "F:\\workspace\\java\\BaiTapLon\\log";
    private static final String OUTPUT_FILE = LOG_DIR + "\\ketqua.txt";
    private static final String KEYWORD = "login by 99";
    private static final int THREAD_COUNT = 8;
    private static final int MAX_PARALLEL_FILES = 10;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        Semaphore semaphore = new Semaphore(MAX_PARALLEL_FILES);
        AtomicInteger totalMatches = new AtomicInteger(0);

        // Xóa file kết quả cũ nếu có
        try {
            Files.deleteIfExists(Paths.get(OUTPUT_FILE));
        } catch (IOException e) {
            System.err.println("⚠️ Không xóa được file cũ: " + e.getMessage());
        }

        try (Stream<Path> paths = Files.list(Paths.get(LOG_DIR))) {
            paths
                .filter(p -> p.getFileName().toString().startsWith("log_") && p.toString().endsWith(".txt"))
                .forEach(path -> executor.submit(() -> {
                    try {
                        semaphore.acquire();

                        // Tìm kiếm và trả về StringBuilder chứa dòng kết quả
                        StringBuilder sb = searchInFile(path, totalMatches);
                        if (sb.length() > 0) {
                            // Ghi vào file kết quả, synchronized để an toàn
                            synchronized (SearchLogKeywordParallel.class) {
                                try (BufferedWriter writer = Files.newBufferedWriter(
                                        Paths.get(OUTPUT_FILE),
                                        StandardOpenOption.CREATE,
                                        StandardOpenOption.APPEND)) {
                                    writer.write(sb.toString());
                                }
                            }
                        }

                    } catch (InterruptedException | IOException e) {
                        Thread.currentThread().interrupt();
                        System.err.println("❌ Lỗi file " + path.getFileName() + ": " + e.getMessage());
                    } finally {
                        semaphore.release();
                    }
                }));
        } catch (IOException e) {
            System.err.println("❌ Lỗi khi liệt kê file log: " + e.getMessage());
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }

        long end = System.currentTimeMillis();
        System.out.println("✅ Hoàn thành trong " + (end - start) / 1000 + " giây.");
        System.out.println("📄 Kết quả lưu tại: " + OUTPUT_FILE);
        System.out.println("🔢 Tổng số dòng tìm thấy: " + totalMatches.get());
    }

    // Tìm kiếm trong 1 file, trả về StringBuilder chứa các dòng chứa từ khóa
    private static StringBuilder searchInFile(Path path, AtomicInteger totalMatches) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.toLowerCase().contains(KEYWORD.toLowerCase())) {
                    sb.append(path.getFileName())
                      .append(" - Dòng ")
                      .append(lineNumber)
                      .append(": ")
                      .append(line)
                      .append(System.lineSeparator());
                    totalMatches.incrementAndGet();
                }
            }
        }
        return sb;
    }
}
