package com.chalchitraghar.applications.customer;

import java.util.List;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.chalchitraghar.modules.tickets.dto.response.*;
import com.chalchitraghar.modules.tickets.service.TicketService;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.shared.exception.AuthenticationException;
import com.chalchitraghar.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController @RequestMapping("/api/customer") @RequiredArgsConstructor
@Tag(name="Customer Tickets",description="Owner-only per-seat ticket reads; QR and check-in are deferred")
@SecurityRequirement(name="bearerAuth")
public class TicketController {
 private final TicketService service;
 private final com.chalchitraghar.modules.tickets.service.TicketPdfService pdfService;
 private final com.chalchitraghar.modules.tickets.service.TicketQueryService queries;
 @GetMapping("/tickets") @Operation(summary="List my tickets",description="Owner-scoped paginated ticket history.")
 public ResponseEntity<ApiResponse<com.chalchitraghar.shared.response.PageResponse<CustomerTicketSummaryResponse>>> list(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size,@RequestParam(defaultValue="issuedAt")String sortBy,@RequestParam(defaultValue="desc")String sortDir,@RequestParam(required=false)String status,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate showDateFrom,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate showDateTo){var c=new com.chalchitraghar.modules.tickets.dto.request.TicketSearchCriteria(null,status,null,null,null,null,null,showDateFrom,showDateTo,null,null,null,null,null,null);return ResponseEntity.ok(ApiResponse.success("Tickets fetched successfully",queries.customer(user(),c,page,size,sortBy,sortDir)));}
 @GetMapping("/tickets/{ticketReference}") @Operation(summary="Get my ticket",description="Unknown and non-owned references return not found.")
 public ResponseEntity<ApiResponse<CustomerTicketDetailResponse>> get(@PathVariable String ticketReference){return ResponseEntity.ok(ApiResponse.success("Ticket fetched successfully",service.customerTicket(ticketReference,user())));}
 @GetMapping("/bookings/{bookingReference}/tickets") @Operation(summary="List tickets for my booking",description="One issued ticket per booked seat, ordered by seat position.")
 public ResponseEntity<ApiResponse<List<CustomerTicketSummaryResponse>>> booking(@PathVariable String bookingReference){return ResponseEntity.ok(ApiResponse.success("Tickets fetched successfully",service.customerBookingTickets(bookingReference,user())));}
 @GetMapping(value="/tickets/{ticketReference}/qr",produces=MediaType.IMAGE_PNG_VALUE) @Operation(summary="View my ticket QR",description="Decrypts the owner-only opaque token and renders a 400x400 PNG on demand. No booking or customer data is encoded.")
 public ResponseEntity<byte[]> qr(@PathVariable String ticketReference){return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).header(HttpHeaders.CACHE_CONTROL,"private, no-store").body(service.customerQrPng(ticketReference,user()));}
 @GetMapping("/tickets/{ticketReference}/qr-data") @Operation(summary="Get safe QR metadata",description="Returns version and issuance time; never returns token, hash, or ciphertext.")
 public ResponseEntity<ApiResponse<CustomerQrDataResponse>> qrData(@PathVariable String ticketReference){return ResponseEntity.ok(ApiResponse.success("QR metadata fetched successfully",service.customerQrData(ticketReference,user())));}
 @GetMapping(value="/tickets/{ticketReference}/pdf",produces="application/pdf") @Operation(summary="Download my ticket PDF")
 public ResponseEntity<byte[]> pdf(@PathVariable String ticketReference){byte[] bytes=pdfService.customerTicket(ticketReference,user());return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).contentLength(bytes.length).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"ticket-"+ticketReference.replaceAll("[^A-Za-z0-9-]","")+".pdf\"").body(bytes);}
 @GetMapping(value="/bookings/{bookingReference}/tickets/pdf",produces="application/pdf") @Operation(summary="Download my booking ticket bundle")
 public ResponseEntity<byte[]> bookingPdf(@PathVariable String bookingReference){byte[] bytes=pdfService.customerBooking(bookingReference,user());return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).contentLength(bytes.length).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"tickets-"+bookingReference.replaceAll("[^A-Za-z0-9-]","")+".pdf\"").body(bytes);}
 private User user(){var a=SecurityContextHolder.getContext().getAuthentication();if(a==null||!(a.getPrincipal() instanceof User u))throw new AuthenticationException("User not authenticated");return u;}
}
