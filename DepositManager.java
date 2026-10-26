import java.util.Scanner;


public class BancoFides {
    static Scanner sc = new Scanner(System.in);


    static String name = "Alex Santos";
    static String accountID = "C1001";
    static double balance = 6000.00;
    static int transactionID = 1001;
    static String history = "";


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


                history += id + " DEPOSIT PHP " + amount
                        + " SUCCESSFUL\n";


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


                history += id + " WITHDRAWAL PHP " + amount
                        + " SUCCESSFUL\n";


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


                history += id + " TRANSFER PHP " + amount
                        + " SUCCESSFUL\n";


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
        }
    }


    static void viewHistory() {
        System.out.println("==========================================");
        System.out.println("             TRANSACTION HISTORY");
        System.out.println("==========================================");


        System.out.println("Account ID: " + accountID);
        System.out.println("Account Name: " + name);


        System.out.println("------------------------------------------");
        System.out.println("ID       TYPE          AMOUNT       STATUS");
        System.out.println("------------------------------------------");


        if (history.equals("")) {
            System.out.println("No transaction records found.");
        } else {
            System.out.println(history);
        }


        System.out.println("------------------------------------------");
    }


    static void displayMenu() {
        int choice;


        do {
            System.out.println("\n==========================================");
            System.out.println("           TRANSACTION MANAGEMENT");
            System.out.println("==========================================");


            System.out.println("Account Name: " + name);
            System.out.println("Available Balance: PHP " + balance);


            System.out.println("\n[1] Cash In / Deposit");
            System.out.println("[2] Cash Out / Withdraw");
            System.out.println("[3] Transfer Funds");
            System.out.println("[4] View Transaction History");
            System.out.println("[5] Return to Dashboard");


            System.out.print("Enter your choice: ");
            choice = sc.nextInt();


            if (choice == 1) {
                deposit();
            } else if (choice == 2) {
                withdraw();
            } else if (choice == 3) {
                transfer();
            } else if (choice == 4) {
                viewHistory();
            } else if (choice == 5) {
                System.out.println("Returning to dashboard...");
            } else {
                System.out.println("Invalid choice.");
            }


        } while (choice != 5);
    }


    static void dashboard() {
        int choice;


        do {
            System.out.println("\n==========================================");
            System.out.println("               BANCOFIDES");
            System.out.println("==========================================");


            System.out.println("Account Name: " + name);
            System.out.println("Account ID: " + accountID);
            System.out.println("Savings Balance: PHP " + balance);


            System.out.println("\n[1] Cash In / Deposit");
            System.out.println("[2] Transaction Management");
            System.out.println("[3] Exit");


            System.out.print("Enter your choice: ");
            choice = sc.nextInt();


            if (choice == 1) {
                deposit();
            } else if (choice == 2) {
                displayMenu();
            } else if (choice == 3) {
                System.out.println("Thank you for using BancoFides!");
            } else {
                System.out.println("Invalid choice.");
            }


        } while (choice != 3);
    }


    public static void main(String[] args) {
        dashboard();
    }
}