package com.chalchitraghar.modules.tickets.service;
import com.chalchitraghar.modules.tickets.dto.response.AdminTicketDetailResponse; import com.chalchitraghar.modules.users.entity.User;
public interface TicketOperationsService { AdminTicketDetailResponse revoke(String reference,String reason,User admin); AdminTicketDetailResponse reissue(String reference,String reason,User admin); int expireBatch(); void revokeForBooking(Long bookingId,String reason,User actor); void revokeForShow(Long showId,String reason,User actor); }
