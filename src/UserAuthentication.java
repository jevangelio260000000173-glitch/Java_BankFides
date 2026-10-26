package src;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;

public class UserAuthentication {

    private final Scanner scanner;
    private final FileDatabase database;
    private final ArrayList<Customer> customers = new ArrayList<>();

    private static final String DEFAULT_ADMIN_ID = "A1001";
    private static final String DEFAULT_ADMIN_NAME = "System Administrator";
    private static final String DEFAULT_ADMIN_USERNAME = "admin001";
    private static final String DEFAULT_ADMIN_PASSWORD = "Admin123!";

    public UserAuthentication(Scanner scanner) {
        this.scanner = scanner;
        this.database = new FileDatabase();

        loadCustomers();
        createDefaultAdmin();
    }

    // =========================================================
    // CUSTOMER REGISTRATION
    // =========================================================

    public void registerCustomer() {
        System.out.println("\n==================================================");
        System.out.println("                CUSTOMER REGISTRATION");
        System.out.println("==================================================");

        String fullName = readRequiredInput("Enter full name: ");
        if (fullName == null) {
            return;
        }

        String username = readRequiredInput("Create username: ");
        if (username == null) {
            return;
        }

        if (!isValidAccountField(username)) {
            System.out.println(
                    "Invalid username. Do not use the | character."
            );
            return;
        }

        if (usernameExists(username)) {
            System.out.println(
                    "\nRegistration failed. Username already exists."
            );
            return;
        }

        String password = readRequiredInput("Create password: ");
        if (password == null) {
            return;
        }

        if (!isValidAccountField(fullName)
                || !isValidAccountField(password)) {
            System.out.println(
                    "Invalid input. The | character is not allowed."
            );
            return;
        }

        String accountID = generateNextCustomerID();

        double balance = 0.0;
        double totalDeposits = 0.0;
        int rewardPoints = 0;

        String customerRecord =
                accountID + "|"
                + encode(fullName) + "|"
                + encode(username) + "|"
                + encode(password) + "|"
                + balance + "|"
                + totalDeposits + "|"
                + rewardPoints;

        try {
            database.saveCustomer(customerRecord);

            customers.add(new Customer(
                    accountID,
                    fullName,
                    username,
                    password,
                    balance,
                    totalDeposits,
                    rewardPoints
            ));

            System.out.println("\n==================================================");
            System.out.println("        REGISTRATION SUCCESSFUL");
            System.out.println("==================================================");
            System.out.println("Customer ID : " + accountID);
            System.out.println("Full Name   : " + fullName);
            System.out.println("Username    : " + username);
            System.out.println("Balance     : PHP 0.00");
            System.out.println("==================================================");
            System.out.println("You can now log in using your account.");
            
            logActivity("CUSTOMER_REGISTERED|" + accountID);

        } catch (RuntimeException e) {
            System.out.println(
                    "\nRegistration failed. Unable to save the account."
            );
        }
    }

    // =========================================================
    // LOGIN
    // =========================================================

    public void login() {
        System.out.println("\n==================================================");
        System.out.println("                    LOGIN");
        System.out.println("==================================================");

        String username = readRequiredInput("Enter username: ");
        if (username == null) {
            return;
        }

        String password = readRequiredInput("Enter password: ");
        if (password == null) {
            return;
        }

        // -----------------------------------------------------
        // CHECK CUSTOMER
        // -----------------------------------------------------

        Customer customer = findCustomer(username);

        if (customer != null && customer.password.equals(password)) {
            System.out.println("\nCustomer login successful!");
            System.out.println("Welcome, " + customer.fullName + "!");

            logActivity("CUSTOMER_LOGIN|" + customer.accountID);

            Dashboard dashboard = new Dashboard(scanner);

            dashboard.customerDashboard(
                    customer.fullName,
                    customer.accountID,
                    customer.username,
                    customer.balance,
                    customer.totalDeposits,
                    customer.rewardPoints
            );

            return;
        }

        // -----------------------------------------------------
        // CHECK STAFF
        // -----------------------------------------------------

        String staffRecord = findAccountRecord(
                database.readStaff(),
                username
        );

        if (staffRecord != null) {
            String[] fields = staffRecord.split("\\|", -1);

            try {
                if (fields.length == 4
                        && decode(fields[3]).equals(password)) {

                    String staffID = fields[0];
                    String staffName = decode(fields[1]);

                    System.out.println("\nStaff login successful!");
                    System.out.println("Welcome, " + staffName + "!");

                    logActivity("STAFF_LOGIN|" + staffID);

                    Dashboard dashboard = new Dashboard(scanner);

                    dashboard.staffDashboard(
                            staffID,
                            staffName
                    );

                    return;
                }
            } catch (IllegalArgumentException e) {
                System.out.println(
                        "Unable to read the staff account record."
                );
                return;
            }
        }

        // -----------------------------------------------------
        // CHECK ADMIN
        // -----------------------------------------------------

        String adminRecord = findAccountRecord(
                database.readAdmins(),
                username
        );

        if (adminRecord != null) {
            String[] fields = adminRecord.split("\\|", -1);

            try {
                if (fields.length == 4
                        && decode(fields[3]).equals(password)) {

                    String adminID = decodeIdentifier(fields[0]);
                    String adminName = decode(fields[1]);

                    System.out.println("\nAdmin login successful!");
                    System.out.println("Welcome, " + adminName + "!");

                    logActivity("ADMIN_LOGIN|" + adminID);

                    Dashboard dashboard = new Dashboard(scanner);

                    dashboard.adminDashboard(
                            adminID,
                            adminName
                    );

                    return;
                }
            } catch (IllegalArgumentException e) {
                System.out.println(
                        "Unable to read the administrator record."
                );
                return;
            }
        }

        System.out.println("\nLogin failed.");
        System.out.println("Incorrect username or password.");

        logActivity("LOGIN_FAILED");
    }

    // =========================================================
    // ADMINISTRATOR: STAFF ACCOUNT MANAGEMENT
    // =========================================================

    public void manageStaffAccounts() {
        while (true) {
            System.out.println("\n==================================================");
            System.out.println("             STAFF ACCOUNT MANAGEMENT");
            System.out.println("==================================================");
            System.out.println("[1] Create Staff Account");
            System.out.println("[2] View All Staff Accounts");
            System.out.println("[3] Return to Admin Dashboard");
            System.out.println("==================================================");

            int choice = readChoice("Choose an option: ", 1, 3);

            switch (choice) {
                case 1:
                    createStaffAccount();
                    break;

                case 2:
                    viewStaffAccounts();
                    break;

                case 3:
                    System.out.println(
                            "\nReturning to the admin dashboard..."
                    );
                    return;

                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    // =========================================================
    // CREATE STAFF ACCOUNT
    // =========================================================

    public void createStaffAccount() {
        System.out.println("\n==================================================");
        System.out.println("               CREATE STAFF ACCOUNT");
        System.out.println("==================================================");

        String fullName = readRequiredInput("Enter staff full name: ");
        if (fullName == null) {
            return;
        }

        String username = readRequiredInput("Create staff username: ");
        if (username == null) {
            return;
        }

        String password = readRequiredInput("Create staff password: ");
        if (password == null) {
            return;
        }

        if (!isValidAccountField(fullName)
                || !isValidAccountField(username)
                || !isValidAccountField(password)) {
            System.out.println(
                    "\nInvalid input. Empty fields and the | character "
                    + "are not allowed."
            );
            return;
        }

        if (usernameExists(username)) {
            System.out.println(
                    "\nUnable to create account. Username already exists."
            );
            return;
        }

        String staffID = generateNextStaffID();

        String staffRecord =
                staffID + "|"
                + encode(fullName) + "|"
                + encode(username) + "|"
                + encode(password);

        try {
            database.saveStaff(staffRecord);

            System.out.println("\n==================================================");
            System.out.println("       STAFF ACCOUNT CREATED SUCCESSFULLY");
            System.out.println("==================================================");
            System.out.println("Staff ID  : " + staffID);
            System.out.println("Full Name : " + fullName);
            System.out.println("Username  : " + username);
            System.out.println("Password  : [Hidden]");
            System.out.println("==================================================");

            logActivity("STAFF_ACCOUNT_CREATED|" + staffID);

        } catch (RuntimeException e) {
            System.out.println(
                    "\nUnable to create staff account."
            );
            System.out.println(
                    "Please check the database files and try again."
            );
        }
    }

    // =========================================================
    // VIEW STAFF ACCOUNTS
    // =========================================================

    public void viewStaffAccounts() {
        System.out.println("\n==================================================");
        System.out.println("                 STAFF ACCOUNTS");
        System.out.println("==================================================");

        List<String> staffRecords = database.readStaff();

        if (staffRecords == null || staffRecords.isEmpty()) {
            System.out.println("No staff accounts found.");
            return;
        }

        int count = 0;

        for (String record : staffRecords) {
            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length != 4) {
                System.out.println(
                        "Warning: An invalid staff record was skipped."
                );
                continue;
            }

            try {
                String staffID = fields[0];
                String fullName = decode(fields[1]);
                String username = decode(fields[2]);

                System.out.println("------------------------------------------");
                System.out.println("Staff ID  : " + staffID);
                System.out.println("Full Name : " + fullName);
                System.out.println("Username  : " + username);

                count++;

            } catch (IllegalArgumentException e) {
                System.out.println(
                        "Warning: An unreadable staff record was skipped."
                );
            }
        }

        System.out.println("------------------------------------------");
        System.out.println("Total staff accounts: " + count);
        System.out.println("==================================================");
    }

    // =========================================================
    // LOAD CUSTOMER RECORDS
    // =========================================================

    private void loadCustomers() {
        customers.clear();

        List<String> records = database.readCustomers();

        if (records == null) {
            return;
        }

        for (String record : records) {
            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length != 7) {
                continue;
            }

            try {
                String accountID = fields[0];
                String fullName = decode(fields[1]);
                String username = decode(fields[2]);
                String password = decode(fields[3]);

                double balance = Double.parseDouble(fields[4]);
                double totalDeposits = Double.parseDouble(fields[5]);
                int rewardPoints = Integer.parseInt(fields[6]);

                customers.add(new Customer(
                        accountID,
                        fullName,
                        username,
                        password,
                        balance,
                        totalDeposits,
                        rewardPoints
                ));

            } catch (IllegalArgumentException e) {
                System.out.println(
                        "Warning: An invalid customer record was skipped."
                );
            }
        }
    }

    // =========================================================
    // CREATE DEFAULT ADMIN
    // =========================================================

    private void createDefaultAdmin() {
        List<String> admins = database.readAdmins();

        if (admins != null && !admins.isEmpty()) {
            return;
        }

        String adminRecord =
                DEFAULT_ADMIN_ID + "|"
                + encode(DEFAULT_ADMIN_NAME) + "|"
                + encode(DEFAULT_ADMIN_USERNAME) + "|"
                + encode(DEFAULT_ADMIN_PASSWORD);

        try {
            database.saveAdmin(adminRecord);

            System.out.println("\nDefault administrator account created.");
            System.out.println("Username: " + DEFAULT_ADMIN_USERNAME);
            System.out.println("Password: " + DEFAULT_ADMIN_PASSWORD);
            System.out.println(
                    "Please change the default credentials before "
                    + "using the system beyond testing."
            );

        } catch (RuntimeException e) {
            System.out.println(
                    "Warning: Unable to create the default admin account."
            );
        }
    }

    // =========================================================
    // FIND CUSTOMER
    // =========================================================

    private Customer findCustomer(String username) {
        for (Customer customer : customers) {
            if (customer.username.equalsIgnoreCase(username)) {
                return customer;
            }
        }

        return null;
    }

    // =========================================================
    // CHECK WHETHER USERNAME ALREADY EXISTS
    // =========================================================

    private boolean usernameExists(String username) {
        if (username == null || username.trim().isEmpty()) {
            return true;
        }

        for (Customer customer : customers) {
            if (customer.username.equalsIgnoreCase(username)) {
                return true;
            }
        }

        if (findAccountRecord(database.readStaff(), username) != null) {
            return true;
        }

        return findAccountRecord(database.readAdmins(), username) != null;
    }

    // =========================================================
    // FIND STAFF OR ADMIN RECORD BY USERNAME
    // =========================================================

    private String findAccountRecord(
            List<String> records,
            String username
    ) {
        if (records == null || username == null) {
            return null;
        }

        for (String record : records) {
            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length != 4) {
                continue;
            }

            try {
                String storedUsername = decode(fields[2]);

                if (storedUsername.equalsIgnoreCase(username)) {
                    return record;
                }

            } catch (IllegalArgumentException e) {
                // Skip records that cannot be decoded.
            }
        }

        return null;
    }

    // =========================================================
    // GENERATE CUSTOMER ID
    // =========================================================

    private String generateNextCustomerID() {
        int highestNumber = 1000;

        for (Customer customer : customers) {
            String accountID = customer.accountID;

            if (accountID != null && accountID.startsWith("C")) {
                try {
                    int number = Integer.parseInt(
                            accountID.substring(1)
                    );

                    if (number > highestNumber) {
                        highestNumber = number;
                    }

                } catch (NumberFormatException e) {
                    // Skip IDs that do not follow the expected format.
                }
            }
        }

        return "C" + (highestNumber + 1);
    }

    // =========================================================
    // GENERATE STAFF ID
    // =========================================================

    private String generateNextStaffID() {
        int highestNumber = 1000;

        List<String> staffRecords = database.readStaff();

        if (staffRecords == null) {
            return "S1001";
        }

        for (String record : staffRecords) {
            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length < 1) {
                continue;
            }

            String staffID = fields[0];

            if (staffID.startsWith("S")) {
                try {
                    int number = Integer.parseInt(
                            staffID.substring(1)
                    );

                    if (number > highestNumber) {
                        highestNumber = number;
                    }

                } catch (NumberFormatException e) {
                    // Skip IDs that do not follow the expected format.
                }
            }
        }

        return "S" + (highestNumber + 1);
    }

    // =========================================================
    // BASE64 ENCODING AND DECODING
    // =========================================================

    private String encode(String value) {
        return Base64.getEncoder().encodeToString(
                value.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String decode(String value) {
        byte[] decodedBytes = Base64.getDecoder().decode(value);

        return new String(
                decodedBytes,
                StandardCharsets.UTF_8
        );
    }

    // Supports both plain-text IDs and older Base64-encoded IDs.
    private String decodeIdentifier(String value) {
        if (value == null || value.trim().isEmpty()) {
            return value;
        }

        if (value.matches("[A-Za-z]\\d+")) {
            return value;
        }

        try {
            String decoded = decode(value);

            if (decoded.matches("[A-Za-z]\\d+")) {
                return decoded;
            }

        } catch (IllegalArgumentException e) {
            // The ID may already be stored as plain text.
        }

        return value;
    }

    // =========================================================
    // INPUT VALIDATION
    // =========================================================

    private String readRequiredInput(String prompt) {
        System.out.print(prompt);

        if (!scanner.hasNextLine()) {
            System.out.println("\nNo input received.");
            return null;
        }

        String input = scanner.nextLine().trim();

        if (input.isEmpty()) {
            System.out.println("Input cannot be empty.");
            return null;
        }

        return input;
    }

    private boolean isValidAccountField(String value) {
        return value != null
                && !value.trim().isEmpty()
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
                        "Please enter a number from " + min + " to " + max + "."
                );

            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }

    // =========================================================
    // ACTIVITY LOGGING
    // =========================================================

    private void logActivity(String activity) {
        try {
            database.saveActivityLog(activity);

        } catch (RuntimeException e) {
            System.out.println(
                    "Warning: The activity could not be recorded in the log."
            );
        }
    }

    // =========================================================
    // CUSTOMER DATA CLASS
    // =========================================================

    private static class Customer {

        private final String accountID;
        private final String fullName;
        private final String username;
        private final String password;

        private final double balance;
        private final double totalDeposits;
        private final int rewardPoints;

        private Customer(
                String accountID,
                String fullName,
                String username,
                String password,
                double balance,
                double totalDeposits,
                int rewardPoints
        ) {
            this.accountID = accountID;
            this.fullName = fullName;
            this.username = username;
            this.password = password;
            this.balance = balance;
            this.totalDeposits = totalDeposits;
            this.rewardPoints = rewardPoints;
        }
    }
}