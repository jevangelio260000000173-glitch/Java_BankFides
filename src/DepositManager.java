
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
    private final Transaction_Protection security;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==========================================
    // CONSTRUCTORS
    // ==========================================

    public DepositManager(Scanner scanner) {
        this(scanner, new FileDatabase());
    }

    public DepositManager(Scanner scanner, FileDatabase database) {
        this.scanner = scanner;
        this.database = database;
        this.security = new Transaction_Protection(database);
    }

    // ==========================================
    // DEPOSIT / CASH IN
    // ==========================================

    public DepositResult deposit(String accountID) {

        // STEP 1: VERIFY ACCOUNT

        if (!security.isAccountActive(accountID)) {
            showFailure(
                    "Customer account does not exist or is inactive."
            );
            return null;
        }

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
        BigDecimal balance;
        BigDecimal totalDeposits;
        int rewardPoints;

        try {
            fullName = decode(fields[1]);

            balance = new BigDecimal(fields[4])
                    .setScale(2, RoundingMode.HALF_UP);

            totalDeposits = new BigDecimal(fields[5])
                    .setScale(2, RoundingMode.HALF_UP);

            rewardPoints = Integer.parseInt(fields[6]);

            if (balance.compareTo(BigDecimal.ZERO) < 0
                    || totalDeposits.compareTo(BigDecimal.ZERO) < 0
                    || rewardPoints < 0) {
                throw new NumberFormatException();
            }

        } catch (NumberFormatException e) {
            showFailure("Unable to read the customer account details.");
            return null;
        }

        // DISPLAY ACCOUNT INFORMATION

        System.out.println("\n==========================================");
        System.out.println("              CASH IN / DEPOSIT");
        System.out.println("==========================================");
        System.out.println("Account Name    : " + fullName);
        System.out.println("Account ID      : " + accountID);

        System.out.printf(
                "Current Balance : PHP %,.2f%n",
                balance
        );

        // STEP 2: ENTER DEPOSIT AMOUNT

        BigDecimal amount;

        while (true) {

            System.out.print("\nEnter deposit amount: PHP ");

            String input = scanner.nextLine().trim();

            try {
                amount = new BigDecimal(input);

                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    System.out.println(
                            "Invalid amount. Deposit must be greater than zero."
                    );
                    continue;
                }

                if (amount.scale() > 2) {
                    System.out.println(
                            "Invalid amount. Please enter up to two decimal places."
                    );
                    continue;
                }

                amount = amount.setScale(2, RoundingMode.UNNECESSARY);

                if (!Double.isFinite(amount.doubleValue())) {
                    System.out.println(
                            "Invalid amount. The deposit exceeds the supported limit."
                    );
                    continue;
                }

                if (!security.validateTransaction(
                        accountID,
                        "DEPOSIT",
                        amount,
                        null)) {

                    showFailure(
                            "The deposit failed the transaction security checks."
                    );
                    return null;
                }

                break;

            } catch (NumberFormatException | ArithmeticException e) {
                System.out.println(
                        "Invalid input. Please enter a valid deposit amount."
                );
            }
        }

        // STEP 3: CHECK EXPECTED BALANCE

        BigDecimal expectedBalance = balance.add(amount);
        BigDecimal expectedTotalDeposits = totalDeposits.add(amount);

        if (!Double.isFinite(expectedBalance.doubleValue())
                || !Double.isFinite(expectedTotalDeposits.doubleValue())) {

            showFailure(
                    "The deposit amount exceeds the supported account limit."
            );
            return null;
        }

        // STEP 4: DEPOSIT REQUEST CONFIRMATION

        System.out.println("\n==========================================");
        System.out.println("             DEPOSIT REQUEST");
        System.out.println("==========================================");
        System.out.println("Account Name    : " + fullName);
        System.out.println("Account ID      : " + accountID);

        System.out.printf(
                "Current Balance : PHP %,.2f%n",
                balance
        );

        System.out.printf(
                "Deposit Amount  : PHP %,.2f%n",
                amount
        );

        System.out.printf(
                "Balance After Approval: PHP %,.2f%n",
                expectedBalance
        );

        System.out.println("------------------------------------------");
        System.out.println("Important:");
        System.out.println("Your balance will NOT change immediately.");
        System.out.println("The deposit must first be reviewed by staff.");
        System.out.println("==========================================");
        System.out.println("[1] Submit Deposit Request");
        System.out.println("[2] Cancel Request");
        System.out.println("==========================================");

        int choice = readChoice(1, 2);

        if (choice == 2) {
            System.out.println("\nDeposit request cancelled.");
            System.out.println("No changes were made to your account.");
            pause();
            return null;
        }

        // STEP 5: VERIFY ACCOUNT AGAIN

        if (!security.validateTransaction(
                accountID,
                "DEPOSIT",
                amount,
                null)) {

            showFailure(
                    "The account could not pass the final security verification."
            );
            return null;
        }

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

            balance = new BigDecimal(fields[4])
                    .setScale(2, RoundingMode.HALF_UP);

            totalDeposits = new BigDecimal(fields[5])
                    .setScale(2, RoundingMode.HALF_UP);

            rewardPoints = Integer.parseInt(fields[6]);

            if (balance.compareTo(BigDecimal.ZERO) < 0
                    || totalDeposits.compareTo(BigDecimal.ZERO) < 0
                    || rewardPoints < 0) {
                throw new NumberFormatException();
            }

        } catch (NumberFormatException e) {
            showFailure(
                    "Unable to verify the current account information."
            );
            return null;
        }

        // Recheck supported account limits.

        expectedBalance = balance.add(amount);
        expectedTotalDeposits = totalDeposits.add(amount);

        if (!Double.isFinite(expectedBalance.doubleValue())
                || !Double.isFinite(expectedTotalDeposits.doubleValue())) {

            showFailure(
                    "The deposit amount exceeds the supported account limit."
            );
            return null;
        }

        // STEP 6: GENERATE PENDING DEPOSIT ID

        String pendingID = generatePendingDepositID();

        if (pendingID == null) {
            showFailure(
                    "Unable to generate a deposit request ID."
            );
            return null;
        }

        // STEP 7: PREPARE PENDING DEPOSIT RECORD

        String dateTime = LocalDateTime.now().format(DATE_FORMAT);

        /*
         * pending_deposits.txt format:
         *
         * pendingID|accountID|amount|submittedAt|
         * status|reviewedBy|reviewedAt
         *
         * Example:
         * P1001|C1001|1000.00|2026-11-04 18:30:00|PENDING||
         *
         * The last two fields remain empty until
         * a staff member reviews the request.
         */

        String pendingDepositRecord =
                pendingID + "|"
                + accountID + "|"
                + amount.toPlainString() + "|"
                + dateTime + "|"
                + "PENDING||";

        // STEP 8: SAVE PENDING DEPOSIT REQUEST

        try {

            boolean requestSaved =
                    database.savePendingDeposit(pendingDepositRecord);

            if (!requestSaved) {
                showFailure(
                        "Unable to save your deposit request. "
                        + "Please try again."
                );
                return null;
            }

            // Save activity log after successful submission.

            boolean activitySaved = database.saveActivityLog(
                    dateTime
                    + "|" + accountID
                    + "|DEPOSIT_REQUEST_SUBMITTED"
                    + "|" + pendingID
            );

            if (!activitySaved) {
                System.out.println(
                        "\nWarning: Activity log could not be saved."
                );
            }

            // STEP 9: DISPLAY SUBMISSION CONFIRMATION

            System.out.println("\n==========================================");
            System.out.println("       DEPOSIT REQUEST SUBMITTED!");
            System.out.println("==========================================");
            System.out.println("Account Name    : " + fullName);
            System.out.println("Account ID      : " + accountID);

            System.out.printf(
                    "Current Balance : PHP %,.2f%n",
                    balance
            );

            System.out.printf(
                    "Requested Amount: PHP %,.2f%n",
                    amount
            );

            System.out.println("------------------------------------------");
            System.out.println("Request ID      : " + pendingID);
            System.out.println("Request Type    : DEPOSIT");
            System.out.println("Status          : PENDING");
            System.out.println("Submitted At    : " + dateTime);
            System.out.println("------------------------------------------");

            System.out.println("Your deposit request has been recorded.");
            System.out.println("Please wait for staff approval.");
            System.out.println();
            System.out.println("Your account balance has NOT been updated.");
            System.out.println("Reward points will only be credited");
            System.out.println("after the deposit is approved.");
            System.out.println("==========================================");

            pause();

            // Return unchanged account values.
            return new DepositResult(
                    balance.doubleValue(),
                    totalDeposits.doubleValue(),
                    rewardPoints
            );

        } catch (RuntimeException e) {

            showFailure(
                    "Unable to submit the deposit request. "
                    + "Please check your database files."
            );

            System.out.println("Reason: " + e.getMessage());

            return null;
        }
    }

    // ==========================================
    // FIND CUSTOMER RECORD
    // ==========================================

    private String findCustomerRecord(String accountID) {

        if (accountID == null || accountID.trim().isEmpty()) {
            return null;
        }

        List<String> records = database.readCustomers();

        for (String record : records) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length == 7
                    && fields[0].equalsIgnoreCase(accountID.trim())) {
                return record;
            }
        }

        return null;
    }

    // ==========================================
    // GENERATE UNIQUE PENDING DEPOSIT ID
    // ==========================================

    private String generatePendingDepositID() {

        int highestNumber = 1000;

        for (String record : database.readPendingDeposits()) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length > 0 && fields[0].startsWith("P")) {

                try {
                    int number = Integer.parseInt(
                            fields[0].substring(1)
                    );

                    if (number > highestNumber) {
                        highestNumber = number;
                    }

                } catch (NumberFormatException e) {
                    // Skip invalid pending deposit IDs.
                }
            }
        }

        if (highestNumber == Integer.MAX_VALUE) {
            return null;
        }

        return "P" + (highestNumber + 1);
    }

    // ==========================================
    // BASE64 DECODING
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
    // INPUT VALIDATION
    // ==========================================

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

    // ==========================================
    // FAILURE MESSAGE
    // ==========================================

    private void showFailure(String message) {

        System.out.println("\n==========================================");
        System.out.println("               DEPOSIT FAILED");
        System.out.println("==========================================");
        System.out.println("Error: " + message);
        System.out.println("Your balance has not been changed.");
        System.out.println("==========================================");

        pause();
    }

    // ==========================================
    // PAUSE
    // ==========================================

    private void pause() {

        System.out.println(
                "\nPress Enter to return to the dashboard."
        );

        scanner.nextLine();
    }

    // ==========================================
    // DEPOSIT RESULT
    // ==========================================

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
