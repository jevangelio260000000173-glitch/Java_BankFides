
package src;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;

public class TransactionIssueManager {

    private final Scanner scanner;
    private final FileDatabase database;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String[] ISSUE_TYPES = {
        "Incorrect Amount",
        "Deposit Not Reflected",
        "Withdrawal Problem",
        "Transfer Problem",
        "Unauthorized Transaction",
        "Duplicate Transaction",
        "Other"
    };

    public TransactionIssueManager(Scanner scanner, FileDatabase database) {
        this.scanner = scanner;
        this.database = database;
    }

    // =========================================================
    // CUSTOMER: REPORT A TRANSACTION ISSUE
    // =========================================================

    public void reportIssue(String accountID) {

        System.out.println("\n==========================================");
        System.out.println("          REPORT TRANSACTION ISSUE");
        System.out.println("==========================================");

        List<String> transactions = database.readTransactions();
        int transactionCount = 0;

        System.out.println("\nYOUR SUCCESSFUL TRANSACTIONS");
        System.out.println("------------------------------------------");

        for (String record : transactions) {

            String[] fields = record.split("\\|", -1);

            if (fields.length < 8) {
                continue;
            }

            if (fields[1].equals(accountID)
                    && fields[7].equalsIgnoreCase("SUCCESS")) {

                transactionCount++;

                System.out.println("[" + transactionCount + "] "
                        + "Transaction ID: " + fields[0]);

                System.out.println("    Type: " + fields[2]);
                System.out.println("    Amount: PHP " + fields[3]);
                System.out.println("    Date: " + fields[6]);

                if (fields.length >= 9 && !fields[8].isEmpty()) {
                    System.out.println("    Other Account: " + fields[8]);
                }

                System.out.println("------------------------------------------");
            }
        }

        if (transactionCount == 0) {
            System.out.println("No successful transactions found.");
            pause();
            return;
        }

        System.out.print("\nEnter the transaction number to report (0 to cancel): ");

        int selectedNumber = readInteger();

        if (selectedNumber == 0) {
            System.out.println("Report cancelled.");
            return;
        }

        if (selectedNumber < 1 || selectedNumber > transactionCount) {
            System.out.println("Invalid transaction selection.");
            return;
        }

        String selectedTransactionID = "";
        int currentNumber = 0;

        for (String record : transactions) {

            String[] fields = record.split("\\|", -1);

            if (fields.length < 8) {
                continue;
            }

            if (fields[1].equals(accountID)
                    && fields[7].equalsIgnoreCase("SUCCESS")) {

                currentNumber++;

                if (currentNumber == selectedNumber) {
                    selectedTransactionID = fields[0];
                    break;
                }
            }
        }

        if (selectedTransactionID.isEmpty()) {
            System.out.println("Unable to find the selected transaction.");
            return;
        }

        // Select issue type
        System.out.println("\nSELECT ISSUE TYPE");
        System.out.println("------------------------------------------");

        for (int i = 0; i < ISSUE_TYPES.length; i++) {
            System.out.println("[" + (i + 1) + "] " + ISSUE_TYPES[i]);
        }

        System.out.print("\nEnter issue type: ");
        int issueChoice = readInteger();

        if (issueChoice < 1 || issueChoice > ISSUE_TYPES.length) {
            System.out.println("Invalid issue type.");
            return;
        }

        String issueType = ISSUE_TYPES[issueChoice - 1];

        // Enter description
        System.out.println("\nDESCRIBE THE ISSUE");
        System.out.println("------------------------------------------");
        System.out.println("Explain what happened with your transaction.");

        System.out.print("Description: ");
        String description = scanner.nextLine().trim();

        if (description.isEmpty()) {
            System.out.println("Description cannot be empty.");
            return;
        }

        // Encode description
        String encodedDescription = Base64.getEncoder().encodeToString(
                description.getBytes(StandardCharsets.UTF_8)
        );

        // Generate unique report ID
        String reportID;

        do {
            reportID = "IR" + System.currentTimeMillis();
        } while (reportIDExists(reportID));

        String submittedAt = LocalDateTime.now().format(DATE_FORMAT);

        String record = reportID + "|"
                + accountID + "|"
                + selectedTransactionID + "|"
                + issueType + "|"
                + encodedDescription + "|"
                + "PENDING|"
                + submittedAt
                + "|||";

        if (database.saveTransactionIssue(record)) {

            System.out.println("\n==========================================");
            System.out.println("       ISSUE REPORT SUBMITTED");
            System.out.println("==========================================");
            System.out.println("Report ID: " + reportID);
            System.out.println("Transaction ID: " + selectedTransactionID);
            System.out.println("Issue Type: " + issueType);
            System.out.println("Status: PENDING");
            System.out.println("Submitted At: " + submittedAt);
            System.out.println("------------------------------------------");
            System.out.println("Your report has been submitted successfully.");
            System.out.println("Please keep your Report ID for reference.");

        } else {
            System.out.println("\nUnable to submit your issue report.");
            System.out.println("Please verify your account and transaction.");
        }

        pause();
    }

    // =========================================================
    // CUSTOMER: VIEW MY ISSUE REPORTS
    // =========================================================

    public void viewMyIssueReports(String accountID) {

        List<String> reports = database.readTransactionIssues();

        System.out.println("\n==================================================");
        System.out.println("                 MY ISSUE REPORTS");
        System.out.println("==================================================");

        if (reports == null || reports.isEmpty()) {
            System.out.println("You have not submitted any issue reports.");
            pause();
            return;
        }

        int count = 0;

        for (String record : reports) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length < 10) {
                continue;
            }

            // Only display reports belonging to the logged-in customer.
            if (!fields[1].equalsIgnoreCase(accountID)) {
                continue;
            }

            count++;

            System.out.println("\n------------------------------------------");
            System.out.println("REPORT #" + count);
            System.out.println("------------------------------------------");

            System.out.println("Report ID       : " + fields[0]);
            System.out.println("Transaction ID  : " + fields[2]);
            System.out.println("Issue Type      : " + fields[3]);
            System.out.println("Description     : " + decodeText(fields[4]));
            System.out.println("Status          : " + getStatusLabel(fields[5]));
            System.out.println("Submitted At    : " + fields[6]);

            if (!fields[7].isEmpty()) {
                System.out.println("Reviewed By     : " + fields[7]);
            }

            if (!fields[8].isEmpty()) {
                System.out.println("Reviewed At     : " + fields[8]);
            }

            if (!fields[9].isEmpty()) {
                System.out.println("Staff Note      : " + decodeText(fields[9]));
            } else {
                System.out.println("Staff Note      : No staff update yet.");
            }

            System.out.println("------------------------------------------");
        }

        if (count == 0) {
            System.out.println("You have not submitted any issue reports.");
        } else {
            System.out.println("\nTotal Issue Reports: " + count);
        }

        System.out.println("==================================================");

        pause();
    }

    // =========================================================
    // CUSTOMER: STATUS DISPLAY
    // =========================================================

    private String getStatusLabel(String status) {

        if (status.equalsIgnoreCase("PENDING")) {
            return "PENDING - Waiting for staff review";
        }

        if (status.equalsIgnoreCase("UNDER_REVIEW")) {
            return "UNDER REVIEW - Staff is investigating";
        }

        if (status.equalsIgnoreCase("RESOLVED")) {
            return "RESOLVED - Issue has been addressed";
        }

        if (status.equalsIgnoreCase("REJECTED")) {
            return "REJECTED - Report was not approved";
        }

        return status;
    }

    // =========================================================
    // STAFF: ISSUE REPORT MANAGEMENT
    // =========================================================

    public void staffIssueMenu(String staffID) {

        boolean running = true;

        while (running) {

            System.out.println("\n==========================================");
            System.out.println("       TRANSACTION ISSUE MANAGEMENT");
            System.out.println("==========================================");

            System.out.println("Pending Reports: "
                    + database.countPendingTransactionIssues());

            System.out.println("------------------------------------------");
            System.out.println("[1] View Pending Reports");
            System.out.println("[2] View All Reports");
            System.out.println("[3] Return to Dashboard");
            System.out.println("------------------------------------------");

            System.out.print("Enter choice: ");
            int choice = readInteger();

            switch (choice) {

                case 1:
                    displayReports(staffID, true);
                    break;

                case 2:
                    displayReports(staffID, false);
                    break;

                case 3:
                    running = false;
                    break;

                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    // =========================================================
    // STAFF: DISPLAY REPORTS
    // =========================================================

    private void displayReports(String staffID, boolean pendingOnly) {

        List<String> reports = database.readTransactionIssues();

        System.out.println("\n==========================================");

        if (pendingOnly) {
            System.out.println("             PENDING REPORTS");
        } else {
            System.out.println("               ALL REPORTS");
        }

        System.out.println("==========================================");

        int count = 0;

        for (String record : reports) {

            String[] fields = record.split("\\|", -1);

            if (fields.length < 10) {
                continue;
            }

            String status = fields[5];

            if (pendingOnly && !status.equalsIgnoreCase("PENDING")) {
                continue;
            }

            count++;

            System.out.println("[" + count + "] Report ID: " + fields[0]);
            System.out.println("    Account ID: " + fields[1]);
            System.out.println("    Transaction ID: " + fields[2]);
            System.out.println("    Issue Type: " + fields[3]);
            System.out.println("    Status: " + status);
            System.out.println("    Submitted At: " + fields[6]);
            System.out.println("------------------------------------------");
        }

        if (count == 0) {
            System.out.println("No reports found.");
            return;
        }

        System.out.print("\nEnter report number to review (0 to return): ");

        int selectedNumber = readInteger();

        if (selectedNumber == 0) {
            return;
        }

        if (selectedNumber < 1 || selectedNumber > count) {
            System.out.println("Invalid report selection.");
            return;
        }

        String selectedReport = "";
        int currentNumber = 0;

        for (String record : reports) {

            String[] fields = record.split("\\|", -1);

            if (fields.length < 10) {
                continue;
            }

            if (pendingOnly && !fields[5].equalsIgnoreCase("PENDING")) {
                continue;
            }

            currentNumber++;

            if (currentNumber == selectedNumber) {
                selectedReport = record;
                break;
            }
        }

        if (!selectedReport.isEmpty()) {
            reviewReport(selectedReport, staffID);
        }
    }

    // =========================================================
    // STAFF: REVIEW REPORT DETAILS
    // =========================================================

    private void reviewReport(String record, String staffID) {

        String[] fields = record.split("\\|", -1);

        if (fields.length < 10) {
            System.out.println("Invalid report record.");
            return;
        }

        String reportID = fields[0];
        String description = decodeText(fields[4]);
        String status = fields[5];
        String staffNote = decodeText(fields[9]);

        System.out.println("\n==========================================");
        System.out.println("             REPORT DETAILS");
        System.out.println("==========================================");

        System.out.println("Report ID: " + reportID);
        System.out.println("Account ID: " + fields[1]);
        System.out.println("Transaction ID: " + fields[2]);
        System.out.println("Issue Type: " + fields[3]);
        System.out.println("Description: " + description);
        System.out.println("Status: " + status);
        System.out.println("Submitted At: " + fields[6]);

        if (!fields[7].isEmpty()) {
            System.out.println("Reviewed By: " + fields[7]);
        }

        if (!fields[8].isEmpty()) {
            System.out.println("Reviewed At: " + fields[8]);
        }

        if (!staffNote.isEmpty()) {
            System.out.println("Staff Note: " + staffNote);
        }

        System.out.println("------------------------------------------");

        if (status.equalsIgnoreCase("RESOLVED")
                || status.equalsIgnoreCase("REJECTED")) {

            System.out.println("This report has already been closed.");
            return;
        }

        System.out.println("\nUPDATE REPORT STATUS");
        System.out.println("[1] Under Review");
        System.out.println("[2] Resolved");
        System.out.println("[3] Rejected");
        System.out.println("[4] Return");

        System.out.print("Enter choice: ");
        int choice = readInteger();

        String newStatus;

        switch (choice) {

            case 1:
                newStatus = "UNDER_REVIEW";
                break;

            case 2:
                newStatus = "RESOLVED";
                break;

            case 3:
                newStatus = "REJECTED";
                break;

            case 4:
                return;

            default:
                System.out.println("Invalid choice.");
                return;
        }

        System.out.print("\nEnter staff note: ");
        String newStaffNote = scanner.nextLine().trim();

        if (newStaffNote.isEmpty()) {
            System.out.println("Please provide a staff note.");
            return;
        }

        String encodedStaffNote = Base64.getEncoder().encodeToString(
                newStaffNote.getBytes(StandardCharsets.UTF_8)
        );

        String reviewedAt = LocalDateTime.now().format(DATE_FORMAT);

        boolean updated = database.updateTransactionIssue(
                reportID,
                newStatus,
                staffID,
                reviewedAt,
                encodedStaffNote
        );

        if (updated) {
            System.out.println("\nReport updated successfully.");
            System.out.println("New Status: " + newStatus);
        } else {
            System.out.println("\nUnable to update the report.");
            System.out.println("Please check the report and staff account.");
        }
    }

    // =========================================================
    // HELPER METHODS
    // =========================================================

    private int readInteger() {

        String input = scanner.nextLine().trim();

        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private boolean reportIDExists(String reportID) {

        List<String> reports = database.readTransactionIssues();

        for (String record : reports) {

            String[] fields = record.split("\\|", -1);

            if (fields.length > 0 && fields[0].equals(reportID)) {
                return true;
            }
        }

        return false;
    }

    private String decodeText(String encodedText) {

        if (encodedText == null || encodedText.isEmpty()) {
            return "";
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(encodedText);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return "[Unable to decode text]";
        }
    }

    // =========================================================
    // PAUSE
    // =========================================================

    private void pause() {

        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }
}
