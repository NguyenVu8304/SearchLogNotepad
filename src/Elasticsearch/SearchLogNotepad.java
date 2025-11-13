package Elasticsearch;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class SearchLogNotepad extends JFrame {

    private JTextField txtFolder;
    private JTextField txtKeyword;
    private JButton btnBrowse;
    private JButton btnSearch;
    private DefaultListModel<LogEntry> listModel;
    private JList<LogEntry> resultList;

    private static final int THREAD_COUNT = 8;
    private static final int MAX_PARALLEL_FILES = 10;

    public SearchLogNotepad() {
        setTitle("Tìm kiếm log với Notepad Highlight");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Top panel
        JPanel panelTop = new JPanel(new GridLayout(3, 1, 5, 5));

        JPanel folderPanel = new JPanel(new BorderLayout(5, 5));
        txtFolder = new JTextField();
        btnBrowse = new JButton("Chọn thư mục");
        folderPanel.add(new JLabel("Thư mục log:"), BorderLayout.WEST);
        folderPanel.add(txtFolder, BorderLayout.CENTER);
        folderPanel.add(btnBrowse, BorderLayout.EAST);

        JPanel keywordPanel = new JPanel(new BorderLayout(5, 5));
        txtKeyword = new JTextField();
        keywordPanel.add(new JLabel("Từ khóa:"), BorderLayout.WEST);
        keywordPanel.add(txtKeyword, BorderLayout.CENTER);

        btnSearch = new JButton("Tìm kiếm");

        panelTop.add(folderPanel);
        panelTop.add(keywordPanel);
        panelTop.add(btnSearch);

        listModel = new DefaultListModel<>();
        resultList = new JList<>(listModel);
        resultList.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(resultList);

        add(panelTop, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // Chọn thư mục
        btnBrowse.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                txtFolder.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });

        // Tìm kiếm
        btnSearch.addActionListener(e -> {
            String folder = txtFolder.getText().trim();
            String keyword = txtKeyword.getText().trim();
            if (folder.isEmpty() || keyword.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập thư mục và từ khóa!");
                return;
            }
            listModel.clear();
            searchLogs(folder, keyword);
        });

        // Double-click -> mở file tạm highlight dòng
        resultList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    int index = resultList.locationToIndex(evt.getPoint());
                    if (index >= 0) {
                        LogEntry entry = listModel.get(index);
                        if (entry != null && entry.path != null) {
                            openFileWithHighlight(entry.path, entry.lineNumber);
                        }
                    }
                }
            }
        });
    }

    private void searchLogs(String folder, String keyword) {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        Semaphore semaphore = new Semaphore(MAX_PARALLEL_FILES);
        AtomicInteger totalMatches = new AtomicInteger(0);
        Path outputFile = Paths.get(folder, "ketqua.txt");

        try { Files.deleteIfExists(outputFile); } catch (IOException e) {}

        try (Stream<Path> paths = Files.list(Paths.get(folder))) {
            paths.filter(p -> p.toString().endsWith(".txt"))
                 .forEach(path -> executor.submit(() -> {
                     try {
                         semaphore.acquire();
                         try (BufferedReader reader = Files.newBufferedReader(path)) {
                             String line;
                             int lineNumber = 0;
                             while ((line = reader.readLine()) != null) {
                                 lineNumber++;
                                 if (line.toLowerCase().contains(keyword.toLowerCase())) {
                                     LogEntry entry = new LogEntry(path, lineNumber, line);
                                     totalMatches.incrementAndGet();
                                     SwingUtilities.invokeLater(() -> listModel.addElement(entry));
                                     synchronized (SearchLogNotepad.class) {
                                         try (BufferedWriter writer = Files.newBufferedWriter(outputFile,
                                                 StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                                             writer.write(entry.toString() + "\n");
                                         }
                                     }
                                 }
                             }
                         }
                     } catch (Exception e) {
                         SwingUtilities.invokeLater(() -> listModel.addElement(
                                 new LogEntry(null, -1, "Lỗi đọc file")));
                     } finally {
                         semaphore.release();
                     }
                 }));
        } catch (IOException e) {
            listModel.addElement(new LogEntry(null, -1, "❌ Lỗi đọc thư mục"));
        }

        executor.shutdown();
        new Thread(() -> {
            try {
                executor.awaitTermination(1, TimeUnit.HOURS);
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                        "Hoàn thành!\nTổng số dòng: " + totalMatches.get() +
                                "\nKết quả lưu tại ketqua.txt"));
            } catch (InterruptedException e) {}
        }).start();
    }

    // Tạo file tạm highlight dòng tìm thấy và mở Notepad
    private void openFileWithHighlight(Path filePath, int lineNumber) {
        if (!Files.exists(filePath)) return;
        try {
            Path tempFile = Files.createTempFile("log_highlight_", ".txt");
            tempFile.toFile().deleteOnExit();

            try (BufferedReader reader = Files.newBufferedReader(filePath);
                 BufferedWriter writer = Files.newBufferedWriter(tempFile)) {

                String line;
                int currentLine = 0;
                while ((line = reader.readLine()) != null) {
                    currentLine++;
                    if (currentLine == lineNumber) {
                        writer.write(">> " + line); // highlight dòng
                    } else {
                        writer.write(line);
                    }
                    writer.newLine();
                }
            }

            new ProcessBuilder("notepad.exe", tempFile.toAbsolutePath().toString()).start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Không mở được file: " + filePath.getFileName());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SearchLogNotepad().setVisible(true));
    }

    // Class lưu thông tin 1 dòng tìm thấy
    static class LogEntry {
        Path path;
        int lineNumber;
        String content;

        LogEntry(Path path, int lineNumber, String content) {
            this.path = path;
            this.lineNumber = lineNumber;
            this.content = content;
        }

        @Override
        public String toString() {
            if (lineNumber > 0)
                return path.getFileName() + " - Dòng " + lineNumber + ": " + content;
            else
                return content;
        }
    }
}
