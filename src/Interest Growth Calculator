
import java.util.Scanner;

public class InterestGrowthCalculator {

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        char again;

        System.out.println("=== INTEREST GROWTH CALCULATOR ===");

        do {
            double principal, rate, interest, finalSavings;
            double duration;
            String unit;

            // Input and validate initial savings
            while (true) {
                System.out.print("\nEnter initial savings: ");

                if (input.hasNextDouble()) {
                    principal = input.nextDouble();

                    if (principal > 0) {
                        break;
                    }
                } else {
                    input.next();
                }

                System.out.println(
                    "Invalid input! Savings must be greater than zero."
                );
            }

            // Input and validate annual interest rate
            while (true) {
                System.out.print("Enter annual interest rate (%): ");

                if (input.hasNextDouble()) {
                    rate = input.nextDouble();

                    if (rate >= 0) {
                        break;
                    }
                } else {
                    input.next();
                }

                System.out.println(
                    "Invalid input! Rate cannot be negative."
                );
            }

            // Input and validate duration
            while (true) {
                System.out.print("Enter duration (number): ");

                if (input.hasNextDouble()) {
                    duration = input.nextDouble();

                    if (duration > 0) {
                        break;
                    }
                } else {
                    input.next();
                }

                System.out.println(
                    "Invalid input! Duration must be greater than zero."
                );
            }

            // Input duration unit
            while (true) {
                System.out.print("Enter unit (month/months/year/years): ");
                unit = input.next().toLowerCase();

                if (unit.equals("month") || unit.equals("months")
                        || unit.equals("year") || unit.equals("years")) {
                    break;
                }

                System.out.println(
                    "Invalid unit! Please enter months or years."
                );
            }

            // Calculate simple interest
            if (unit.equals("month") || unit.equals("months")) {
                interest = principal * (rate / 100) * (duration / 12);
            } else {
                interest = principal * (rate / 100) * duration;
            }

            // Calculate final savings
            finalSavings = principal + interest;

            // Display results
            System.out.println("\n=== CALCULATION RESULTS ===");
            System.out.printf("Initial Savings: %.2f%n", principal);
            System.out.printf("Annual Interest Rate: %.2f%%%n", rate);
            System.out.println("Duration: " + duration + " " + unit);
            System.out.printf("Interest Earned: %.2f%n", interest);
            System.out.printf("Final Savings: %.2f%n", finalSavings);

            // Ask for another calculation
            while (true) {
                System.out.print(
                    "\nPerform another calculation? (Y/N): "
                );
                again = input.next().charAt(0);

                if (again == 'Y' || again == 'y'
                        || again == 'N' || again == 'n') {
                    break;
                }

                System.out.println("Invalid choice! Enter Y or N.");
            }

        } while (again == 'Y' || again == 'y');

        System.out.println("\nThank you for using the calculator!");
        System.out.println("END");

        input.close();
    }
}
