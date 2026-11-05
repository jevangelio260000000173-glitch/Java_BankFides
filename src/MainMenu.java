
package src;

import java.util.Scanner;

public class MainMenu {

    private final Scanner scanner;
    private final FileDatabase database;
    private final UserAuthentication auth;

    private boolean firstMenuDisplay = true;

    // UI SPACING AND DIVIDERS

    private static final int SCREEN_GAP = 2;

    private static final String MAIN_LINE =
            "============================================================";

    private static final String SECTION_LINE =
            "------------------------------------------------------------";

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public MainMenu() {

        this.scanner = new Scanner(System.in);
        this.database = new FileDatabase();
        this.auth = new UserAuthentication(scanner, database);
    }

    // =========================================================
    // MAIN MENU
    // =========================================================

    public void displayMenu() {

        int choice;

        do {

            displayMainMenu();
            choice = readMenuChoice();

            switch (choice) {

                case 1:

                    auth.registerCustomer();

                    pauseBeforeReturning();

                    break;

                case 2:

                    auth.login();

                    pauseBeforeReturning();

                    break;

                case 3:

                    displayExitMessage();

                    break;

                default:

                    System.out.println();
                    System.out.println(
                            "Invalid choice! Please select 1, 2, or 3."
                    );

                    pauseBeforeReturning();

                    break;
            }

        } while (choice != 3);
    }

    // =========================================================
    // DISPLAY MAIN MENU
    // =========================================================

    private void displayMainMenu() {

        if (!firstMenuDisplay) {
            screenGap();
        }

        firstMenuDisplay = false;

        System.out.println(MAIN_LINE);
        System.out.println();
        System.out.println("                    WELCOME TO BANCOFIDES!");
        System.out.println("                  SAVINGS MANAGEMENT SYSTEM");
        System.out.println();
        System.out.println("                    Your Savings, Your Future.");
        System.out.println();
        System.out.println(MAIN_LINE);

        System.out.println();
        System.out.println("                         MAIN MENU");
        System.out.println();

        System.out.println("                  [1] Register Account");
        System.out.println();
        System.out.println("                  [2] Login");
        System.out.println();
        System.out.println("                  [3] Exit");

        System.out.println();
        System.out.println(MAIN_LINE);
        System.out.println();
    }

    // =========================================================
    // MENU INPUT VALIDATION
    // =========================================================

    private int readMenuChoice() {

        while (true) {

            System.out.print("Select an option (1-3): ");

            if (!scanner.hasNextLine()) {
                return 3;
            }

            String input = scanner.nextLine().trim();

            try {

                int choice = Integer.parseInt(input);

                if (choice >= 1 && choice <= 3) {
                    return choice;
                }

                System.out.println();
                System.out.println(
                        "Invalid choice! Please enter 1, 2, or 3."
                );
                System.out.println();

            } catch (NumberFormatException e) {

                System.out.println();
                System.out.println(
                        "Invalid input! Please enter a number."
                );
                System.out.println();
            }
        }
    }

    // =========================================================
    // EXIT MESSAGE
    // =========================================================

    private void displayExitMessage() {

        screenGap();

        System.out.println(MAIN_LINE);
        System.out.println();
        System.out.println("              THANK YOU FOR USING BANCOFIDES!");
        System.out.println();
        System.out.println("                  Your Savings, Your Future.");
        System.out.println();
        System.out.println(MAIN_LINE);
        System.out.println();
        System.out.println("Exiting the system. Goodbye!");
        System.out.println();
    }

    // =========================================================
    // PAUSE BEFORE RETURNING TO MAIN MENU
    // =========================================================

    private void pauseBeforeReturning() {

        System.out.println();
        System.out.println(SECTION_LINE);
        System.out.println("                 END OF CURRENT SCREEN");
        System.out.println(SECTION_LINE);
        System.out.println();

        System.out.print("Press Enter to return to the Main Menu...");

        if (scanner.hasNextLine()) {
            scanner.nextLine();
        }
    }

    // =========================================================
    // SCREEN SPACING
    // =========================================================

    private void screenGap() {

        for (int i = 0; i < SCREEN_GAP; i++) {
            System.out.println();
        }
    }
}
