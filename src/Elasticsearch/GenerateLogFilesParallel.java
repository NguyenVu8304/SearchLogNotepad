package Elasticsearch;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

public class GenerateLogFilesParallel {

    private static final String OUTPUT_DIR = "F:\\workspace\\java\\BaiTapLon\\log";
    private static final int FILE_COUNT = 3000;
    private static final int LINES_PER_FILE = 20000;
    private static final int THREAD_COUNT = 8;
    private static final double KEYWORD_FILE_RATIO = 0.1; // 10% file có chứa login by 99
    private static final Random random = new Random();

    private static final String[] LEVELS = {"INFO", "WARN", "ERROR"};
    private static final String[] EVENTS = {
            "User login successful",
            "User login failed",
            "Database connection established",
            "Database connection timeout",
            "File uploaded",
            "File deleted",
            "System reboot scheduled",
            "Memory usage high",
            "CPU load normal",
            "Request processed",
            "Request timeout",
            "Cache cleared",
            "New session started",
            "Server started",
            "Server stopped unexpectedly"
    };

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        try {
            Files.createDirectories(Paths.get(OUTPUT_DIR));
            System.out.println("Đang tạo log tại: " + OUTPUT_DIR);

            // Xác định file nào có chứa login by 99
            Set<Integer> filesWithKeyword = new HashSet<>();
            int keywordFileCount = (int) (FILE_COUNT * KEYWORD_FILE_RATIO);
            while (filesWithKeyword.size() < keywordFileCount) {
                filesWithKeyword.add(random.nextInt(FILE_COUNT) + 1);
            }

            for (int i = 1; i <= FILE_COUNT; i++) {
                final int index = i;
                final boolean includeKeyword = filesWithKeyword.contains(i);
                executor.submit(() -> createLogFile(index, includeKeyword));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Hoàn thành trong " + (endTime - startTime) / 1000 + " giây.");
    }

    private static void createLogFile(int i, boolean includeKeyword) {
        LocalDate date = LocalDate.now().minusDays(FILE_COUNT - i);
        String fileName = "log_" + date.format(DateTimeFormatter.ofPattern("dd_MM_yy")) + ".txt";
        Path filePath = Paths.get(OUTPUT_DIR, fileName);

        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
            for (int j = 0; j < LINES_PER_FILE; j++) {
                if (includeKeyword && random.nextInt(500) == 0) {
                    writer.write(generateLogLine("User login by 99 successful"));
                } else {
                    writer.write(generateLogLine(EVENTS[random.nextInt(EVENTS.length)]));
                }
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Lỗi khi tạo file " + filePath + ": " + e.getMessage());
        }

        if (i % 100 == 0) {
            System.out.println("Đã tạo " + i + "/" + FILE_COUNT + " file");
        }
    }

    private static String generateLogLine(String event) {
        String time = LocalTime.now().minusSeconds(random.nextInt(86400))
                .format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String level = LEVELS[random.nextInt(LEVELS.length)];
        return "[" + time + "] [" + level + "] " + event;
    }
}
