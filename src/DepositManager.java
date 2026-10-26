package src;


import java.util.Scanner;

public class DepositManager {
    static Scanner sc = new Scanner(System.in);

    static String name = "Alex Santos";
    static String accountID = "C1001";
    static double balance = 6000.00;
    static int transactionID = 1001;
    static String history = "";

    public static void main(String[] args) {
        int option = 0;
        do {
            System.out.println("\n==========================================");
            System.out.println("          WELCOME TO BANCO FIDES          ");
            System.out.println("==========================================");
            System.out.println("[1] Deposit / Cash In");
            System.out.println("[2] Withdraw / Cash Out");
            System.out.println("[3] Transfer Funds");
            System.out.println("[4] Transaction History");
            System.out.println("[5] Exit");
            System.out.print("Choose an option: ");
            
            if (sc.hasNextInt()) {
                option = sc.nextInt();
                switch (option) {
                    case 1 -> deposit();
                    case 2 -> withdraw();
                    case 3 -> transfer();
                    case 4 -> showHistory();
                    case 5 -> System.out.println("\nThank you for using Banco Fides. Goodbye!");
                    default -> System.out.println("Invalid option. Please try again.");
                }
            } else {
                System.out.println("Please enter a valid number.");
                sc.next(); // Clear invalid input
            }
        } while (option != 5);
    }

    static void deposit() {
        System.out.println("==========================================");
        System.out.println("              CASH IN / DEPOSIT");
        System.out.println("==========================================");

        System.out.println("Account Name  : " + name);
        System.out.println("Account ID    : " + accountID);
        System.out.println("Current Balance: PHP " + balance);

        System.out.print("\nEnter deposit amount: PHP ");
        double amount = sc.nextDouble();

        if (amount > 0) {
            System.out.println("\nDeposit Amount: PHP " + amount);
            System.out.println("==========================================");
            System.out.println("[1] Confirm Deposit");
            System.out.println("[2] Cancel Deposit");
            System.out.print("Enter your choice: ");
            int choice = sc.nextInt();

            if (choice == 1) {
                double oldBalance = balance;
                balance = balance + amount;

                String id = "T" + transactionID;
                transactionID++;

                history += id + " DEPOSIT PHP " + amount + " SUCCESSFUL\n";

                System.out.println("\n==========================================");
                System.out.println("             DEPOSIT SUCCESSFUL!");
                System.out.println("==========================================");

                System.out.println("Account Name   : " + name);
                System.out.println("Account ID     : " + accountID);
                System.out.println("Previous Balance: PHP " + oldBalance);
                System.out.println("Deposit Amount : PHP " + amount);
                System.out.println("New Balance    : PHP " + balance);

                System.out.println("------------------------------------------");
                System.out.println("Transaction ID : " + id);
                System.out.println("Transaction Type: DEPOSIT");
                System.out.println("Status         : SUCCESSFUL");
                System.out.println("Reward Points  : +10");
                System.out.println("------------------------------------------");
            } else {
                System.out.println("Deposit cancelled.");
                System.out.println("Your balance has not changed.");
            }
        } else {
            System.out.println("\n==========================================");
            System.out.println("               DEPOSIT FAILED");
            System.out.println("==========================================");
            System.out.println("Error: Deposit amount must be greater than zero.");
            System.out.println("Your balance has not changed.");
            System.out.println("Current Balance: PHP " + balance);
        }
    }

    static void withdraw() {
        System.out.println("==========================================");
        System.out.println("             CASH OUT / WITHDRAWAL");
        System.out.println("==========================================");

        System.out.println("Account Name: " + name);
        System.out.println("Current Balance: PHP " + balance);

        System.out.print("Enter withdrawal amount: PHP ");
        double amount = sc.nextDouble();

        if (amount > 0 && amount <= balance) {
            System.out.println("\nWithdrawal Amount: PHP " + amount);
            System.out.println("Remaining Balance: PHP " + (balance - amount));

            System.out.println("\n[1] Confirm Withdrawal");
            System.out.println("[2] Cancel Withdrawal");
            System.out.print("Enter your choice: ");
            int choice = sc.nextInt();

            if (choice == 1) {
                double oldBalance = balance;
                balance = balance - amount;

                String id = "T" + transactionID;
                transactionID++;

                history += id + " WITHDRAWAL PHP " + amount + " SUCCESSFUL\n";

                System.out.println("\n==========================================");
                System.out.println("           WITHDRAWAL SUCCESSFUL!");
                System.out.println("==========================================");

                System.out.println("Previous Balance: PHP " + oldBalance);
                System.out.println("Withdrawal Amount: PHP " + amount);
                System.out.println("New Balance: PHP " + balance);
                System.out.println("Transaction ID: " + id);
                System.out.println("Status: SUCCESSFUL");
            } else {
                System.out.println("Withdrawal cancelled.");
                System.out.println("Your balance has not changed.");
            }
        } else {
            System.out.println("\n==========================================");
            System.out.println("             TRANSACTION FAILED");
            System.out.println("==========================================");

            if (amount <= 0) {
                System.out.println("Error: Amount must be greater than zero.");
            } else {
                System.out.println("Error: Insufficient balance.");
            }

            System.out.println("Your balance has not changed.");
            System.out.println("Available Balance: PHP " + balance);
        }
    }

    static void transfer() {
        System.out.println("==========================================");
        System.out.println("               TRANSFER FUNDS");
        System.out.println("==========================================");

        System.out.println("Sender Account: " + accountID);
        System.out.println("Available Balance: PHP " + balance);

        System.out.print("Enter recipient account ID: ");
        String recipient = sc.next();

        System.out.print("Enter transfer amount: PHP ");
        double amount = sc.nextDouble();

        if (amount > 0 && amount <= balance) {
            System.out.println("\n------------------------------------------");
            System.out.println("             TRANSFER DETAILS");
            System.out.println("------------------------------------------");
            System.out.println("Recipient Account: " + recipient);
            System.out.println("Transfer Amount: PHP " + amount);

            System.out.println("\n[1] Confirm Transfer");
            System.out.println("[2] Cancel Transfer");
            System.out.print("Enter your choice: ");
            int choice = sc.nextInt();

            if (choice == 1) {
                double oldBalance = balance;
                balance = balance - amount;

                String id = "T" + transactionID;
                transactionID++;

                history += id + " TRANSFER PHP " + amount + " SUCCESSFUL\n";

                System.out.println("\n==========================================");
                System.out.println("            TRANSFER SUCCESSFUL!");
                System.out.println("==========================================");

                System.out.println("Sender Account: " + accountID);
                System.out.println("Recipient Account: " + recipient);
                System.out.println("Transfer Amount: PHP " + amount);
                System.out.println("Previous Balance: PHP " + oldBalance);
                System.out.println("New Balance: PHP " + balance);
                System.out.println("Transaction ID: " + id);
                System.out.println("Status: SUCCESSFUL");
            } else {
                System.out.println("Transfer cancelled.");
                System.out.println("Your balance has not changed.");
            }
        } else {
            System.out.println("\n==========================================");
            System.out.println("             TRANSFER FAILED");
            System.out.println("==========================================");
            
            if (amount <= 0) {
                System.out.println("Error: Amount must be greater than zero.");
            } else {
                System.out.println("Error: Insufficient balance.");
            }
            System.out.println("Your balance has not changed.");
            System.out.println("Available Balance: PHP " + balance);
        }
    }

    static void showHistory() {

