package src;

import java.util.Scanner;

public class Dashboard {

    private final Scanner scanner;

    public Dashboard(Scanner scanner) {
        this.scanner = scanner;
    }

    // ==========================================
    // CUSTOMER DASHBOARD
    // ==========================================

    public void customerDashboard(
            String fullName,
            String accountID,
            String username,
            double balance,
            double totalDeposits,
            int rewardPoints) {

        int choice;

        do {
            System.out.println("\n==================================================");
            System.out.println("          SAVINGS MANAGEMENT SYSTEM");
            System.out.println("                    CUSTOMER");
            System.out.println("==================================================");
            System.out.println("Welcome, " + fullName + "!");
            System.out.println();
            System.out.println("Account ID : " + accountID);
            System.out.println("Username   : " + username);

            System.out.println("--------------------------------------------------");
            System.out.println("                 ACCOUNT SUMMARY");
            System.out.println("--------------------------------------------------");
            System.out.printf("Available Balance : PHP %,.2f%n", balance);
            System.out.printf("Total Deposits    : PHP %,.2f%n", totalDeposits);
            System.out.println("Reward Points     : " + rewardPoints);

            System.out.println("--------------------------------------------------");
            System.out.println("                  CUSTOMER MENU");
            System.out.println("--------------------------------------------------");
            System.out.println("[1] Cash In / Deposit");
            System.out.println("[2] Transactions");
            System.out.println("[3] Interest Growth Calculator");
            System.out.println("[4] Save-to-Earn Rewards");
            System.out.println("[5] Transaction History");
            System.out.println("[6] Account Information");
            System.out.println("[7] Logout");
            System.out.println("==================================================");

            choice = readChoice(1, 7);

            switch (choice) {

                case 1:
                    showMessage(
                        "Cash In / Deposit",
                        "Deposit functionality is not connected yet."
                    );
                    break;

                case 2:
                    showMessage(
                        "Transactions",
                        "Transaction functions are not connected yet."
                    );
                    break;

                case 3:
                    showMessage(
                        "Interest Growth Calculator",
                        "Interest calculator is not connected yet."
                    );
                    break;

                case 4:
                    showMessage(
                        "Save-to-Earn Rewards",
                        "Rewards functionality is not connected yet."
                    );
                    break;

                case 5:
                    showMessage(
                        "Transaction History",
                        "No transaction history is connected yet."
                    );
                    break;

                case 6:
                    showCustomerInformation(
                        fullName,
                        accountID,
                        username,
                        balance,
                        totalDeposits,
                        rewardPoints
                    );
                    break;

                case 7:
                    System.out.println("\nLogging out...");
                    System.out.println("Returning to the main menu.");
                    break;
            }

        } while (choice != 7);
    }

    // ==========================================
    // STAFF DASHBOARD
    // ==========================================

    // Keeps compatibility with existing calls.
    public void staffDashboard() {
        staffDashboard("S1001", "Staff Member");
    }

    // Displays the actual staff account details.
    public void staffDashboard(String staffID, String fullName) {

        int choice;

        do {
            System.out.println("\n==================================================");
            System.out.println("          SAVINGS MANAGEMENT SYSTEM");
            System.out.println("                      STAFF");
            System.out.println("==================================================");
            System.out.println("Welcome, " + fullName + "!");
            System.out.println();
            System.out.println("Staff ID : " + staffID);

            System.out.println("--------------------------------------------------");
            System.out.println("                  STAFF SUMMARY");
            System.out.println("--------------------------------------------------");
            System.out.println("Total Customers       : Not connected");
            System.out.println("Pending Deposits      : Not connected");
            System.out.println("Transactions Today    : Not connected");
            System.out.println("Reported Issues       : Not connected");

            System.out.println("--------------------------------------------------");
            System.out.println("                    STAFF MENU");
            System.out.println("--------------------------------------------------");
            System.out.println("[1] Search Customer Account");
            System.out.println("[2] Review Transactions");
            System.out.println("[3] Verify Pending Deposits");
            System.out.println("[4] View Transaction History");
            System.out.println("[5] Report Transaction Issue");
            System.out.println("[6] Logout");
            System.out.println("==================================================");

            choice = readChoice(1, 6);

            switch (choice) {

                case 1:
                    showMessage(
                        "Search Customer Account",
                        "Customer search is not connected yet."
                    );
                    break;

                case 2:
                    showMessage(
                        "Review Transactions",
                        "Transaction review is not connected yet."
                    );
                    break;

                case 3:
                    showMessage(
                        "Verify Pending Deposits",
                        "Deposit verification is not connected yet."
                    );
                    break;

                case 4:
                    showMessage(
                        "Transaction History",
                        "Transaction history is not connected yet."
                    );
                    break;

                case 5:
                    showMessage(
                        "Report Transaction Issue",
                        "Issue reporting is not connected yet."
                    );
                    break;

                case 6:
                    System.out.println("\nStaff logged out successfully.");
                    break;
            }

        } while (choice != 6);
    }

    // ==========================================
    // ADMIN DASHBOARD
    // ==========================================

    // Keeps compatibility with existing calls.
    public void adminDashboard() {
        adminDashboard("A1001", "System Administrator");
    }

    // Displays the actual administrator details.
    public void adminDashboard(String adminID, String fullName) {

        int choice;

        do {
            System.out.println("\n==================================================");
            System.out.println("          SAVINGS MANAGEMENT SYSTEM");
            System.out.println("                     ADMIN");
            System.out.println("==================================================");
            System.out.println("Welcome, " + fullName + "!");
            System.out.println();
            System.out.println("Admin ID : " + adminID);

            System.out.println("--------------------------------------------------");
            System.out.println("                 SYSTEM OVERVIEW");
            System.out.println("--------------------------------------------------");
            System.out.println("Total Customers       : Not connected");
            System.out.println("Total Staff           : Not connected");
            System.out.println("Total Transactions    : Not connected");
            System.out.println("Flagged Activities    : Not connected");

            System.out.println("--------------------------------------------------");
            System.out.println("                    ADMIN MENU");
            System.out.println("--------------------------------------------------");
            System.out.println("[1] Manage Customer Accounts");
            System.out.println("[2] Manage Staff Accounts");
            System.out.println("[3] View All Transactions");
            System.out.println("[4] Monitor Transaction Security");
            System.out.println("[5] Manage Rewards and Interest Settings");
            System.out.println("[6] View System Activity Logs");
            System.out.println("[7] Logout");
            System.out.println("==================================================");

            choice = readChoice(1, 7);

            switch (choice) {

                case 1:
                    showMessage(
                        "Manage Customer Accounts",
                        "Customer account management is not connected yet."
                    );
                    break;

                case 2:
                    // Open the staff account management menu.
                    UserAuthentication auth =
                        new UserAuthentication(scanner);

                    auth.manageStaffAccounts();
                    break;

                case 3:
                    showMessage(
                        "View All Transactions",
                        "Transaction records are not connected yet."
                    );
                    break;

                case 4:
                    showMessage(
                        "Transaction Security",
                        "Security monitoring is not connected yet."
                    );
                    break;

                case 5:
                    showMessage(
                        "Rewards and Interest Settings",
                        "Settings management is not connected yet."
                    );
                    break;

                case 6:
                    showMessage(
                        "System Activity Logs",
                        "Activity logs are not connected yet."
                    );
                    break;

                case 7:
                    System.out.println(
                        "\nAdministrator logged out successfully."
                    );
                    break;
            }

        } while (choice != 7);
    }

    // ==========================================
    // CUSTOMER ACCOUNT INFORMATION
    // ==========================================

    private void showCustomerInformation(
            String fullName,
            String accountID,
            String username,
            double balance,
            double totalDeposits,
            int rewardPoints) {

        System.out.println("\n==================================================");
        System.out.println("               ACCOUNT INFORMATION");
        System.out.println("==================================================");
        System.out.println("Full Name     : " + fullName);
        System.out.println("Account ID    : " + accountID);
        System.out.println("Username      : " + username);
        System.out.println("Role          : Customer");
        System.out.printf("Balance       : PHP %,.2f%n", balance);
        System.out.printf("Total Deposits: PHP %,.2f%n", totalDeposits);
        System.out.println("Reward Points : " + rewardPoints);
        System.out.println("==================================================");

        System.out.println("Press Enter to return to the dashboard.");
        scanner.nextLine();
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
                    "Invalid choice! Enter a number from "
                    + min + " to " + max + "."
                );

            } catch (NumberFormatException e) {
                System.out.println(
                    "Invalid input! Please enter a number."
                );
            }
        }
    }

    // ==========================================
    // TEMPORARY FEATURE MESSAGE
    // ==========================================

    private void showMessage(String title, String message) {

        System.out.println("\n--------------------------------------------------");
        System.out.println(title.toUpperCase());
        System.out.println("--------------------------------------------------");
        System.out.println(message);
        System.out.println("--------------------------------------------------");
        System.out.println("Press Enter to return to the dashboard.");

        scanner.nextLine();
    }
}