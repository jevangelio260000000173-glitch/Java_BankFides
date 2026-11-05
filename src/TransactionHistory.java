
package src;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

public class TransactionHistory {

    private final Scanner scanner;
    private final FileDatabase database;
    private final Transaction_Protection security;

    private static final int SCREEN_GAP = 2;

    private static final String MAIN_LINE =
            "============================================================";

    private static final String SECTION_LINE =
            "------------------------------------------------------------";

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==========================================
    // CONSTRUCTORS
    // ==========================================

    public TransactionHistory(Scanner scanner) {
        this(scanner, new FileDatabase());
    }

    public TransactionHistory(Scanner scanner, FileDatabase database) {
        this.scanner = scanner;
        this.database = database;
        this.security = new Transaction_Protection(database);
    }

    // ==========================================
    // SCREEN FORMATTING
    // ==========================================

    private void screenGap() {
        for (int i = 0; i < SCREEN_GAP; i++) {
            System.out.println();
        }
    }

    private void displayScreenHeader(String title) {

        screenGap();

        System.out.println(MAIN_LINE);
        System.out.println();
        System.out.println("                    BANCOFIDES");
        System.out.println("                  SAVINGS MANAGEMENT SYSTEM");
        System.out.println();
        System.out.println("                    " + title);
        System.out.println();
        System.out.println(MAIN_LINE);
        System.out.println();
    }

    private void displaySectionHeader(String title) {

        System.out.println();
        System.out.println(SECTION_LINE);
        System.out.println("                    " + title);
        System.out.println(SECTION_LINE);
        System.out.println();
    }

    // ==========================================
    // WITHDRAWAL
    // ==========================================

    public void withdraw(String accountID) {

        if (!security.hasPermission("CUSTOMER", "WITHDRAWAL")
                || !security.isAccountActive(accountID)) {
            showMessage(
                    "TRANSACTION FAILED",
                    "Your account is not authorized or active."
            );
            return;
        }

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

        displayScreenHeader("CASH OUT / WITHDRAWAL");

        System.out.println("Account ID       : " + accountID);
        System.out.printf("Available Balance: PHP %,.2f%n", balance);

        BigDecimal amount = readAmount("Enter withdrawal amount: PHP ");

        if (!validateTransaction(accountID, "WITHDRAWAL", amount, null)) {
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

        displaySectionHeader("WITHDRAWAL DETAILS");

        System.out.printf("Withdrawal Amount: PHP %,.2f%n", amount);
        System.out.printf("Remaining Balance: PHP %,.2f%n", newBalance);

        if (!confirmTransaction("Withdrawal")) {
            return;
        }

        if (!validateTransaction(accountID, "WITHDRAWAL", amount, null)) {
            return;
        }

        customerRecord = findCustomer(accountID);

        if (customerRecord == null) {
            showMessage("TRANSACTION FAILED", "Customer account not found.");
            return;
        }

        fields = customerRecord.split("\\|", -1);

        try {
            if (fields.length != 7) {
                throw new NumberFormatException();
            }

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

            boolean processed = database.processWithdrawal(
                    accountID,
                    updatedRecord,
                    transactionRecord
            );

            if (!processed) {
                showMessage(
                        "TRANSACTION FAILED",
                        "The withdrawal could not be saved. "
                        + "Your transaction was not confirmed."
                );
                return;
            }

            saveActivityLog(
                    "WITHDRAWAL_SUCCESS|" + accountID + "|" + transactionID
            );

            displayScreenHeader("WITHDRAWAL SUCCESSFUL!");

            System.out.printf("Previous Balance : PHP %,.2f%n", balance);
            System.out.printf("Withdrawal Amount: PHP %,.2f%n", amount);
            System.out.printf("New Balance      : PHP %,.2f%n", newBalance);

            System.out.println("Transaction ID   : " + transactionID);
            System.out.println("Status           : SUCCESSFUL");

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

    // ==========================================
    // TRANSFER
    // ==========================================

    public void transfer(String senderID) {

        if (!security.hasPermission("CUSTOMER", "TRANSFER")
                || !security.isAccountActive(senderID)) {
            showMessage(
                    "TRANSFER FAILED",
                    "Your account is not authorized or active."
            );
            return;
        }

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

        displayScreenHeader("TRANSFER FUNDS");

        System.out.println("Sender Account   : " + senderID);
        System.out.printf("Available Balance: PHP %,.2f%n", senderBalance);

        System.out.print("\nEnter recipient account ID: ");
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

        if (!security.isAccountActive(recipientID)) {
            showMessage(
                    "TRANSFER FAILED",
                    "The recipient account is inactive."
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

        System.out.println("\nRecipient Name   : " + recipientName);
        System.out.println("Recipient Account: " + recipientID);

        BigDecimal amount = readAmount("Enter transfer amount: PHP ");

        if (!validateTransaction(senderID, "TRANSFER", amount, recipientID)) {
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

        displaySectionHeader("TRANSFER DETAILS");

        System.out.println("Sender Account   : " + senderID);
        System.out.println("Recipient Name   : " + recipientName);
        System.out.println("Recipient Account: " + recipientID);
        System.out.printf("Transfer Amount  : PHP %,.2f%n", amount);
        System.out.printf("Remaining Balance: PHP %,.2f%n", senderNewBalance);

        if (!confirmTransaction("Transfer")) {
            return;
        }

        if (!validateTransaction(senderID, "TRANSFER", amount, recipientID)) {
            return;
        }

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
            if (senderFields.length != 7 || recipientFields.length != 7) {
                throw new NumberFormatException();
            }

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

            boolean processed = database.processTransfer(
                    senderID,
                    updatedSender,
                    recipientID,
                    updatedRecipient,
                    senderTransaction,
                    recipientTransaction
            );

            if (!processed) {
                showMessage(
                        "TRANSFER FAILED",
                        "The transfer could not be saved. "
                        + "Your balances were not confirmed as updated."
                );
                return;
            }

            saveActivityLog(
                    "TRANSFER_SUCCESS|" + senderID + "|" + transactionID
            );

            displayScreenHeader("TRANSFER SUCCESSFUL!");

            System.out.println("Sender Account   : " + senderID);
            System.out.println("Recipient Account: " + recipientID);
            System.out.printf("Transfer Amount  : PHP %,.2f%n", amount);
            System.out.printf("Previous Balance : PHP %,.2f%n", senderBalance);
            System.out.printf("New Balance      : PHP %,.2f%n", senderNewBalance);

            System.out.println("Transaction ID   : " + transactionID);
            System.out.println("Status           : SUCCESSFUL");

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

    // ==========================================
    // TRANSACTION HISTORY MENU
    // ==========================================

    public void showHistory(String accountID) {

        while (true) {

            displayScreenHeader("TRANSACTION HISTORY");

            System.out.println("Account ID: " + accountID);

            displaySectionHeader("TRANSACTION OPTIONS");

            System.out.println("[1] View All Transactions");
            System.out.println("[2] Filter by Transaction Type");
            System.out.println("[3] Filter by Date");
            System.out.println("[4] View Deposit Request History");
            System.out.println("[5] Return");
            System.out.println();

            int choice = readChoice(1, 5);

            switch (choice) {

                case 1:
                    displayHistory(accountID, "ALL", null);
                    break;

                case 2:
                    filterHistoryByType(accountID);
                    break;

                case 3:
                    filterHistoryByDate(accountID);
                    break;

                case 4:
                    displayDepositRequestHistory(accountID);
                    break;

                case 5:
                    return;

                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    // ==========================================
    // FILTER BY TRANSACTION TYPE
    // ==========================================

    private void filterHistoryByType(String accountID) {

        displayScreenHeader("FILTER BY TRANSACTION TYPE");

        System.out.println("[1] Deposits");
        System.out.println("[2] Withdrawals");
        System.out.println("[3] Transfers");
        System.out.println("[4] Return");
        System.out.println();

        int choice = readChoice(1, 4);

        String type;

        switch (choice) {

            case 1:
                type = "DEPOSIT";
                break;

            case 2:
                type = "WITHDRAWAL";
                break;

            case 3:
                type = "TRANSFER";
                break;

            case 4:
                return;

            default:
                return;
        }

        displayHistory(accountID, type, null);
    }

    // ==========================================
    // FILTER BY DATE
    // ==========================================

    private void filterHistoryByDate(String accountID) {

        displayScreenHeader("FILTER BY DATE");

        LocalDate selectedDate;

        while (true) {

            System.out.print("Enter date (YYYY-MM-DD): ");
            String input = scanner.nextLine().trim();

            try {
                selectedDate = LocalDate.parse(input);
                break;

            } catch (DateTimeParseException e) {
                System.out.println(
                        "Invalid date. Please use YYYY-MM-DD."
                );
            }
        }

        displayHistory(
                accountID,
                "ALL",
                selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        );
    }

    // ==========================================
    // DISPLAY FILTERED TRANSACTION HISTORY
    // ==========================================

    private void displayHistory(
            String accountID,
            String selectedType,
            String selectedDate) {

        displayScreenHeader("TRANSACTION HISTORY");

        System.out.println("Account ID: " + accountID);

        if (!selectedType.equals("ALL")) {
            System.out.println("Transaction Type: " + selectedType);
        }

        if (selectedDate != null) {
            System.out.println("Date: " + selectedDate);
        }

        displaySectionHeader("TRANSACTION RECORDS");

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

                if (!fields[1].equalsIgnoreCase(accountID)) {
                    continue;
                }

                String transactionType = fields[2];

                if (!matchesType(transactionType, selectedType)) {
                    continue;
                }

                if (selectedDate != null
                        && !fields[6].startsWith(selectedDate)) {
                    continue;
                }

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

                System.out.println(SECTION_LINE);
            }
        }

        if (!found) {
            System.out.println("No matching transactions found.");
        }

        pause();
    }

    // ==========================================
    // DISPLAY DEPOSIT REQUEST HISTORY
    // ==========================================

    private void displayDepositRequestHistory(String accountID) {

        displayScreenHeader("DEPOSIT REQUEST HISTORY");

        System.out.println("Account ID: " + accountID);

        displaySectionHeader("DEPOSIT REQUEST RECORDS");

        List<String> requests = database.readPendingDeposits();

        boolean found = false;

        if (requests != null) {

            for (String record : requests) {

                if (record == null || record.trim().isEmpty()) {
                    continue;
                }

                String[] fields = record.split("\\|", -1);

                // Correct pending deposit record contains 7 fields.
                if (fields.length != 7) {
                    continue;
                }

                // Only show requests belonging to the logged-in customer.
                if (!fields[1].equalsIgnoreCase(accountID)) {
                    continue;
                }

                found = true;

                System.out.println("Request ID      : " + fields[0]);
                System.out.println("Request Type    : DEPOSIT");

                try {
                    BigDecimal amount = new BigDecimal(fields[2]);

                    System.out.printf(
                            "Amount          : PHP %,.2f%n",
                            amount
                    );

                } catch (NumberFormatException e) {
                    System.out.println("Amount          : Invalid record");
                }

                System.out.println("Submitted At    : " + fields[3]);
                System.out.println("Status          : " + fields[4]);

                if (fields[4].equalsIgnoreCase("PENDING")) {

                    System.out.println("Reviewed By     : Not yet reviewed");
                    System.out.println("Reviewed At     : Not yet reviewed");

                } else {

                    System.out.println("Reviewed By     : " + fields[5]);
                    System.out.println("Reviewed At     : " + fields[6]);
                }

                System.out.println(SECTION_LINE);
            }
        }

        if (!found) {
            System.out.println("You have no deposit requests yet.");
        }

        pause();
    }

    // ==========================================
    // MATCH TRANSACTION TYPE
    // ==========================================

    private boolean matchesType(String transactionType, String selectedType) {

        if (selectedType.equals("ALL")) {
            return true;
        }

        if (selectedType.equals("TRANSFER")) {
            return transactionType.equalsIgnoreCase("TRANSFER_IN")
                    || transactionType.equalsIgnoreCase("TRANSFER_OUT")
                    || transactionType.equalsIgnoreCase("TRANSFER");
        }

        return transactionType.equalsIgnoreCase(selectedType);
    }

    // ==========================================
    // TRANSACTION SECURITY VALIDATION
    // ==========================================

    private boolean validateTransaction(
            String accountID,
            String transactionType,
            BigDecimal amount,
            String recipientID) {

        if (!security.isAccountActive(accountID)) {
            showMessage(
                    "TRANSACTION FAILED",
                    "Your account is inactive or could not be verified."
            );
            return false;
        }

        if (recipientID != null
                && !security.isAccountActive(recipientID)) {
            showMessage(
                    "TRANSACTION FAILED",
                    "The recipient account is inactive or could not be verified."
            );
            return false;
        }

        boolean valid = security.validateTransaction(
                accountID,
                transactionType,
                amount,
                recipientID
        );

        if (!valid) {
            showMessage(
                    "TRANSACTION FAILED",
                    "Transaction validation failed. Please check your account, "
                    + "balance, and transaction details."
            );
            return false;
        }

        return true;
    }

    // ==========================================
    // FIND CUSTOMER
    // ==========================================

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

    // ==========================================
    // UPDATE CUSTOMER RECORD
    // ==========================================

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

    // ==========================================
    // GENERATE TRANSACTION ID
    // ==========================================

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

        if (highestNumber == Integer.MAX_VALUE) {
            throw new IllegalStateException(
                    "Transaction ID limit has been reached."
            );
        }

        return "T" + (highestNumber + 1);
    }

    // ==========================================
    // READ AMOUNT
    // ==========================================

    private BigDecimal readAmount(String message) {

        while (true) {

            System.out.print("\n" + message);
            String input = scanner.nextLine().trim();

            try {
                BigDecimal amount = new BigDecimal(input);

                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    System.out.println(
                            "Amount must be greater than zero."
                    );
                    continue;
                }

                if (amount.scale() > 2) {
                    System.out.println(
                            "Please enter an amount with up to 2 decimal places."
                    );
                    continue;
                }

                return amount.setScale(2, RoundingMode.UNNECESSARY);

            } catch (NumberFormatException | ArithmeticException e) {
                System.out.println(
                        "Invalid amount. Please enter a valid positive number."
                );
            }
        }
    }

    // ==========================================
    // READ MENU CHOICE
    // ==========================================

    private int readChoice(int min, int max) {

        while (true) {

            System.out.print("Enter your choice: ");

            if (!scanner.hasNextLine()) {
                return max;
            }

            String input = scanner.nextLine().trim();

            try {
                int choice = Integer.parseInt(input);

                if (choice >= min && choice <= max) {
                    return choice;
                }

            } catch (NumberFormatException e) {
                // Display validation message below.
            }

            System.out.println(
                    "Invalid choice. Please enter a number from "
                    + min + " to " + max + "."
            );
        }
    }

    // ==========================================
    // CONFIRM TRANSACTION
    // ==========================================

    private boolean confirmTransaction(String transactionType) {

        System.out.println("\n[1] Confirm " + transactionType);
        System.out.println("[2] Cancel " + transactionType);

        while (true) {

            System.out.print("Enter your choice: ");

            if (!scanner.hasNextLine()) {
                return false;
            }

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

    // ==========================================
    // DECODE CUSTOMER NAME
    // ==========================================

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

    // ==========================================
    // SAVE ACTIVITY LOG
    // ==========================================

    private void saveActivityLog(String log) {

        try {
            boolean saved = database.saveActivityLog(log);

            if (!saved) {
                System.out.println(
                        "Warning: Transaction succeeded, but the activity log "
                        + "could not be saved."
                );
            }

        } catch (RuntimeException e) {
            System.out.println(
                    "Warning: Transaction succeeded, but the activity log "
                    + "could not be saved."
            );
        }
    }

    // ==========================================
    // MESSAGE AND PAUSE
    // ==========================================

    private void showMessage(String title, String message) {

        displayScreenHeader(title);

        System.out.println(message);

        pause();
    }

    private void pause() {

        System.out.print("\nPress Enter to continue...");

        if (scanner.hasNextLine()) {
            scanner.nextLine();
        }
    }
}
