
package src;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Transaction_Protection {

    private final FileDatabase database;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Transaction_Protection() {
        this.database = new FileDatabase();
    }

    public Transaction_Protection(FileDatabase database) {
        this.database = database;
    }

    // ==========================================
    // ROLE-BASED PERMISSION VALIDATION
    // ==========================================

    public boolean hasPermission(String role, String transactionType) {

        if (role == null || transactionType == null) {
            return false;
        }

        String normalizedRole = role.trim().toUpperCase();
        String normalizedType = transactionType.trim().toUpperCase();

        // Customers can perform their own transactions.
        if (normalizedRole.equals("CUSTOMER")
                || normalizedRole.equals("USER")) {

            return normalizedType.equals("DEPOSIT")
                    || normalizedType.equals("WITHDRAWAL")
                    || normalizedType.equals("TRANSFER");
        }

        // Staff and Admin do not directly perform customer transactions
        // through the customer transaction menu.
        return false;
    }

    // ==========================================
    // CUSTOMER ACCOUNT VALIDATION
    // ==========================================

    public boolean isValidCustomerAccount(String accountID) {

        if (accountID == null || accountID.trim().isEmpty()) {
            return false;
        }

        List<String> customers = database.readCustomers();

        for (String record : customers) {

            if (record == null || record.trim().isEmpty()) {
                continue;
            }

            String[] fields = record.split("\\|", -1);

            if (fields.length == 7
                    && fields[0].equalsIgnoreCase(accountID.trim())) {
                return true;
            }
        }

        return false;
    }

    // ==========================================
    // ACCOUNT STATUS VALIDATION
    // ==========================================

    public boolean isAccountActive(String accountID) {

        if (!isValidCustomerAccount(accountID)) {
            logSecurityEvent(
                    accountID,
                    "Rejected operation: account does not exist."
            );

            return false;
        }

        String status = database.getAccountStatus(accountID);

        if (!status.equalsIgnoreCase("ACTIVE")) {

            logSecurityEvent(
                    accountID,
                    "Rejected operation: inactive account."
            );

            return false;
        }

        return true;
    }

    // ==========================================
    // TRANSACTION VALIDATION
    // ==========================================

    /*
     * Validates a customer's deposit, withdrawal, or transfer
     * before the transaction manager processes it.
     *
     * recipientID is required only for transfers.
     */

    public boolean validateTransaction(
            String accountID,
            String transactionType,
            BigDecimal amount,
            String recipientID) {

        if (accountID == null
                || transactionType == null
                || amount == null) {

            logSecurityEvent(
                    accountID,
                    "Rejected transaction: missing transaction information."
            );

            return false;
        }

        String type = transactionType.trim().toUpperCase();

        // Check supported transaction type.
        if (!type.equals("DEPOSIT")
                && !type.equals("WITHDRAWAL")
                && !type.equals("TRANSFER")) {

            logSecurityEvent(
                    accountID,
                    "Rejected transaction: unsupported transaction type."
            );

            return false;
        }

        // Check amount.
        if (amount.compareTo(BigDecimal.ZERO) <= 0
                || amount.scale() > 2) {

            logSecurityEvent(
                    accountID,
                    "Rejected " + type + ": invalid transaction amount."
            );

            return false;
        }

        // Check source account.
        if (!isAccountActive(accountID)) {
            return false;
        }

        // Check current customer balance.
        BigDecimal currentBalance = getCustomerBalance(accountID);

        if (currentBalance == null) {
            logSecurityEvent(
                    accountID,
                    "Rejected " + type + ": unable to verify account balance."
            );

            return false;
        }

        // Withdrawal and transfer require sufficient balance.
        if (type.equals("WITHDRAWAL")
                || type.equals("TRANSFER")) {

            if (amount.compareTo(currentBalance) > 0) {

                logSecurityEvent(
                        accountID,
                        "Rejected " + type + ": insufficient balance."
                );

                return false;
            }
        }

        // Validate recipient for transfers.
        if (type.equals("TRANSFER")) {

            if (recipientID == null
                    || recipientID.trim().isEmpty()) {

                logSecurityEvent(
                        accountID,
                        "Rejected transfer: missing recipient account."
                );

                return false;
            }

            if (accountID.equalsIgnoreCase(recipientID.trim())) {

                logSecurityEvent(
                        accountID,
                        "Rejected transfer: same source and recipient account."
                );

                return false;
            }

            if (!isAccountActive(recipientID)) {

                logSecurityEvent(
                        accountID,
                        "Rejected transfer: recipient account is invalid or inactive."
                );

                return false;
            }
        }

        return true;
    }

    // ==========================================
    // GET CUSTOMER BALANCE
    // ==========================================

    public BigDecimal getCustomerBalance(String accountID) {

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

                try {

                    BigDecimal balance = new BigDecimal(fields[4]);

                    if (balance.compareTo(BigDecimal.ZERO) < 0) {
                        return null;
                    }

                    return balance;

                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }

        return null;
    }

    // ==========================================
    // SECURITY ACTIVITY LOGGING
    // ==========================================

    public void logSecurityEvent(
            String accountID,
            String description) {

        String safeAccountID =
                accountID == null || accountID.trim().isEmpty()
                ? "UNKNOWN"
                : accountID.trim();

        String safeDescription =
                description == null
                ? "Unspecified security event."
                : description.replace("|", "/")
                        .replace("\n", " ")
                        .replace("\r", " ");

        String dateTime = LocalDateTime.now().format(DATE_FORMAT);

        String activityRecord =
                dateTime
                + "|" + safeAccountID
                + "|SECURITY"
                + "|" + safeDescription;

        if (!database.saveActivityLog(activityRecord)) {

            System.out.println(
                    "Warning: Security event could not be saved."
            );
        }
    }
}
