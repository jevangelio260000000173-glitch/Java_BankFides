
package src;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;

public class RewardsManager {

    private final Scanner scanner;
    private final FileDatabase database;

    public RewardsManager(Scanner scanner, FileDatabase database) {
        this.scanner = scanner;
        this.database = database;
    }

    // ==========================================
    // REWARDS CENTER
    // ==========================================

    public void open(String accountID) {

        int choice;

        do {
            System.out.println("\n==================================================");
            System.out.println("                 SAVE-TO-EARN REWARDS");
            System.out.println("==================================================");
            System.out.println("[1] View Reward Points");
            System.out.println("[2] View Reward History");
            System.out.println("[3] Return to Customer Dashboard");
            System.out.println("==================================================");
            System.out.print("Enter your choice: ");

            choice = readChoice();

            switch (choice) {

                case 1:
                    viewRewardPoints(accountID);
                    break;

                case 2:
                    viewRewardHistory(accountID);
                    break;

                case 3:
                    System.out.println(
                            "\nReturning to Customer Dashboard..."
                    );
                    break;

                default:
                    System.out.println(
                            "\nInvalid choice. Please select 1, 2, or 3."
                    );
                    pause();
            }

        } while (choice != 3);
    }

    // ==========================================
    // VIEW REWARD POINTS
    // ==========================================

    private void viewRewardPoints(String accountID) {

        String[] customer = findCustomer(accountID);

        if (customer == null) {
            System.out.println("\nCustomer account not found.");
            pause();
            return;
        }

        try {

            String fullName = decode(customer[1]);

            double totalDeposits = Double.parseDouble(customer[5]);
            int rewardPoints = Integer.parseInt(customer[6]);

            System.out.println("\n==================================================");
            System.out.println("                  REWARD SUMMARY");
            System.out.println("==================================================");
            System.out.println("Customer Name       : " + fullName);
            System.out.println("Account ID          : " + accountID);
            System.out.printf("Total Deposits      : PHP %,.2f%n", totalDeposits);
            System.out.println("Available Points    : " + rewardPoints);
            System.out.println("==================================================");
            System.out.println("Keep saving to earn more reward points!");
            System.out.println("==================================================");

        } catch (NumberFormatException e) {

            System.out.println(
                    "\nUnable to read the customer's reward information."
            );
        }

        pause();
    }

    // ==========================================
    // VIEW REWARD HISTORY
    // ==========================================

    private void viewRewardHistory(String accountID) {

        List<String> rewards = database.readRewards();

        List<String[]> customerRewards = new ArrayList<>();

        for (String record : rewards) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            /*
             * Reward record format:
             *
             * rewardID|accountID|transactionID|
             * depositAmount|pointsEarned|dateTime
             */

            if (fields.length == 6
                    && fields[1].equalsIgnoreCase(accountID)) {

                customerRewards.add(fields);
            }
        }

        System.out.println("\n==================================================");
        System.out.println("                  REWARD HISTORY");
        System.out.println("==================================================");

        if (customerRewards.isEmpty()) {

            System.out.println("No reward history found.");
            System.out.println("Make a qualifying deposit to earn rewards.");

        } else {

            System.out.println("Account ID: " + accountID);
            System.out.println("--------------------------------------------------");

            int totalPointsEarned = 0;

            for (String[] reward : customerRewards) {

                try {

                    double depositAmount =
                            Double.parseDouble(reward[3]);

                    int pointsEarned =
                            Integer.parseInt(reward[4]);

                    totalPointsEarned += pointsEarned;

                    System.out.println(
                            "\nReward ID       : " + reward[0]
                    );

                    System.out.println(
                            "Transaction ID  : " + reward[2]
                    );

                    System.out.printf(
                            "Deposit Amount  : PHP %,.2f%n",
                            depositAmount
                    );

                    System.out.println(
                            "Points Earned   : " + pointsEarned
                    );

                    System.out.println(
                            "Date and Time   : " + reward[5]
                    );

                    System.out.println(
                            "--------------------------------------------------"
                    );

                } catch (NumberFormatException e) {

                    System.out.println(
                            "\nA reward record contains invalid numerical data."
                    );
                }
            }

            System.out.println("\nTotal Points Earned: " + totalPointsEarned);
        }

        System.out.println("==================================================");

        pause();
    }

    // ==========================================
    // FIND CUSTOMER ACCOUNT
    // ==========================================

    private String[] findCustomer(String accountID) {

        if (accountID == null || accountID.trim().isEmpty()) {
            return null;
        }

        List<String> customers = database.readCustomers();

        for (String record : customers) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length == 7
                    && fields[0].equalsIgnoreCase(accountID.trim())) {

                return fields;
            }
        }

        return null;
    }

    // ==========================================
    // DECODE CUSTOMER NAME
    // ==========================================

    private String decode(String value) {

        try {

            byte[] decoded = Base64.getDecoder().decode(value);

            return new String(
                    decoded,
                    StandardCharsets.UTF_8
            );

        } catch (IllegalArgumentException e) {

            return value;
        }
    }

    // ==========================================
    // INPUT AND DISPLAY HELPERS
    // ==========================================

    private int readChoice() {

        String input = scanner.nextLine().trim();

        try {
            return Integer.parseInt(input);

        } catch (NumberFormatException e) {

            return -1;
        }
    }

    private void pause() {

        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }
}
