package com.BankingSystem.notifications;
import com.BankingSystem.repo.NotificationPreferenceRepository;
import com.BankingSystem.repo.UserRepository;
import com.BankingSystem.util.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import static com.BankingSystem.BankConfig.BANK_NAME;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationListener {

    private final JavaMailSender mailSender;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final UserRepository userRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    @EventListener
    public void handleNotification(NotificationEvent event) {
        try {
            userRepository.findByEmailAndIsActiveTrue(event.getRecipientEmail()).ifPresent(user -> {
                notificationPreferenceRepository.findByUser(user).ifPresent(pref -> {
                    if(pref.isEmailEnabled()){
                        sendEmail(event);
                    }
                });
            });
        } catch (Exception e) {
            log.error("Failed to send email for event: {} to : {} | Error: {}",
                    event.getEventType(),event.getRecipientEmail(), e.getMessage());
        }
    }

    private void sendEmail(NotificationEvent event) {
        try{
            var message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(event.getRecipientEmail());
            helper.setSubject(buildSubject(event));
            helper.setText(buildBody(event), true);

            mailSender.send(message);

            log.info("Email sent to : {} | Subject : {}", event.getRecipientEmail(), buildSubject(event));
        }
        catch (Exception e){
            log.error("Failed to send email to :{} | Error : {}", event.getRecipientEmail(), e.getMessage());
        }
    }

    private String buildSubject(NotificationEvent event) {
        return switch (event.getEventType()) {
            case DEPOSIT -> "Money Deposited - " + event.getStringData("transactionReference");
            case WITHDRAWAL -> "Money Withdrawn - " + event.getStringData("transactionReference");
            case TRANSFER_SENT -> "Money Sent - " + event.getStringData("transactionReference");
            case TRANSFER_RECEIVED -> "Money Received - " + event.getStringData("transactionReference");
            case ACCOUNT_CREATED -> "Welcome to " + BANK_NAME +"- Account Created Successfully";
            case ACCOUNT_CLOSED -> "Account Closed Successfully";
            case PRIMARY_ACCOUNT_UPDATED -> "Primary Account Updated";
            case LOGIN_SUCCESSFUL -> "New Login Detected on Your Account";
            case PASSWORD_CHANGED -> "Your Password Has Been Changed";
            case EMAIL_UPDATED -> "Your Email Has Been Updated";
            case PHONE_UPDATED -> "Your Phone Number Has Been Updated";
            case LOAN_APPLICATION_SUBMITTED -> "Loan Application Submitted Successfully";
            case LOAN_UNDER_REVIEW -> "Your Loan Application is Under Review";
            case LOAN_APPROVED -> "Congratulations! Your Loan is Approved";
            case LOAN_REJECTED -> "Loan Application Update";
            case LOAN_DISBURSED -> "Loan Amount Disbursed to Your Account";
            case EMI_DUE_REMINDER -> "EMI Due Reminder";
            case EMI_DEDUCTED -> "EMI Deducted Successfully";
            case EMI_MISSED -> "Missed EMI Payment Alert";
            case LOAN_FULLY_REPAID -> "Congratulations! Loan Fully Repaid";
            case CARD_APPLICATION_SUBMITTED -> "Credit Card Application Submitted";
            case CARD_APPROVED -> "Your Credit Card is Approved";
            case CARD_REJECTED -> "Credit Card Application Update";
            case CARD_ACTIVATED -> "Your Credit Card is Now Active";
            case CARD_BLOCKED -> "Your Credit Card Has Been Blocked";
            case BILLING_CYCLE_GENERATED -> "Your Monthly Statement is Ready";
            case PAYMENT_DUE_REMINDER -> "Credit Card Payment Due Reminder";
            case CARD_PAYMENT_RECEIVED -> "Credit Card Payment Received";
            case MONEY_REQUEST_RECEIVED -> "You Have a New Money Request";
            case MONEY_REQUEST_ACCEPTED -> "Money Request Accepted";
            case MONEY_REQUEST_REJECTED -> "Money Request Rejected";
            case MONEY_REQUEST_EXPIRED -> "Money Request Expired";
            case INTEREST_CREDITED -> "Monthly Interest Credited to Your Account";
            case BRANCH_TRANSFER_REQUESTED -> "Branch Transfer Request Submitted";
            case BRANCH_TRANSFER_APPROVED -> "Branch Transfer Approved";
            case BRANCH_TRANSFER_REJECTED -> "Branch Transfer Request Rejected";
            case BRANCH_TRANSFER_COMPLETED -> "Branch Transfer Completed Successfully";
        };
    }

    private String buildBody(NotificationEvent event) {
        String name = event.getRecipientName();
        // HTML email body
        String content = switch (event.getEventType()) {
            case DEPOSIT -> String.format(
                    "Dear %s,<br><br>" +
                            "<b>₹%s</b> has been credited to your account <b>%s</b>.<br>" +
                            "Available balance: <b>₹%s</b><br>" +
                            "Reference: %s<br><br>" +
                            "Thank you for banking with us.",
                    name,
                    event.getStringData("amount"),
                    event.getStringData("accountNumber"),
                    event.getStringData("balance"),
                    event.getStringData("transactionReference"));
            case WITHDRAWAL -> String.format(
                    "Dear %s,<br><br>" +
                            "<b>₹%s</b> has been debited from your account <b>%s</b>.<br>" +
                            "Available balance: <b>₹%s</b><br>" +
                            "Reference: %s",
                    name,
                    event.getStringData("amount"),
                    event.getStringData("accountNumber"),
                    event.getStringData("balance"),
                    event.getStringData("transactionReference"));
            case LOAN_APPROVED -> String.format(
                    "Dear %s,<br><br>" +
                            "Congratulations! Your loan application has been <b>approved</b>.<br>" +
                            "Loan Amount: <b>₹%s</b><br>" +
                            "Please wait for disbursement confirmation.",
                    name,
                    event.getStringData("requestedAmount"));
            case EMI_MISSED -> String.format(
                    "Dear %s,<br><br>" +
                            "Your EMI of <b>₹%s</b> due on <b>%s</b> could not be processed.<br>" +
                            "Attempts remaining: <b>%s</b><br>" +
                            "A penalty of <b>₹%s</b> may be applied if payment is not made.<br><br>" +
                            "Please ensure sufficient balance in your account.",
                    name,
                    event.getStringData("emiAmount"),
                    event.getStringData("dueDate"),
                    event.getStringData("retriesRemaining"),
                    event.getStringData("penalty"));
            case INTEREST_CREDITED -> String.format(
                    "Dear %s,<br><br>" +
                            "Monthly interest of <b>₹%s</b> has been credited to your account <b>%s</b>.<br>" +
                            "Period: <b>%s</b><br>" +
                            "New Balance: <b>₹%s</b>",
                    name,
                    event.getStringData("interestAmount"),
                    event.getStringData("accountNumber"),
                    event.getStringData("period"),
                    event.getStringData("newBalance"));
            default -> String.format(
                    "Dear %s,<br><br>%s<br><br>" +
                            "For any queries contact us at %s.",
                    name,
                    event.getEventType().toString()
                            .replace("_", " ").toLowerCase(),
                    com.BankingSystem.BankConfig.BANK_SUPPORT_EMAIL);
        };

        return "<html><body style='font-family:Arial,sans-serif;'>" +
                content +
                "<br><br><hr><small>This is an automated message from " +
                com.BankingSystem.BankConfig.BANK_NAME +
                ". Please do not reply.</small></body></html>";
    }
}