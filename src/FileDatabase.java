
package src;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
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

    /*
     * Returns null when reading fails.
     * This is important because an empty list must not be
     * mistaken for an empty database when an error occurs.
     */

    private List<String> readFileSafely(Path file) {

        try {
            createFileIfMissing(file);

            return Files.readAllLines(
                    file,
                    StandardCharsets.UTF_8
            );

        } catch (IOException e) {

            System.out.println("Error reading "
                    + file.getFileName() + ": " + e.getMessage());

            return null;
        }
    }

    /*
     * Existing public methods retain their List<String> return type
     * for compatibility with the other project classes.
     */

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
    // BASIC RECORD VALIDATION
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

    private boolean validateCustomerRecord(String record) {

        if (!validateRecord(record)) {
            return false;
        }

        String[] fields = record.split("\\|", -1);

        if (fields.length != 7
                || !validateAccountID(fields[0])) {
            return false;
        }

        try {
            double balance = Double.parseDouble(fields[4]);
            double deposits = Double.parseDouble(fields[5]);
            int points = Integer.parseInt(fields[6]);

            return Double.isFinite(balance)
                    && Double.isFinite(deposits)
                    && balance >= 0
                    && deposits >= 0
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
                || fields[2].trim().isEmpty()) {
            return false;
        }

        try {

            double amount = Double.parseDouble(fields[3]);
            double previousBalance = Double.parseDouble(fields[4]);
            double newBalance = Double.parseDouble(fields[5]);

            return Double.isFinite(amount)
                    && Double.isFinite(previousBalance)
                    && Double.isFinite(newBalance)
                    && amount > 0
                    && previousBalance >= 0
                    && newBalance >= 0
                    && !fields[6].trim().isEmpty()
                    && fields[7].equalsIgnoreCase("SUCCESS");

        } catch (NumberFormatException e) {
            return false;
        }
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

        try {

            double depositAmount = Double.parseDouble(fields[3]);
            int pointsEarned = Integer.parseInt(fields[4]);

            return Double.isFinite(depositAmount)
                    && depositAmount > 0
                    && pointsEarned >= 0;

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

            if (fields.length == 6) {

                if (fields[0].equalsIgnoreCase(rewardID)
                        || fields[2].equalsIgnoreCase(transactionID)) {
                    return true;
                }
            }
        }

        return false;
    }

    // ==========================================
    // APPEND DATABASE RECORDS
    // ==========================================

    private synchronized boolean appendLine(
            Path file,
            String record) {

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

            System.out.println("Error saving record: "
                    + e.getMessage());

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

            if (fields.length == 2
                    && fields[0].equalsIgnoreCase(accountID.trim())) {

                String status = fields[1].trim();

                return status.equalsIgnoreCase("ACTIVE");
            }
        }

        // Existing accounts without status records remain active.
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

            if (fields.length == 2
                    && fields[0].equalsIgnoreCase(accountID.trim())) {

                String status = fields[1].trim().toUpperCase();

                if (status.equals("ACTIVE")
                        || status.equals("INACTIVE")) {
                    return status;
                }

                return "INACTIVE";
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

            if (fields.length == 2
                    && fields[0].equalsIgnoreCase(accountID.trim())) {
                return true;
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

            if (fields.length == 2
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
            updatedRecords.add(
                    accountID.trim() + "|" + normalizedStatus
            );
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
     *
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
            // Fail closed if login security information cannot be read.
            return true;
        }

        for (String record : records) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length == 3
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

            if (fields.length == 3
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
            lockUntil = System.currentTimeMillis()
                    + LOCK_DURATION_MILLIS;
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
            return 0;
        }

        for (String record : records) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length == 3
                    && fields[0].equalsIgnoreCase(accountID.trim())) {

                if (!validLoginSecurityRecord(fields)) {
                    return 0;
                }

                return Integer.parseInt(fields[1]);
            }
        }

        return 0;
    }

    public synchronized long getLoginLockRemainingMillis(
            String accountID) {

        if (!validateAccountID(accountID)) {
            return 0;
        }

        List<String> records = readFileSafely(loginSecurityFile);

        if (records == null) {
            return 0;
        }

        for (String record : records) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length == 3
                    && fields[0].equalsIgnoreCase(accountID.trim())) {

                if (!validLoginSecurityRecord(fields)) {
                    return 0;
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

            if (fields.length == 3
                    && fields[0].equalsIgnoreCase(accountID.trim())) {

                if (!found) {
                    updatedRecords.add(
                            accountID.trim()
                            + "|" + attempts
                            + "|" + lockUntil
                    );
                    found = true;
                }

            } else {
                updatedRecords.add(record);
            }
        }

        if (!found) {
            updatedRecords.add(
                    accountID.trim()
                    + "|" + attempts
                    + "|" + lockUntil
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

            if (fields.length == 7
                    && fields[0].equalsIgnoreCase(accountID.trim())) {

                if (found) {
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

            if (fields.length == 4
                    && fields[0].equalsIgnoreCase(accountID.trim())) {

                if (found) {
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
    // CUSTOMER LOOKUP FOR TRANSACTION PROCESSING
    // ==========================================

    private boolean customerExistsInRecords(
            String accountID,
            List<String> customers) {

        for (String record : customers) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length == 7
                    && fields[0].equalsIgnoreCase(accountID.trim())) {
                return true;
            }
        }

        return false;
    }

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
                transactionRecord
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

        String[] updatedFields =
                updatedCustomerRecord.split("\\|", -1);

        String[] transactionFields =
                transactionRecord.split("\\|", -1);

        String[] rewardFields =
                rewardRecord.split("\\|", -1);

        if (!updatedFields[0].equalsIgnoreCase(accountID.trim())
                || !transactionFields[1].equalsIgnoreCase(accountID.trim())
                || !transactionFields[2].equalsIgnoreCase("DEPOSIT")
                || !rewardFields[1].equalsIgnoreCase(accountID.trim())
                || !rewardFields[2].equalsIgnoreCase(transactionFields[0])) {
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

        List<String> updatedCustomers = new ArrayList<>();
        boolean customerFound = false;

        for (String record : originalCustomers) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length == 7
                    && fields[0].equalsIgnoreCase(accountID.trim())) {

                if (customerFound) {
                    return false;
                }

                updatedCustomers.add(updatedCustomerRecord);
                customerFound = true;

            } else {
                updatedCustomers.add(record);
            }
        }

        if (!customerFound) {
            return false;
        }

        List<String> updatedTransactions =
                new ArrayList<>(originalTransactions);
        updatedTransactions.add(transactionRecord);

        List<String> updatedRewards =
                new ArrayList<>(originalRewards);
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
                transactionRecord
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

        if (!senderFields[0].equalsIgnoreCase(recipientFields[0])
                || !senderFields[1].equalsIgnoreCase(senderID.trim())
                || !recipientFields[1].equalsIgnoreCase(recipientID.trim())
                || !senderFields[2].equalsIgnoreCase("TRANSFER_OUT")
                || !recipientFields[2].equalsIgnoreCase("TRANSFER_IN")) {
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

        List<String> updatedCustomers = new ArrayList<>();

        boolean senderFound = false;
        boolean recipientFound = false;

        for (String record : originalCustomers) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length != 7) {
                updatedCustomers.add(record);
                continue;
            }

            if (fields[0].equalsIgnoreCase(senderID.trim())) {

                if (senderFound) {
                    return false;
                }

                updatedCustomers.add(updatedSenderRecord);
                senderFound = true;

            } else if (fields[0].equalsIgnoreCase(recipientID.trim())) {

                if (recipientFound) {
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

        List<String> updatedTransactions =
                new ArrayList<>(originalTransactions);

        // Two records are allowed for one transfer:
        // TRANSFER_OUT and TRANSFER_IN share the same transaction ID.
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

    // ==========================================
    // SHARED DEPOSIT / WITHDRAWAL PROCESSING
    // ==========================================

    private synchronized boolean updateCustomerAndSaveTransactions(
            String accountID,
            String updatedCustomerRecord,
            String transactionRecord) {

        if (!validateAccountID(accountID)
                || !validateCustomerRecord(updatedCustomerRecord)
                || !validateTransactionRecord(transactionRecord)) {
            return false;
        }

        String[] updatedFields =
                updatedCustomerRecord.split("\\|", -1);

        String[] transactionFields =
                transactionRecord.split("\\|", -1);

        if (!updatedFields[0].equalsIgnoreCase(accountID.trim())
                || !transactionFields[1].equalsIgnoreCase(accountID.trim())) {
            return false;
        }

        String type = transactionFields[2];

        if (!type.equalsIgnoreCase("DEPOSIT")
                && !type.equalsIgnoreCase("WITHDRAWAL")) {
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

        List<String> updatedCustomers = new ArrayList<>();
        boolean found = false;

        for (String record : originalCustomers) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length == 7
                    && fields[0].equalsIgnoreCase(accountID.trim())) {

                if (found) {
                    return false;
                }

                updatedCustomers.add(updatedCustomerRecord);
                found = true;

            } else {
                updatedCustomers.add(record);
            }
        }

        if (!found) {
            return false;
        }

        List<String> updatedTransactions =
                new ArrayList<>(originalTransactions);

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
