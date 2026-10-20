package com.kodegladiators.kaasutracker.service;

import com.kodegladiators.kaasutracker.model.Emi;
import com.kodegladiators.kaasutracker.repository.EmiRepository;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmiSchedulerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmiSchedulerService.class);
    private static final int REMINDER_DAYS = 3;

    private final EmiRepository emiRepository;

    public EmiSchedulerService(EmiRepository emiRepository) {
        this.emiRepository = emiRepository;
    }

    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional(readOnly = true)
    public void processUpcomingEmis() {
        int targetDay = LocalDate.now().plusDays(REMINDER_DAYS).getDayOfMonth();
        List<Emi> upcomingEmis = emiRepository.findAllActiveByDueDateDay(targetDay);

        for (Emi emi : upcomingEmis) {
            String userId = emi.getUser().getId();
            // Replace this log with FCM/push-notification delivery when that integration is added.
            LOGGER.info(
                    "Reminder: EMI '{}' of {} is due in {} days for User ID: {}",
                    emi.getTitle(),
                    emi.getAmount(),
                    REMINDER_DAYS,
                    userId
            );
        }
    }
}