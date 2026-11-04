package src;

import java.util.List;
import java.util.Scanner;

public class Dashboard {

    private final Scanner scanner;
    private final FileDatabase database;

    public Dashboard(Scanner scanner) {
        this(scanner, new FileDatabase());
    }

    public Dashboard(Scanner scanner, FileDatabase database) {
        this.scanner = scanner;
        this.database = database;
    }

    // =========================================================
    // CUSTOMER DASHBOARD
    // =========================================================

    public void customerDashboard(
            String fullName,
            String accountID,
            String username,
            double balance,
            double totalDeposits,
            int rewardPoints) {

        int choice;

        do {
            double[] latestData = refreshCustomerData(accountID);

            if (latestData != null) {
                balance = latestData[0];
                totalDeposits = latestData[1];
                rewardPoints = (int) latestData[2];
            }

            System.out.println("\n==================================================");
            System.out.println("              BANCOFIDES DASHBOARD");
            System.out.println("==================================================");
            System.out.println("Welcome, " + fullName + "!");
            System.out.println("Account ID        : " + accountID);
            System.out.println("Username          : " + username);
            System.out.println("--------------------------------------------------");

            System.out.printf(
                    "Available Balance : PHP %,.2f%n",
                    balance
            );

            System.out.printf(
                    "Total Deposits    : PHP %,.2f%n",
                    totalDeposits
            );

            System.out.println("==================================================");
            System.out.println("                  CUSTOMER MENU");
            System.out.println("==================================================");
            System.out.println("[1] Cash In / Deposit");
            System.out.println("[2] Transactions");
            System.out.println("[3] Interest Growth Calculator");
            System.out.println("[4] Transaction History");
            System.out.println("[5] Account Information");
            System.out.println("[6] Report Transaction Issue");
            System.out.println("[7] My Issue Reports");
            System.out.println("[8] Logout");
            System.out.println("==================================================");

            choice = readChoice(1, 8);

            switch (choice) {

                case 1: {

                    DepositManager depositManager =
                            new DepositManager(scanner, database);

                    DepositManager.DepositResult result =
                            depositManager.deposit(accountID);

                    if (result != null) {
                        balance = result.balance;
                        totalDeposits = result.totalDeposits;
                    }

                    double[] depositData =
                            refreshCustomerData(accountID);

                    if (depositData != null) {
                        balance = depositData[0];
                        totalDeposits = depositData[1];
                    }

                    break;
                }

                case 2: {

                    TransactionHistory transactionHistory =
                            new TransactionHistory(scanner, database);

                    System.out.println("\n==================================================");
                    System.out.println("                  TRANSACTIONS");
                    System.out.println("==================================================");
                    System.out.println("[1] Withdraw");
                    System.out.println("[2] Transfer");
                    System.out.println("[3] Return to Dashboard");
                    System.out.println("==================================================");

                    int transactionChoice = readChoice(1, 3);

                    switch (transactionChoice) {

                        case 1:
                            transactionHistory.withdraw(accountID);
                            break;

                        case 2:
                            transactionHistory.transfer(accountID);
                            break;

                        case 3:
                            break;
                    }

                    double[] transactionData =
                            refreshCustomerData(accountID);

                    if (transactionData != null) {
                        balance = transactionData[0];
                        totalDeposits = transactionData[1];
                    }

                    break;
                }

                case 3: {

                    InterestGrowthCalculator calculator =
                            new InterestGrowthCalculator(scanner);

                    calculator.calculateInterest();
                    break;
                }

                case 4: {

                    TransactionHistory historyManager =
                            new TransactionHistory(scanner, database);

                    historyManager.showHistory(accountID);
                    break;
                }

                case 5: {

                    accountInformationMenu(
                            fullName,
                            accountID,
                            username
                    );

                    double[] accountData =
                            refreshCustomerData(accountID);

                    if (accountData != null) {
                        balance = accountData[0];
                        totalDeposits = accountData[1];
                    }

                    break;
                }

                case 6: {

                    TransactionIssueManager issueManager =
                            new TransactionIssueManager(scanner, database);

                    issueManager.reportIssue(accountID);
                    break;
                }

                case 7: {

                    TransactionIssueManager issueManager =
                            new TransactionIssueManager(scanner, database);

                    issueManager.viewMyIssueReports(accountID);
                    break;
                }

                case 8:

                    System.out.println("\n==================================================");
                    System.out.println("              LOGGING OUT");
                    System.out.println("==================================================");
                    System.out.println(
                            "Thank you for using BancoFides, "
                            + fullName + "!"
                    );
                    System.out.println(
                            "You have been logged out successfully."
                    );
                    System.out.println("==================================================");

                    break;
            }

        } while (choice != 8);
    }

    // =========================================================
    // ACCOUNT INFORMATION MENU
    // =========================================================

    private void accountInformationMenu(
            String fullName,
            String accountID,
            String username) {

        int choice;

        do {
            System.out.println("\n==================================================");
            System.out.println("              ACCOUNT INFORMATION");
            System.out.println("==================================================");
            System.out.println("[1] View Account Information");
            System.out.println("[2] Save-to-Earn Rewards");
            System.out.println("[3] Return to Dashboard");
            System.out.println("==================================================");

            choice = readChoice(1, 3);

            switch (choice) {

                case 1: {

                    double[] latestData =
                            refreshCustomerData(accountID);

                    if (latestData != null) {
                        showCustomerInformation(
                                fullName,
                                accountID,
                                username,
                                latestData[0],
                                latestData[1]
                        );
                    }

                    break;
                }

                case 2: {

                    RewardsManager rewardsManager =
                            new RewardsManager(scanner, database);

                    rewardsManager.open(accountID);
                    break;
                }

                case 3:

                    System.out.println(
                            "\nReturning to Customer Dashboard..."
                    );

                    break;
            }

        } while (choice != 3);
    }

    // =========================================================
    // REFRESH CUSTOMER DATA
    // =========================================================

    private double[] refreshCustomerData(String accountID) {

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

                try {
                    double balance =
                            Double.parseDouble(fields[4]);

                    double totalDeposits =
                            Double.parseDouble(fields[5]);

                    int rewardPoints =
                            Integer.parseInt(fields[6]);

                    return new double[]{
                        balance,
                        totalDeposits,
                        rewardPoints
                    };

                } catch (NumberFormatException e) {

                    System.out.println(
                            "Unable to refresh customer account information."
                    );

                    return null;
                }
            }
        }

        System.out.println("Customer account could not be found.");
        return null;
    }

    // =========================================================
    // CUSTOMER ACCOUNT INFORMATION DISPLAY
    // =========================================================

    private void showCustomerInformation(
            String fullName,
            String accountID,
            String username,
            double balance,
            double totalDeposits) {

        System.out.println("\n==================================================");
        System.out.println("               ACCOUNT INFORMATION");
        System.out.println("==================================================");
        System.out.println("Full Name         : " + fullName);
        System.out.println("Account ID        : " + accountID);
        System.out.println("Username          : " + username);

        System.out.printf(
                "Available Balance : PHP %,.2f%n",
                balance
        );

        System.out.printf(
                "Total Deposits    : PHP %,.2f%n",
                totalDeposits
        );

        System.out.println(
                "Account Status    : "
                + database.getAccountStatus(accountID)
        );

        System.out.println("==================================================");

        pause();
    }

    // =========================================================
    // STAFF DASHBOARD
    // =========================================================

    public void staffDashboard() {
        staffDashboard("S1001", "Staff Member");
    }

    public void staffDashboard(String staffID, String fullName) {

        int choice;

        do {
            List<String> customers = database.readCustomers();
            List<String> transactions = database.readTransactions();

            int pendingDeposits = database.countPendingDeposits();

            int pendingIssues =
                    database.countPendingTransactionIssues();

            System.out.println("\n==================================================");
            System.out.println("               BANCOFIDES STAFF");
            System.out.println("==================================================");
            System.out.println("Welcome, " + fullName + "!");
            System.out.println("Staff ID: " + staffID);
            System.out.println("--------------------------------------------------");
            System.out.println("              STAFF DASHBOARD");
            System.out.println("--------------------------------------------------");

            System.out.println(
                    "Total Customers       : " + countRecords(customers)
            );

            System.out.println(
                    "Total Transactions    : " + countRecords(transactions)
            );

            System.out.println(
                    "Pending Deposits      : " + pendingDeposits
            );

            System.out.println(
                    "Pending Issue Reports : " + pendingIssues
            );

            System.out.println("==================================================");
            System.out.println("                   STAFF MENU");
            System.out.println("==================================================");
            System.out.println("[1] Search Customer Account");
            System.out.println("[2] Review Transactions");
            System.out.println("[3] Verify Pending Deposits");
            System.out.println("[4] View Transaction History");
            System.out.println("[5] Report Transaction Issue");
            System.out.println("[6] Logout");
            System.out.println("==================================================");

            choice = readChoice(1, 6);

            switch (choice) {

                case 1: {

                    AccountManager accountManager =
                            new AccountManager(scanner, database);

                    accountManager.staffCustomerAccountSearch();
                    break;
                }

                case 2:

                    displayAllTransactions("REVIEW TRANSACTIONS");
                    break;

                case 3: {

                    PendingDepositManager pendingDepositManager =
                            new PendingDepositManager(scanner, database);

                    pendingDepositManager.staffMenu(staffID);
                    break;
                }

                case 4: {

                    System.out.println("\n==================================================");
                    System.out.println("            CUSTOMER TRANSACTION HISTORY");
                    System.out.println("==================================================");

                    System.out.print("Enter Customer ID: ");
                    String customerID = scanner.nextLine().trim();

                    if (customerID.isEmpty()) {
                        System.out.println("Customer ID cannot be empty.");
                        pause();
                        break;
                    }

                    if (!customerExists(customerID)) {
                        System.out.println("Customer account not found.");
                        pause();
                        break;
                    }

                    displayCustomerTransactions(customerID);
                    break;
                }

                case 5: {

                    TransactionIssueManager issueManager =
                            new TransactionIssueManager(scanner, database);

                    issueManager.staffIssueMenu(staffID);
                    break;
                }

                case 6:

                    System.out.println(
                            "\nStaff logged out successfully."
                    );

                    break;
            }

        } while (choice != 6);
    }

    // =========================================================
    // ADMIN DASHBOARD
    // =========================================================

    public void adminDashboard() {
        adminDashboard("A1001", "System Administrator");
    }

    public void adminDashboard(String adminID, String fullName) {

        int choice;

        do {
            List<String> customers = database.readCustomers();
            List<String> staff = database.readStaff();
            List<String> transactions = database.readTransactions();
            List<String> activityLogs = database.readActivityLogs();

            System.out.println("\n==================================================");
            System.out.println("              BANCOFIDES ADMIN");
            System.out.println("==================================================");
            System.out.println("Welcome, " + fullName + "!");
            System.out.println("Admin ID: " + adminID);
            System.out.println("--------------------------------------------------");
            System.out.println("              ADMIN DASHBOARD");
            System.out.println("--------------------------------------------------");

            System.out.println(
                    "Total Customers       : " + countRecords(customers)
            );

            System.out.println(
                    "Total Staff           : " + countRecords(staff)
            );

            System.out.println(
                    "Total Transactions    : " + countRecords(transactions)
            );

            System.out.println(
                    "Total Activity Logs   : " + countRecords(activityLogs)
            );

            System.out.println("==================================================");
            System.out.println("                    ADMIN MENU");
            System.out.println("==================================================");
            System.out.println("[1] Manage Customer Accounts");
            System.out.println("[2] Manage Staff Accounts");
            System.out.println("[3] View All Transactions");
            System.out.println("[4] Manage Admin Accounts");
            System.out.println("[5] View Activity Logs");
            System.out.println("[6] Logout");
            System.out.println("==================================================");

            choice = readChoice(1, 6);

            switch (choice) {

                // MANAGE CUSTOMER ACCOUNTS
                case 1: {

                    AccountManager accountManager =
                            new AccountManager(scanner, database);

                    accountManager.manageCustomerAccounts();
                    break;
                }

                // MANAGE STAFF ACCOUNTS
                case 2: {

                    UserAuthentication auth =
                            new UserAuthentication(scanner, database);

                    auth.manageStaffAccounts();
                    break;
                }

                // VIEW ALL TRANSACTIONS
                case 3:

                    displayAllTransactions("ALL TRANSACTIONS");
                    break;

                // MANAGE ADMIN ACCOUNTS
                case 4: {

                    AccountManager accountManager =
                            new AccountManager(scanner, database);

                    accountManager.manageAdminAccounts();
                    break;
                }

                // VIEW ACTIVITY LOGS
                case 5:

                    displayActivityLogs();
                    break;

                // LOGOUT
                case 6:

                    System.out.println(
                            "\nAdmin logged out successfully."
                    );

                    break;
            }

        } while (choice != 6);
    }

    // =========================================================
    // DISPLAY ALL TRANSACTIONS
    // =========================================================

    private void displayAllTransactions(String title) {

        List<String> transactions = database.readTransactions();

        System.out.println("\n==================================================");
        System.out.println("                 " + title);
        System.out.println("==================================================");

        if (transactions == null || transactions.isEmpty()) {
            System.out.println("No transactions found.");
            pause();
            return;
        }

        int count = 0;

        for (String record : transactions) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            System.out.println("------------------------------------------");

            if (fields.length >= 8) {

                System.out.println("Transaction ID  : " + fields[0]);
                System.out.println("Account ID      : " + fields[1]);
                System.out.println("Transaction Type: " + fields[2]);
                System.out.println("Amount          : PHP " + fields[3]);
                System.out.println("Previous Balance: PHP " + fields[4]);
                System.out.println("New Balance     : PHP " + fields[5]);
                System.out.println("Date and Time   : " + fields[6]);
                System.out.println("Status          : " + fields[7]);

                if (fields.length >= 9) {
                    System.out.println("Other Account   : " + fields[8]);
                }

            } else {
                System.out.println("Transaction Record: " + record);
            }

            count++;
        }

        System.out.println("------------------------------------------");
        System.out.println("Total Records: " + count);
        System.out.println("==================================================");

        pause();
    }

    // =========================================================
    // DISPLAY CUSTOMER TRANSACTIONS
    // =========================================================

    private void displayCustomerTransactions(String customerID) {

        List<String> transactions = database.readTransactions();

        System.out.println("\n==================================================");
        System.out.println("             TRANSACTION HISTORY");
        System.out.println("==================================================");
        System.out.println("Customer ID: " + customerID);

        int count = 0;

        if (transactions != null) {

            for (String record : transactions) {

                if (record == null || record.trim().isEmpty()) {
                    continue;
                }

                String[] fields = record.split("\\|", -1);

                if (fields.length < 2) {
                    continue;
                }

                boolean belongsToCustomer =
                        fields[1].equalsIgnoreCase(customerID);

                if (belongsToCustomer) {

                    System.out.println("------------------------------------------");

                    if (fields.length >= 8) {

                        System.out.println("Transaction ID  : " + fields[0]);
                        System.out.println("Transaction Type: " + fields[2]);
                        System.out.println("Amount          : PHP " + fields[3]);
                        System.out.println("Previous Balance: PHP " + fields[4]);
                        System.out.println("New Balance     : PHP " + fields[5]);
                        System.out.println("Date and Time   : " + fields[6]);
                        System.out.println("Status          : " + fields[7]);

                        if (fields.length >= 9) {
                            System.out.println("Other Account   : " + fields[8]);
                        }

                    } else {
                        System.out.println(record);
                    }

                    count++;
                }
            }
        }

        if (count == 0) {
            System.out.println(
                    "No transactions found for this customer."
            );
        }

        System.out.println("------------------------------------------");
        System.out.println("Total Transactions: " + count);
        System.out.println("==================================================");

        pause();
    }

    // =========================================================
    // CHECK IF CUSTOMER EXISTS
    // =========================================================

    private boolean customerExists(String customerID) {

        List<String> customers = database.readCustomers();

        if (customers == null) {
            return false;
        }

        for (String record : customers) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length == 7
                    && fields[0].equalsIgnoreCase(customerID)) {
                return true;
            }
        }

        return false;
    }

    // =========================================================
    // DISPLAY ACTIVITY LOGS
    // =========================================================

    private void displayActivityLogs() {

        List<String> activityLogs = database.readActivityLogs();

        System.out.println("\n==================================================");
        System.out.println("                  ACTIVITY LOGS");
        System.out.println("==================================================");

        if (activityLogs == null || activityLogs.isEmpty()) {
            System.out.println("No activity logs found.");
            pause();
            return;
        }

        int count = 0;

        for (String log : activityLogs) {

            if (log == null || log.trim().isEmpty()) {
                continue;
            }

            System.out.println((count + 1) + ". " + log);
            count++;
        }

        System.out.println("--------------------------------------------------");
        System.out.println("Total Activity Logs: " + count);
        System.out.println("==================================================");

        pause();
    }

    // =========================================================
    // COUNT DATABASE RECORDS
    // =========================================================

    private int countRecords(List<String> records) {

        int count = 0;

        if (records == null) {
            return count;
        }

        for (String record : records) {

            if (record != null && !record.trim().isEmpty()) {
                count++;
            }
        }

        return count;
    }

    // =========================================================
    // READ MENU CHOICE
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
                        "Invalid choice. Please enter a number from "
                        + min + " to " + max + "."
                );

            } catch (NumberFormatException e) {

                System.out.println(
                        "Invalid input. Please enter a valid number."
                );
            }
        }
    }

    // =========================================================
    // PAUSE
    // =========================================================

    private void pause() {

        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }
}