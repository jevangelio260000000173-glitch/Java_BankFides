package src;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class FileDatabase {

    private final Path databaseFolder;

    private final Path customersFile;
    private final Path staffFile;
    private final Path adminsFile;
    private final Path transactionsFile;
    private final Path activityLogsFile;

    public FileDatabase() {

        databaseFolder = Paths.get("database");

        customersFile = databaseFolder.resolve("customers.txt");
        staffFile = databaseFolder.resolve("staff.txt");
        adminsFile = databaseFolder.resolve("admins.txt");
        transactionsFile = databaseFolder.resolve("transactions.txt");
        activityLogsFile = databaseFolder.resolve("activity_logs.txt");

        initializeDatabase();
    }

    private void initializeDatabase() {

        try {
            Files.createDirectories(databaseFolder);

            createFileIfMissing(customersFile);
            createFileIfMissing(staffFile);
            createFileIfMissing(adminsFile);
            createFileIfMissing(transactionsFile);
            createFileIfMissing(activityLogsFile);

            System.out.println("BancoFides database initialized successfully.");

        } catch (IOException e) {
            throw new IllegalStateException(
                "Unable to initialize the database.", e
            );
        }
    }

    private void createFileIfMissing(Path file) throws IOException {

        if (Files.notExists(file)) {
            Files.createFile(file);
        }
    }

    public List<String> readCustomers() {
        return readLines(customersFile);
    }

    public List<String> readStaff() {
        return readLines(staffFile);
    }

    public List<String> readAdmins() {
        return readLines(adminsFile);
    }

    public List<String> readTransactions() {
        return readLines(transactionsFile);
    }

    public List<String> readActivityLogs() {
        return readLines(activityLogsFile);
    }

    private List<String> readLines(Path file) {

        try {
            return new ArrayList<>(
                Files.readAllLines(file, StandardCharsets.UTF_8)
            );

        } catch (IOException e) {
            throw new IllegalStateException(
                "Unable to read database file: " + file, e
            );
        }
    }

    public void saveCustomer(String record) {
        appendLine(customersFile, record);
    }

    public void saveStaff(String record) {
        appendLine(staffFile, record);
    }

    public void saveAdmin(String record) {
        appendLine(adminsFile, record);
    }

    public void saveTransaction(String record) {
        appendLine(transactionsFile, record);
    }

    public void saveActivityLog(String record) {
        appendLine(activityLogsFile, record);
    }

    private void appendLine(Path file, String record) {

        if (record == null || record.trim().isEmpty()
                || record.contains("\n") || record.contains("\r")) {

            throw new IllegalArgumentException(
                "Database record must be a non-empty single line."
            );
        }

        try {
            Files.write(
                file,
                (record + System.lineSeparator())
                    .getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );

        } catch (IOException e) {
            throw new IllegalStateException(
                "Unable to save record to: " + file, e
            );
        }
    }
}