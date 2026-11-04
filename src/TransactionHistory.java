
package src;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

public class TransactionHistory {

    private final Scanner scanner;
    private final FileDatabase database;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // =========================================================
    // CONSTRUCTORS
    // =========================================================

    // Retained for compatibility with existing code.
    public TransactionHistory(Scanner scanner) {
        this(scanner, new FileDatabase());
    }

    // Allows Dashboard and TransactionHistory to share a database.
    public TransactionHistory(Scanner scanner, FileDatabase database) {
        this.scanner = scanner;
        this.database = database;
    }

    // =========================================================
    // WITHDRAWAL
    // =========================================================

    public void withdraw(String accountID) {

        String customerRecord = findCustomer(accountID);

        if (customerRecord == null) {
            showMessage("TRANSACTION FAILED", "Customer account not found.");
            return;
        }

        String[] fields = customerRecord.split("\\|", -1);

        BigDecimal balance;

        try {
            balance = new BigDecimal(fields[4]);
        } catch (NumberFormatException e) {
            showMessage(
                    "TRANSACTION FAILED",
                    "Customer balance contains invalid data."
            );
            return;
        }

        System.out.println("\n==================================================");
        System.out.println("              CASH OUT / WITHDRAWAL");
        System.out.println("==================================================");
        System.out.println("Account ID: " + accountID);
        System.out.printf("Available Balance: PHP %,.2f%n", balance);
        System.out.println("--------------------------------------------------");

        BigDecimal amount = readAmount("Enter withdrawal amount: PHP ");

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            showMessage(
                    "TRANSACTION FAILED",
                    "Amount must be greater than zero."
            );
            return;
        }

        if (amount.compareTo(balance) > 0) {
            showMessage(
                    "TRANSACTION FAILED",
                    "Insufficient balance. Your balance has not changed."
            );
            return;
        }

        BigDecimal newBalance = balance.subtract(amount);

        System.out.println("\n------------------------------------------");
        System.out.println("          WITHDRAWAL DETAILS");
        System.out.println("------------------------------------------");
        System.out.printf("Withdrawal Amount: PHP %,.2f%n", amount);
        System.out.printf("Remaining Balance: PHP %,.2f%n", newBalance);

        if (!confirmTransaction("Withdrawal")) {
            return;
        }

        // Verify the latest account balance before processing.
        customerRecord = findCustomer(accountID);

        if (customerRecord == null) {
            showMessage("TRANSACTION FAILED", "Customer account not found.");
            return;
        }

        fields = customerRecord.split("\\|", -1);

        try {
            balance = new BigDecimal(fields[4]);

            if (amount.compareTo(balance) > 0) {
                showMessage(
                        "TRANSACTION FAILED",
                        "Your balance changed. Insufficient funds."
                );
                return;
            }

            newBalance = balance.subtract(amount);

            String updatedRecord = updateCustomerRecord(
                    fields,
                    newBalance,
                    new BigDecimal(fields[5]),
                    Integer.parseInt(fields[6])
            );

            String transactionID = generateTransactionID();

            String dateTime = LocalDateTime.now().format(DATE_FORMAT);

            String transactionRecord =
                    transactionID + "|"
                    + accountID + "|"
                    + "WITHDRAWAL" + "|"
                    + amount.toPlainString() + "|"
                    + balance.toPlainString() + "|"
                    + newBalance.toPlainString() + "|"
                    + dateTime + "|"
                    + "SUCCESS";

            // Save the updated customer and transaction.
            database.processWithdrawal(
                    accountID,
                    updatedRecord,
                    transactionRecord
            );

            saveActivityLog(
                    "WITHDRAWAL_SUCCESS|" + accountID + "|" + transactionID
            );

            System.out.println("\n==================================================");
            System.out.println("           WITHDRAWAL SUCCESSFUL!");
            System.out.println("==================================================");
            System.out.printf("Previous Balance : PHP %,.2f%n", balance);
            System.out.printf("Withdrawal Amount: PHP %,.2f%n", amount);
            System.out.printf("New Balance      : PHP %,.2f%n", newBalance);
            System.out.println("Transaction ID   : " + transactionID);
            System.out.println("Status           : SUCCESSFUL");
            System.out.println("==================================================");

        } catch (NumberFormatException e) {

            showMessage(
                    "TRANSACTION FAILED",
                    "Invalid customer account information."
            );

        } catch (RuntimeException e) {

            showMessage(
                    "TRANSACTION FAILED",
                    "Your withdrawal could not be completed.\nReason: "
                    + e.getMessage()
            );
        }

        pause();
    }

    // =========================================================
    // TRANSFER
    // =========================================================

    public void transfer(String senderID) {

        String senderRecord = findCustomer(senderID);

        if (senderRecord == null) {
            showMessage("TRANSFER FAILED", "Sender account not found.");
            return;
        }

        String[] senderFields = senderRecord.split("\\|", -1);

        BigDecimal senderBalance;

        try {
            senderBalance = new BigDecimal(senderFields[4]);
        } catch (NumberFormatException e) {
            showMessage(
                    "TRANSFER FAILED",
                    "Sender account contains invalid balance information."
            );
            return;
        }

        System.out.println("\n==================================================");
        System.out.println("                 TRANSFER FUNDS");
        System.out.println("==================================================");
        System.out.println("Sender Account: " + senderID);
        System.out.printf("Available Balance: PHP %,.2f%n", senderBalance);
        System.out.println("--------------------------------------------------");

        System.out.print("Enter recipient account ID: ");
        String recipientID = scanner.nextLine().trim();

        if (recipientID.isEmpty()) {
            showMessage(
                    "TRANSFER FAILED",
                    "Recipient account ID is required."
            );
            return;
        }

        if (recipientID.equalsIgnoreCase(senderID)) {
            showMessage(
                    "TRANSFER FAILED",
                    "You cannot transfer money to your own account."
            );
            return;
        }

        String recipientRecord = findCustomer(recipientID);

        if (recipientRecord == null) {
            showMessage(
                    "TRANSFER FAILED",
                    "Recipient account does not exist."
            );
            return;
        }

        String[] recipientFields = recipientRecord.split("\\|", -1);

        BigDecimal recipientBalance;
        String recipientName;

        try {
            recipientBalance = new BigDecimal(recipientFields[4]);
            recipientName = decode(recipientFields[1]);
        } catch (NumberFormatException e) {
            showMessage(
                    "TRANSFER FAILED",
                    "Recipient account contains invalid balance information."
            );
            return;
        }

        System.out.println("\nRecipient Name: " + recipientName);
        System.out.println("Recipient Account: " + recipientID);

        BigDecimal amount = readAmount("Enter transfer amount: PHP ");

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            showMessage(
                    "TRANSFER FAILED",
                    "Amount must be greater than zero."
            );
            return;
        }

        if (amount.compareTo(senderBalance) > 0) {
            showMessage(
                    "TRANSFER FAILED",
                    "Insufficient balance. Your balance has not changed."
            );
            return;
        }

        BigDecimal senderNewBalance = senderBalance.subtract(amount);
        BigDecimal recipientNewBalance = recipientBalance.add(amount);

        System.out.println("\n------------------------------------------");
        System.out.println("             TRANSFER DETAILS");
        System.out.println("------------------------------------------");
        System.out.println("Sender Account   : " + senderID);
        System.out.println("Recipient Name   : " + recipientName);
        System.out.println("Recipient Account: " + recipientID);
        System.out.printf("Transfer Amount  : PHP %,.2f%n", amount);
        System.out.printf("Remaining Balance: PHP %,.2f%n", senderNewBalance);

        if (!confirmTransaction("Transfer")) {
            return;
        }

        // Recheck both accounts before processing.
        senderRecord = findCustomer(senderID);
        recipientRecord = findCustomer(recipientID);

        if (senderRecord == null || recipientRecord == null) {
            showMessage(
                    "TRANSFER FAILED",
                    "Sender or recipient account could not be verified."
            );
            return;
        }

        senderFields = senderRecord.split("\\|", -1);
        recipientFields = recipientRecord.split("\\|", -1);

        try {
            senderBalance = new BigDecimal(senderFields[4]);
            recipientBalance = new BigDecimal(recipientFields[4]);

            if (amount.compareTo(senderBalance) > 0) {
                showMessage(
                        "TRANSFER FAILED",
                        "Insufficient balance. Your balance has not changed."
                );
                return;
            }

            senderNewBalance = senderBalance.subtract(amount);
            recipientNewBalance = recipientBalance.add(amount);

            String updatedSender = updateCustomerRecord(
                    senderFields,
                    senderNewBalance,
                    new BigDecimal(senderFields[5]),
                    Integer.parseInt(senderFields[6])
            );

            String updatedRecipient = updateCustomerRecord(
                    recipientFields,
                    recipientNewBalance,
                    new BigDecimal(recipientFields[5]),
                    Integer.parseInt(recipientFields[6])
            );

            String transactionID = generateTransactionID();
            String dateTime = LocalDateTime.now().format(DATE_FORMAT);

            // Sender's transaction record.
            String senderTransaction =
                    transactionID + "|"
                    + senderID + "|"
                    + "TRANSFER_OUT" + "|"
                    + amount.toPlainString() + "|"
                    + senderBalance.toPlainString() + "|"
                    + senderNewBalance.toPlainString() + "|"
                    + dateTime + "|"
                    + "SUCCESS|"
                    + recipientID;

            // Recipient's transaction record.
            String recipientTransaction =
                    transactionID + "|"
                    + recipientID + "|"
                    + "TRANSFER_IN" + "|"
                    + amount.toPlainString() + "|"
                    + recipientBalance.toPlainString() + "|"
                    + recipientNewBalance.toPlainString() + "|"
                    + dateTime + "|"
                    + "SUCCESS|"
                    + senderID;

            // Process both accounts and both transaction records.
            database.processTransfer(
                    senderID,
                    updatedSender,
                    recipientID,
                    updatedRecipient,
                    senderTransaction,
                    recipientTransaction
            );

            saveActivityLog(
                    "TRANSFER_SUCCESS|" + senderID + "|" + transactionID
            );

            System.out.println("\n==================================================");
            System.out.println("             TRANSFER SUCCESSFUL!");
            System.out.println("==================================================");
            System.out.println("Sender Account   : " + senderID);
            System.out.println("Recipient Account: " + recipientID);
            System.out.printf("Transfer Amount  : PHP %,.2f%n", amount);
            System.out.printf("Previous Balance : PHP %,.2f%n", senderBalance);
            System.out.printf("New Balance      : PHP %,.2f%n", senderNewBalance);
            System.out.println("Transaction ID   : " + transactionID);
            System.out.println("Status           : SUCCESSFUL");
            System.out.println("==================================================");

        } catch (NumberFormatException e) {

            showMessage(
                    "TRANSFER FAILED",
                    "Invalid sender or recipient account information."
            );

        } catch (RuntimeException e) {

            showMessage(
                    "TRANSFER FAILED",
                    "The transfer could not be completed.\nReason: "
                    + e.getMessage()
            );
        }

        pause();
    }

    // =========================================================
    // TRANSACTION HISTORY
    // =========================================================

    public void showHistory(String accountID) {

        System.out.println("\n==================================================");
        System.out.println("               TRANSACTION HISTORY");
        System.out.println("==================================================");
        System.out.println("Account ID: " + accountID);
        System.out.println("--------------------------------------------------");

        List<String> transactions = database.readTransactions();

        boolean found = false;

        if (transactions != null) {

            for (String record : transactions) {

                if (record == null || record.trim().isEmpty()) {
                    continue;
                }

                String[] fields = record.split("\\|", -1);

                if (fields.length < 8) {
                    continue;
                }

                if (fields[1].equalsIgnoreCase(accountID)) {

                    found = true;

                    System.out.println("Transaction ID  : " + fields[0]);
                    System.out.println("Type            : " + fields[2]);
                    System.out.println("Amount          : PHP " + fields[3]);
                    System.out.println("Previous Balance: PHP " + fields[4]);
                    System.out.println("New Balance     : PHP " + fields[5]);
                    System.out.println("Date and Time   : " + fields[6]);
                    System.out.println("Status          : " + fields[7]);

                    if (fields.length >= 9) {
                        System.out.println("Other Account   : " + fields[8]);
                    }

                    System.out.println("--------------------------------------------------");
                }
            }
        }

        if (!found) {
            System.out.println("No transactions found.");
        }

        System.out.println("==================================================");

        pause();
    }

    // =========================================================
    // FIND CUSTOMER
    // =========================================================

    private String findCustomer(String accountID) {

        List<String> customers = database.readCustomers();

        if (customers == null) {
            return null;
        }

        for (String record : customers) {

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
    // UPDATE CUSTOMER RECORD
    // =========================================================

    private String updateCustomerRecord(
            String[] fields,
            BigDecimal balance,
            BigDecimal totalDeposits,
            int rewardPoints) {

        return fields[0] + "|"
                + fields[1] + "|"
                + fields[2] + "|"
                + fields[3] + "|"
                + balance.setScale(2, RoundingMode.HALF_UP).toPlainString() + "|"
                + totalDeposits.setScale(2, RoundingMode.HALF_UP).toPlainString() + "|"
                + rewardPoints;
    }

    // =========================================================
    // GENERATE TRANSACTION ID
    // =========================================================

    private String generateTransactionID() {

        int highestNumber = 1000;

        List<String> transactions = database.readTransactions();

        if (transactions != null) {

            for (String record : transactions) {

                if (record == null || record.trim().isEmpty()) {
                    continue;
                }

                String[] fields = record.split("\\|", -1);

                if (fields.length > 0 && fields[0].startsWith("T")) {

                    try {
                        int number = Integer.parseInt(fields[0].substring(1));

                        if (number > highestNumber) {
                            highestNumber = number;
                        }

                    } catch (NumberFormatException e) {
                        // Ignore invalid transaction IDs.
                    }
                }
            }
        }

        return "T" + (highestNumber + 1);
    }

    // =========================================================
    // READ AMOUNT
    // =========================================================

    private BigDecimal readAmount(String message) {

        while (true) {

            System.out.print(message);
            String input = scanner.nextLine().trim();

            try {
                BigDecimal amount = new BigDecimal(input);

                if (amount.scale() > 2) {
                    System.out.println(
                            "Please enter an amount with up to 2 decimal places."
                    );
                    continue;
                }

                return amount.setScale(2, RoundingMode.HALF_UP);

            } catch (NumberFormatException e) {
                System.out.println(
                        "Invalid amount. Please enter a valid number."
                );
            }
        }
    }

    // =========================================================
    // CONFIRM TRANSACTION
    // =========================================================

    private boolean confirmTransaction(String transactionType) {

        System.out.println("\n[1] Confirm " + transactionType);
        System.out.println("[2] Cancel " + transactionType);

        while (true) {

            System.out.print("Enter your choice: ");
            String input = scanner.nextLine().trim();

            if (input.equals("1")) {
                return true;
            }

            if (input.equals("2")) {
                System.out.println("\n" + transactionType + " cancelled.");
                System.out.println("Your balance has not changed.");
                pause();
                return false;
            }

            System.out.println("Invalid choice. Please enter 1 or 2.");
        }
    }

    // =========================================================
    // DECODE CUSTOMER NAME
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
    // SAVE ACTIVITY LOG
    // =========================================================

    private void saveActivityLog(String log) {

        try {
            database.saveActivityLog(log);

        } catch (RuntimeException e) {
            System.out.println(
                    "Warning: Transaction succeeded, but the activity log "
                    + "could not be saved."
            );
        }
    }

    // =========================================================
    // MESSAGE AND PAUSE
    // =========================================================

    private void showMessage(String title, String message) {

        System.out.println("\n==================================================");
        System.out.println("               " + title);
        System.out.println("==================================================");
        System.out.println(message);
        System.out.println("==================================================");

        pause();
    }

    private void pause() {

        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }
}
