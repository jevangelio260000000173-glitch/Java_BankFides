package src;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

public class FileDatabase {

    // ==========================================
    // DATABASE FILE PATHS
    // ==========================================

    private final Path databaseFolder = Paths.get("database");

    private final Path customersFile = databaseFolder.resolve("customers.txt");
    private final Path staffFile = databaseFolder.resolve("staff.txt");
    private final Path adminsFile = databaseFolder.resolve("admins.txt");
    private final Path transactionsFile = databaseFolder.resolve("transactions.txt");
    private final Path activityLogsFile = databaseFolder.resolve("activity_logs.txt");
    private final Path accountStatusesFile = databaseFolder.resolve("account_status.txt");
    private final Path loginSecurityFile = databaseFolder.resolve("login_security.txt");
    private final Path rewardsFile = databaseFolder.resolve("rewards.txt");
    private final Path pendingDepositsFile = databaseFolder.resolve("pending_deposits.txt");
    private final Path transactionIssuesFile = databaseFolder.resolve("transaction_issues.txt");

    // ==========================================
    // LOGIN SECURITY SETTINGS
    // ==========================================

    private static final int MAX_LOGIN_ATTEMPTS = 3;
    private static final long LOCK_DURATION_MILLIS = 5 * 60 * 1000;

    public FileDatabase() {
        initializeDatabase();
    }

    // ==========================================
    // DATABASE INITIALIZATION
    // ==========================================

    public synchronized void initializeDatabase() {

        try {
            Files.createDirectories(databaseFolder);

            createFileIfMissing(customersFile);
            createFileIfMissing(staffFile);
            createFileIfMissing(adminsFile);
            createFileIfMissing(transactionsFile);
            createFileIfMissing(activityLogsFile);
            createFileIfMissing(accountStatusesFile);
            createFileIfMissing(loginSecurityFile);
            createFileIfMissing(rewardsFile);
            createFileIfMissing(pendingDepositsFile);
            createFileIfMissing(transactionIssuesFile);

            System.out.println("BancoFides database initialized successfully.");
            System.out.println("Database location: "
                    + databaseFolder.toAbsolutePath());

        } catch (IOException e) {
            System.out.println("Database initialization failed: "
                    + e.getMessage());
        }
    }

    private void createFileIfMissing(Path file) throws IOException {

        if (Files.notExists(file)) {
            Files.createFile(file);
            System.out.println("Created database file: " + file.getFileName());
        }
    }

    // ==========================================
    // SAFE DATABASE READING
    // ==========================================

    private List<String> readFileSafely(Path file) {

        try {
            createFileIfMissing(file);

            return Files.readAllLines(file, StandardCharsets.UTF_8);

        } catch (IOException e) {

            System.out.println("Error reading "
                    + file.getFileName() + ": " + e.getMessage());

            return null;
        }
    }

    private List<String> readFile(Path file) {

        List<String> records = readFileSafely(file);

        return records == null
                ? new ArrayList<>()
                : records;
    }

    public synchronized List<String> readCustomers() {
        return readFile(customersFile);
    }

    public synchronized List<String> readStaff() {
        return readFile(staffFile);
    }

    public synchronized List<String> readAdmins() {
        return readFile(adminsFile);
    }

    public synchronized List<String> readTransactions() {
        return readFile(transactionsFile);
    }

    public synchronized List<String> readActivityLogs() {
        return readFile(activityLogsFile);
    }

    public synchronized List<String> readAccountStatuses() {
        return readFile(accountStatusesFile);
    }

    public synchronized List<String> readLoginSecurityRecords() {
        return readFile(loginSecurityFile);
    }

    public synchronized List<String> readRewards() {
        return readFile(rewardsFile);
    }

    // ==========================================
    // BASIC VALIDATION
    // ==========================================

    private boolean validateRecord(String record) {

        return record != null
                && !record.trim().isEmpty()
                && !record.contains("\n")
                && !record.contains("\r");
    }

    private boolean validateAccountID(String accountID) {

        return accountID != null
                && !accountID.trim().isEmpty()
                && !accountID.contains("|")
                && !accountID.contains("\n")
                && !accountID.contains("\r");
    }

    private BigDecimal money(String value) {

        try {
            BigDecimal amount = new BigDecimal(value);

            if (amount.scale() > 2) {
                return null;
            }

            return amount;

        } catch (Exception e) {
            return null;
        }
    }

    private boolean sameMoney(String first, String second) {

        BigDecimal a = money(first);
        BigDecimal b = money(second);

        return a != null
                && b != null
                && a.compareTo(b) == 0;
    }

    private boolean validateCustomerRecord(String record) {

        if (!validateRecord(record)) {
            return false;
        }

        String[] fields = record.split("\\|", -1);

        if (fields.length != 7
                || !validateAccountID(fields[0])) {
            return false;
        }

        BigDecimal balance = money(fields[4]);
        BigDecimal deposits = money(fields[5]);

        try {
            int points = Integer.parseInt(fields[6]);

            return balance != null
                    && deposits != null
                    && balance.compareTo(BigDecimal.ZERO) >= 0
                    && deposits.compareTo(BigDecimal.ZERO) >= 0
                    && points >= 0;

        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean validateStaffOrAdminRecord(String record) {

        if (!validateRecord(record)) {
            return false;
        }

        String[] fields = record.split("\\|", -1);

        return fields.length == 4
                && validateAccountID(fields[0])
                && !fields[1].isEmpty()
                && !fields[2].isEmpty()
                && !fields[3].isEmpty();
    }

    // ==========================================
    // TRANSACTION VALIDATION
    // ==========================================

    private boolean validateTransactionRecord(String record) {

        if (!validateRecord(record)) {
            return false;
        }

        String[] fields = record.split("\\|", -1);

        if (fields.length < 8
                || fields.length > 9
                || fields[0].trim().isEmpty()
                || !validateAccountID(fields[1])
                || fields[2].trim().isEmpty()
                || fields[6].trim().isEmpty()
                || !fields[7].equalsIgnoreCase("SUCCESS")) {
            return false;
        }

        BigDecimal amount = money(fields[3]);
        BigDecimal previous = money(fields[4]);
        BigDecimal updated = money(fields[5]);

        return amount != null
                && previous != null
                && updated != null
                && amount.compareTo(BigDecimal.ZERO) > 0
                && previous.compareTo(BigDecimal.ZERO) >= 0
                && updated.compareTo(BigDecimal.ZERO) >= 0;
    }

    private boolean transactionIDExists(
            List<String> transactions,
            String transactionID) {

        for (String record : transactions) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length >= 8
                    && fields[0].equalsIgnoreCase(transactionID)) {
                return true;
            }
        }

        return false;
    }

    // ==========================================
    // REWARD VALIDATION
    // ==========================================

    private boolean validateRewardRecord(String record) {

        if (!validateRecord(record)) {
            return false;
        }

        String[] fields = record.split("\\|", -1);

        if (fields.length != 6
                || fields[0].trim().isEmpty()
                || !validateAccountID(fields[1])
                || fields[2].trim().isEmpty()
                || fields[5].trim().isEmpty()) {
            return false;
        }

        BigDecimal depositAmount = money(fields[3]);

        try {
            int points = Integer.parseInt(fields[4]);

            return depositAmount != null
                    && depositAmount.compareTo(BigDecimal.ZERO) > 0
                    && points >= 0;

        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean rewardExists(
            List<String> rewards,
            String rewardID,
            String transactionID) {

        for (String record : rewards) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length >= 3
                    && (fields[0].equalsIgnoreCase(rewardID)
                    || fields[2].equalsIgnoreCase(transactionID))) {
                return true;
            }
        }

        return false;
    }

    // ==========================================
    // PENDING DEPOSIT VALIDATION
    // ==========================================

    /*
     * pending_deposits.txt format:
     *
     * pendingID|accountID|amount|submittedAt|status|reviewedBy|reviewedAt
     *
     * Status:
     * PENDING
     * APPROVED
     * REJECTED
     */

    private boolean validatePendingDepositRecord(String record) {

        if (!validateRecord(record)) {
            return false;
        }

        String[] fields = record.split("\\|", -1);

        if (fields.length != 7
                || fields[0].trim().isEmpty()
                || !validateAccountID(fields[1])
                || fields[3].trim().isEmpty()
                || fields[4].trim().isEmpty()) {
            return false;
        }

        BigDecimal amount = money(fields[2]);

        if (amount == null
                || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        String status = fields[4].trim().toUpperCase();

        if (!status.equals("PENDING")
                && !status.equals("APPROVED")
                && !status.equals("REJECTED")) {
            return false;
        }

        if (status.equals("PENDING")) {
            return fields[5].isEmpty() && fields[6].isEmpty();
        }

        return validateAccountID(fields[5])
                && !fields[6].trim().isEmpty();
    }

    private boolean pendingDepositIDExists(
            List<String> records,
            String pendingID) {

        for (String record : records) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length == 7
                    && fields[0].equalsIgnoreCase(pendingID)) {
                return true;
            }
        }

        return false;
    }

    private int findPendingDepositIndex(
            List<String> records,
            String pendingID) {

        int found = -1;

        for (int i = 0; i < records.size(); i++) {

            String record = records.get(i);

            if (!validatePendingDepositRecord(record)) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields[0].equalsIgnoreCase(pendingID.trim())) {

                if (found != -1) {
                    return -2;
                }

                found = i;
            }
        }

        return found;
    }

    // ==========================================
    // PENDING DEPOSIT STORAGE
    // ==========================================

    public synchronized List<String> readPendingDeposits() {

        List<String> records = readFileSafely(pendingDepositsFile);

        if (records == null) {
            return new ArrayList<>();
        }

        return records;
    }

    public synchronized int countPendingDeposits() {

        List<String> records = readFileSafely(pendingDepositsFile);

        if (records == null) {
            return 0;
        }

        int count = 0;

        for (String record : records) {

            if (!validatePendingDepositRecord(record)) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields[4].equalsIgnoreCase("PENDING")) {
                count++;
            }
        }

        return count;
    }

    public synchronized boolean savePendingDeposit(String record) {

        if (!validatePendingDepositRecord(record)) {
            return false;
        }

        String[] fields = record.split("\\|", -1);

        if (!fields[4].equalsIgnoreCase("PENDING")) {
            return false;
        }

        List<String> records = readFileSafely(pendingDepositsFile);

        if (records == null) {
            return false;
        }

        if (pendingDepositIDExists(records, fields[0])) {
            System.out.println("Duplicate pending deposit ID detected.");
            return false;
        }

        List<String> customers = readFileSafely(customersFile);
        List<String> statuses = readFileSafely(accountStatusesFile);

        if (customers == null || statuses == null
                || !customerStatusIsActive(fields[1], customers, statuses)) {
            System.out.println("Pending deposit rejected: customer account is unavailable.");
            return false;
        }

        return appendLine(pendingDepositsFile, record);
    }

    // ==========================================
    // TRANSACTION ISSUE VALIDATION
    // ==========================================

    /*
     * transaction_issues.txt format:
     *
     * reportID|accountID|transactionID|issueType|description|
     * status|submittedAt|reviewedBy|reviewedAt|staffNote
     *
     * Description and staffNote are Base64 encoded.
     *
     * Status:
     * PENDING
     * UNDER_REVIEW
     * RESOLVED
     * REJECTED
     */

    private boolean isValidBase64(String value) {

        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        try {
            Base64.getDecoder().decode(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean validateTransactionIssueRecord(String record) {

        if (!validateRecord(record)) {
            return false;
        }

        String[] fields = record.split("\\|", -1);

        if (fields.length != 10
                || !validateAccountID(fields[0])
                || !validateAccountID(fields[1])
                || !validateAccountID(fields[2])
                || fields[3].trim().isEmpty()
                || !isValidBase64(fields[4])
                || fields[6].trim().isEmpty()) {
            return false;
        }

        String status = fields[5].trim().toUpperCase();

        if (!status.equals("PENDING")
                && !status.equals("UNDER_REVIEW")
                && !status.equals("RESOLVED")
                && !status.equals("REJECTED")) {
            return false;
        }

        if (status.equals("PENDING")) {
            return fields[7].isEmpty()
                    && fields[8].isEmpty()
                    && fields[9].isEmpty();
        }

        return validateAccountID(fields[7])
                && !fields[8].trim().isEmpty()
                && (fields[9].isEmpty() || isValidBase64(fields[9]));
    }

    private boolean transactionIssueIDExists(
            List<String> records,
            String reportID) {

        for (String record : records) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length == 10
                    && fields[0].equalsIgnoreCase(reportID.trim())) {
                return true;
            }
        }

        return false;
    }

    private int findTransactionIssueIndex(
            List<String> records,
            String reportID) {

        int found = -1;

        for (int i = 0; i < records.size(); i++) {

            if (!validateTransactionIssueRecord(records.get(i))) {
                continue;
            }

            String[] fields = records.get(i).split("\\|", -1);

            if (fields[0].equalsIgnoreCase(reportID.trim())) {

                if (found != -1) {
                    return -2;
                }

                found = i;
            }
        }

        return found;
    }

    private boolean staffAccountExists(String staffID) {

        List<String> staffRecords = readFileSafely(staffFile);

        if (staffRecords == null) {
            return false;
        }

        int matches = 0;

        for (String record : staffRecords) {

            if (!validateStaffOrAdminRecord(record)) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields[0].equalsIgnoreCase(staffID.trim())) {
                matches++;
            }
        }

        return matches == 1;
    }

    // ==========================================
    // TRANSACTION ISSUE STORAGE
    // ==========================================

    public synchronized List<String> readTransactionIssues() {

        List<String> records = readFileSafely(transactionIssuesFile);

        if (records == null) {
            return new ArrayList<>();
        }

        return records;
    }

    public synchronized int countPendingTransactionIssues() {

        List<String> records = readFileSafely(transactionIssuesFile);

        if (records == null) {
            return 0;
        }

        int count = 0;

        for (String record : records) {

            if (!validateTransactionIssueRecord(record)) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields[5].equalsIgnoreCase("PENDING")
                    || fields[5].equalsIgnoreCase("UNDER_REVIEW")) {
                count++;
            }
        }

        return count;
    }

    public synchronized boolean saveTransactionIssue(String record) {

        if (!validateTransactionIssueRecord(record)) {
            return false;
        }

        String[] fields = record.split("\\|", -1);

        if (!fields[5].equalsIgnoreCase("PENDING")) {
            return false;
        }

        List<String> issues = readFileSafely(transactionIssuesFile);
        List<String> transactions = readFileSafely(transactionsFile);
        List<String> customers = readFileSafely(customersFile);
        List<String> statuses = readFileSafely(accountStatusesFile);

        if (issues == null
                || transactions == null
                || customers == null
                || statuses == null) {
            return false;
        }

        if (transactionIssueIDExists(issues, fields[0])) {
            System.out.println("Duplicate report ID detected.");
            return false;
        }

        if (!customerStatusIsActive(fields[1], customers, statuses)) {
            System.out.println(
                    "Issue report rejected: customer account is unavailable."
            );
            return false;
        }

        boolean transactionFound = false;

        for (String transaction : transactions) {

            if (!validateTransactionRecord(transaction)) {
                continue;
            }

            String[] transactionFields = transaction.split("\\|", -1);

            if (transactionFields[0].equalsIgnoreCase(fields[2])
                    && transactionFields[1].equalsIgnoreCase(fields[1])
                    && transactionFields[7].equalsIgnoreCase("SUCCESS")) {

                transactionFound = true;
                break;
            }
        }

        if (!transactionFound) {
            System.out.println(
                    "Issue report rejected: transaction not found, "
                    + "unsuccessful, or does not belong to this account."
            );
            return false;
        }

        return appendLine(transactionIssuesFile, record);
    }

    // ==========================================
    // STAFF REVIEW OF TRANSACTION ISSUES
    // ==========================================

    public synchronized boolean updateTransactionIssue(
            String reportID,
            String newStatus,
            String staffID,
            String reviewedAt,
            String encodedStaffNote) {

        if (!validateAccountID(reportID)
                || !validateAccountID(staffID)
                || newStatus == null
                || reviewedAt == null
                || reviewedAt.trim().isEmpty()
                || encodedStaffNote == null
                || encodedStaffNote.contains("|")
                || encodedStaffNote.contains("\n")
                || encodedStaffNote.contains("\r")) {
            return false;
        }

        String status = newStatus.trim().toUpperCase();

        if (!status.equals("UNDER_REVIEW")
                && !status.equals("RESOLVED")
                && !status.equals("REJECTED")) {
            return false;
        }

        if (!encodedStaffNote.isEmpty()
                && !isValidBase64(encodedStaffNote)) {
            return false;
        }

        if (!staffAccountExists(staffID)) {
            System.out.println("Staff account not found.");
            return false;
        }

        List<String> records =
                readFileSafely(transactionIssuesFile);

        if (records == null) {
            return false;
        }

        int index = findTransactionIssueIndex(records, reportID);

        if (index < 0) {
            System.out.println(
                    "Report was not found or its ID is duplicated."
            );
            return false;
        }

        String[] fields = records.get(index).split("\\|", -1);

        String currentStatus = fields[5].toUpperCase();

        if (currentStatus.equals("RESOLVED")
                || currentStatus.equals("REJECTED")) {
            System.out.println("This report has already been closed.");
            return false;
        }

        List<String> updatedRecords = new ArrayList<>(records);

        updatedRecords.set(
                index,
                fields[0] + "|"
                + fields[1] + "|"
                + fields[2] + "|"
                + fields[3] + "|"
                + fields[4] + "|"
                + status + "|"
                + fields[6] + "|"
                + staffID.trim() + "|"
                + reviewedAt.trim() + "|"
                + encodedStaffNote
        );

        return writeLinesAtomically(
                transactionIssuesFile,
                updatedRecords,
                "transaction_issues_temp.txt"
        );
    }

    // ==========================================
    // APPROVE PENDING DEPOSIT
    // ==========================================

    public synchronized boolean approvePendingDeposit(
            String pendingID,
            String updatedCustomerRecord,
            String transactionRecord,
            String rewardRecord,
            String staffID,
            String reviewedAt) {

        if (!validateAccountID(pendingID)
                || !validateAccountID(staffID)
                || reviewedAt == null
                || reviewedAt.trim().isEmpty()
                || !validateCustomerRecord(updatedCustomerRecord)
                || !validateTransactionRecord(transactionRecord)
                || !validateRewardRecord(rewardRecord)) {
            return false;
        }

        List<String> originalPending =
                readFileSafely(pendingDepositsFile);

        List<String> originalCustomers =
                readFileSafely(customersFile);

        List<String> originalTransactions =
                readFileSafely(transactionsFile);

        List<String> originalRewards =
                readFileSafely(rewardsFile);

        List<String> statuses =
                readFileSafely(accountStatusesFile);

        if (originalPending == null
                || originalCustomers == null
                || originalTransactions == null
                || originalRewards == null
                || statuses == null) {
            return false;
        }

        int pendingIndex =
                findPendingDepositIndex(originalPending, pendingID);

        if (pendingIndex < 0) {
            System.out.println("Pending deposit was not found or is duplicated.");
            return false;
        }

        String[] pending =
                originalPending.get(pendingIndex).split("\\|", -1);

        if (!pending[4].equalsIgnoreCase("PENDING")) {
            System.out.println("This deposit has already been reviewed.");
            return false;
        }

        String accountID = pending[1];

        String[] updatedFields =
                updatedCustomerRecord.split("\\|", -1);

        String[] transactionFields =
                transactionRecord.split("\\|", -1);

        String[] rewardFields =
                rewardRecord.split("\\|", -1);

        if (!updatedFields[0].equalsIgnoreCase(accountID)
                || !transactionFields[1].equalsIgnoreCase(accountID)
                || !transactionFields[2].equalsIgnoreCase("DEPOSIT")
                || !rewardFields[1].equalsIgnoreCase(accountID)
                || !rewardFields[2].equalsIgnoreCase(transactionFields[0])
                || !sameMoney(pending[2], transactionFields[3])
                || !sameMoney(rewardFields[3], pending[2])) {

            System.out.println("Deposit approval information does not match.");
            return false;
        }

        if (!customerStatusIsActive(accountID, originalCustomers, statuses)) {
            System.out.println("Approval rejected: customer account is inactive.");
            return false;
        }

        if (transactionIDExists(originalTransactions, transactionFields[0])) {
            System.out.println("Duplicate transaction ID detected.");
            return false;
        }

        if (rewardExists(originalRewards, rewardFields[0], rewardFields[2])) {
            System.out.println("Duplicate reward record detected.");
            return false;
        }

        String originalCustomer =
                findCustomerRecord(accountID, originalCustomers);

        if (originalCustomer == null
                || !validateDepositChanges(
                        originalCustomer,
                        updatedCustomerRecord,
                        transactionFields,
                        rewardFields)) {

            System.out.println("Approval rejected: customer balance or reward calculation is invalid.");
            return false;
        }

        List<String> updatedCustomers = replaceCustomerRecord(
                accountID,
                updatedCustomerRecord,
                originalCustomers
        );

        if (updatedCustomers == null) {
            return false;
        }

        List<String> updatedTransactions =
                new ArrayList<>(originalTransactions);

        updatedTransactions.add(transactionRecord);

        List<String> updatedRewards =
                new ArrayList<>(originalRewards);

        updatedRewards.add(rewardRecord);

        List<String> updatedPending =
                new ArrayList<>(originalPending);

        updatedPending.set(
                pendingIndex,
                pending[0] + "|"
                + pending[1] + "|"
                + pending[2] + "|"
                + pending[3] + "|APPROVED|"
                + staffID.trim() + "|"
                + reviewedAt.trim()
        );

        // Save customer balance first.
        if (!writeLinesAtomically(
                customersFile, updatedCustomers, "customers_temp.txt")) {
            return false;
        }

        // Save successful transaction.
        if (!writeLinesAtomically(
                transactionsFile, updatedTransactions, "transactions_temp.txt")) {

            writeLinesAtomically(
                    customersFile, originalCustomers, "customers_temp.txt");
            return false;
        }

        // Save earned rewards.
        if (!writeLinesAtomically(
                rewardsFile, updatedRewards, "rewards_temp.txt")) {

            writeLinesAtomically(
                    customersFile, originalCustomers, "customers_temp.txt");

            writeLinesAtomically(
                    transactionsFile, originalTransactions, "transactions_temp.txt");
            return false;
        }

        // Mark pending deposit as approved last.
        if (!writeLinesAtomically(
                pendingDepositsFile, updatedPending, "pending_deposits_temp.txt")) {

            writeLinesAtomically(
                    customersFile, originalCustomers, "customers_temp.txt");

            writeLinesAtomically(
                    transactionsFile, originalTransactions, "transactions_temp.txt");

            writeLinesAtomically(
                    rewardsFile, originalRewards, "rewards_temp.txt");

            return false;
        }

        return true;
    }

    // ==========================================
    // REJECT PENDING DEPOSIT
    // ==========================================

    public synchronized boolean rejectPendingDeposit(
            String pendingID,
            String staffID,
            String reviewedAt) {

        if (!validateAccountID(pendingID)
                || !validateAccountID(staffID)
                || reviewedAt == null
                || reviewedAt.trim().isEmpty()) {
            return false;
        }

        List<String> records =
                readFileSafely(pendingDepositsFile);

        if (records == null) {
            return false;
        }

        int index = findPendingDepositIndex(records, pendingID);

        if (index < 0) {
            System.out.println("Pending deposit was not found or is duplicated.");
            return false;
        }

        String[] fields = records.get(index).split("\\|", -1);

        if (!fields[4].equalsIgnoreCase("PENDING")) {
            System.out.println("This deposit has already been reviewed.");
            return false;
        }

        List<String> updatedRecords = new ArrayList<>(records);

        updatedRecords.set(
                index,
                fields[0] + "|"
                + fields[1] + "|"
                + fields[2] + "|"
                + fields[3] + "|REJECTED|"
                + staffID.trim() + "|"
                + reviewedAt.trim()
        );

        return writeLinesAtomically(
                pendingDepositsFile,
                updatedRecords,
                "pending_deposits_temp.txt"
        );
    }

    // ==========================================
    // APPEND DATABASE RECORDS
    // ==========================================

    private synchronized boolean appendLine(Path file, String record) {

        if (!validateRecord(record)) {
            return false;
        }

        try {

            createFileIfMissing(file);

            Files.write(
                    file,
                    Collections.singletonList(record),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );

            return true;

        } catch (IOException e) {

            System.out.println("Error saving record: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean saveCustomer(String record) {

        if (!validateCustomerRecord(record)) {
            return false;
        }

        return appendLine(customersFile, record);
    }

    public synchronized boolean saveStaff(String record) {

        if (!validateStaffOrAdminRecord(record)) {
            return false;
        }

        return appendLine(staffFile, record);
    }

    public synchronized boolean saveAdmin(String record) {

        if (!validateStaffOrAdminRecord(record)) {
            return false;
        }

        return appendLine(adminsFile, record);
    }

    public synchronized boolean saveTransaction(String record) {

        if (!validateTransactionRecord(record)) {
            return false;
        }

        List<String> transactions = readFileSafely(transactionsFile);

        if (transactions == null) {
            return false;
        }

        String[] fields = record.split("\\|", -1);

        if (transactionIDExists(transactions, fields[0])) {
            System.out.println("Duplicate transaction ID detected.");
            return false;
        }

        return appendLine(transactionsFile, record);
    }

    public synchronized boolean saveActivityLog(String record) {
        return appendLine(activityLogsFile, record);
    }

    public synchronized boolean saveReward(String record) {

        if (!validateRewardRecord(record)) {
            return false;
        }

        List<String> rewards = readFileSafely(rewardsFile);

        if (rewards == null) {
            return false;
        }

        String[] fields = record.split("\\|", -1);

        if (rewardExists(rewards, fields[0], fields[2])) {
            System.out.println("Duplicate reward record detected.");
            return false;
        }

        return appendLine(rewardsFile, record);
    }

    // ==========================================
    // CUSTOMER LOOKUP HELPERS
    // ==========================================

    private String findCustomerRecord(
            String accountID,
            List<String> customers) {

        String found = null;

        for (String record : customers) {

            if (!validateCustomerRecord(record)) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields[0].equalsIgnoreCase(accountID.trim())) {

                if (found != null) {
                    return null;
                }

                found = record;
            }
        }

        return found;
    }

    private boolean customerExistsInRecords(
            String accountID,
            List<String> customers) {

        return findCustomerRecord(accountID, customers) != null;
    }

    // ==========================================
    // ACCOUNT STATUS MANAGEMENT
    // ==========================================

    private boolean isAccountActiveInRecords(
            String accountID,
            List<String> statusRecords) {

        for (String record : statusRecords) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length >= 1
                    && fields[0].equalsIgnoreCase(accountID.trim())) {

                if (fields.length != 2) {
                    return false;
                }

                return fields[1].trim().equalsIgnoreCase("ACTIVE");
            }
        }

        return true;
    }

    public synchronized String getAccountStatus(String accountID) {

        if (!validateAccountID(accountID)) {
            return "INACTIVE";
        }

        List<String> records = readFileSafely(accountStatusesFile);

        if (records == null) {
            return "INACTIVE";
        }

        for (String record : records) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length >= 1
                    && fields[0].equalsIgnoreCase(accountID.trim())) {

                if (fields.length != 2) {
                    return "INACTIVE";
                }

                String status = fields[1].trim().toUpperCase();

                return status.equals("ACTIVE") ? "ACTIVE" : "INACTIVE";
            }
        }

        return "ACTIVE";
    }

    public synchronized boolean ensureAccountStatus(String accountID) {

        if (!validateAccountID(accountID)) {
            return false;
        }

        List<String> records = readFileSafely(accountStatusesFile);

        if (records == null) {
            return false;
        }

        for (String record : records) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length >= 1
                    && fields[0].equalsIgnoreCase(accountID.trim())) {
                return fields.length == 2
                        && (fields[1].equalsIgnoreCase("ACTIVE")
                        || fields[1].equalsIgnoreCase("INACTIVE"));
            }
        }

        return saveAccountStatus(accountID, "ACTIVE");
    }

    public synchronized boolean saveAccountStatus(
            String accountID,
            String status) {

        if (!validateAccountID(accountID) || status == null) {
            return false;
        }

        String normalizedStatus = status.trim().toUpperCase();

        if (!normalizedStatus.equals("ACTIVE")
                && !normalizedStatus.equals("INACTIVE")) {
            return false;
        }

        List<String> records = readFileSafely(accountStatusesFile);

        if (records == null) {
            return false;
        }

        List<String> updatedRecords = new ArrayList<>();
        boolean found = false;

        for (String record : records) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length >= 1
                    && fields[0].equalsIgnoreCase(accountID.trim())) {

                if (!found) {
                    updatedRecords.add(
                            accountID.trim() + "|" + normalizedStatus
                    );
                    found = true;
                }

            } else {
                updatedRecords.add(record);
            }
        }

        if (!found) {
            updatedRecords.add(accountID.trim() + "|" + normalizedStatus);
        }

        return writeLinesAtomically(
                accountStatusesFile,
                updatedRecords,
                "account_status_temp.txt"
        );
    }

    public synchronized boolean updateAccountStatus(
            String accountID,
            String status) {

        return saveAccountStatus(accountID, status);
    }

    // ==========================================
    // LOGIN SECURITY MANAGEMENT
    // ==========================================

    /*
     * login_security.txt format:
     * accountID|failedAttempts|lockUntil
     */

    private boolean validLoginSecurityRecord(String[] fields) {

        if (fields.length != 3
                || !validateAccountID(fields[0])) {
            return false;
        }

        try {

            int attempts = Integer.parseInt(fields[1]);
            long lockUntil = Long.parseLong(fields[2]);

            return attempts >= 0 && lockUntil >= 0;

        } catch (NumberFormatException e) {
            return false;
        }
    }

    public synchronized boolean isLoginLocked(String accountID) {

        if (!validateAccountID(accountID)) {
            return false;
        }

        List<String> records = readFileSafely(loginSecurityFile);

        if (records == null) {
            return true;
        }

        for (String record : records) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length >= 1
                    && fields[0].equalsIgnoreCase(accountID.trim())) {

                if (!validLoginSecurityRecord(fields)) {
                    return true;
                }

                int attempts = Integer.parseInt(fields[1]);
                long lockUntil = Long.parseLong(fields[2]);

                if (attempts >= MAX_LOGIN_ATTEMPTS
                        && System.currentTimeMillis() < lockUntil) {
                    return true;
                }

                if (attempts >= MAX_LOGIN_ATTEMPTS) {
                    resetLoginAttempts(accountID);
                }

                return false;
            }
        }

        return false;
    }

    public synchronized int recordFailedLogin(String accountID) {

        if (!validateAccountID(accountID)) {
            return -1;
        }

        if (isLoginLocked(accountID)) {
            return MAX_LOGIN_ATTEMPTS;
        }

        List<String> records = readFileSafely(loginSecurityFile);

        if (records == null) {
            return -1;
        }

        int attempts = 0;

        for (String record : records) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length >= 1
                    && fields[0].equalsIgnoreCase(accountID.trim())) {

                if (!validLoginSecurityRecord(fields)) {
                    return -1;
                }

                attempts = Integer.parseInt(fields[1]);
                break;
            }
        }

        attempts++;

        long lockUntil = 0;

        if (attempts >= MAX_LOGIN_ATTEMPTS) {
            lockUntil = System.currentTimeMillis() + LOCK_DURATION_MILLIS;
        }

        if (!saveLoginSecurityRecord(accountID, attempts, lockUntil)) {
            return -1;
        }

        return attempts;
    }

    public synchronized boolean resetLoginAttempts(String accountID) {

        if (!validateAccountID(accountID)) {
            return false;
        }

        return saveLoginSecurityRecord(accountID, 0, 0);
    }

    public synchronized int getFailedLoginAttempts(String accountID) {

        if (!validateAccountID(accountID)) {
            return 0;
        }

        List<String> records = readFileSafely(loginSecurityFile);

        if (records == null) {
            return MAX_LOGIN_ATTEMPTS;
        }

        for (String record : records) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length >= 1
                    && fields[0].equalsIgnoreCase(accountID.trim())) {

                if (!validLoginSecurityRecord(fields)) {
                    return MAX_LOGIN_ATTEMPTS;
                }

                return Integer.parseInt(fields[1]);
            }
        }

        return 0;
    }

    public synchronized long getLoginLockRemainingMillis(String accountID) {

        if (!validateAccountID(accountID)) {
            return 0;
        }

        List<String> records = readFileSafely(loginSecurityFile);

        if (records == null) {
            return LOCK_DURATION_MILLIS;
        }

        for (String record : records) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length >= 1
                    && fields[0].equalsIgnoreCase(accountID.trim())) {

                if (!validLoginSecurityRecord(fields)) {
                    return LOCK_DURATION_MILLIS;
                }

                int attempts = Integer.parseInt(fields[1]);
                long lockUntil = Long.parseLong(fields[2]);

                if (attempts < MAX_LOGIN_ATTEMPTS) {
                    return 0;
                }

                return Math.max(
                        lockUntil - System.currentTimeMillis(),
                        0
                );
            }
        }

        return 0;
    }

    private synchronized boolean saveLoginSecurityRecord(
            String accountID,
            int attempts,
            long lockUntil) {

        if (!validateAccountID(accountID)
                || attempts < 0
                || lockUntil < 0) {
            return false;
        }

        List<String> records = readFileSafely(loginSecurityFile);

        if (records == null) {
            return false;
        }

        List<String> updatedRecords = new ArrayList<>();
        boolean found = false;

        for (String record : records) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length >= 1
                    && fields[0].equalsIgnoreCase(accountID.trim())) {

                if (!found) {
                    updatedRecords.add(
                            accountID.trim() + "|" + attempts + "|" + lockUntil
                    );
                    found = true;
                }

            } else {
                updatedRecords.add(record);
            }
        }

        if (!found) {
            updatedRecords.add(
                    accountID.trim() + "|" + attempts + "|" + lockUntil
            );
        }

        return writeLinesAtomically(
                loginSecurityFile,
                updatedRecords,
                "login_security_temp.txt"
        );
    }

    // ==========================================
    // CUSTOMER ACCOUNT INFORMATION UPDATE
    // ==========================================

    public synchronized boolean updateCustomerAccount(
            String accountID,
            String updatedRecord) {

        if (!validateAccountID(accountID)
                || !validateCustomerRecord(updatedRecord)) {
            return false;
        }

        String[] updatedFields = updatedRecord.split("\\|", -1);

        if (!updatedFields[0].equalsIgnoreCase(accountID.trim())) {
            return false;
        }

        List<String> records = readFileSafely(customersFile);

        if (records == null) {
            return false;
        }

        List<String> updatedRecords = new ArrayList<>();
        boolean found = false;

        for (String record : records) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length >= 1
                    && fields[0].equalsIgnoreCase(accountID.trim())) {

                if (found || fields.length != 7) {
                    return false;
                }

                updatedRecords.add(updatedRecord);
                found = true;

            } else {
                updatedRecords.add(record);
            }
        }

        if (!found) {
            return false;
        }

        return writeLinesAtomically(
                customersFile,
                updatedRecords,
                "customers_temp.txt"
        );
    }

    // ==========================================
    // STAFF ACCOUNT INFORMATION UPDATE
    // ==========================================

    public synchronized boolean updateStaffAccount(
            String accountID,
            String updatedRecord) {

        if (!validateAccountID(accountID)
                || !validateStaffOrAdminRecord(updatedRecord)) {
            return false;
        }

        String[] updatedFields = updatedRecord.split("\\|", -1);

        if (!updatedFields[0].equalsIgnoreCase(accountID.trim())) {
            return false;
        }

        List<String> records = readFileSafely(staffFile);

        if (records == null) {
            return false;
        }

        List<String> updatedRecords = new ArrayList<>();
        boolean found = false;

        for (String record : records) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length >= 1
                    && fields[0].equalsIgnoreCase(accountID.trim())) {

                if (found || fields.length != 4) {
                    return false;
                }

                updatedRecords.add(updatedRecord);
                found = true;

            } else {
                updatedRecords.add(record);
            }
        }

        if (!found) {
            return false;
        }

        return writeLinesAtomically(
                staffFile,
                updatedRecords,
                "staff_temp.txt"
        );
    }

    // ==========================================
    // ADMIN ACCOUNT INFORMATION UPDATE
    // ==========================================

    public synchronized boolean updateAdminAccount(
            String accountID,
            String updatedRecord) {

        if (!validateAccountID(accountID)
                || !validateStaffOrAdminRecord(updatedRecord)) {
            return false;
        }

        String[] updatedFields = updatedRecord.split("\\|", -1);

        // Ensure the updated record belongs to the correct Admin ID.
        if (!updatedFields[0].equalsIgnoreCase(accountID.trim())) {
            return false;
        }

        List<String> records = readFileSafely(adminsFile);

        if (records == null) {
            return false;
        }

        List<String> updatedRecords = new ArrayList<>();
        boolean found = false;

        for (String record : records) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length >= 1
                    && fields[0].equalsIgnoreCase(accountID.trim())) {

                // Reject duplicate or invalid Admin records.
                if (found || fields.length != 4) {
                    return false;
                }

                updatedRecords.add(updatedRecord);
                found = true;

            } else {

                updatedRecords.add(record);
            }
        }

        // Admin account does not exist.
        if (!found) {
            return false;
        }

        return writeLinesAtomically(
                adminsFile,
                updatedRecords,
                "admins_temp.txt"
        );
    }

    // ==========================================
    // ACCOUNT STATUS CHECK FOR TRANSACTIONS
    // ==========================================

    private boolean customerStatusIsActive(
            String accountID,
            List<String> customers,
            List<String> statuses) {

        return customerExistsInRecords(accountID, customers)
                && isAccountActiveInRecords(accountID, statuses);
    }

    // ==========================================
    // DEPOSIT PROCESSING
    // ==========================================

    public synchronized boolean processDeposit(
            String accountID,
            String updatedCustomerRecord,
            String transactionRecord) {

        return updateCustomerAndSaveTransactions(
                accountID,
                updatedCustomerRecord,
                transactionRecord,
                "DEPOSIT"
        );
    }

    public synchronized boolean processDeposit(
            String accountID,
            String updatedCustomerRecord,
            String transactionRecord,
            String rewardRecord) {

        if (!validateAccountID(accountID)
                || !validateCustomerRecord(updatedCustomerRecord)
                || !validateTransactionRecord(transactionRecord)
                || !validateRewardRecord(rewardRecord)) {
            return false;
        }

        String[] updatedFields = updatedCustomerRecord.split("\\|", -1);
        String[] transactionFields = transactionRecord.split("\\|", -1);
        String[] rewardFields = rewardRecord.split("\\|", -1);

        if (!updatedFields[0].equalsIgnoreCase(accountID.trim())
                || !transactionFields[1].equalsIgnoreCase(accountID.trim())
                || !transactionFields[2].equalsIgnoreCase("DEPOSIT")
                || !rewardFields[1].equalsIgnoreCase(accountID.trim())
                || !rewardFields[2].equalsIgnoreCase(transactionFields[0])
                || !sameMoney(rewardFields[3], transactionFields[3])) {
            return false;
        }

        List<String> originalCustomers = readFileSafely(customersFile);
        List<String> originalTransactions = readFileSafely(transactionsFile);
        List<String> originalRewards = readFileSafely(rewardsFile);
        List<String> statuses = readFileSafely(accountStatusesFile);

        if (originalCustomers == null
                || originalTransactions == null
                || originalRewards == null
                || statuses == null) {
            return false;
        }

        if (!customerStatusIsActive(accountID, originalCustomers, statuses)) {
            System.out.println("Deposit rejected: account is inactive or unavailable.");
            return false;
        }

        if (transactionIDExists(originalTransactions, transactionFields[0])) {
            System.out.println("Duplicate transaction ID detected.");
            return false;
        }

        if (rewardExists(originalRewards, rewardFields[0], rewardFields[2])) {
            System.out.println("Duplicate reward record detected.");
            return false;
        }

        String originalRecord = findCustomerRecord(accountID, originalCustomers);

        if (originalRecord == null
                || !validateDepositChanges(
                        originalRecord,
                        updatedCustomerRecord,
                        transactionFields,
                        rewardFields)) {
            System.out.println("Deposit rejected: account balance or reward information does not match.");
            return false;
        }

        List<String> updatedCustomers = replaceCustomerRecord(
                accountID, updatedCustomerRecord, originalCustomers);

        if (updatedCustomers == null) {
            return false;
        }

        List<String> updatedTransactions = new ArrayList<>(originalTransactions);
        updatedTransactions.add(transactionRecord);

        List<String> updatedRewards = new ArrayList<>(originalRewards);
        updatedRewards.add(rewardRecord);

        if (!writeLinesAtomically(
                customersFile, updatedCustomers, "customers_temp.txt")) {
            return false;
        }

        if (!writeLinesAtomically(
                transactionsFile, updatedTransactions, "transactions_temp.txt")) {

            writeLinesAtomically(
                    customersFile, originalCustomers, "customers_temp.txt");
            return false;
        }

        if (!writeLinesAtomically(
                rewardsFile, updatedRewards, "rewards_temp.txt")) {

            writeLinesAtomically(
                    customersFile, originalCustomers, "customers_temp.txt");

            writeLinesAtomically(
                    transactionsFile, originalTransactions, "transactions_temp.txt");

            return false;
        }

        return true;
    }

    private boolean validateDepositChanges(
            String originalRecord,
            String updatedRecord,
            String[] transaction,
            String[] reward) {

        String[] original = originalRecord.split("\\|", -1);
        String[] updated = updatedRecord.split("\\|", -1);

        BigDecimal amount = money(transaction[3]);
        BigDecimal previous = money(transaction[4]);
        BigDecimal newBalance = money(transaction[5]);

        BigDecimal oldBalance = money(original[4]);
        BigDecimal oldDeposits = money(original[5]);
        BigDecimal oldPoints;
        BigDecimal newDeposits;
        BigDecimal newBalanceFromRecord;
        BigDecimal newPoints;
        BigDecimal earnedPoints;

        try {
            oldPoints = new BigDecimal(original[6]);
            newPoints = new BigDecimal(updated[6]);
            earnedPoints = new BigDecimal(reward[4]);
            newDeposits = money(updated[5]);
            newBalanceFromRecord = money(updated[4]);
        } catch (Exception e) {
            return false;
        }

        if (amount == null || previous == null || newBalance == null
                || oldBalance == null || oldDeposits == null
                || newDeposits == null || newBalanceFromRecord == null
                || oldPoints == null || newPoints == null
                || earnedPoints == null) {
            return false;
        }

        return previous.compareTo(oldBalance) == 0
                && newBalance.compareTo(previous.add(amount)) == 0
                && newBalanceFromRecord.compareTo(newBalance) == 0
                && newDeposits.compareTo(oldDeposits.add(amount)) == 0
                && newPoints.compareTo(oldPoints.add(earnedPoints)) == 0;
    }

    // ==========================================
    // WITHDRAWAL PROCESSING
    // ==========================================

    public synchronized boolean processWithdrawal(
            String accountID,
            String updatedCustomerRecord,
            String transactionRecord) {

        return updateCustomerAndSaveTransactions(
                accountID,
                updatedCustomerRecord,
                transactionRecord,
                "WITHDRAWAL"
        );
    }

    // ==========================================
    // TRANSFER PROCESSING
    // ==========================================

    public synchronized boolean processTransfer(
            String senderID,
            String updatedSenderRecord,
            String recipientID,
            String updatedRecipientRecord,
            String senderTransaction,
            String recipientTransaction) {

        if (!validateAccountID(senderID)
                || !validateAccountID(recipientID)
                || senderID.equalsIgnoreCase(recipientID)
                || !validateCustomerRecord(updatedSenderRecord)
                || !validateCustomerRecord(updatedRecipientRecord)
                || !validateTransactionRecord(senderTransaction)
                || !validateTransactionRecord(recipientTransaction)) {
            return false;
        }

        String[] senderFields = senderTransaction.split("\\|", -1);
        String[] recipientFields = recipientTransaction.split("\\|", -1);

        if (senderFields.length != 9
                || recipientFields.length != 9
                || !senderFields[0].equalsIgnoreCase(recipientFields[0])
                || !senderFields[1].equalsIgnoreCase(senderID.trim())
                || !recipientFields[1].equalsIgnoreCase(recipientID.trim())
                || !senderFields[2].equalsIgnoreCase("TRANSFER_OUT")
                || !recipientFields[2].equalsIgnoreCase("TRANSFER_IN")
                || !senderFields[8].equalsIgnoreCase(recipientID.trim())
                || !recipientFields[8].equalsIgnoreCase(senderID.trim())
                || !sameMoney(senderFields[3], recipientFields[3])) {

            System.out.println("Transfer rejected: transaction information does not match.");
            return false;
        }

        List<String> originalCustomers = readFileSafely(customersFile);
        List<String> originalTransactions = readFileSafely(transactionsFile);
        List<String> statuses = readFileSafely(accountStatusesFile);

        if (originalCustomers == null
                || originalTransactions == null
                || statuses == null) {
            return false;
        }

        if (!customerStatusIsActive(senderID, originalCustomers, statuses)
                || !customerStatusIsActive(recipientID, originalCustomers, statuses)) {

            System.out.println("Transfer rejected: one or both accounts are inactive.");
            return false;
        }

        if (transactionIDExists(originalTransactions, senderFields[0])) {
            System.out.println("Duplicate transaction ID detected.");
            return false;
        }

        String originalSender = findCustomerRecord(senderID, originalCustomers);
        String originalRecipient = findCustomerRecord(recipientID, originalCustomers);

        if (originalSender == null || originalRecipient == null
                || !validateTransferChanges(
                        originalSender, updatedSenderRecord, senderFields, true)
                || !validateTransferChanges(
                        originalRecipient, updatedRecipientRecord, recipientFields, false)) {

            System.out.println("Transfer rejected: account balances do not match.");
            return false;
        }

        List<String> updatedCustomers = new ArrayList<>();
        boolean senderFound = false;
        boolean recipientFound = false;

        for (String record : originalCustomers) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length >= 1
                    && fields[0].equalsIgnoreCase(senderID.trim())) {

                if (senderFound || fields.length != 7) {
                    return false;
                }

                updatedCustomers.add(updatedSenderRecord);
                senderFound = true;

            } else if (fields.length >= 1
                    && fields[0].equalsIgnoreCase(recipientID.trim())) {

                if (recipientFound || fields.length != 7) {
                    return false;
                }

                updatedCustomers.add(updatedRecipientRecord);
                recipientFound = true;

            } else {
                updatedCustomers.add(record);
            }
        }

        if (!senderFound || !recipientFound) {
            return false;
        }

        List<String> updatedTransactions = new ArrayList<>(originalTransactions);
        updatedTransactions.add(senderTransaction);
        updatedTransactions.add(recipientTransaction);

        if (!writeLinesAtomically(
                customersFile, updatedCustomers, "customers_temp.txt")) {
            return false;
        }

        if (!writeLinesAtomically(
                transactionsFile, updatedTransactions, "transactions_temp.txt")) {

            writeLinesAtomically(
                    customersFile, originalCustomers, "customers_temp.txt");
            return false;
        }

        return true;
    }

    private boolean validateTransferChanges(
            String originalRecord,
            String updatedRecord,
            String[] transaction,
            boolean sender) {

        String[] original = originalRecord.split("\\|", -1);
        String[] updated = updatedRecord.split("\\|", -1);

        BigDecimal amount = money(transaction[3]);
        BigDecimal previous = money(transaction[4]);
        BigDecimal newBalance = money(transaction[5]);
        BigDecimal oldBalance = money(original[4]);
        BigDecimal updatedBalance = money(updated[4]);
        BigDecimal oldDeposits = money(original[5]);
        BigDecimal updatedDeposits = money(updated[5]);

        if (amount == null || previous == null || newBalance == null
                || oldBalance == null || updatedBalance == null
                || oldDeposits == null || updatedDeposits == null) {
            return false;
        }

        if (sender) {

            if (oldBalance.compareTo(amount) < 0
                    || previous.compareTo(oldBalance) != 0
                    || newBalance.compareTo(previous.subtract(amount)) != 0) {
                return false;
            }

        } else {

            if (previous.compareTo(oldBalance) != 0
                    || newBalance.compareTo(previous.add(amount)) != 0) {
                return false;
            }
        }

        return updatedBalance.compareTo(newBalance) == 0
                && updatedDeposits.compareTo(oldDeposits) == 0
                && updated[6].equals(original[6]);
    }

    // ==========================================
    // SHARED DEPOSIT / WITHDRAWAL PROCESSING
    // ==========================================

    private synchronized boolean updateCustomerAndSaveTransactions(
            String accountID,
            String updatedCustomerRecord,
            String transactionRecord,
            String expectedType) {

        if (!validateAccountID(accountID)
                || !validateCustomerRecord(updatedCustomerRecord)
                || !validateTransactionRecord(transactionRecord)) {
            return false;
        }

        String[] updatedFields = updatedCustomerRecord.split("\\|", -1);
        String[] transactionFields = transactionRecord.split("\\|", -1);

        if (!updatedFields[0].equalsIgnoreCase(accountID.trim())
                || !transactionFields[1].equalsIgnoreCase(accountID.trim())
                || !transactionFields[2].equalsIgnoreCase(expectedType)) {
            return false;
        }

        List<String> originalCustomers = readFileSafely(customersFile);
        List<String> originalTransactions = readFileSafely(transactionsFile);
        List<String> statuses = readFileSafely(accountStatusesFile);

        if (originalCustomers == null
                || originalTransactions == null
                || statuses == null) {
            return false;
        }

        if (!customerStatusIsActive(accountID, originalCustomers, statuses)) {
            System.out.println("Transaction rejected: account is inactive or unavailable.");
            return false;
        }

        if (transactionIDExists(originalTransactions, transactionFields[0])) {
            System.out.println("Duplicate transaction ID detected.");
            return false;
        }

        String originalRecord = findCustomerRecord(accountID, originalCustomers);

        if (originalRecord == null
                || !validateBalanceChange(
                        originalRecord, updatedCustomerRecord,
                        transactionFields, expectedType)) {
            System.out.println("Transaction rejected: balance information does not match.");
            return false;
        }

        List<String> updatedCustomers = replaceCustomerRecord(
                accountID, updatedCustomerRecord, originalCustomers);

        if (updatedCustomers == null) {
            return false;
        }

        List<String> updatedTransactions = new ArrayList<>(originalTransactions);
        updatedTransactions.add(transactionRecord);

        if (!writeLinesAtomically(
                customersFile, updatedCustomers, "customers_temp.txt")) {
            return false;
        }

        if (!writeLinesAtomically(
                transactionsFile, updatedTransactions, "transactions_temp.txt")) {

            writeLinesAtomically(
                    customersFile, originalCustomers, "customers_temp.txt");
            return false;
        }

        return true;
    }

    private boolean validateBalanceChange(
            String originalRecord,
            String updatedRecord,
            String[] transaction,
            String type) {

        String[] original = originalRecord.split("\\|", -1);
        String[] updated = updatedRecord.split("\\|", -1);

        BigDecimal amount = money(transaction[3]);
        BigDecimal previous = money(transaction[4]);
        BigDecimal newBalance = money(transaction[5]);
        BigDecimal oldBalance = money(original[4]);
        BigDecimal updatedBalance = money(updated[4]);
        BigDecimal oldDeposits = money(original[5]);
        BigDecimal updatedDeposits = money(updated[5]);

        if (amount == null || previous == null || newBalance == null
                || oldBalance == null || updatedBalance == null
                || oldDeposits == null || updatedDeposits == null) {
            return false;
        }

        if (previous.compareTo(oldBalance) != 0
                || updatedBalance.compareTo(newBalance) != 0) {
            return false;
        }

        if (type.equalsIgnoreCase("DEPOSIT")) {

            return newBalance.compareTo(previous.add(amount)) == 0
                    && updatedDeposits.compareTo(oldDeposits.add(amount)) == 0
                    && updated[6].equals(original[6]);

        }

        if (type.equalsIgnoreCase("WITHDRAWAL")) {

            return oldBalance.compareTo(amount) >= 0
                    && newBalance.compareTo(previous.subtract(amount)) == 0
                    && updatedDeposits.compareTo(oldDeposits) == 0
                    && updated[6].equals(original[6]);
        }

        return false;
    }

    // ==========================================
    // CUSTOMER RECORD REPLACEMENT
    // ==========================================

    private List<String> replaceCustomerRecord(
            String accountID,
            String updatedCustomerRecord,
            List<String> originalCustomers) {

        List<String> updatedCustomers = new ArrayList<>();
        boolean found = false;

        for (String record : originalCustomers) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length >= 1
                    && fields[0].equalsIgnoreCase(accountID.trim())) {

                if (found || fields.length != 7) {
                    return null;
                }

                updatedCustomers.add(updatedCustomerRecord);
                found = true;

            } else {
                updatedCustomers.add(record);
            }
        }

        return found ? updatedCustomers : null;
    }

    // ==========================================
    // ATOMIC FILE WRITING
    // ==========================================

    private synchronized boolean writeLinesAtomically(
            Path targetFile,
            List<String> records,
            String temporaryFileName) {

        Path temporaryFile = databaseFolder.resolve(temporaryFileName);

        try {

            Files.write(
                    temporaryFile,
                    records,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );

            try {

                Files.move(
                        temporaryFile,
                        targetFile,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );

            } catch (AtomicMoveNotSupportedException e) {

                Files.move(
                        temporaryFile,
                        targetFile,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            return true;

        } catch (IOException e) {

            System.out.println(
                    "Error updating " + targetFile.getFileName()
                    + ": " + e.getMessage()
            );

            try {
                Files.deleteIfExists(temporaryFile);
            } catch (IOException ignored) {
                // Ignore cleanup failure.
            }

            return false;
        }
    }
}