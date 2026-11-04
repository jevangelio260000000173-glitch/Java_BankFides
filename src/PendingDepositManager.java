
package src;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;

public class PendingDepositManager {

    private final Scanner scanner;
    private final FileDatabase database;
    private final Transaction_Protection security;

    // ==========================================
    // REWARD SETTINGS
    // ==========================================

    private static final BigDecimal MINIMUM_REWARD_DEPOSIT =
            new BigDecimal("100.00");

    private static final BigDecimal POINTS_DIVISOR =
            new BigDecimal("100");

    private static final BigDecimal[] MILESTONES = {
        new BigDecimal("5000.00"),
        new BigDecimal("10000.00"),
        new BigDecimal("20000.00")
    };

    private static final int[] BONUS_POINTS = {
        25, 50, 100
    };

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public PendingDepositManager(
            Scanner scanner,
            FileDatabase database) {

        this.scanner = scanner;
        this.database = database;
        this.security = new Transaction_Protection(database);
    }

    // =========================================================
    // STAFF PENDING DEPOSIT MENU
    // =========================================================

    public void staffMenu(String staffID) {

        if (!staffExists(staffID)) {

            showMessage(
                    "ACCESS DENIED",
                    "The staff account could not be verified."
            );

            return;
        }

        int choice;

        do {

            System.out.println("\n==================================================");
            System.out.println("             PENDING DEPOSIT MANAGEMENT");
            System.out.println("==================================================");
            System.out.println("Staff ID: " + staffID);
            System.out.println("--------------------------------------------------");
            System.out.println(
                    "Pending Requests: " + database.countPendingDeposits()
            );
            System.out.println("==================================================");
            System.out.println("[1] View Pending Deposits");
            System.out.println("[2] Review Deposit Request");
            System.out.println("[3] Return to Staff Dashboard");
            System.out.println("==================================================");

            choice = readChoice(1, 3);

            switch (choice) {

                case 1:
                    displayPendingDeposits();
                    break;

                case 2:
                    reviewDepositRequest(staffID);
                    break;

                case 3:
                    System.out.println(
                            "\nReturning to Staff Dashboard..."
                    );
                    break;
            }

        } while (choice != 3);
    }

    // =========================================================
    // DISPLAY PENDING DEPOSITS
    // =========================================================

    private void displayPendingDeposits() {

        List<String> records = database.readPendingDeposits();

        System.out.println("\n==================================================");
        System.out.println("               PENDING DEPOSIT LIST");
        System.out.println("==================================================");

        int count = 0;

        for (String record : records) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            // Current FileDatabase format contains 7 fields.
            if (fields.length != 7) {
                continue;
            }

            if (!fields[4].equalsIgnoreCase("PENDING")) {
                continue;
            }

            BigDecimal amount;

            try {
                amount = new BigDecimal(fields[2]);
            } catch (NumberFormatException e) {
                System.out.println(
                        "Skipping invalid deposit record: " + fields[0]
                );
                continue;
            }

            System.out.println("------------------------------------------");
            System.out.println("Pending ID      : " + fields[0]);
            System.out.println("Account ID      : " + fields[1]);

            System.out.printf(
                    "Amount          : PHP %,.2f%n",
                    amount
            );

            System.out.println("Submitted At    : " + fields[3]);
            System.out.println("Status          : " + fields[4]);

            count++;
        }

        System.out.println("------------------------------------------");

        if (count == 0) {
            System.out.println(
                    "There are currently no pending deposit requests."
            );
        } else {
            System.out.println("Total Pending Requests: " + count);
        }

        System.out.println("==================================================");

        pause();
    }

    // =========================================================
    // REVIEW DEPOSIT REQUEST
    // =========================================================

    private void reviewDepositRequest(String staffID) {

        System.out.println("\n==================================================");
        System.out.println("             REVIEW DEPOSIT REQUEST");
        System.out.println("==================================================");

        if (database.countPendingDeposits() == 0) {

            System.out.println("No pending deposit requests to review.");
            pause();
            return;
        }

        System.out.print("Enter Pending Deposit ID: ");
        String pendingID = scanner.nextLine().trim();

        if (pendingID.isEmpty()) {

            System.out.println("Pending ID cannot be empty.");
            pause();
            return;
        }

        String record = findPendingDeposit(pendingID);

        if (record == null) {

            System.out.println(
                    "Deposit request not found or multiple matching records exist."
            );

            pause();
            return;
        }

        String[] fields = record.split("\\|", -1);

        if (fields.length != 7
                || !fields[4].equalsIgnoreCase("PENDING")) {

            System.out.println(
                    "This deposit request is invalid or has already been reviewed."
            );

            pause();
            return;
        }

        String accountID = fields[1];

        BigDecimal amount;

        try {

            amount = new BigDecimal(fields[2])
                    .setScale(2, RoundingMode.UNNECESSARY);

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new NumberFormatException();
            }

        } catch (NumberFormatException | ArithmeticException e) {

            System.out.println("Invalid deposit amount in the request.");
            pause();
            return;
        }

        String customerRecord = findCustomerRecord(accountID);

        if (customerRecord == null) {

            System.out.println("Customer account could not be found.");
            pause();
            return;
        }

        String[] customerFields = customerRecord.split("\\|", -1);

        String fullName = decode(customerFields[1]);

        System.out.println("\n--------------------------------------------------");
        System.out.println("              DEPOSIT REQUEST DETAILS");
        System.out.println("--------------------------------------------------");
        System.out.println("Pending ID      : " + fields[0]);
        System.out.println("Customer Name   : " + fullName);
        System.out.println("Account ID      : " + accountID);

        System.out.printf(
                "Deposit Amount  : PHP %,.2f%n",
                amount
        );

        System.out.println("Submitted At    : " + fields[3]);
        System.out.println("Current Status  : " + fields[4]);
        System.out.println("--------------------------------------------------");
        System.out.println("[1] Approve Deposit");
        System.out.println("[2] Reject Deposit");
        System.out.println("[3] Cancel / Return");
        System.out.println("--------------------------------------------------");

        int choice = readChoice(1, 3);

        switch (choice) {

            case 1:
                approveDeposit(pendingID, staffID);
                break;

            case 2:
                rejectDeposit(pendingID, staffID);
                break;

            case 3:
                System.out.println(
                        "\nReturning to Pending Deposits Menu."
                );
                break;
        }
    }

    // =========================================================
    // APPROVE DEPOSIT
    // =========================================================

    private void approveDeposit(
            String pendingID,
            String staffID) {

        // STEP 1: GET THE LATEST PENDING REQUEST

        String pendingRecord = findPendingDeposit(pendingID);

        if (pendingRecord == null) {

            showMessage(
                    "APPROVAL FAILED",
                    "The deposit request could not be found."
            );

            return;
        }

        String[] pendingFields = pendingRecord.split("\\|", -1);

        if (pendingFields.length != 7
                || !pendingFields[4].equalsIgnoreCase("PENDING")) {

            showMessage(
                    "APPROVAL FAILED",
                    "This request has already been reviewed or is invalid."
            );

            return;
        }

        String accountID = pendingFields[1];

        BigDecimal amount;

        try {

            amount = new BigDecimal(pendingFields[2])
                    .setScale(2, RoundingMode.UNNECESSARY);

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new NumberFormatException();
            }

        } catch (NumberFormatException | ArithmeticException e) {

            showMessage(
                    "APPROVAL FAILED",
                    "The deposit amount is invalid."
            );

            return;
        }

        // STEP 2: VERIFY CUSTOMER ACCOUNT

        if (!security.isAccountActive(accountID)) {

            showMessage(
                    "APPROVAL FAILED",
                    "The customer account does not exist or is inactive."
            );

            return;
        }

        String customerRecord = findCustomerRecord(accountID);

        if (customerRecord == null) {

            showMessage(
                    "APPROVAL FAILED",
                    "Customer account could not be found."
            );

            return;
        }

        String[] customerFields = customerRecord.split("\\|", -1);

        if (customerFields.length != 7) {

            showMessage(
                    "APPROVAL FAILED",
                    "Customer account information is invalid."
            );

            return;
        }

        // STEP 3: READ CURRENT ACCOUNT INFORMATION

        String fullName;
        BigDecimal balance;
        BigDecimal totalDeposits;
        int currentRewardPoints;

        try {

            fullName = decode(customerFields[1]);

            balance = new BigDecimal(customerFields[4])
                    .setScale(2, RoundingMode.HALF_UP);

            totalDeposits = new BigDecimal(customerFields[5])
                    .setScale(2, RoundingMode.HALF_UP);

            currentRewardPoints = Integer.parseInt(customerFields[6]);

            if (balance.compareTo(BigDecimal.ZERO) < 0
                    || totalDeposits.compareTo(BigDecimal.ZERO) < 0
                    || currentRewardPoints < 0) {

                throw new NumberFormatException();
            }

        } catch (NumberFormatException e) {

            showMessage(
                    "APPROVAL FAILED",
                    "Unable to read the customer's account information."
            );

            return;
        }

        // STEP 4: RECHECK TRANSACTION SECURITY

        if (!security.validateTransaction(
                accountID,
                "DEPOSIT",
                amount,
                null)) {

            showMessage(
                    "APPROVAL FAILED",
                    "The deposit failed the transaction security checks."
            );

            return;
        }

        // STEP 5: CALCULATE UPDATED ACCOUNT VALUES

        BigDecimal newBalance = balance.add(amount);
        BigDecimal newTotalDeposits = totalDeposits.add(amount);

        if (!Double.isFinite(newBalance.doubleValue())
                || !Double.isFinite(newTotalDeposits.doubleValue())) {

            showMessage(
                    "APPROVAL FAILED",
                    "The deposit exceeds the supported account limit."
            );

            return;
        }

        // STEP 6: CALCULATE REGULAR REWARD POINTS

        int regularPoints;

        try {

            regularPoints = calculateRewardPoints(amount);

        } catch (ArithmeticException e) {

            showMessage(
                    "APPROVAL FAILED",
                    "The reward point limit has been reached."
            );

            return;
        }

        // STEP 7: CALCULATE MILESTONE BONUS

        int milestoneBonus;

        try {

            milestoneBonus = calculateMilestoneBonus(
                    totalDeposits,
                    newTotalDeposits
            );

        } catch (ArithmeticException e) {

            showMessage(
                    "APPROVAL FAILED",
                    "The milestone bonus limit has been reached."
            );

            return;
        }

        int totalEarnedPoints;
        int newRewardPoints;

        try {

            totalEarnedPoints = Math.addExact(
                    regularPoints,
                    milestoneBonus
            );

            newRewardPoints = Math.addExact(
                    currentRewardPoints,
                    totalEarnedPoints
            );

        } catch (ArithmeticException e) {

            showMessage(
                    "APPROVAL FAILED",
                    "The reward point limit has been reached."
            );

            return;
        }

        // STEP 8: DISPLAY APPROVAL SUMMARY

        System.out.println("\n==================================================");
        System.out.println("               APPROVAL SUMMARY");
        System.out.println("==================================================");
        System.out.println("Pending ID      : " + pendingID);
        System.out.println("Customer Name   : " + fullName);
        System.out.println("Account ID      : " + accountID);

        System.out.printf(
                "Deposit Amount  : PHP %,.2f%n",
                amount
        );

        System.out.printf(
                "Previous Balance: PHP %,.2f%n",
                balance
        );

        System.out.printf(
                "New Balance     : PHP %,.2f%n",
                newBalance
        );

        System.out.println("------------------------------------------");
        System.out.println("Regular Points  : +" + regularPoints);
        System.out.println("Milestone Bonus : +" + milestoneBonus);
        System.out.println("Total Points    : +" + totalEarnedPoints);
        System.out.println("New Reward Total: " + newRewardPoints);
        System.out.println("==================================================");
        System.out.println("[1] Confirm Approval");
        System.out.println("[2] Cancel");
        System.out.println("==================================================");

        int choice = readChoice(1, 2);

        if (choice == 2) {

            System.out.println("\nDeposit approval cancelled.");
            pause();
            return;
        }

        // STEP 9: PREPARE APPROVAL RECORDS

        String transactionID = generateTransactionID();
        String rewardID = generateRewardID();

        if (transactionID == null || rewardID == null) {

            showMessage(
                    "APPROVAL FAILED",
                    "Unable to generate transaction or reward ID."
            );

            return;
        }

        String reviewedAt = LocalDateTime.now().format(DATE_FORMAT);

        String updatedCustomerRecord =
                customerFields[0] + "|"
                + customerFields[1] + "|"
                + customerFields[2] + "|"
                + customerFields[3] + "|"
                + newBalance.toPlainString() + "|"
                + newTotalDeposits.toPlainString() + "|"
                + newRewardPoints;

        String transactionRecord =
                transactionID + "|"
                + accountID + "|"
                + "DEPOSIT" + "|"
                + amount.toPlainString() + "|"
                + balance.toPlainString() + "|"
                + newBalance.toPlainString() + "|"
                + reviewedAt + "|"
                + "SUCCESS";

        String rewardRecord =
                rewardID + "|"
                + accountID + "|"
                + transactionID + "|"
                + amount.toPlainString() + "|"
                + totalEarnedPoints + "|"
                + reviewedAt;

        // STEP 10: PROCESS APPROVAL USING FILEDATABASE

        try {

            boolean approved = database.approvePendingDeposit(
                    pendingID,
                    updatedCustomerRecord,
                    transactionRecord,
                    rewardRecord,
                    staffID,
                    reviewedAt
            );

            if (!approved) {

                showMessage(
                        "APPROVAL FAILED",
                        "The deposit could not be approved. "
                        + "The account may have changed or the request "
                        + "may already have been reviewed."
                );

                return;
            }

            // STEP 11: SAVE ACTIVITY LOG

            boolean activitySaved = database.saveActivityLog(
                    reviewedAt
                    + "|" + staffID
                    + "|DEPOSIT_APPROVED"
                    + "|" + pendingID
                    + "|" + transactionID
            );

            if (!activitySaved) {

                System.out.println(
                        "\nWarning: Activity log could not be saved."
                );
            }

            // STEP 12: DISPLAY APPROVAL RECEIPT

            System.out.println("\n==================================================");
            System.out.println("              DEPOSIT APPROVED!");
            System.out.println("==================================================");
            System.out.println("Pending ID      : " + pendingID);
            System.out.println("Transaction ID  : " + transactionID);
            System.out.println("Reward ID       : " + rewardID);
            System.out.println("Customer Name   : " + fullName);
            System.out.println("Account ID      : " + accountID);

            System.out.printf(
                    "Approved Amount : PHP %,.2f%n",
                    amount
            );

            System.out.printf(
                    "Previous Balance: PHP %,.2f%n",
                    balance
            );

            System.out.printf(
                    "New Balance     : PHP %,.2f%n",
                    newBalance
            );

            System.out.println("------------------------------------------");
            System.out.println("Regular Points  : +" + regularPoints);
            System.out.println("Milestone Bonus : +" + milestoneBonus);
            System.out.println("Total Points    : +" + totalEarnedPoints);
            System.out.println("Available Points: " + newRewardPoints);
            System.out.println("------------------------------------------");
            System.out.println("Reviewed By     : " + staffID);
            System.out.println("Reviewed At     : " + reviewedAt);
            System.out.println("Status          : APPROVED");
            System.out.println("==================================================");

            pause();

        } catch (RuntimeException e) {

            showMessage(
                    "APPROVAL FAILED",
                    "An unexpected error occurred while processing "
                    + "the deposit. " + e.getMessage()
            );
        }
    }

    // =========================================================
    // REJECT DEPOSIT
    // =========================================================

    private void rejectDeposit(
            String pendingID,
            String staffID) {

        String pendingRecord = findPendingDeposit(pendingID);

        if (pendingRecord == null) {

            showMessage(
                    "REJECTION FAILED",
                    "The deposit request could not be found."
            );

            return;
        }

        String[] fields = pendingRecord.split("\\|", -1);

        if (fields.length != 7
                || !fields[4].equalsIgnoreCase("PENDING")) {

            showMessage(
                    "REJECTION FAILED",
                    "This request has already been reviewed or is invalid."
            );

            return;
        }

        System.out.println("\n==================================================");
        System.out.println("               REJECT DEPOSIT");
        System.out.println("==================================================");
        System.out.println("Pending ID : " + pendingID);
        System.out.println("Account ID : " + fields[1]);
        System.out.println("Amount     : PHP " + fields[2]);
        System.out.println("--------------------------------------------------");

        System.out.print("Enter reason for rejection: ");
        String reason = scanner.nextLine().trim();

        if (reason.isEmpty()) {

            System.out.println("A rejection reason is required.");
            pause();
            return;
        }

        System.out.println("\n[1] Confirm Rejection");
        System.out.println("[2] Cancel");

        int choice = readChoice(1, 2);

        if (choice == 2) {

            System.out.println("\nDeposit rejection cancelled.");
            pause();
            return;
        }

        String reviewedAt = LocalDateTime.now().format(DATE_FORMAT);

        try {

            // Use the existing FileDatabase rejection method.
            boolean rejected = database.rejectPendingDeposit(
                    pendingID,
                    staffID,
                    reviewedAt
            );

            if (!rejected) {

                showMessage(
                        "REJECTION FAILED",
                        "The request could not be rejected. "
                        + "It may have already been reviewed."
                );

                return;
            }

            // Encode the reason so pipe characters and special
            // characters do not break the activity log format.
            String encodedReason = Base64.getEncoder().encodeToString(
                    reason.getBytes(StandardCharsets.UTF_8)
            );

            boolean activitySaved = database.saveActivityLog(
                    reviewedAt
                    + "|" + staffID
                    + "|DEPOSIT_REJECTED"
                    + "|" + pendingID
                    + "|" + encodedReason
            );

            if (!activitySaved) {

                System.out.println(
                        "\nWarning: Activity log could not be saved."
                );
            }

            System.out.println("\n==================================================");
            System.out.println("              DEPOSIT REJECTED");
            System.out.println("==================================================");
            System.out.println("Pending ID  : " + pendingID);
            System.out.println("Account ID  : " + fields[1]);
            System.out.println("Amount      : PHP " + fields[2]);
            System.out.println("Reviewed By : " + staffID);
            System.out.println("Reviewed At : " + reviewedAt);
            System.out.println("Status      : REJECTED");
            System.out.println("------------------------------------------");
            System.out.println("Reason      : " + reason);
            System.out.println("------------------------------------------");
            System.out.println(
                    "The customer's balance and rewards were not changed."
            );
            System.out.println("==================================================");

            pause();

        } catch (RuntimeException e) {

            showMessage(
                    "REJECTION FAILED",
                    "Unable to update the deposit request. "
                    + e.getMessage()
            );
        }
    }

    // =========================================================
    // FIND PENDING DEPOSIT
    // =========================================================

    private String findPendingDeposit(String pendingID) {

        if (pendingID == null || pendingID.trim().isEmpty()) {
            return null;
        }

        List<String> records = database.readPendingDeposits();

        String found = null;

        for (String record : records) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length != 7) {
                continue;
            }

            if (fields[0].equalsIgnoreCase(pendingID.trim())) {

                // Do not process duplicate pending IDs.
                if (found != null) {
                    return null;
                }

                found = record;
            }
        }

        return found;
    }

    // =========================================================
    // CALCULATE REGULAR REWARD POINTS
    // =========================================================

    private int calculateRewardPoints(BigDecimal amount) {

        if (amount.compareTo(MINIMUM_REWARD_DEPOSIT) < 0) {
            return 0;
        }

        return amount
                .divideToIntegralValue(POINTS_DIVISOR)
                .intValueExact();
    }

    // =========================================================
    // CALCULATE MILESTONE BONUS
    // =========================================================

    private int calculateMilestoneBonus(
            BigDecimal previousTotalDeposits,
            BigDecimal newTotalDeposits) {

        int bonus = 0;

        for (int i = 0; i < MILESTONES.length; i++) {

            boolean reachedMilestone =
                    previousTotalDeposits.compareTo(MILESTONES[i]) < 0
                    && newTotalDeposits.compareTo(MILESTONES[i]) >= 0;

            if (reachedMilestone) {

                bonus = Math.addExact(
                        bonus,
                        BONUS_POINTS[i]
                );
            }
        }

        return bonus;
    }

    // =========================================================
    // FIND CUSTOMER RECORD
    // =========================================================

    private String findCustomerRecord(String accountID) {

        if (accountID == null || accountID.trim().isEmpty()) {
            return null;
        }

        List<String> customers = database.readCustomers();

        String found = null;

        for (String record : customers) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length == 7
                    && fields[0].equalsIgnoreCase(accountID.trim())) {

                // Avoid processing duplicate customer accounts.
                if (found != null) {
                    return null;
                }

                found = record;
            }
        }

        return found;
    }

    // =========================================================
    // VERIFY STAFF ACCOUNT
    // =========================================================

    private boolean staffExists(String staffID) {

        if (staffID == null || staffID.trim().isEmpty()) {
            return false;
        }

        List<String> staffRecords = database.readStaff();

        for (String record : staffRecords) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length == 4
                    && fields[0].equalsIgnoreCase(staffID.trim())) {

                return true;
            }
        }

        return false;
    }

    // =========================================================
    // GENERATE TRANSACTION ID
    // =========================================================

    private String generateTransactionID() {

        int highestNumber = 1000;

        for (String record : database.readTransactions()) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length > 0 && fields[0].startsWith("T")) {

                try {

                    int number = Integer.parseInt(
                            fields[0].substring(1)
                    );

                    if (number > highestNumber) {
                        highestNumber = number;
                    }

                } catch (NumberFormatException e) {
                    // Skip invalid transaction IDs.
                }
            }
        }

        if (highestNumber == Integer.MAX_VALUE) {
            return null;
        }

        return "T" + (highestNumber + 1);
    }

    // =========================================================
    // GENERATE REWARD ID
    // =========================================================

    private String generateRewardID() {

        int highestNumber = 1000;

        for (String record : database.readRewards()) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length > 0 && fields[0].startsWith("R")) {

                try {

                    int number = Integer.parseInt(
                            fields[0].substring(1)
                    );

                    if (number > highestNumber) {
                        highestNumber = number;
                    }

                } catch (NumberFormatException e) {
                    // Skip invalid reward IDs.
                }
            }
        }

        if (highestNumber == Integer.MAX_VALUE) {
            return null;
        }

        return "R" + (highestNumber + 1);
    }

    // =========================================================
    // BASE64 DECODING
    // =========================================================

    private String decode(String value) {

        try {

            byte[] decoded = Base64.getDecoder().decode(value);

            return new String(
                    decoded,
                    StandardCharsets.UTF_8
            );

        } catch (IllegalArgumentException e) {

            return value;
        }
    }

    // =========================================================
    // INPUT VALIDATION
    // =========================================================

    private int readChoice(int min, int max) {

        while (true) {

            System.out.print("\nEnter your choice: ");

            String input = scanner.nextLine().trim();

            try {

                int choice = Integer.parseInt(input);

                if (choice >= min && choice <= max) {
                    return choice;
                }

                System.out.println(
                        "Invalid choice. Enter a number from "
                        + min + " to " + max + "."
                );

            } catch (NumberFormatException e) {

                System.out.println(
                        "Please enter a valid number."
                );
            }
        }
    }

    // =========================================================
    // DISPLAY MESSAGE
    // =========================================================

    private void showMessage(String title, String message) {

        System.out.println("\n==================================================");
        System.out.println("               " + title);
        System.out.println("==================================================");
        System.out.println(message);
        System.out.println("==================================================");

        pause();
    }

    // =========================================================
    // PAUSE
    // =========================================================

    private void pause() {

        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }
}
