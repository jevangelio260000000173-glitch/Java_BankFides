package src;
import java.util.Scanner;

public class Transaction_Protection {

    static Scanner sc = new Scanner(System.in);

    // Sample account data
    static int[] accountIds = {1001, 1002, 1003};
    static String[] usernames = {"ana", "ben", "staff"};
    static String[] passwords = {"ana123", "ben123", "staff123"};
    static String[] roles = {"USER", "USER", "STAFF"};
    static double[] balances = {5000.00, 3000.00, 1500.00};
    static boolean[] active = {true, true, true};

    // Login protection
    static int[] failedLogins = new int[accountIds.length];
    static boolean[] locked = new boolean[accountIds.length];
    static final int MAX_LOGIN_ATTEMPTS = 3;

    // Basic transaction records
    static String[] logs = new String[500];
    static int logCount = 0;

    static String[] transactionKeys = new String[500];
    static int keyCount = 0;

    static int transactionId = 1;

    public static void main(String[] args) {

        System.out.println("=== BANCOFIDES TRANSACTION PROTECTION ===");

        while (true) {
            int user = login();

            if (user == -1) {
                System.out.println("Program closed.");
                break;
            }

            if (user == -2) {
                continue;
            }

            int choice;

            do {
                System.out.println("\n1. Deposit");
                System.out.println("2. Withdraw");
                System.out.println("3. Transfer");
                System.out.println("4. View Balance");
                System.out.println("5. Logout");
                System.out.print("Choose: ");

                choice = readInt();

                switch (choice) {
                    case 1:
                        transact(user, "DEPOSIT");
                        break;
                    case 2:
                        transact(user, "WITHDRAWAL");
                        break;
                    case 3:
                        transact(user, "TRANSFER");
                        break;
                    case 4:
                        System.out.printf("Balance: PHP %.2f%n",
                                balances[user]);
                        break;
                    case 5:
                        System.out.println("Logged out.");
                        break;
                    default:
                        System.out.println("Invalid choice.");
                }

            } while (choice != 5);
        }

        sc.close();
    }

    // LOGIN AND TEMPORARY LOCKING
    static int login() {

        System.out.print("\nUsername (or EXIT): ");
        String username = sc.nextLine().trim();

        if (username.equalsIgnoreCase("EXIT")) {
            return -1;
        }

        int user = findUsername(username);

        if (user == -1) {
            System.out.println("Invalid username or password.");
            log("Failed login: unknown username.");
            return -2;
        }

        if (locked[user]) {
            System.out.println(
                "Account temporarily locked. Contact staff or admin."
            );
            log("Blocked login for account " + accountIds[user]);
            return -2;
        }

        System.out.print("Password: ");
        String password = sc.nextLine();

        if (!passwords[user].equals(password)) {
            failedLogins[user]++;

            System.out.println("Invalid username or password.");
            log("Failed login for account " + accountIds[user]);

            if (failedLogins[user] >= MAX_LOGIN_ATTEMPTS) {
                locked[user] = true;
                System.out.println("Account temporarily locked.");
                log("Account locked: " + accountIds[user]);
            } else {
                System.out.println("Attempts remaining: "
                        + (MAX_LOGIN_ATTEMPTS - failedLogins[user]));
            }

            return -2;
        }

        failedLogins[user] = 0;

        if (!active[user]) {
            System.out.println("Account is inactive.");
            log("Login denied for inactive account "
                    + accountIds[user]);
            return -2;
        }

        System.out.println("Welcome! Role: " + roles[user]);
        log("Successful login: account " + accountIds[user]);

        return user;
    }

    // TRANSACTION VALIDATION AND PROCESSING
    static void transact(int user, String type) {

        // Permission check
        if (!hasPermission(user, type)) {
            System.out.println(
                "You are not authorized to perform this action."
            );
            log("Unauthorized " + type + " attempt.");
            return;
        }

        int source = user;
        int recipient = -1;

        // Verify source account exists and is active
        if (source < 0 || source >= accountIds.length
                || !active[source]) {
            System.out.println("Account not found or inactive.");
            log("Rejected transaction: invalid source account.");
            return;
        }

        // Transfer recipient validation
        if (type.equals("TRANSFER")) {
            System.out.print("Recipient account ID: ");
            int recipientId = readInt();

            recipient = findAccount(recipientId);

            if (recipient == -1 || !active[recipient]) {
                System.out.println(
                    "Recipient account not found or inactive."
                );
                log("Rejected transfer: invalid recipient.");
                return;
            }

            if (recipient == source) {
                System.out.println(
                    "Cannot transfer to the same account."
                );
                log("Rejected transfer: same source and recipient.");
                return;
            }
        }

        System.out.print("Amount: PHP ");
        double amount = readDouble();

        // Reject zero or negative amounts
        if (amount <= 0) {
            System.out.println(
                "Invalid amount. Must be greater than zero."
            );
            log("Rejected " + type + ": invalid amount.");
            return;
        }

        // Sufficient balance check
        if ((type.equals("WITHDRAWAL") || type.equals("TRANSFER"))
                && amount > balances[source]) {
            System.out.println("Insufficient balance.");
            log("Rejected " + type + ": insufficient balance.");
            return;
        }

        // Detect duplicate transaction in current session
        String key = type + "|" + accountIds[source] + "|"
                + (recipient == -1 ? 0 : accountIds[recipient])
                + "|" + String.format("%.2f", amount);

        if (isDuplicate(key)) {
            System.out.println("Duplicate transaction detected.");
            log("Rejected duplicate transaction.");
            return;
        }

        // Confirmation
        System.out.println("\n--- CONFIRM TRANSACTION ---");
        System.out.println("Type: " + type);
        System.out.println("Source account: " + accountIds[source]);

        if (recipient != -1) {
            System.out.println("Recipient account: "
                    + accountIds[recipient]);
        }

        System.out.printf("Amount: PHP %.2f%n", amount);
        System.out.print("Confirm? (Y/N): ");

        String confirm = sc.nextLine().trim();

        if (!confirm.equalsIgnoreCase("Y")) {
            System.out.println("Transaction cancelled.");
            log("Cancelled " + type + " transaction.");
            return;
        }

        // Recheck balance immediately before processing
        if ((type.equals("WITHDRAWAL") || type.equals("TRANSFER"))
                && amount > balances[source]) {
            System.out.println("Insufficient balance.");
            log("Rejected transaction during final balance check.");
            return;
        }

        // Process the transaction
        if (type.equals("DEPOSIT")) {
            balances[source] += amount;
        } else if (type.equals("WITHDRAWAL")) {
            balances[source] -= amount;
        } else if (type.equals("TRANSFER")) {
            balances[source] -= amount;
            balances[recipient] += amount;
        }

        saveKey(key);

        System.out.println("Transaction successful!");
        System.out.println("Transaction ID: " + transactionId);
        transactionId++;

        System.out.printf("Updated balance: PHP %.2f%n",
                balances[source]);

        log("Successful " + type + " transaction for account "
                + accountIds[source] + ", amount PHP "
                + String.format("%.2f", amount));
    }

    // ROLE-BASED PERMISSIONS
    static boolean hasPermission(int user, String type) {

        if (roles[user].equals("USER")) {
            return type.equals("DEPOSIT")
                    || type.equals("WITHDRAWAL")
                    || type.equals("TRANSFER");
        }

        if (roles[user].equals("STAFF")) {
            return type.equals("DEPOSIT");
        }

        return false;
    }

    // ACCOUNT SEARCH
    static int findUsername(String username) {
        for (int i = 0; i < usernames.length; i++) {
            if (usernames[i].equals(username)) {
                return i;
            }
        }
        return -1;
    }

    static int findAccount(int id) {
        for (int i = 0; i < accountIds.length; i++) {
            if (accountIds[i] == id) {
                return i;
            }
        }
        return -1;
    }

    // DUPLICATE CHECK
    static boolean isDuplicate(String key) {
        for (int i = 0; i < keyCount; i++) {
            if (transactionKeys[i].equals(key)) {
                return true;
            }
        }
        return false;
    }

    static void saveKey(String key) {
        if (keyCount < transactionKeys.length) {
            transactionKeys[keyCount] = key;
            keyCount++;
        }
    }

    // BASIC LOGGING
    static void log(String message) {
        if (logCount < logs.length) {
            logs[logCount] = message;
            logCount++;
        }
    }

    // INPUT VALIDATION
    static int readInt() {
        while (true) {
            try {
                return Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.print("Enter a valid whole number: ");
            }
        }
    }

    static double readDouble() {
        while (true) {
            try {
                double amount = Double.parseDouble(
                    sc.nextLine().trim()
                );

                if (Double.isNaN(amount)
                        || Double.isInfinite(amount)) {
                    System.out.print("Enter a valid amount: ");
                    continue;
                }

                return amount;

            } catch (NumberFormatException e) {
                System.out.print("Enter a valid amount: ");
            }
        }
    }
}
