
package src;

import java.util.Scanner;

public class InterestGrowthCalculator {

    private final Scanner scanner;

    public InterestGrowthCalculator(Scanner scanner) {
        this.scanner = scanner;
    }

    public void calculateInterest() {

        char again;

        System.out.println("\n==============================================");
        System.out.println("          INTEREST GROWTH CALCULATOR");
        System.out.println("==============================================");

        do {
            double principal, rate, interest, finalSavings;
            double duration;
            String unit;

            // Input initial savings
            while (true) {
                System.out.print("\nEnter initial savings: PHP ");

                if (scanner.hasNextDouble()) {
                    principal = scanner.nextDouble();

                    if (principal > 0) {
                        break;
                    }
                } else {
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
                    scanner.next();
                }

                System.out.println(
                    "Invalid input! Duration must be greater than zero."
                );
            }

            // Input duration unit
            while (true) {
                System.out.print("Enter unit (months/years): ");
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
            System.out.println("\n==============================================");
            System.out.println("             CALCULATION RESULTS");
            System.out.println("==============================================");

            System.out.printf("Initial Savings       : PHP %.2f%n", principal);
            System.out.printf("Annual Interest Rate  : %.2f%%%n", rate);
            System.out.println("Duration              : " + duration + " " + unit);
            System.out.printf("Interest Earned       : PHP %.2f%n", interest);
            System.out.printf("Final Savings         : PHP %.2f%n", finalSavings);

            System.out.println("==============================================");

            // Ask for another calculation
            while (true) {
                System.out.print("\nPerform another calculation? (Y/N): ");
                String choice = scanner.next().trim();

                if (choice.equalsIgnoreCase("Y")
                        || choice.equalsIgnoreCase("N")) {
                    again = choice.charAt(0);
                    break;
                }

                System.out.println("Invalid choice! Enter Y or N.");
            }

        } while (again == 'Y' || again == 'y');

        System.out.println("\nThank you for using the Interest Growth Calculator!");
    }
}
