package com.chalchitraghar.modules.payments.esewa;
import java.time.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import com.chalchitraghar.modules.payments.dto.esewa.EsewaStatusResponse; import com.chalchitraghar.modules.payments.entity.Payment;
import com.chalchitraghar.modules.payments.enums.*; import com.chalchitraghar.modules.payments.repository.PaymentRepository;
import com.chalchitraghar.modules.bookings.repository.*; import com.chalchitraghar.modules.bookings.enums.*;
import com.chalchitraghar.modules.seats.repository.SeatRepository; import com.chalchitraghar.modules.seats.enums.SeatStatus;
import com.chalchitraghar.modules.shows.service.ShowLifecycleService; import com.chalchitraghar.shared.exception.*;
import lombok.RequiredArgsConstructor;
@Service @RequiredArgsConstructor
public class EsewaPaymentFinalizer {
 private final PaymentRepository payments; private final BookingRepository bookings; private final BookingSeatRepository bookingSeats;
 private final SeatRepository seats; private final ShowLifecycleService shows; private final Clock clock;
 @Transactional
 public Payment finalizeStatus(String reference,EsewaStatusResponse status,String transactionCode){
  Payment p=payments.findByPaymentReferenceForUpdate(reference).orElseThrow(()->new ResourceNotFoundException("Payment not found with reference: "+reference));
  p.setProviderStatus(status.status()); p.setVerificationTime(LocalDateTime.now(clock)); if(status.refId()!=null)p.setProviderReference(status.refId());
  switch(status.status()){
   case "PENDING"->{return payments.save(p);} case "NOT_FOUND"->{p.setStatus(PaymentStatus.FAILED);p.setFailureCode("ESEWA_TRANSACTION_NOT_FOUND");p.setFailureMessage("eSewa transaction was not found");p.setFailedAt(LocalDateTime.now(clock));return payments.save(p);}
   case "CANCELED"->{p.setStatus(PaymentStatus.CANCELLED);p.setCancelledAt(LocalDateTime.now(clock));return payments.save(p);}
   case "AMBIGUOUS","FULL_REFUND","PARTIAL_REFUND"->{p.setManualReviewRequired(true);p.setManualReviewReason("eSewa reported "+status.status());return payments.save(p);}
   case "COMPLETE"->{} default->throw new PaymentConflictException("Unsupported eSewa status: "+status.status());
  }
  if(p.getStatus()==PaymentStatus.SUCCESS)return p;
  var duplicate=payments.findByProviderAndProviderTransactionId(PaymentProvider.ESEWA,transactionCode);
  if(duplicate.isPresent()&&!duplicate.get().getId().equals(p.getId()))throw new PaymentConflictException("eSewa transaction code is already associated with another payment");
  p.setStatus(PaymentStatus.SUCCESS);p.setProviderTransactionId(transactionCode);p.setCompletedAt(LocalDateTime.now(clock));p.setFailureCode(null);p.setFailureMessage(null);
  var b=bookings.findByIdForUpdate(p.getBooking().getId()).orElseThrow();
  if(b.getStatus()==BookingStatus.CONFIRMED){p.setManualReviewRequired(true);p.setManualReviewReason("Booking was already confirmed");return payments.save(p);}
  if(b.getStatus()!=BookingStatus.INITIATED||b.getExpiresAt()!=null&&!LocalDateTime.now(clock).isBefore(b.getExpiresAt()))return review(p,"Booking is no longer confirmable");
  try{shows.assertBookable(b.getShow());}catch(RuntimeException e){return review(p,"Show is no longer bookable");}
  var claims=bookingSeats.findByBookingId(b.getId());var ids=claims.stream().map(x->x.getSeat().getId()).sorted().toList();var locked=seats.findByShowIdAndSeatIdsWithLock(b.getShow().getId(),ids);
  if(locked.size()!=ids.size()||locked.stream().anyMatch(s->s.getSeatStatus()!=SeatStatus.RESERVED))return review(p,"Reserved seats are no longer available");
  locked.forEach(s->s.setSeatStatus(SeatStatus.BOOKED));seats.saveAll(locked);b.setStatus(BookingStatus.CONFIRMED);b.setConfirmedAt(LocalDateTime.now(clock));b.setConfirmationSource(ConfirmationSource.SYSTEM);bookings.save(b);return payments.save(p);
 }
 private Payment review(Payment p,String reason){p.setManualReviewRequired(true);p.setManualReviewReason(reason);return payments.save(p);}
}
