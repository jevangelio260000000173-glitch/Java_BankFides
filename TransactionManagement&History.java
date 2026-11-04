
import java.util.Scanner;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TransactionHistory {

    static Scanner sc = new Scanner(System.in);

    static String name = "Alex Santos";
    static String accountID = "C1001";
    static double balance = 6000.00;
    static int transactionID = 1001;

    static ArrayList<String> history = new ArrayList<String>();
    static ArrayList<String> customerRecords = new ArrayList<String>();
    static ArrayList<String> staffRecords = new ArrayList<String>();

    static ArrayList<String> transactionTypes = new ArrayList<String>();
    static ArrayList<String> transactionDates = new ArrayList<String>();

    static void recordTransaction(String type, double amount) {

        String id = "T" + transactionID;
        transactionID++;

        String date = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        history.add(id + " | " + type + " | PHP " + amount
                + " | " + date + " | SUCCESSFUL");

        customerRecords.add(accountID);
        staffRecords.add("S1001");
        transactionTypes.add(type);
        transactionDates.add(date);

    }

    
    static void withdraw() {

        System.out.println("\n==========================================");
        System.out.println("            CASH OUT / WITHDRAWAL");
        System.out.println("==========================================");

        System.out.println("Account Name    : " + name);
        System.out.println("Account ID      : " + accountID);
        System.out.println("Current Balance : PHP " + balance);

        System.out.print("\nEnter withdrawal amount: PHP ");
        double amount = sc.nextDouble();

        
        if (amount > 0 && amount <= balance) {

            double oldBalance = balance;
            double newBalance = oldBalance - amount;

            System.out.println("\nWithdrawal Amount: PHP " + amount);
            System.out.println("Remaining Balance: PHP " + newBalance);

            System.out.println("\n[1] Confirm Withdrawal");
            System.out.println("[2] Cancel Withdrawal");

            System.out.print("Enter your choice: ");
            int choice = sc.nextInt();

            if (choice == 1) {

                balance = newBalance;

                recordTransaction("WITHDRAWAL", amount);

                String id = "T" + (transactionID - 1);

                System.out.println("\n==========================================");
                System.out.println("          WITHDRAWAL SUCCESSFUL!");
                System.out.println("==========================================");

                System.out.println("Transaction ID  : " + id);
                System.out.println("Previous Balance: PHP " + oldBalance);
                System.out.println("Withdrawal Amount: PHP " + amount);
                System.out.println("New Balance     : PHP " + balance);
                System.out.println("Status          : SUCCESSFUL");

            } else {

                System.out.println("\nWithdrawal cancelled.");
                System.out.println("Your balance has not changed.");
            }

        } else {

            System.out.println("\n==========================================");
            System.out.println("           WITHDRAWAL FAILED");
            System.out.println("==========================================");

            if (amount <= 0) {
                System.out.println("Error: Amount must be greater than zero.");
            } else {
                System.out.println("Error: Insufficient balance.");
            }

            System.out.println("Current Balance: PHP " + balance);
        }
    }

    
    static void transfer() {

        System.out.println("\n==========================================");
        System.out.println("               TRANSFER FUNDS");
        System.out.println("==========================================");

        System.out.println("Sender Account  : " + accountID);
        System.out.println("Available Balance: PHP " + balance);

        System.out.print("\nEnter recipient account ID: ");
        String recipient = sc.next();

        System.out.print("Enter transfer amount: PHP ");
        double amount = sc.nextDouble();

        // Check amount, balance, and recipient
        if (amount > 0 && amount <= balance
                && recipient.matches("C[0-9]+")
                && !recipient.equals(accountID)) {

            double oldBalance = balance;
            double newBalance = oldBalance - amount;

            System.out.println("\n==========================================");
            System.out.println("             TRANSFER DETAILS");
            System.out.println("==========================================");

            System.out.println("Sender Account   : " + accountID);
            System.out.println("Recipient Account: " + recipient);
            System.out.println("Transfer Amount  : PHP " + amount);

            System.out.println("\n[1] Confirm Transfer");
            System.out.println("[2] Cancel Transfer");

            System.out.print("Enter your choice: ");
            int choice = sc.nextInt();

            if (choice == 1) {

                balance = newBalance;

                recordTransaction("TRANSFER", amount);

                String id = "T" + (transactionID - 1);

                System.out.println("\n==========================================");
                System.out.println("           TRANSFER SUCCESSFUL!");
                System.out.println("==========================================");

                System.out.println("Transaction ID  : " + id);
                System.out.println("Sender Account  : " + accountID);
                System.out.println("Recipient       : " + recipient);
                System.out.println("Previous Balance: PHP " + oldBalance);
                System.out.println("Transfer Amount : PHP " + amount);
                System.out.println("New Balance     : PHP " + balance);
                System.out.println("Status          : SUCCESSFUL");

            } else {

                System.out.println("\nTransfer cancelled.");
                System.out.println("Your balance has not changed.");
            }

        } else {

            System.out.println("\n==========================================");
            System.out.println("             TRANSFER FAILED");
            System.out.println("==========================================");

            if (amount <= 0) {
                System.out.println("Error: Amount must be greater than zero.");
            } else if (amount > balance) {
                System.out.println("Error: Insufficient balance.");
            } else {
                System.out.println("Error: Invalid recipient account.");
            }

            System.out.println("Your balance has not changed.");
        }
    }

    
    static void viewHistory() {

        System.out.println("\n==========================================");
        System.out.println("            TRANSACTION HISTORY");
        System.out.println("==========================================");

        System.out.println("[1] Filter by Date");
        System.out.println("[2] Filter by Transaction Type");
        System.out.println("[3] View All Transactions");

        System.out.print("\nEnter your choice: ");
        int filter = sc.nextInt();
        sc.nextLine();

        String filterValue = "";

        if (filter == 1) {

            System.out.print("Enter date (yyyy-MM-dd): ");
            filterValue = sc.nextLine();

        } else if (filter == 2) {

            System.out.println("DEPOSIT / WITHDRAWAL / TRANSFER");
            System.out.print("Enter transaction type: ");
            filterValue = sc.nextLine().toUpperCase();

        } else if (filter != 3) {

            System.out.println("Invalid filter choice.");
            return;
        }

        System.out.println("\nFilter applied successfully.");
        System.out.println("Retrieving transaction records...");

        
        checkUserRole(filter, filterValue);
    }

    
    static void checkUserRole(int filter, String filterValue) {

        System.out.println("\n==========================================");
        System.out.println("              CHECK USER ROLE");
        System.out.println("==========================================");

        System.out.println("[1] Customer");
        System.out.println("[2] Staff");
        System.out.println("[3] Admin");

        System.out.print("Enter your role: ");
        int role = sc.nextInt();

        System.out.print("Enter your user ID: ");
        String userID = sc.next();

        if (role == 1) {

            System.out.println("\nCUSTOMER");
            System.out.println("Showing personal transaction history...");

        } else if (role == 2) {

            System.out.println("\nSTAFF");
            System.out.println("Showing assigned customer records...");

        } else if (role == 3) {

            System.out.println("\nADMIN");
            System.out.println("Showing system-wide transaction records...");

        } else {

            System.out.println("Invalid user role.");
            return;
        }

        displayHistory(role, userID, filter, filterValue);
    }

    
    static void displayHistory(int role, String userID,
                               int filter, String filterValue) {

        System.out.println("\n==========================================");
        System.out.println("          TRANSACTION RECORDS");
        System.out.println("==========================================");

        System.out.println("ID | TYPE | AMOUNT | DATE | STATUS");
        System.out.println("------------------------------------------");

        boolean found = false;

        for (int i = 0; i < history.size(); i++) {

            boolean authorized = false;

            
            if (role == 1 && customerRecords.get(i).equals(userID)) {
                authorized = true;
            }

            
            else if (role == 2 && staffRecords.get(i).equals(userID)) {
                authorized = true;
            }

            
            else if (role == 3) {
                authorized = true;
            }

            
            boolean matchesFilter = true;

            if (filter == 1) {

                matchesFilter = transactionDates.get(i).startsWith(
                    filterValue);

            } else if (filter == 2) {

                matchesFilter = transactionTypes.get(i).equals(
                    filterValue);
            }

            if (authorized && matchesFilter) {

                System.out.println(history.get(i));
                found = true;
            }
        }

        if (!found) {
            System.out.println("No matching transaction records found.");
        }

        System.out.println("------------------------------------------");
    }

    
    static void displayMenu() {

        int choice;

        do {

            System.out.println("\n==========================================");
            System.out.println("          TRANSACTION MANAGEMENT");
            System.out.println("==========================================");

            System.out.println("Account Name    : " + name);
            System.out.println("Account ID      : " + accountID);
            System.out.println("Current Balance : PHP " + balance);

            System.out.println("\n[1] Withdraw");
            System.out.println("[2] Transfer");
            System.out.println("[3] View History");
            System.out.println("[4] Return to Dashboard");

            System.out.print("\nEnter your choice: ");
            choice = sc.nextInt();

            if (choice == 1) {

                withdraw();
                checkUserRole(3, "");

            } else if (choice == 2) {

                transfer();
                checkUserRole(3, "");

            } else if (choice == 3) {

                viewHistory();

            } else if (choice == 4) {

                System.out.println("Returning to dashboard...");

            } else {

                System.out.println("Invalid choice.");
            }

        } while (choice != 4);
    }

    public static void main(String[] args) {

        displayMenu();

    }
}
