
package src;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;

public class AccountManager {

    private final Scanner scanner;
    private final FileDatabase database;

    public AccountManager(Scanner scanner) {
        this.scanner = scanner;
        this.database = new FileDatabase();
    }

    // =========================================================
    // ADMIN: MANAGE CUSTOMER ACCOUNTS
    // =========================================================

    public void manageCustomerAccounts() {

        while (true) {

            System.out.println("\n==================================================");
            System.out.println("          MANAGE CUSTOMER ACCOUNTS");
            System.out.println("==================================================");
            System.out.println("[1] Search Customer Account");
            System.out.println("[2] View All Customer Accounts");
            System.out.println("[3] Update Customer Information");
            System.out.println("[4] Activate / Deactivate Account");
            System.out.println("[5] Return to Admin Dashboard");
            System.out.println("==================================================");

            int choice = readChoice("Enter your choice: ", 1, 5);

            switch (choice) {

                case 1:
                    searchCustomerAccount();
                    break;

                case 2:
                    viewAllCustomerAccounts();
                    break;

                case 3:
                    updateCustomerInformation();
                    break;

                case 4:
                    changeCustomerAccountStatus();
                    break;

                case 5:
                    return;
            }
        }
    }

    // =========================================================
    // SEARCH CUSTOMER ACCOUNT
    // =========================================================

    public void searchCustomerAccount() {

        System.out.println("\n==================================================");
        System.out.println("             SEARCH CUSTOMER ACCOUNT");
        System.out.println("==================================================");

        String search = readRequiredInput(
                "Enter Customer ID or Username: "
        );

        if (search == null) {
            return;
        }

        String record = findCustomerRecord(search);

        if (record == null) {

            System.out.println("\nCustomer account not found.");
            pause();
            return;
        }

        displayCustomerInformation(record);
        pause();
    }

    // =========================================================
    // VIEW ALL CUSTOMER ACCOUNTS
    // =========================================================

    public void viewAllCustomerAccounts() {

        System.out.println("\n==================================================");
        System.out.println("              ALL CUSTOMER ACCOUNTS");
        System.out.println("==================================================");

        List<String> records = database.readCustomers();

        if (records == null || records.isEmpty()) {

            System.out.println("No customer accounts found.");
            pause();
            return;
        }

        int count = 0;

        for (String record : records) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length != 7) {
                continue;
            }

            try {

                String fullName = decode(fields[1]);
                String username = decode(fields[2]);
                String status = database.getAccountStatus(fields[0]);

                System.out.println("------------------------------------------");
                System.out.println("Customer ID : " + fields[0]);
                System.out.println("Full Name   : " + fullName);
                System.out.println("Username    : " + username);
                System.out.println("Balance     : PHP " + fields[4]);
                System.out.println("Status      : " + status);

                count++;

            } catch (IllegalArgumentException e) {

                System.out.println(
                        "Warning: An unreadable customer record was skipped."
                );
            }
        }

        System.out.println("------------------------------------------");
        System.out.println("Total Customers: " + count);
        System.out.println("==================================================");

        pause();
    }

    // =========================================================
    // UPDATE CUSTOMER INFORMATION
    // =========================================================

    public void updateCustomerInformation() {

        System.out.println("\n==================================================");
        System.out.println("           UPDATE CUSTOMER INFORMATION");
        System.out.println("==================================================");

        String accountID = readRequiredInput(
                "Enter Customer ID: "
        );

        if (accountID == null) {
            return;
        }

        String record = findCustomerRecord(accountID);

        if (record == null) {

            System.out.println("\nCustomer account not found.");
            pause();
            return;
        }

        String[] fields = record.split("\\|", -1);

        try {

            String oldName = decode(fields[1]);
            String oldUsername = decode(fields[2]);

            System.out.println("\nCurrent Customer Information");
            System.out.println("------------------------------------------");
            System.out.println("Customer ID : " + fields[0]);
            System.out.println("Full Name   : " + oldName);
            System.out.println("Username    : " + oldUsername);
            System.out.println("------------------------------------------");

            System.out.println(
                    "Leave a field blank to keep its current value."
            );

            String newName = readOptionalInput(
                    "\nEnter new full name: "
            );

            if (newName == null) {
                return;
            }

            String newUsername = readOptionalInput(
                    "Enter new username: "
            );

            if (newUsername == null) {
                return;
            }

            if (newName.isEmpty()) {
                newName = oldName;
            }

            if (newUsername.isEmpty()) {
                newUsername = oldUsername;
            }

            if (!isValidField(newName)
                    || !isValidField(newUsername)) {

                System.out.println("\nInvalid information entered.");
                pause();
                return;
            }

            // Check if the new username belongs to another account.
            if (!newUsername.equalsIgnoreCase(oldUsername)
                    && usernameExists(newUsername, accountID)) {

                System.out.println(
                        "\nUsername already exists. Update cancelled."
                );

                pause();
                return;
            }

            System.out.println("\n==================================================");
            System.out.println("             CONFIRM INFORMATION");
            System.out.println("==================================================");
            System.out.println("Customer ID : " + accountID);
            System.out.println("Old Name    : " + oldName);
            System.out.println("New Name    : " + newName);
            System.out.println("Old Username: " + oldUsername);
            System.out.println("New Username: " + newUsername);
            System.out.println("==================================================");
            System.out.println("[1] Confirm Update");
            System.out.println("[2] Cancel");
            System.out.println("==================================================");

            int choice = readChoice("Enter your choice: ", 1, 2);

            if (choice == 2) {

                System.out.println("\nUpdate cancelled.");
                pause();
                return;
            }

            // Preserve the existing balance, deposits and reward points.
            String updatedRecord =
                    fields[0] + "|"
                    + encode(newName) + "|"
                    + encode(newUsername) + "|"
                    + fields[3] + "|"
                    + fields[4] + "|"
                    + fields[5] + "|"
                    + fields[6];

            if (database.updateCustomerAccount(
                    accountID,
                    updatedRecord
            )) {

                if (!oldName.equals(newName)) {
                    logActivity(
                            "ACCOUNT_NAME_UPDATED|" + accountID
                    );
                }

                if (!oldUsername.equalsIgnoreCase(newUsername)) {
                    logActivity(
                            "ACCOUNT_USERNAME_UPDATED|" + accountID
                    );
                }

                System.out.println(
                        "\nCustomer information updated successfully!"
                );

            } else {

                System.out.println(
                        "\nUnable to update customer information."
                );
            }

        } catch (IllegalArgumentException e) {

            System.out.println(
                    "\nUnable to process the customer account record."
            );
        }

        pause();
    }

    // =========================================================
    // ACTIVATE / DEACTIVATE CUSTOMER ACCOUNT
    // =========================================================

    public void changeCustomerAccountStatus() {

        System.out.println("\n==================================================");
        System.out.println("          CHANGE CUSTOMER ACCOUNT STATUS");
        System.out.println("==================================================");

        String accountID = readRequiredInput(
                "Enter Customer ID: "
        );

        if (accountID == null) {
            return;
        }

        String record = findCustomerRecord(accountID);

        if (record == null) {

            System.out.println("\nCustomer account not found.");
            pause();
            return;
        }

        String currentStatus = database.getAccountStatus(accountID);

        System.out.println("\nCustomer ID : " + accountID);
        System.out.println("Full Name   : " + decodeCustomerName(record));
        System.out.println("Current Status: " + currentStatus);

        if ("ACTIVE".equals(currentStatus)) {

            System.out.println("\n[1] Deactivate Account");
            System.out.println("[2] Cancel");

        } else {

            System.out.println("\n[1] Activate Account");
            System.out.println("[2] Cancel");
        }

        int choice = readChoice("Enter your choice: ", 1, 2);

        if (choice == 2) {

            System.out.println("\nOperation cancelled.");
            pause();
            return;
        }

        String newStatus = "ACTIVE".equals(currentStatus)
                ? "INACTIVE"
                : "ACTIVE";

        System.out.println("\nAre you sure you want to change");
        System.out.println("the account status to " + newStatus + "?");
        System.out.println("[1] Confirm");
        System.out.println("[2] Cancel");

        int confirmation = readChoice(
                "Enter your choice: ", 1, 2
        );

        if (confirmation == 2) {

            System.out.println("\nOperation cancelled.");
            pause();
            return;
        }

        if (database.updateAccountStatus(accountID, newStatus)) {

            logActivity(
                    "ACCOUNT_" + newStatus + "|" + accountID
            );

            System.out.println(
                    "\nAccount status successfully changed to "
                    + newStatus + "!"
            );

        } else {

            System.out.println(
                    "\nUnable to update the account status."
            );
        }

        pause();
    }

    // =========================================================
    // STAFF: SEARCH AND VIEW CUSTOMER ACCOUNTS ONLY
    // =========================================================

    public void staffCustomerAccountSearch() {

        while (true) {

            System.out.println("\n==================================================");
            System.out.println("          CUSTOMER ACCOUNT SEARCH");
            System.out.println("==================================================");
            System.out.println("[1] Search Customer Account");
            System.out.println("[2] View All Customer Accounts");
            System.out.println("[3] Return");
            System.out.println("==================================================");

            int choice = readChoice("Enter your choice: ", 1, 3);

            switch (choice) {

                case 1:
                    searchCustomerAccount();
                    break;

                case 2:
                    viewAllCustomerAccounts();
                    break;

                case 3:
                    return;
            }
        }
    }

    // =========================================================
    // FIND CUSTOMER RECORD
    // =========================================================

    private String findCustomerRecord(String search) {

        if (search == null || search.trim().isEmpty()) {
            return null;
        }

        for (String record : database.readCustomers()) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length != 7) {
                continue;
            }

            try {

                String accountID = fields[0];
                String username = decode(fields[2]);

                if (accountID.equalsIgnoreCase(search.trim())
                        || username.equalsIgnoreCase(search.trim())) {

                    return record;
                }

            } catch (IllegalArgumentException e) {
                // Skip unreadable records.
            }
        }

        return null;
    }

    // =========================================================
    // DISPLAY CUSTOMER INFORMATION
    // =========================================================

    private void displayCustomerInformation(String record) {

        String[] fields = record.split("\\|", -1);

        try {

            System.out.println("\n==================================================");
            System.out.println("            CUSTOMER ACCOUNT DETAILS");
            System.out.println("==================================================");
            System.out.println("Customer ID   : " + fields[0]);
            System.out.println("Full Name     : " + decode(fields[1]));
            System.out.println("Username      : " + decode(fields[2]));
            System.out.println("Balance       : PHP " + fields[4]);
            System.out.println("Total Deposits: PHP " + fields[5]);
            System.out.println("Reward Points : " + fields[6]);
            System.out.println("Account Status: "
                    + database.getAccountStatus(fields[0]));
            System.out.println("==================================================");

        } catch (IllegalArgumentException e) {

            System.out.println("Unable to display customer information.");
        }
    }

    // =========================================================
    // CHECK DUPLICATE USERNAME
    // =========================================================

    private boolean usernameExists(
            String username,
            String excludedAccountID
    ) {

        // Check customer usernames.
        for (String record : database.readCustomers()) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length != 7) {
                continue;
            }

            try {

                if (fields[0].equalsIgnoreCase(excludedAccountID)) {
                    continue;
                }

                if (decode(fields[2]).equalsIgnoreCase(username)) {
                    return true;
                }

            } catch (IllegalArgumentException e) {
                // Skip unreadable records.
            }
        }

        // Check staff usernames.
        for (String record : database.readStaff()) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length != 4) {
                continue;
            }

            try {

                if (decode(fields[2]).equalsIgnoreCase(username)) {
                    return true;
                }

            } catch (IllegalArgumentException e) {
                // Skip unreadable records.
            }
        }

        // Check admin usernames.
        for (String record : database.readAdmins()) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length != 4) {
                continue;
            }

            try {

                if (decode(fields[2]).equalsIgnoreCase(username)) {
                    return true;
                }

            } catch (IllegalArgumentException e) {
                // Skip unreadable records.
            }
        }

        return false;
    }

    // =========================================================
    // INPUT METHODS
    // =========================================================

    private String readRequiredInput(String prompt) {

        while (true) {

            System.out.print(prompt);

            if (!scanner.hasNextLine()) {
                return null;
            }

            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {

                System.out.println("Input cannot be empty.");
                continue;
            }

            if (!isValidField(input)) {

                System.out.println("Invalid input. Please try again.");
                continue;
            }

            return input;
        }
    }

    private String readOptionalInput(String prompt) {

        System.out.print(prompt);

        if (!scanner.hasNextLine()) {
            return null;
        }

        String input = scanner.nextLine().trim();

        if (!input.isEmpty() && !isValidField(input)) {

            System.out.println("Invalid input.");
            return null;
        }

        return input;
    }

    private boolean isValidField(String value) {

        return value != null
                && !value.contains("|")
                && !value.contains("\n")
                && !value.contains("\r");
    }

    private int readChoice(String prompt, int min, int max) {

        while (true) {

            System.out.print(prompt);

            if (!scanner.hasNextLine()) {
                return max;
            }

            String input = scanner.nextLine().trim();

            try {

                int choice = Integer.parseInt(input);

                if (choice >= min && choice <= max) {
                    return choice;
                }

                System.out.println(
                        "Enter a number from " + min + " to " + max + "."
                );

            } catch (NumberFormatException e) {

                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }

    // =========================================================
    // BASE64 METHODS
    // =========================================================

    private String encode(String value) {

        return Base64.getEncoder().encodeToString(
                value.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String decode(String value) {

        byte[] decoded = Base64.getDecoder().decode(value);

        return new String(decoded, StandardCharsets.UTF_8);
    }

    private String decodeCustomerName(String record) {

        String[] fields = record.split("\\|", -1);

        try {
            return decode(fields[1]);
        } catch (IllegalArgumentException e) {
            return "Unknown";
        }
    }

    // =========================================================
    // ACTIVITY LOGGING
    // =========================================================

    private void logActivity(String activity) {

        if (!database.saveActivityLog(activity)) {

            System.out.println(
                    "Warning: Activity could not be recorded."
            );
        }
    }

    // =========================================================
    // PAUSE
    // =========================================================

    private void pause() {

        System.out.println("\nPress Enter to continue...");

        if (scanner.hasNextLine()) {
            scanner.nextLine();
        }
    }
}
