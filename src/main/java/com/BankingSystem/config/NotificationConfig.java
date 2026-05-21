package com.BankingSystem.config;

import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Configuration
public class NotificationConfig {

    @Value("${twilio.account.sid}")
    private String twilioAccountSid;

    @Value("{twilio.auth.token}")
    private String twilioAuthToken;

    @PostConstruct
    public void initTwilio(){
        try{
            Twilio.init(twilioAccountSid, twilioAuthToken);
            log.info("Twilio initialized successfully");
        } catch (Exception e) {
            log.warn("Twilio initialization failed - SMS will not be sent." +
            "Error : {}", e.getMessage());
        }
    }

    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }
}