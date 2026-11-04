
package src;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;

public class RewardsManager {

    private final Scanner scanner;
    private final FileDatabase database;

    // SAVE-TO-EARN MILESTONES
    private static final BigDecimal[] MILESTONES = {
        new BigDecimal("5000.00"),
        new BigDecimal("10000.00"),
        new BigDecimal("20000.00")
    };

    private static final int[] BONUS_POINTS = {
        25, 50, 100
    };

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
            System.out.println("[3] View Milestone Progress");
            System.out.println("[4] Return to Customer Dashboard");
            System.out.println("==================================================");

            choice = readChoice(1, 4);

            switch (choice) {

                case 1:
                    viewRewardPoints(accountID);
                    break;

                case 2:
                    viewRewardHistory(accountID);
                    break;

                case 3:
                    viewMilestoneProgress(accountID);
                    break;

                case 4:
                    System.out.println(
                            "\nReturning to Customer Dashboard..."
                    );
                    break;
            }

        } while (choice != 4);
    }

    // ==========================================
    // VIEW REWARD POINTS
    // ==========================================

    private void viewRewardPoints(String accountID) {

        String[] customer = findCustomer(accountID);

        if (customer == null) {
            showMessage("Customer account not found.");
            return;
        }

        try {

            String fullName = decode(customer[1]);

            BigDecimal totalDeposits =
                    new BigDecimal(customer[5]);

            int rewardPoints =
                    Integer.parseInt(customer[6]);

            System.out.println("\n==================================================");
            System.out.println("                  REWARD SUMMARY");
            System.out.println("==================================================");
            System.out.println("Customer Name       : " + fullName);
            System.out.println("Account ID          : " + accountID);

            System.out.printf(
                    "Total Deposits      : PHP %,.2f%n",
                    totalDeposits
            );

            System.out.println("Available Points    : " + rewardPoints);
            System.out.println("==================================================");
            System.out.println("Keep saving to earn more reward points!");
            System.out.println("==================================================");

        } catch (NumberFormatException e) {

            showMessage(
                    "Unable to read the customer's reward information."
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

            if (fields.length == 6
                    && fields[1].equalsIgnoreCase(accountID)) {

                customerRewards.add(fields);
            }
        }

        System.out.println("\n==================================================");
        System.out.println("                  REWARD HISTORY");
        System.out.println("==================================================");
        System.out.println("Account ID: " + accountID);
        System.out.println("--------------------------------------------------");

        if (customerRewards.isEmpty()) {

            System.out.println("No reward history found.");
            System.out.println("Make a qualifying deposit to earn rewards.");

        } else {

            int totalPointsEarned = 0;

            for (String[] reward : customerRewards) {

                try {

                    BigDecimal depositAmount =
                            new BigDecimal(reward[3]);

                    int pointsEarned =
                            Integer.parseInt(reward[4]);

                    totalPointsEarned += pointsEarned;

                    System.out.println("\nReward ID       : " + reward[0]);
                    System.out.println("Transaction ID  : " + reward[2]);

                    System.out.printf(
                            "Deposit Amount  : PHP %,.2f%n",
                            depositAmount
                    );

                    System.out.println("Points Earned   : " + pointsEarned);
                    System.out.println("Date and Time   : " + reward[5]);
                    System.out.println("--------------------------------------------------");

                } catch (NumberFormatException e) {

                    System.out.println(
                            "\nA reward record contains invalid numerical data."
                    );
                }
            }

            System.out.println(
                    "\nTotal Points Earned from Recorded Rewards: "
                    + totalPointsEarned
            );
        }

        System.out.println("==================================================");
        pause();
    }

    // ==========================================
    // VIEW MILESTONE PROGRESS
    // ==========================================

    private void viewMilestoneProgress(String accountID) {

        String[] customer = findCustomer(accountID);

        if (customer == null) {
            showMessage("Customer account not found.");
            return;
        }

        try {

            BigDecimal totalDeposits =
                    new BigDecimal(customer[5]);

            System.out.println("\n==================================================");
            System.out.println("                MILESTONE PROGRESS");
            System.out.println("==================================================");

            System.out.printf(
                    "Your Cumulative Deposits: PHP %,.2f%n",
                    totalDeposits
            );

            System.out.println("--------------------------------------------------");

            BigDecimal nextMilestone = null;

            int totalUnlockedBonus = 0;

            for (int i = 0; i < MILESTONES.length; i++) {

                boolean reached =
                        totalDeposits.compareTo(MILESTONES[i]) >= 0;

                if (reached) {

                    totalUnlockedBonus += BONUS_POINTS[i];

                    System.out.printf(
                            "[UNLOCKED] PHP %,.2f - %d Bonus Points%n",
                            MILESTONES[i],
                            BONUS_POINTS[i]
                    );

                } else {

                    System.out.printf(
                            "[LOCKED]   PHP %,.2f - %d Bonus Points%n",
                            MILESTONES[i],
                            BONUS_POINTS[i]
                    );

                    if (nextMilestone == null) {
                        nextMilestone = MILESTONES[i];
                    }
                }
            }

            System.out.println("--------------------------------------------------");

            if (nextMilestone != null) {

                BigDecimal remaining =
                        nextMilestone.subtract(totalDeposits);

                System.out.printf(
                        "Amount Needed for Next Milestone: PHP %,.2f%n",
                        remaining
                );

            } else {

                System.out.println(
                        "Congratulations! You have reached all milestones!"
                );
            }

            System.out.println(
                    "Total Milestone Bonus Points Unlocked: "
                    + totalUnlockedBonus
            );

            System.out.println("==================================================");

        } catch (NumberFormatException e) {

            System.out.println(
                    "\nUnable to calculate milestone progress."
            );
        }

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

            } catch (NumberFormatException e) {
                // Display validation message below.
            }

            System.out.println(
                    "Invalid choice. Please select "
                    + min + " to " + max + "."
            );
        }
    }

    // ==========================================
    // MESSAGE AND PAUSE
    // ==========================================

    private void showMessage(String message) {

        System.out.println("\n" + message);
        pause();
    }

    private void pause() {

        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }
}
