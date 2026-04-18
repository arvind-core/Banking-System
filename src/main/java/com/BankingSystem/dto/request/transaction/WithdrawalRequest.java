package com.BankingSystem.dto.request.transaction;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class WithdrawalRequest {

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.0", message = "Withdrawal amount must be at least 1")
    private BigDecimal amount;

    private String description;
}

//ok, i got your point about pending status in real life, and this certainly happens because there are multiple parties involved in real life scenario, like the sender's bank, RBI server, reciever's bank. so here if one is working and another is not pending totally makes sense here but in our application, we have only one server there are not multiple parties involved here so if our server busy the send request doesn't get served and if the server is free and send request is serves it mean there is no chance that of being in pending status, and as you told that due to lack of network or internet issue the response may not be sent to UI or other services calling it, so if the resoponse couldn't be sent due to internet problem the PENDING status can also not be send due to internet problem so putting this status clearly makes no sense here and even if we want to use this we have to use it in UI or in othere services calling it that if there is no response or broken data then they can put the pending request automatically after in a certain time response is not recieved.one more thing, as you mentioned that if @Transactional is used it is helpful if anything fails it just rolls back everything, if anything breaks it rolls back everything to it's original state but then why are we using it in service layer, it must be used on repository layer or where we are interacting with database i mean i thought we would use it on database layer, i did not get the point exactly, explain how this works exactly.