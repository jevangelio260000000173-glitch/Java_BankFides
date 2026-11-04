package src;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class UserAuthentication {

    private final Scanner scanner;
    private final FileDatabase database;
    private final ArrayList<Customer> customers = new ArrayList<>();

    private static final String DEFAULT_ADMIN_ID = "A1001";
    private static final String DEFAULT_ADMIN_NAME = "System Administrator";
    private static final String DEFAULT_ADMIN_USERNAME = "admin001";
    private static final String DEFAULT_ADMIN_PASSWORD = "Admin123!";

    private static final int MAX_LOGIN_ATTEMPTS = 3;

    // Password hashing settings
    private static final String HASH_PREFIX = "PBKDF2";
    private static final int HASH_ITERATIONS = 120000;
    private static final int HASH_KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // =========================================================
    // CONSTRUCTORS
    // =========================================================

    public UserAuthentication(Scanner scanner) {
        this(scanner, new FileDatabase());
    }

    public UserAuthentication(Scanner scanner, FileDatabase database) {

        this.scanner = scanner;
        this.database = database;

        loadCustomers();
        createDefaultAdmin();
        initializeAccountStatuses();
    }

    // =========================================================
    // CUSTOMER REGISTRATION
    // =========================================================

    public void registerCustomer() {

        System.out.println("\n==================================================");
        System.out.println("                CUSTOMER REGISTRATION");
        System.out.println("==================================================");

        String fullName = readValidAccountField("Enter full name: ");

        if (fullName == null) {
            return;
        }

        String username;

        while (true) {

            username = readValidAccountField("Create username: ");

            if (username == null) {
                return;
            }

            if (usernameExists(username)) {

                System.out.println(
                        "Username already exists. Please choose another.\n"
                );

            } else {
                break;
            }
        }

        String password;

        while (true) {

            password = readPassword("Create password: ");

            if (password == null) {
                return;
            }

            if (password.isEmpty()) {

                System.out.println(
                        "Password cannot be empty. Please try again.\n"
                );

                continue;
            }

            String confirmPassword =
                    readPassword("Confirm password: ");

            if (confirmPassword == null) {
                return;
            }

            if (password.equals(confirmPassword)) {
                break;
            }

            System.out.println(
                    "\nPasswords do not match. Please try again.\n"
            );
        }

        System.out.println("\n==================================================");
        System.out.println("             CONFIRM ACCOUNT DETAILS");
        System.out.println("==================================================");
        System.out.println("Full Name       : " + fullName);
        System.out.println("Username        : " + username);
        System.out.println("Account Type    : Customer");
        System.out.println("Initial Balance : PHP 0.00");
        System.out.println("Password        : [Hidden]");
        System.out.println("==================================================");
        System.out.println("[1] Confirm and Create Account");
        System.out.println("[2] Cancel Registration");
        System.out.println("==================================================");

        int confirmation = readChoice(
                "Enter your choice: ", 1, 2
        );

        if (confirmation == 2) {

            System.out.println("\nRegistration cancelled.");
            return;
        }

        if (usernameExists(username)) {

            System.out.println(
                    "\nRegistration failed. The username is already in use."
            );

            return;
        }

        String accountID = generateNextCustomerID();

        double balance = 0.0;
        double totalDeposits = 0.0;
        int rewardPoints = 0;

        String hashedPassword;

        try {

            hashedPassword = hashPassword(password);

        } catch (RuntimeException e) {

            System.out.println(
                    "\nRegistration failed. Unable to secure the password."
            );

            return;
        }

        String customerRecord =
                accountID + "|"
                + encode(fullName) + "|"
                + encode(username) + "|"
                + encode(hashedPassword) + "|"
                + balance + "|"
                + totalDeposits + "|"
                + rewardPoints;

        try {

            if (!database.ensureAccountStatus(accountID)) {

                throw new RuntimeException(
                        "Unable to initialize account status."
                );
            }

            if (!database.saveCustomer(customerRecord)) {

                throw new RuntimeException(
                        "Unable to save customer information."
                );
            }

            loadCustomers();

            logActivity("CUSTOMER_REGISTERED|" + accountID);

            System.out.println("\n==================================================");
            System.out.println("        REGISTRATION SUCCESSFUL");
            System.out.println("==================================================");
            System.out.println("Customer ID : " + accountID);
            System.out.println("Full Name   : " + fullName);
            System.out.println("Username    : " + username);
            System.out.println("Balance     : PHP 0.00");
            System.out.println("Password    : [Hidden]");
            System.out.println("Status      : ACTIVE");
            System.out.println("==================================================");

            System.out.println("[1] Proceed to Login");
            System.out.println("[2] Return to Main Menu");
            System.out.println("==================================================");

            int nextChoice = readChoice(
                    "Enter your choice: ", 1, 2
            );

            if (nextChoice == 1) {
                login();
            }

        } catch (RuntimeException e) {

            System.out.println(
                    "\nRegistration failed. Unable to save the account."
            );

            System.out.println("Reason: " + e.getMessage());
        }
    }

    // =========================================================
    // LOGIN WITH ACCOUNT LOCKOUT PROTECTION
    // =========================================================

    public void login() {

        while (true) {

            loadCustomers();

            System.out.println("\n==================================================");
            System.out.println("                     LOGIN");
            System.out.println("==================================================");

            String username = readValidAccountField("Enter username: ");

            if (username == null) {
                return;
            }

            String password = readPassword("Enter password: ");

            if (password == null) {
                return;
            }

            String accountID = null;
            String accountName = null;
            String accountRole = null;
            String storedPassword = null;

            Customer customer = findCustomer(username);

            // -------------------------------------------------
            // IDENTIFY CUSTOMER ACCOUNT
            // -------------------------------------------------

            if (customer != null) {

                accountID = customer.accountID;
                accountName = customer.fullName;
                accountRole = "CUSTOMER";
                storedPassword = customer.password;
            }

            // -------------------------------------------------
            // IDENTIFY STAFF ACCOUNT
            // -------------------------------------------------

            if (accountID == null) {

                String staffRecord = findAccountRecord(
                        database.readStaff(),
                        username
                );

                if (staffRecord != null) {

                    String[] fields = staffRecord.split("\\|", -1);

                    try {

                        if (fields.length == 4) {

                            accountID = fields[0];
                            accountName = decode(fields[1]);
                            storedPassword = decode(fields[3]);
                            accountRole = "STAFF";
                        }

                    } catch (IllegalArgumentException e) {

                        System.out.println(
                                "\nUnable to read the staff account record."
                        );

                        logActivity("LOGIN_FAILED_INVALID_STAFF_RECORD");
                        return;
                    }
                }
            }

            // -------------------------------------------------
            // IDENTIFY ADMIN ACCOUNT
            // -------------------------------------------------

            if (accountID == null) {

                String adminRecord = findAccountRecord(
                        database.readAdmins(),
                        username
                );

                if (adminRecord != null) {

                    String[] fields = adminRecord.split("\\|", -1);

                    try {

                        if (fields.length == 4) {

                            accountID = decodeIdentifier(fields[0]);
                            accountName = decode(fields[1]);
                            storedPassword = decode(fields[3]);
                            accountRole = "ADMIN";
                        }

                    } catch (IllegalArgumentException e) {

                        System.out.println(
                                "\nUnable to read the administrator record."
                        );

                        logActivity("LOGIN_FAILED_INVALID_ADMIN_RECORD");
                        return;
                    }
                }
            }

            // -------------------------------------------------
            // UNKNOWN USERNAME
            // -------------------------------------------------

            if (accountID == null) {

                System.out.println("\n==================================================");
                System.out.println("                  LOGIN FAILED");
                System.out.println("==================================================");
                System.out.println("Incorrect username or password.");
                System.out.println("==================================================");

                logActivity("LOGIN_FAILED_UNKNOWN_USERNAME");

                if (!askRetryLogin()) {
                    return;
                }

                continue;
            }

            // -------------------------------------------------
            // CHECK TEMPORARY ACCOUNT LOCKOUT
            // -------------------------------------------------

            if (database.isLoginLocked(accountID)) {

                showAccountLockout(accountID);

                logActivity(
                        "LOGIN_BLOCKED_TEMPORARY_LOCK|" + accountID
                );

                if (!askRetryLogin()) {
                    return;
                }

                continue;
            }

            // -------------------------------------------------
            // VERIFY PASSWORD
            // -------------------------------------------------

            boolean passwordCorrect;

            try {

                passwordCorrect = verifyPassword(
                        password,
                        storedPassword
                );

            } catch (RuntimeException e) {

                System.out.println(
                        "\nUnable to verify the account password."
                );

                logActivity(
                        "LOGIN_FAILED_PASSWORD_VERIFICATION|" + accountID
                );

                return;
            }

            if (!passwordCorrect) {

                int attempts = database.recordFailedLogin(accountID);

                System.out.println("\n==================================================");
                System.out.println("                  LOGIN FAILED");
                System.out.println("==================================================");
                System.out.println("Incorrect username or password.");

                logActivity(
                        "LOGIN_FAILED|" + accountRole + "|" + accountID
                );

                if (attempts == -1) {

                    System.out.println(
                            "\nUnable to save login security information."
                    );

                    System.out.println(
                            "Please try again later."
                    );

                } else if (attempts >= MAX_LOGIN_ATTEMPTS) {

                    System.out.println("\nACCOUNT TEMPORARILY LOCKED!");

                    System.out.println(
                            "You have reached the maximum of "
                            + MAX_LOGIN_ATTEMPTS + " failed attempts."
                    );

                    System.out.println(
                            "Your account is locked for 5 minutes."
                    );

                    showAccountLockout(accountID);

                } else {

                    int remaining = MAX_LOGIN_ATTEMPTS - attempts;

                    System.out.println(
                            "\nFailed attempts: " + attempts
                            + " out of " + MAX_LOGIN_ATTEMPTS
                    );

                    System.out.println(
                            "Remaining attempts: " + remaining
                    );
                }

                System.out.println("==================================================");

                if (!askRetryLogin()) {
                    return;
                }

                continue;
            }

            // -------------------------------------------------
            // RESET FAILED LOGIN ATTEMPTS
            // -------------------------------------------------

            if (!database.resetLoginAttempts(accountID)) {

                System.out.println(
                        "\nUnable to reset login security information."
                );

                System.out.println(
                        "Login cancelled for your account's protection."
                );

                logActivity(
                        "LOGIN_FAILED_SECURITY_RESET|" + accountID
                );

                return;
            }

            // -------------------------------------------------
            // VERIFY ACCOUNT STATUS
            // -------------------------------------------------

            if (!canLogin(accountID)) {
                return;
            }

            // -------------------------------------------------
            // CUSTOMER LOGIN
            // -------------------------------------------------

            if ("CUSTOMER".equals(accountRole)) {

                System.out.println("\nCustomer login successful!");
                System.out.println("Welcome, " + accountName + "!");

                logActivity("CUSTOMER_LOGIN|" + accountID);

                customer = findCustomer(username);

                if (customer == null) {

                    System.out.println(
                            "Unable to load customer account information."
                    );

                    return;
                }

                Dashboard dashboard = new Dashboard(
                        scanner,
                        database
                );

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

            // -------------------------------------------------
            // STAFF LOGIN
            // -------------------------------------------------

            if ("STAFF".equals(accountRole)) {

                System.out.println("\nStaff login successful!");
                System.out.println("Welcome, " + accountName + "!");

                logActivity("STAFF_LOGIN|" + accountID);

                Dashboard dashboard = new Dashboard(
                        scanner,
                        database
                );

                dashboard.staffDashboard(
                        accountID,
                        accountName
                );

                return;
            }

            // -------------------------------------------------
            // ADMIN LOGIN
            // -------------------------------------------------

            if ("ADMIN".equals(accountRole)) {

                System.out.println("\nAdmin login successful!");
                System.out.println("Welcome, " + accountName + "!");

                logActivity("ADMIN_LOGIN|" + accountID);

                Dashboard dashboard = new Dashboard(
                        scanner,
                        database
                );

                dashboard.adminDashboard(
                        accountID,
                        accountName
                );

                return;
            }
        }
    }

    // =========================================================
    // PASSWORD INPUT
    // =========================================================

    private String readPassword(String prompt) {

        java.io.Console console = System.console();

        // Hide password input when running in a supported terminal.
        if (console != null) {

            char[] password = console.readPassword("%s", prompt);

            if (password == null) {
                return null;
            }

            try {

                return new String(password);

            } finally {

                java.util.Arrays.fill(password, '\0');
            }
        }

        // Fallback for NetBeans Output window.
        // Scanner keeps the application compatible with the IDE.
        System.out.print(prompt);

        if (!scanner.hasNextLine()) {
            return null;
        }

        return scanner.nextLine();
    }

    // =========================================================
    // PASSWORD HASHING
    // =========================================================

    private String hashPassword(String password) {

        byte[] salt = new byte[SALT_LENGTH];
        SECURE_RANDOM.nextBytes(salt);

        byte[] hash = generatePasswordHash(
                password,
                salt,
                HASH_ITERATIONS
        );

        return HASH_PREFIX + "$"
                + HASH_ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    private byte[] generatePasswordHash(
            String password,
            byte[] salt,
            int iterations
    ) {

        PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                iterations,
                HASH_KEY_LENGTH
        );

        try {

            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

            return factory.generateSecret(spec).getEncoded();

        } catch (GeneralSecurityException e) {

            throw new IllegalStateException(
                    "Password hashing is unavailable.",
                    e
            );

        } finally {

            spec.clearPassword();
        }
    }

    private boolean verifyPassword(
            String enteredPassword,
            String storedPassword
    ) {

        if (enteredPassword == null || storedPassword == null) {
            return false;
        }

        // Verify new PBKDF2 passwords.
        if (storedPassword.startsWith(HASH_PREFIX + "$")) {

            String[] parts = storedPassword.split("\\$", -1);

            if (parts.length != 4) {
                return false;
            }

            try {

                int iterations = Integer.parseInt(parts[1]);

                if (iterations < 1 || iterations > 1000000) {
                    return false;
                }

                byte[] salt = Base64.getDecoder().decode(parts[2]);
                byte[] storedHash = Base64.getDecoder().decode(parts[3]);

                if (salt.length == 0 || storedHash.length == 0) {
                    return false;
                }

                byte[] enteredHash = generatePasswordHash(
                        enteredPassword,
                        salt,
                        iterations
                );

                return MessageDigest.isEqual(
                        storedHash,
                        enteredHash
                );

            } catch (IllegalArgumentException e) {

                return false;
            }
        }

        // Backward compatibility with older plaintext password records.
        return storedPassword.equals(enteredPassword);
    }

    // =========================================================
    // DISPLAY ACCOUNT LOCKOUT INFORMATION
    // =========================================================

    private void showAccountLockout(String accountID) {

        long remainingMillis =
                database.getLoginLockRemainingMillis(accountID);

        if (remainingMillis <= 0) {

            System.out.println(
                    "\nYour account lockout has expired. You may try again."
            );

            return;
        }

        long remainingSeconds =
                (remainingMillis + 999) / 1000;

        long minutes = remainingSeconds / 60;
        long seconds = remainingSeconds % 60;

        System.out.println("\n==================================================");
        System.out.println("              ACCOUNT TEMPORARILY LOCKED");
        System.out.println("==================================================");
        System.out.println("Account ID       : " + accountID);
        System.out.println("Maximum Attempts : " + MAX_LOGIN_ATTEMPTS);
        System.out.println("Reason           : Too many incorrect passwords.");

        System.out.printf(
                "Time Remaining   : %02d:%02d%n",
                minutes,
                seconds
        );

        System.out.println("==================================================");
    }

    // =========================================================
    // ASK USER WHETHER TO RETRY LOGIN
    // =========================================================

    private boolean askRetryLogin() {

        System.out.println("\n[1] Try Again");
        System.out.println("[2] Return to Main Menu");
        System.out.println("==================================================");

        int choice = readChoice(
                "Enter your choice: ", 1, 2
        );

        return choice == 1;
    }

    // =========================================================
    // CHECK ACCOUNT STATUS BEFORE LOGIN
    // =========================================================

    private boolean canLogin(String accountID) {

        if (!database.ensureAccountStatus(accountID)) {

            System.out.println(
                    "\nUnable to verify account status."
            );

            return false;
        }

        String status = database.getAccountStatus(accountID);

        if (!"ACTIVE".equals(status)) {

            System.out.println("\n==================================================");
            System.out.println("                 LOGIN BLOCKED");
            System.out.println("==================================================");
            System.out.println("This account is currently INACTIVE.");
            System.out.println("Please contact the administrator.");
            System.out.println("==================================================");

            logActivity(
                    "LOGIN_BLOCKED_INACTIVE|" + accountID
            );

            return false;
        }

        return true;
    }

    // =========================================================
    // ADMIN: STAFF ACCOUNT MANAGEMENT
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

            int choice = readChoice(
                    "Choose an option: ", 1, 3
            );

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

        String fullName = readValidAccountField(
                "Enter staff full name: "
        );

        if (fullName == null) {
            return;
        }

        String username;

        while (true) {

            username = readValidAccountField(
                    "Create staff username: "
            );

            if (username == null) {
                return;
            }

            if (usernameExists(username)) {

                System.out.println(
                        "Username already exists. Please choose another.\n"
                );

            } else {
                break;
            }
        }

        String password;

        while (true) {

            password = readPassword("Create staff password: ");

            if (password == null) {
                return;
            }

            if (password.isEmpty()) {

                System.out.println(
                        "Password cannot be empty. Please try again.\n"
                );

                continue;
            }

            String confirmPassword =
                    readPassword("Confirm staff password: ");

            if (confirmPassword == null) {
                return;
            }

            if (password.equals(confirmPassword)) {
                break;
            }

            System.out.println(
                    "\nPasswords do not match. Please try again.\n"
            );
        }

        if (usernameExists(username)) {

            System.out.println(
                    "\nUnable to create staff account. Username already exists."
            );

            return;
        }

        String hashedPassword;

        try {

            hashedPassword = hashPassword(password);

        } catch (RuntimeException e) {

            System.out.println(
                    "\nUnable to secure the staff account password."
            );

            return;
        }

        String staffID = generateNextStaffID();

        String staffRecord =
                staffID + "|"
                + encode(fullName) + "|"
                + encode(username) + "|"
                + encode(hashedPassword);

        try {

            if (!database.ensureAccountStatus(staffID)) {

                throw new RuntimeException(
                        "Unable to initialize staff account status."
                );
            }

            if (!database.saveStaff(staffRecord)) {

                throw new RuntimeException(
                        "Unable to save staff information."
                );
            }

            System.out.println("\n==================================================");
            System.out.println("       STAFF ACCOUNT CREATED SUCCESSFULLY");
            System.out.println("==================================================");
            System.out.println("Staff ID  : " + staffID);
            System.out.println("Full Name : " + fullName);
            System.out.println("Username  : " + username);
            System.out.println("Password  : [Hidden]");
            System.out.println("Status    : ACTIVE");
            System.out.println("==================================================");

            logActivity(
                    "STAFF_ACCOUNT_CREATED|" + staffID
            );

        } catch (RuntimeException e) {

            System.out.println(
                    "\nUnable to create staff account."
            );

            System.out.println("Reason: " + e.getMessage());
        }
    }

    // =========================================================
    // VIEW STAFF ACCOUNTS
    // =========================================================

    public void viewStaffAccounts() {

        System.out.println("\n==================================================");
        System.out.println("                  STAFF ACCOUNTS");
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
                String status = database.getAccountStatus(staffID);

                System.out.println("------------------------------------------");
                System.out.println("Staff ID  : " + staffID);
                System.out.println("Full Name : " + fullName);
                System.out.println("Username  : " + username);
                System.out.println("Status    : " + status);

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
    // ADMIN: CREATE ADMIN ACCOUNT
    // =========================================================

    public void createAdminAccount() {

        System.out.println("\n==================================================");
        System.out.println("               CREATE ADMIN ACCOUNT");
        System.out.println("==================================================");

        String fullName = readValidAccountField(
                "Enter admin full name: "
        );

        if (fullName == null) {
            return;
        }

        String username;

        while (true) {

            username = readValidAccountField(
                    "Create admin username: "
            );

            if (username == null) {
                return;
            }

            if (usernameExists(username)) {

                System.out.println(
                        "Username already exists. Please choose another.\n"
                );

            } else {
                break;
            }
        }

        String password;

        while (true) {

            password = readPassword("Create admin password: ");

            if (password == null) {
                return;
            }

            if (password.isEmpty()) {

                System.out.println(
                        "Password cannot be empty. Please try again.\n"
                );

                continue;
            }

            String confirmPassword =
                    readPassword("Confirm admin password: ");

            if (confirmPassword == null) {
                return;
            }

            if (password.equals(confirmPassword)) {
                break;
            }

            System.out.println(
                    "\nPasswords do not match. Please try again.\n"
            );
        }

        // Check again before saving the account.
        if (usernameExists(username)) {

            System.out.println(
                    "\nUnable to create admin account. Username already exists."
            );

            return;
        }

        String hashedPassword;

        try {

            hashedPassword = hashPassword(password);

        } catch (RuntimeException e) {

            System.out.println(
                    "\nUnable to secure the admin account password."
            );

            return;
        }

        String adminID = generateNextAdminID();

        String adminRecord =
                adminID + "|"
                + encode(fullName) + "|"
                + encode(username) + "|"
                + encode(hashedPassword);

        try {

            if (!database.saveAdmin(adminRecord)) {

                throw new RuntimeException(
                        "Unable to save admin information."
                );
            }

            if (!database.ensureAccountStatus(adminID)) {

                throw new RuntimeException(
                        "Unable to initialize admin account status."
                );
            }

            System.out.println("\n==================================================");
            System.out.println("       ADMIN ACCOUNT CREATED SUCCESSFULLY");
            System.out.println("==================================================");
            System.out.println("Admin ID  : " + adminID);
            System.out.println("Full Name : " + fullName);
            System.out.println("Username  : " + username);
            System.out.println("Password  : [Hidden]");
            System.out.println("Status    : ACTIVE");
            System.out.println("==================================================");

            logActivity(
                    "ADMIN_ACCOUNT_CREATED|" + adminID
            );

        } catch (RuntimeException e) {

            System.out.println(
                    "\nUnable to create admin account."
            );

            System.out.println("Reason: " + e.getMessage());
        }
    }

    // =========================================================
    // LOAD CUSTOMER RECORDS FROM DATABASE
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

                System.out.println(
                        "Warning: An invalid customer record was skipped."
                );

                continue;
            }

            try {

                String accountID = fields[0];
                String fullName = decode(fields[1]);
                String username = decode(fields[2]);
                String storedPassword = decode(fields[3]);

                double balance = Double.parseDouble(fields[4]);
                double totalDeposits = Double.parseDouble(fields[5]);
                int rewardPoints = Integer.parseInt(fields[6]);

                if (accountID.trim().isEmpty()
                        || fullName.trim().isEmpty()
                        || username.trim().isEmpty()
                        || storedPassword.trim().isEmpty()
                        || !Double.isFinite(balance)
                        || !Double.isFinite(totalDeposits)
                        || balance < 0
                        || totalDeposits < 0
                        || rewardPoints < 0) {

                    throw new IllegalArgumentException(
                            "Invalid customer account values."
                    );
                }

                customers.add(new Customer(
                        accountID,
                        fullName,
                        username,
                        storedPassword,
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
    // CREATE DEFAULT ADMIN ACCOUNT
    // =========================================================

    private void createDefaultAdmin() {

        List<String> admins = database.readAdmins();

        if (admins != null) {

            for (String record : admins) {

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
                    String storedPassword = decode(fields[3]);

                    if (adminID != null
                            && !adminID.trim().isEmpty()
                            && fullName != null
                            && !fullName.trim().isEmpty()
                            && username != null
                            && !username.trim().isEmpty()
                            && storedPassword != null
                            && !storedPassword.trim().isEmpty()) {

                        return;
                    }

                } catch (IllegalArgumentException e) {

                    // Skip invalid administrator records.
                }
            }
        }

        String hashedPassword;

        try {

            hashedPassword = hashPassword(DEFAULT_ADMIN_PASSWORD);

        } catch (RuntimeException e) {

            System.out.println(
                    "Warning: Unable to secure the default admin password."
            );

            return;
        }

        String adminRecord =
                DEFAULT_ADMIN_ID + "|"
                + encode(DEFAULT_ADMIN_NAME) + "|"
                + encode(DEFAULT_ADMIN_USERNAME) + "|"
                + encode(hashedPassword);

        try {

            if (!database.saveAdmin(adminRecord)) {

                System.out.println(
                        "Warning: Unable to save the default admin account."
                );

                return;
            }

            if (!database.ensureAccountStatus(DEFAULT_ADMIN_ID)) {

                System.out.println(
                        "Warning: Unable to initialize default admin status."
                );

                return;
            }

            System.out.println("\nDefault administrator account created.");
            System.out.println("Username: " + DEFAULT_ADMIN_USERNAME);
            System.out.println("Password: [Hidden]");

            System.out.println(
                    "Change the default credentials before using "
                    + "the system beyond testing."
            );

        } catch (RuntimeException e) {

            System.out.println(
                    "Warning: Unable to create the default admin account."
            );
        }
    }

    // =========================================================
    // INITIALIZE ACCOUNT STATUSES
    // =========================================================

    private void initializeAccountStatuses() {

        List<String> customerRecords = database.readCustomers();

        if (customerRecords != null) {

            for (String record : customerRecords) {

                if (record == null || record.trim().isEmpty()) {
                    continue;
                }

                String[] fields = record.split("\\|", -1);

                if (fields.length == 7) {

                    if (!database.ensureAccountStatus(fields[0])) {

                        System.out.println(
                                "Warning: Unable to initialize status for "
                                + fields[0]
                        );
                    }
                }
            }
        }

        List<String> staffRecords = database.readStaff();

        if (staffRecords != null) {

            for (String record : staffRecords) {

                if (record == null || record.trim().isEmpty()) {
                    continue;
                }

                String[] fields = record.split("\\|", -1);

                if (fields.length == 4) {

                    if (!database.ensureAccountStatus(fields[0])) {

                        System.out.println(
                                "Warning: Unable to initialize status for "
                                + fields[0]
                        );
                    }
                }
            }
        }

        List<String> adminRecords = database.readAdmins();

        if (adminRecords != null) {

            for (String record : adminRecords) {

                if (record == null || record.trim().isEmpty()) {
                    continue;
                }

                String[] fields = record.split("\\|", -1);

                if (fields.length == 4) {

                    String adminID = decodeIdentifier(fields[0]);

                    if (!database.ensureAccountStatus(adminID)) {

                        System.out.println(
                                "Warning: Unable to initialize status for "
                                + adminID
                        );
                    }
                }
            }
        }
    }

    // =========================================================
    // FIND CUSTOMER BY USERNAME
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
    // CHECK IF USERNAME ALREADY EXISTS
    // =========================================================

    private boolean usernameExists(String username) {

        if (username == null || username.trim().isEmpty()) {
            return true;
        }

        loadCustomers();

        for (Customer customer : customers) {

            if (customer.username.equalsIgnoreCase(username)) {
                return true;
            }
        }

        if (findAccountRecord(database.readStaff(), username) != null) {
            return true;
        }

        return findAccountRecord(
                database.readAdmins(),
                username
        ) != null;
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

                // Skip unreadable records.
            }
        }

        return null;
    }

    // =========================================================
    // GENERATE NEXT CUSTOMER ID
    // =========================================================

    private String generateNextCustomerID() {

        int highestNumber = 1000;

        List<String> records = database.readCustomers();

        if (records == null) {
            return "C" + (highestNumber + 1);
        }

        for (String record : records) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length < 1) {
                continue;
            }

            String accountID = fields[0];

            if (accountID.startsWith("C")) {

                try {

                    int number = Integer.parseInt(
                            accountID.substring(1)
                    );

                    if (number > highestNumber) {
                        highestNumber = number;
                    }

                } catch (NumberFormatException e) {

                    // Skip IDs with an invalid format.
                }
            }
        }

        return "C" + (highestNumber + 1);
    }

    // =========================================================
    // GENERATE NEXT STAFF ID
    // =========================================================

    private String generateNextStaffID() {

        int highestNumber = 1000;

        List<String> staffRecords = database.readStaff();

        if (staffRecords == null) {
            return "S" + (highestNumber + 1);
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

                    // Skip IDs with an invalid format.
                }
            }
        }

        return "S" + (highestNumber + 1);
    }

    // =========================================================
    // GENERATE NEXT ADMIN ID
    // =========================================================

    private String generateNextAdminID() {

        int highestNumber = 1000;

        List<String> adminRecords = database.readAdmins();

        if (adminRecords == null) {
            return "A" + (highestNumber + 1);
        }

        for (String record : adminRecords) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length < 1) {
                continue;
            }

            String adminID = decodeIdentifier(fields[0]);

            if (adminID != null && adminID.startsWith("A")) {

                try {

                    int number = Integer.parseInt(
                            adminID.substring(1)
                    );

                    if (number > highestNumber) {
                        highestNumber = number;
                    }

                } catch (NumberFormatException e) {

                    // Skip IDs with an invalid format.
                }
            }
        }

        return "A" + (highestNumber + 1);
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

    // Supports plain-text IDs and older Base64-encoded IDs.
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

    private String readValidAccountField(String prompt) {

        while (true) {

            System.out.print(prompt);

            if (!scanner.hasNextLine()) {

                System.out.println("\nNo input received.");
                return null;
            }

            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {

                System.out.println(
                        "This field cannot be empty. Please try again.\n"
                );

                continue;
            }

            if (input.contains("|")
                    || input.contains("\n")
                    || input.contains("\r")) {

                System.out.println(
                        "Invalid character detected. Please try again.\n"
                );

                continue;
            }

            return input;
        }
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
                        "Please enter a number from "
                        + min + " to " + max + "."
                );

            } catch (NumberFormatException e) {

                System.out.println(
                        "Invalid input. Please enter a number."
                );
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