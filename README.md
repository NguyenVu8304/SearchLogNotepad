1. Overview

This project is implemented in Java Swing combined with multithreading to:

Generate simulated log files (GenerateLogFilesParallel) in large quantities (3000 files, each with 20,000 lines), with a subset containing the special keyword login by 99.

Search for a keyword across all log files (SearchLogKeywordParallel & SearchLogNotepad) using parallel processing to ensure fast execution, writing results to ketqua.txt.

Provide a GUI for users to select a folder, enter a keyword, view results, and open a temporary file in Notepad highlighting the found line.

2. Main Components
a) GenerateLogFilesParallel.java

Purpose: Generate test log data.

Key Features:

Multithreading: Uses ExecutorService with a fixed thread pool (THREAD_COUNT) for parallel file creation.

Random keyword insertion: 10% of the files randomly contain the keyword login by 99.

Log format: [HH:mm:ss] [LEVEL] MESSAGE with LEVEL = INFO/WARN/ERROR, MESSAGE random.

Algorithm:

Identify which files will contain the keyword using a Set<Integer>.

For each file: write LINES_PER_FILE lines, randomly inserting the keyword if applicable.

Use BufferedWriter with try-with-resources to ensure proper closing of streams.

b) SearchLogKeywordParallel.java

Purpose: Search for a keyword in multiple log files concurrently and save results to ketqua.txt.

Key Features:

Semaphore: Limits the number of files processed simultaneously to avoid IO bottlenecks (MAX_PARALLEL_FILES).

AtomicInteger: Thread-safe counting of total matched lines.

StringBuilder: Collects file-level results before writing to disk to reduce contention.

Multithreading: Uses ExecutorService to parallelize reading of multiple files.

c) SearchLogNotepad.java (GUI)

Purpose:

Allows users to select a folder containing logs.

Enter the keyword to search.

Display results in a JList.

Double-click opens Notepad with the matched line highlighted using >>.

GUI Layout:

Top Panel: 3 rows:

Row 1: Folder selection (txtFolder, btnBrowse)

Row 2: Keyword input (txtKeyword)

Row 3: Search button (btnSearch)

Center Panel: JScrollPane containing the JList of search results.

Search Algorithm:

List files using Files.list().

For each file: read lines using BufferedReader and check for the keyword.

For each match: create a LogEntry storing Path, lineNumber, content.

Add entries to the JList and append them to ketqua.txt (thread-safe).

Opening file with highlight:

Create a temporary file (Files.createTempFile).

Prepend >> to the line containing the keyword.

Open the temporary file using Notepad (ProcessBuilder).

3. Algorithm and Optimizations

Parallelism:

Uses ExecutorService with a fixed thread pool (THREAD_COUNT) for CPU efficiency.

Uses Semaphore to limit concurrent file reading (MAX_PARALLEL_FILES) to prevent IO congestion.

Thread Safety:

AtomicInteger to safely count matched lines across threads.

synchronized blocks for writing to the results file.

Performance:

BufferedReader/Writer: Reduces IO overhead.

StringBuilder: Avoids excessive string concatenation.

Temporary file for highlight: Avoids locking the original file.

4. Advanced Features and Possible Enhancements

GUI Enhancements:

Add filtering by file name or line number range.

Add a progress bar for searching large datasets.

Multithreading Optimizations:

Use ForkJoinPool for better parallelism on large files.

Opening file directly at the line in Notepad++:

Notepad++ supports -n<line> to jump directly to a line.

Advanced Highlighting:

Use Notepad++ plugins or syntax highlighting for multiple keywords.

Support for different log formats:

CSV, JSON logs, or integration with distributed log systems (ELK stack).

5. Code Evaluation for Future Developers

Pros:

Clear separation of modules (log generation, search, GUI).

Thread-safe and high-performance.

Simple and intuitive GUI.

Cons:

Standard Notepad cannot jump directly to the line; workaround is needed.

Limited exception handling for large file errors.

Temporary highlight files consume disk space.

Extensibility:

Add filters, log analysis, frequency statistics.

Integrate directly with ElasticSearch or Logstash for real log processing.
