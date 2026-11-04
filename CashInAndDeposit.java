
import java.util.Scanner;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CashInDepositManagement {

    static Scanner sc = new Scanner(System.in);

    static String name = "Alex Santos";
    static String accountID = "C1001";
    static double balance = 6000.00;
    static int transactionID = 1001;
    static int rewardPoints = 0;
    static String history = "";

    static void deposit() {

        System.out.println("==========================================");
        System.out.println("           CASH IN / DEPOSIT");
        System.out.println("==========================================");

        System.out.println("Account Name    : " + name);
        System.out.println("Account ID      : " + accountID);
        System.out.println("Current Balance : PHP " + balance);

        System.out.print("\nEnter deposit amount: PHP ");
        double amount = sc.nextDouble();

        // Check if amount is greater than zero
        if (amount > 0) {

            double oldBalance = balance;
            double newBalance = oldBalance + amount;

            System.out.println("\n==========================================");
            System.out.println("             DEPOSIT DETAILS");
            System.out.println("==========================================");

            System.out.println("Deposit Amount  : PHP " + amount);
            System.out.println("Previous Balance: PHP " + oldBalance);
            System.out.println("New Balance     : PHP " + newBalance);

            // Ask for confirmation
            System.out.println("\n[1] Confirm Deposit");
            System.out.println("[2] Cancel Deposit");

            System.out.print("Enter your choice: ");
            int choice = sc.nextInt();

            if (choice == 1) {

                // Update savings balance
                balance = newBalance;

                // Generate unique transaction ID
                String id = "T" + transactionID;
                transactionID++;

                // Record date and transaction type
                String date = LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                history += id + " DEPOSIT PHP " + amount
                        + " " + date + " SUCCESSFUL\n";

                // Award reward points if eligible
                if (amount >= 500) {
                    rewardPoints += 10;
                }

                // Display transaction details
                System.out.println("\n==========================================");
                System.out.println("          DEPOSIT SUCCESSFUL!");
                System.out.println("==========================================");

                System.out.println("Account Name    : " + name);
                System.out.println("Account ID      : " + accountID);
                System.out.println("Transaction ID  : " + id);
                System.out.println("Transaction Type: DEPOSIT");
                System.out.println("Date            : " + date);
                System.out.println("Deposit Amount  : PHP " + amount);
                System.out.println("Previous Balance: PHP " + oldBalance);
                System.out.println("New Balance     : PHP " + balance);
                System.out.println("Reward Points   : " + rewardPoints);
                System.out.println("Status          : SUCCESSFUL");

                System.out.println("==========================================");

            } else if (choice == 2) {

                // Cancel deposit
                System.out.println("\n==========================================");
                System.out.println("             DEPOSIT CANCELLED");
                System.out.println("==========================================");

                System.out.println("Your balance has not changed.");
                System.out.println("Current Balance: PHP " + balance);

            } else {
                System.out.println("Invalid choice.");
                System.out.println("Deposit cancelled.");
                System.out.println("Your balance has not changed.");
            }

        } else {

            // Display error for invalid amount
            System.out.println("\n==========================================");
            System.out.println("              DEPOSIT FAILED");
            System.out.println("==========================================");

            System.out.println("Error: Amount must be greater than zero.");
            System.out.println("Your balance has not changed.");
            System.out.println("Current Balance: PHP " + balance);
        }
    }

    public static void main(String[] args) {

        char choice;

        do {
            deposit();

            System.out.print("\nMake another deposit? (Y/N): ");
            choice = sc.next().charAt(0);

        } while (choice == 'Y' || choice == 'y');

        System.out.println("\nThank you for using BancoFides!");
    }
}
