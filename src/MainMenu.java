
package src;

import java.util.Scanner;

public class MainMenu {

    private final Scanner scanner;
    private final FileDatabase database;
    private final UserAuthentication auth;

    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public MainMenu() {

        this.scanner = new Scanner(System.in);
        this.database = new FileDatabase();
        this.auth = new UserAuthentication(scanner, database);
    }

    // ==========================================
    // MAIN MENU
    // ==========================================

    public void displayMenu() {

        int choice;

        do {

            displayMainMenu();
            choice = readMenuChoice();

            switch (choice) {

                case 1:

                    System.out.println("\nOpening Registration...");

                    auth.registerCustomer();

                    pauseBeforeReturning();

                    break;

                case 2:

                    System.out.println("\nOpening Login...");

                    auth.login();

                    pauseBeforeReturning();

                    break;

                case 3:

                    displayExitMessage();

                    break;

                default:

                    System.out.println(
                            "\nInvalid choice! Please select 1, 2, or 3."
                    );

                    pauseBeforeReturning();

                    break;
            }

        } while (choice != 3);
    }

    // ==========================================
    // DISPLAY MAIN MENU
    // ==========================================

    private void displayMainMenu() {

        System.out.println("\n==================================================");
        System.out.println("             WELCOME TO BANCOFIDES!");
        System.out.println("           SAVINGS MANAGEMENT SYSTEM");
        System.out.println("==================================================");
        System.out.println("             Your Savings, Your Future.");
        System.out.println("==================================================");

        System.out.println();
        System.out.println("                    MAIN MENU");
        System.out.println();

        System.out.println("              [1] Register Account");
        System.out.println("              [2] Login");
        System.out.println("              [3] Exit");

        System.out.println();
        System.out.println("==================================================");
    }

    // ==========================================
    // MENU INPUT VALIDATION
    // ==========================================

    private int readMenuChoice() {

        while (true) {

            System.out.print("Enter your choice: ");

            if (!scanner.hasNextLine()) {
                return 3;
            }

            String input = scanner.nextLine().trim();

            try {

                int choice = Integer.parseInt(input);

                if (choice >= 1 && choice <= 3) {
                    return choice;
                }

                System.out.println(
                        "\nInvalid choice! Please enter 1, 2, or 3.\n"
                );

            } catch (NumberFormatException e) {

                System.out.println(
                        "\nInvalid input! Please enter a number.\n"
                );
            }
        }
    }

    // ==========================================
    // EXIT MESSAGE
    // ==========================================

    private void displayExitMessage() {

        System.out.println("\n==================================================");
        System.out.println("          THANK YOU FOR USING BANCOFIDES!");
        System.out.println("             Your Savings, Your Future.");
        System.out.println("==================================================");
        System.out.println("Exiting the system. Goodbye!");
    }

    // ==========================================
    // PAUSE BEFORE RETURNING TO MENU
    // ==========================================

    private void pauseBeforeReturning() {

        System.out.println(
                "\nPress Enter to return to the main menu..."
        );

        if (scanner.hasNextLine()) {
            scanner.nextLine();
        }
    }
}
