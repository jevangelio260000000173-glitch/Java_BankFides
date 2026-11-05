
package src;

import java.util.Scanner;

public class InterestGrowthCalculator {

    private static final int SCREEN_GAP = 2;
    private static final String MAIN_LINE =
            "============================================================";
    private static final String SECTION_LINE =
            "------------------------------------------------------------";

    private final Scanner scanner;

    public InterestGrowthCalculator(Scanner scanner) {
        this.scanner = scanner;
    }

    public void calculateInterest() {

        char again;

        displayScreenHeader("INTEREST GROWTH CALCULATOR");

        do {
            double principal, rate, interest, finalSavings;
            double duration;
            String unit;

            displaySectionHeader("SAVINGS INFORMATION");

            // Input initial savings
            while (true) {
                System.out.print("Enter initial savings: PHP ");

                if (scanner.hasNextDouble()) {
                    principal = scanner.nextDouble();

                    if (principal > 0) {
                        break;
                    }
                } else {
                    if (!scanner.hasNextLine()) {
                        return;
                    }
                    scanner.next();
                }

                System.out.println(
                        "Invalid input! Savings must be greater than zero."
                );
            }

            // Input annual interest rate
            while (true) {
                System.out.print("Enter annual interest rate (%): ");

                if (scanner.hasNextDouble()) {
                    rate = scanner.nextDouble();

                    if (rate >= 0) {
                        break;
                    }
                } else {
                    if (!scanner.hasNextLine()) {
                        return;
                    }
                    scanner.next();
                }

                System.out.println(
                        "Invalid input! Rate cannot be negative."
                );
            }

            // Input duration
            while (true) {
                System.out.print("Enter duration (number): ");

                if (scanner.hasNextDouble()) {
                    duration = scanner.nextDouble();

                    if (duration > 0) {
                        break;
                    }
                } else {
                    if (!scanner.hasNextLine()) {
                        return;
                    }
                    scanner.next();
                }

                System.out.println(
                        "Invalid input! Duration must be greater than zero."
                );
            }

            // Input duration unit
            while (true) {
                System.out.print("Enter unit (months/years): ");

                if (!scanner.hasNext()) {
                    return;
                }

                unit = scanner.next().toLowerCase();

                if (unit.equals("month") || unit.equals("months")
                        || unit.equals("year") || unit.equals("years")) {
                    break;
                }

                System.out.println(
                        "Invalid unit! Please enter months or years."
                );
            }

            // Convert duration to years
            double timeInYears;

            if (unit.equals("month") || unit.equals("months")) {
                timeInYears = duration / 12;
            } else {
                timeInYears = duration;
            }

            // Calculate simple interest
            interest = principal * (rate / 100) * timeInYears;

            // Calculate final savings
            finalSavings = principal + interest;

            // Display results
            displaySectionHeader("CALCULATION RESULTS");

            System.out.printf("Initial Savings       : PHP %.2f%n", principal);
            System.out.printf("Annual Interest Rate  : %.2f%%%n", rate);
            System.out.println("Duration              : " + duration + " " + unit);
            System.out.printf("Interest Earned       : PHP %.2f%n", interest);
            System.out.printf("Final Savings         : PHP %.2f%n", finalSavings);

            System.out.println();
            System.out.println(SECTION_LINE);

            // Ask for another calculation
            while (true) {
                System.out.print("\nPerform another calculation? (Y/N): ");

                if (!scanner.hasNext()) {
                    return;
                }

                String choice = scanner.next().trim();

                if (choice.equalsIgnoreCase("Y")
                        || choice.equalsIgnoreCase("N")) {
                    again = choice.charAt(0);
                    break;
                }

                System.out.println("Invalid choice! Enter Y or N.");
            }

            if (again == 'Y' || again == 'y') {
                screenGap();
            }

        } while (again == 'Y' || again == 'y');

        System.out.println();
        System.out.println("Thank you for using the Interest Growth Calculator!");
        System.out.println();

    }

    private void displayScreenHeader(String title) {
        screenGap();
        System.out.println(MAIN_LINE);
        System.out.println();
        System.out.println("                    BANCOFIDES");
        System.out.println("                  SAVINGS MANAGEMENT SYSTEM");
        System.out.println();
        System.out.println("                    " + title);
        System.out.println();
        System.out.println(MAIN_LINE);
        System.out.println();
    }

    private void displaySectionHeader(String title) {
        System.out.println();
        System.out.println(SECTION_LINE);
        System.out.println("                    " + title);
        System.out.println(SECTION_LINE);
        System.out.println();
    }

    private void screenGap() {
        for (int i = 0; i < SCREEN_GAP; i++) {
            System.out.println();
        }
    }
}
