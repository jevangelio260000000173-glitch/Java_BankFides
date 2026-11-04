
package src;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;

public class AccountManager {

    private final Scanner scanner;
    private final FileDatabase database;

    public AccountManager(Scanner scanner) {
        this(scanner, new FileDatabase());
    }

    public AccountManager(Scanner scanner, FileDatabase database) {
        this.scanner = scanner;
        this.database = database;
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

        String accountID = readRequiredInput("Enter Customer ID: ");

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

            // Preserve the existing password, balance,
            // deposits, and reward points.
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
                    logActivity("ACCOUNT_NAME_UPDATED|" + accountID);
                }

                if (!oldUsername.equalsIgnoreCase(newUsername)) {
                    logActivity("ACCOUNT_USERNAME_UPDATED|" + accountID);
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

        String accountID = readRequiredInput("Enter Customer ID: ");

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

        int confirmation = readChoice("Enter your choice: ", 1, 2);

        if (confirmation == 2) {
            System.out.println("\nOperation cancelled.");
            pause();
            return;
        }

        if (database.updateAccountStatus(accountID, newStatus)) {

            logActivity("ACCOUNT_" + newStatus + "|" + accountID);

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
    // ADMIN: MANAGE ADMIN ACCOUNTS
    // =========================================================

    public void manageAdminAccounts() {

        while (true) {

            System.out.println("\n==================================================");
            System.out.println("             MANAGE ADMIN ACCOUNTS");
            System.out.println("==================================================");
            System.out.println("[1] Create Admin Account");
            System.out.println("[2] Search Admin Account");
            System.out.println("[3] View All Admin Accounts");
            System.out.println("[4] Update Admin Information");
            System.out.println("[5] Activate / Deactivate Admin Account");
            System.out.println("[6] Return to Admin Dashboard");
            System.out.println("==================================================");

            int choice = readChoice("Enter your choice: ", 1, 6);

            switch (choice) {
                case 1:
                    createAdminAccount();
                    break;
                case 2:
                    searchAdminAccount();
                    break;
                case 3:
                    viewAllAdminAccounts();
                    break;
                case 4:
                    updateAdminInformation();
                    break;
                case 5:
                    changeAdminAccountStatus();
                    break;
                case 6:
                    return;
            }
        }
    }

    // =========================================================
    // CREATE ADMIN ACCOUNT
    // =========================================================

    private void createAdminAccount() {

        System.out.println("\n==================================================");
        System.out.println("               CREATE ADMIN ACCOUNT");
        System.out.println("==================================================");

        /*
         * UserAuthentication handles Admin registration,
         * password hashing, duplicate username checking,
         * and Admin ID generation.
         */
        UserAuthentication authentication =
                new UserAuthentication(scanner, database);

        authentication.createAdminAccount();

        pause();
    }

    // =========================================================
    // SEARCH ADMIN ACCOUNT
    // =========================================================

    public void searchAdminAccount() {

        System.out.println("\n==================================================");
        System.out.println("               SEARCH ADMIN ACCOUNT");
        System.out.println("==================================================");

        String search = readRequiredInput(
                "Enter Admin ID or Username: "
        );

        if (search == null) {
            return;
        }

        String record = findAdminRecord(search);

        if (record == null) {
            System.out.println("\nAdmin account not found.");
            pause();
            return;
        }

        displayAdminInformation(record);
        pause();
    }

    // =========================================================
    // VIEW ALL ADMIN ACCOUNTS
    // =========================================================

    public void viewAllAdminAccounts() {

        System.out.println("\n==================================================");
        System.out.println("                ALL ADMIN ACCOUNTS");
        System.out.println("==================================================");

        List<String> records = database.readAdmins();

        if (records == null || records.isEmpty()) {
            System.out.println("No Admin accounts found.");
            pause();
            return;
        }

        int count = 0;

        for (String record : records) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length != 4) {
                continue;
            }

            try {

                String adminID = decodeIdentifier(fields[0]);
                String fullName = decode(fields[1]);
                String username = decode(fields[2]);
                String status = database.getAccountStatus(adminID);

                System.out.println("------------------------------------------");
                System.out.println("Admin ID  : " + adminID);
                System.out.println("Full Name : " + fullName);
                System.out.println("Username  : " + username);
                System.out.println("Status    : " + status);

                count++;

            } catch (IllegalArgumentException e) {

                System.out.println(
                        "Warning: An unreadable Admin record was skipped."
                );
            }
        }

        System.out.println("------------------------------------------");
        System.out.println("Total Admin Accounts: " + count);
        System.out.println("==================================================");

        pause();
    }

    // =========================================================
    // UPDATE ADMIN INFORMATION
    // =========================================================

    public void updateAdminInformation() {

        System.out.println("\n==================================================");
        System.out.println("             UPDATE ADMIN INFORMATION");
        System.out.println("==================================================");

        String adminID = readRequiredInput("Enter Admin ID: ");

        if (adminID == null) {
            return;
        }

        String record = findAdminRecord(adminID);

        if (record == null) {
            System.out.println("\nAdmin account not found.");
            pause();
            return;
        }

        String[] fields = record.split("\\|", -1);

        try {

            String actualAdminID = decodeIdentifier(fields[0]);
            String oldName = decode(fields[1]);
            String oldUsername = decode(fields[2]);

            System.out.println("\nCurrent Admin Information");
            System.out.println("------------------------------------------");
            System.out.println("Admin ID  : " + actualAdminID);
            System.out.println("Full Name : " + oldName);
            System.out.println("Username  : " + oldUsername);
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

            if (!newUsername.equalsIgnoreCase(oldUsername)
                    && adminUsernameExists(newUsername, actualAdminID)) {

                System.out.println(
                        "\nUsername already exists. Update cancelled."
                );

                pause();
                return;
            }

            System.out.println("\n==================================================");
            System.out.println("             CONFIRM ADMIN UPDATE");
            System.out.println("==================================================");
            System.out.println("Admin ID   : " + actualAdminID);
            System.out.println("Old Name   : " + oldName);
            System.out.println("New Name   : " + newName);
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

            // Preserve the existing password hash.
            String updatedRecord =
                    actualAdminID + "|"
                    + encode(newName) + "|"
                    + encode(newUsername) + "|"
                    + fields[3];

            if (database.updateAdminAccount(
                    actualAdminID,
                    updatedRecord
            )) {

                if (!oldName.equals(newName)) {
                    logActivity(
                            "ADMIN_NAME_UPDATED|" + actualAdminID
                    );
                }

                if (!oldUsername.equalsIgnoreCase(newUsername)) {
                    logActivity(
                            "ADMIN_USERNAME_UPDATED|" + actualAdminID
                    );
                }

                System.out.println(
                        "\nAdmin information updated successfully!"
                );

            } else {

                System.out.println(
                        "\nUnable to update Admin information."
                );
            }

        } catch (IllegalArgumentException e) {

            System.out.println(
                    "\nUnable to process the Admin account record."
            );
        }

        pause();
    }

    // =========================================================
    // ACTIVATE / DEACTIVATE ADMIN ACCOUNT
    // =========================================================

    public void changeAdminAccountStatus() {

        System.out.println("\n==================================================");
        System.out.println("          CHANGE ADMIN ACCOUNT STATUS");
        System.out.println("==================================================");

        String adminID = readRequiredInput("Enter Admin ID: ");

        if (adminID == null) {
            return;
        }

        String record = findAdminRecord(adminID);

        if (record == null) {
            System.out.println("\nAdmin account not found.");
            pause();
            return;
        }

        String[] fields = record.split("\\|", -1);

        try {

            String actualAdminID = decodeIdentifier(fields[0]);
            String fullName = decode(fields[1]);
            String currentStatus =
                    database.getAccountStatus(actualAdminID);

            System.out.println("\nAdmin ID      : " + actualAdminID);
            System.out.println("Full Name     : " + fullName);
            System.out.println("Current Status: " + currentStatus);

            if ("ACTIVE".equalsIgnoreCase(currentStatus)) {
                System.out.println("\n[1] Deactivate Admin Account");
            } else {
                System.out.println("\n[1] Activate Admin Account");
            }

            System.out.println("[2] Cancel");

            int choice = readChoice("Enter your choice: ", 1, 2);

            if (choice == 2) {
                System.out.println("\nOperation cancelled.");
                pause();
                return;
            }

            String newStatus =
                    "ACTIVE".equalsIgnoreCase(currentStatus)
                    ? "INACTIVE"
                    : "ACTIVE";

            // Prevent deactivation of the last active Admin.
            if ("INACTIVE".equals(newStatus)
                    && countActiveAdminAccounts() <= 1) {

                System.out.println(
                        "\nOperation denied. The last active Admin "
                        + "account cannot be deactivated."
                );

                pause();
                return;
            }

            System.out.println("\nAre you sure you want to change");
            System.out.println("the Admin status to " + newStatus + "?");
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

            if (database.updateAccountStatus(
                    actualAdminID,
                    newStatus
            )) {

                logActivity(
                        "ADMIN_ACCOUNT_" + newStatus
                        + "|" + actualAdminID
                );

                System.out.println(
                        "\nAdmin account status successfully changed to "
                        + newStatus + "!"
                );

            } else {

                System.out.println(
                        "\nUnable to update the Admin account status."
                );
            }

        } catch (IllegalArgumentException e) {

            System.out.println(
                    "\nUnable to process the Admin account record."
            );
        }

        pause();
    }

    // =========================================================
    // COUNT ACTIVE ADMIN ACCOUNTS
    // =========================================================

    private int countActiveAdminAccounts() {

        int count = 0;

        for (String record : database.readAdmins()) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length != 4) {
                continue;
            }

            try {

                String adminID = decodeIdentifier(fields[0]);

                if ("ACTIVE".equalsIgnoreCase(
                        database.getAccountStatus(adminID))) {
                    count++;
                }

            } catch (IllegalArgumentException e) {
                // Skip unreadable records.
            }
        }

        return count;
    }

    // =========================================================
    // FIND ADMIN RECORD
    // =========================================================

    private String findAdminRecord(String search) {

        if (search == null || search.trim().isEmpty()) {
            return null;
        }

        String foundRecord = null;

        for (String record : database.readAdmins()) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length != 4) {
                continue;
            }

            try {

                String adminID = decodeIdentifier(fields[0]);
                String username = decode(fields[2]);

                if (adminID.equalsIgnoreCase(search.trim())
                        || username.equalsIgnoreCase(search.trim())) {

                    // Avoid returning an ambiguous result.
                    if (foundRecord != null) {
                        return null;
                    }

                    foundRecord = record;
                }

            } catch (IllegalArgumentException e) {
                // Skip unreadable records.
            }
        }

        return foundRecord;
    }

    // =========================================================
    // DISPLAY ADMIN INFORMATION
    // =========================================================

    private void displayAdminInformation(String record) {

        String[] fields = record.split("\\|", -1);

        try {

            String adminID = decodeIdentifier(fields[0]);

            System.out.println("\n==================================================");
            System.out.println("              ADMIN ACCOUNT DETAILS");
            System.out.println("==================================================");
            System.out.println("Admin ID      : " + adminID);
            System.out.println("Full Name     : " + decode(fields[1]));
            System.out.println("Username      : " + decode(fields[2]));
            System.out.println("Account Status: "
                    + database.getAccountStatus(adminID));
            System.out.println("==================================================");

        } catch (IllegalArgumentException e) {

            System.out.println("Unable to display Admin information.");
        }
    }

    // =========================================================
    // CHECK ADMIN USERNAME DUPLICATES
    // =========================================================

    private boolean adminUsernameExists(
            String username,
            String excludedAdminID
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

        // Check other Admin usernames.
        for (String record : database.readAdmins()) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length != 4) {
                continue;
            }

            try {

                String existingAdminID =
                        decodeIdentifier(fields[0]);

                if (existingAdminID.equalsIgnoreCase(excludedAdminID)) {
                    continue;
                }

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
    // CHECK CUSTOMER UPDATE USERNAME DUPLICATES
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

        // Check Admin usernames.
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

                System.out.println(
                        "Invalid input. Please enter a number."
                );
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

    private String decodeIdentifier(String value) {

        if (value == null || value.trim().isEmpty()) {
            return value;
        }

        // Current IDs are stored as plain text, such as A1001.
        if (value.matches("[A-Za-z]\\d+")) {
            return value;
        }

        // Support older Base64-encoded IDs.
        try {

            String decoded = decode(value);

            if (decoded.matches("[A-Za-z]\\d+")) {
                return decoded;
            }

        } catch (IllegalArgumentException e) {
            // Keep the original value.
        }

        return value;
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
