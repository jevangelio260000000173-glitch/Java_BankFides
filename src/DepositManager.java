
package src;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

public class DepositManager {

    private final Scanner scanner;
    private final FileDatabase database;

    // ==========================================
    // REWARD SETTINGS
    // ==========================================

    private static final double MINIMUM_REWARD_DEPOSIT = 100.00;
    private static final int POINTS_PER_100 = 1;

    public DepositManager(Scanner scanner) {
        this.scanner = scanner;
        this.database = new FileDatabase();
    }

    // =========================================================
    // DEPOSIT / CASH IN
    // =========================================================

    public DepositResult deposit(String accountID) {

        String customerRecord = findCustomerRecord(accountID);

        if (customerRecord == null) {
            showFailure("Customer account could not be found.");
            return null;
        }

        String[] fields = customerRecord.split("\\|", -1);

        if (fields.length != 7) {
            showFailure("Unable to read the customer account.");
            return null;
        }

        String fullName;
        double balance;
        double totalDeposits;
        int rewardPoints;

        try {

            fullName = decode(fields[1]);
            balance = Double.parseDouble(fields[4]);
            totalDeposits = Double.parseDouble(fields[5]);
            rewardPoints = Integer.parseInt(fields[6]);

        } catch (Exception e) {

            showFailure("Unable to read the customer account details.");
            return null;
        }

        // ==========================================
        // DISPLAY ACCOUNT INFORMATION
        // ==========================================

        System.out.println("\n==========================================");
        System.out.println("              CASH IN / DEPOSIT");
        System.out.println("==========================================");
        System.out.println("Account Name    : " + fullName);
        System.out.println("Account ID      : " + accountID);
        System.out.printf("Current Balance : PHP %,.2f%n", balance);

        // ==========================================
        // STEP 1: ENTER DEPOSIT AMOUNT
        // ==========================================

        double amount;

        while (true) {

            System.out.print("\nEnter deposit amount: PHP ");
            String input = scanner.nextLine().trim();

            try {

                BigDecimal enteredAmount = new BigDecimal(input);

                if (enteredAmount.compareTo(BigDecimal.ZERO) <= 0) {

                    System.out.println(
                            "Invalid amount. Deposit must be greater than zero."
                    );

                    continue;
                }

                enteredAmount = enteredAmount.setScale(
                        2,
                        RoundingMode.HALF_UP
                );

                amount = enteredAmount.doubleValue();

                if (!Double.isFinite(amount)
                        || amount <= 0
                        || !Double.isFinite(balance + amount)) {

                    System.out.println(
                            "Invalid amount. Please enter a valid deposit amount."
                    );

                    continue;
                }

                break;

            } catch (NumberFormatException e) {

                System.out.println(
                        "Invalid input. Please enter a valid amount."
                );
            }
        }

        double newBalance = balance + amount;
        double newTotalDeposits = totalDeposits + amount;

        // ==========================================
        // STEP 2: DEPOSIT CONFIRMATION
        // ==========================================

        System.out.println("\n==========================================");
        System.out.println("              DEPOSIT DETAILS");
        System.out.println("==========================================");
        System.out.println("Account Name    : " + fullName);
        System.out.println("Account ID      : " + accountID);
        System.out.printf("Current Balance : PHP %,.2f%n", balance);
        System.out.printf("Deposit Amount  : PHP %,.2f%n", amount);
        System.out.printf("New Balance     : PHP %,.2f%n", newBalance);
        System.out.println("==========================================");
        System.out.println("[1] Confirm Deposit");
        System.out.println("[2] Cancel Deposit");
        System.out.println("==========================================");

        int choice = readChoice(1, 2);

        if (choice == 2) {

            System.out.println("\nDeposit cancelled.");
            System.out.println("Your balance has not changed.");

            pause();
            return null;
        }

        // ==========================================
        // STEP 3: VERIFY CUSTOMER ACCOUNT AGAIN
        // ==========================================

        customerRecord = findCustomerRecord(accountID);

        if (customerRecord == null) {
            showFailure("Customer account could not be verified.");
            return null;
        }

        fields = customerRecord.split("\\|", -1);

        if (fields.length != 7) {
            showFailure("Customer account information is invalid.");
            return null;
        }

        try {

            fullName = decode(fields[1]);
            balance = Double.parseDouble(fields[4]);
            totalDeposits = Double.parseDouble(fields[5]);
            rewardPoints = Integer.parseInt(fields[6]);

        } catch (Exception e) {

            showFailure("Unable to verify the current account information.");
            return null;
        }

        // Recalculate using the latest account information.
        newBalance = balance + amount;
        newTotalDeposits = totalDeposits + amount;

        if (!Double.isFinite(newBalance)
                || !Double.isFinite(newTotalDeposits)) {

            showFailure(
                    "The deposit amount exceeds the supported account limit."
            );

            return null;
        }

        // ==========================================
        // STEP 4: CALCULATE REWARD POINTS
        // ==========================================

        int earnedPoints = calculateRewardPoints(amount);

        int newRewardPoints;

        try {

            newRewardPoints = Math.addExact(
                    rewardPoints,
                    earnedPoints
            );

        } catch (ArithmeticException e) {

            showFailure("The reward point limit has been reached.");
            return null;
        }

        // ==========================================
        // STEP 5: GENERATE TRANSACTION AND REWARD IDs
        // ==========================================

        String transactionID = generateTransactionID();
        String rewardID = generateRewardID();

        // ==========================================
        // STEP 6: PREPARE UPDATED CUSTOMER RECORD
        // ==========================================

        String updatedCustomerRecord =
                fields[0] + "|"
                + fields[1] + "|"
                + fields[2] + "|"
                + fields[3] + "|"
                + newBalance + "|"
                + newTotalDeposits + "|"
                + newRewardPoints;

        String dateTime = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        );

        // ==========================================
        // STEP 7: PREPARE TRANSACTION RECORD
        // ==========================================

        String transactionRecord =
                transactionID + "|"
                + accountID + "|"
                + "DEPOSIT" + "|"
                + amount + "|"
                + balance + "|"
                + newBalance + "|"
                + dateTime + "|"
                + "SUCCESS";

        // ==========================================
        // STEP 8: PREPARE REWARD RECORD
        // ==========================================

        /*
         * rewards.txt format:
         *
         * rewardID|accountID|transactionID|
         * depositAmount|pointsEarned|dateTime
         */

        String rewardRecord =
                rewardID + "|"
                + accountID + "|"
                + transactionID + "|"
                + amount + "|"
                + earnedPoints + "|"
                + dateTime;

        // ==========================================
        // STEP 9: SAVE DEPOSIT AND REWARD RECORD
        // ==========================================

        try {

            boolean depositSaved = database.processDeposit(
                    accountID,
                    updatedCustomerRecord,
                    transactionRecord,
                    rewardRecord
            );

            if (!depositSaved) {

                showFailure(
                        "Unable to save the deposit, transaction, and reward records."
                );

                return null;
            }

            // Save activity log after successful deposit.
            boolean activitySaved = database.saveActivityLog(
                    "DEPOSIT_SUCCESS|"
                    + accountID + "|"
                    + transactionID
            );

            if (!activitySaved) {

                System.out.println(
                        "\nWarning: Activity log could not be saved."
                );
            }

            // ==========================================
            // STEP 10: DISPLAY DEPOSIT RECEIPT
            // ==========================================

            System.out.println("\n==========================================");
            System.out.println("             DEPOSIT SUCCESSFUL!");
            System.out.println("==========================================");
            System.out.println("Account Name    : " + fullName);
            System.out.println("Account ID      : " + accountID);
            System.out.printf("Previous Balance: PHP %,.2f%n", balance);
            System.out.printf("Deposit Amount  : PHP %,.2f%n", amount);
            System.out.printf("New Balance     : PHP %,.2f%n", newBalance);
            System.out.println("------------------------------------------");
            System.out.println("Transaction ID  : " + transactionID);
            System.out.println("Transaction Type: DEPOSIT");
            System.out.println("Status          : SUCCESSFUL");
            System.out.println("------------------------------------------");
            System.out.println("Reward ID       : " + rewardID);
            System.out.println("Reward Points   : +" + earnedPoints);
            System.out.println("Total Points    : " + newRewardPoints);
            System.out.println("Date and Time   : " + dateTime);
            System.out.println("==========================================");
            System.out.println("Your deposit has been recorded.");

            if (earnedPoints > 0) {

                System.out.println(
                        "Congratulations! You earned reward points."
                );

            } else {

                System.out.println(
                        "This deposit did not qualify for reward points."
                );
            }

            pause();

            return new DepositResult(
                    newBalance,
                    newTotalDeposits,
                    newRewardPoints
            );

        } catch (RuntimeException e) {

            showFailure(
                    "Unable to process the deposit. Please check your database files."
            );

            System.out.println("Reason: " + e.getMessage());

            return null;
        }
    }

    // =========================================================
    // FIND CUSTOMER RECORD
    // =========================================================

    private String findCustomerRecord(String accountID) {

        List<String> records = database.readCustomers();

        for (String record : records) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length == 7
                    && fields[0].equalsIgnoreCase(accountID)) {

                return record;
            }
        }

        return null;
    }

    // =========================================================
    // REWARD POINT CALCULATION
    // =========================================================

    private int calculateRewardPoints(double amount) {

        if (amount < MINIMUM_REWARD_DEPOSIT) {
            return 0;
        }

        return (int) (amount / 100.00) * POINTS_PER_100;
    }

    // =========================================================
    // GENERATE UNIQUE TRANSACTION ID
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

        return "T" + (highestNumber + 1);
    }

    // =========================================================
    // GENERATE UNIQUE REWARD ID
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

            System.out.print("Enter your choice: ");

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
    // FAILURE MESSAGE
    // =========================================================

    private void showFailure(String message) {

        System.out.println("\n==========================================");
        System.out.println("               DEPOSIT FAILED");
        System.out.println("==========================================");
        System.out.println("Error: " + message);
        System.out.println("Your balance has not been changed.");
        System.out.println("==========================================");

        pause();
    }

    // =========================================================
    // PAUSE
    // =========================================================

    private void pause() {

        System.out.println(
                "\nPress Enter to return to the dashboard."
        );

        scanner.nextLine();
    }

    // =========================================================
    // DEPOSIT RESULT
    // =========================================================

    public static class DepositResult {

        public final double balance;
        public final double totalDeposits;
        public final int rewardPoints;

        public DepositResult(
                double balance,
                double totalDeposits,
                int rewardPoints) {

            this.balance = balance;
            this.totalDeposits = totalDeposits;
            this.rewardPoints = rewardPoints;
        }
    }
}
