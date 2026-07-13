package com.chalchitraghar.modules.tickets.service.impl;

import com.chalchitraghar.modules.bookings.enums.BookingStatus;
import com.chalchitraghar.modules.bookings.repository.BookingRepository;
import com.chalchitraghar.modules.bookings.repository.BookingSeatRepository;
import com.chalchitraghar.modules.tickets.config.TicketOperationsProperties;
import com.chalchitraghar.modules.tickets.entity.Ticket;
import com.chalchitraghar.modules.tickets.repository.TicketRepository;
import com.chalchitraghar.modules.tickets.service.*;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.shared.exception.*;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import java.io.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketPdfServiceImpl implements TicketPdfService {
    private final TicketRepository tickets;
    private final BookingRepository bookings;
    private final BookingSeatRepository bookingSeats;
    private final QrTokenService tokens;
    private final QrImageService images;
    private final TicketOperationsProperties props;
    private final Clock clock;

    public byte[] customerTicket(String ref, User u) {
        Ticket t =
                tickets.findByTicketReferenceAndBookingUserId(ref.trim().toUpperCase(), u.getId())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Ticket not found with reference: " + ref));
        return render(java.util.List.of(t));
    }

    public byte[] customerBooking(String ref, User u) {
        var b =
                bookings.findByBookingReference(ref.trim().toUpperCase())
                        .filter(x -> x.getUser().getId().equals(u.getId()))
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Booking not found with reference: " + ref));
        if (b.getStatus() != BookingStatus.CONFIRMED)
            throw new InvalidBookingStateException(
                    "Only confirmed booking tickets can be downloaded");
        var list = ordered(tickets.findByBookingIdOrderByIssuedAtAsc(b.getId()));
        if (list.size() != bookingSeats.findByBookingId(b.getId()).size())
            throw new InvalidBookingStateException(
                    "Ticket issuance is incomplete for this booking");
        return render(list);
    }

    public byte[] bookingPdf(Long id) {
        var list = ordered(tickets.findByBookingIdOrderByIssuedAtAsc(id));
        if (list.isEmpty())
            throw new ResourceNotFoundException("Tickets not found for booking: " + id);
        return render(list);
    }

    private java.util.List<Ticket> ordered(java.util.List<Ticket> l) {
        return l.stream()
                .sorted(Comparator.comparing(t -> t.getBookingSeat().getSeat().getPositionIndex()))
                .toList();
    }

    private byte[] render(java.util.List<Ticket> list) {
        if (!props.pdfEnabled())
            throw new IllegalStateException("Ticket PDF generation is disabled");
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 48, 48, 48, 48);
            PdfWriter.getInstance(doc, out);
            doc.addTitle("Hamro Chalchitraghar Tickets");
            doc.open();
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) doc.newPage();
                page(doc, list.get(i));
            }
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to generate ticket PDF", e);
        }
    }

    private void page(Document d, Ticket t) throws Exception {
        var b = t.getBooking();
        var s = t.getBookingSeat().getSeat();
        var show = b.getShow();
        Font title = new Font(Font.HELVETICA, 22, Font.BOLD);
        Font heading = new Font(Font.HELVETICA, 13, Font.BOLD);
        d.add(new Paragraph("Hamro Chalchitraghar", title));
        d.add(new Paragraph("Cinema Admission Ticket", heading));
        d.add(Chunk.NEWLINE);
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        add(table, "Ticket reference", t.getTicketReference());
        add(table, "Booking reference", b.getBookingReference());
        add(table, "Customer", b.getUser().getName());
        add(table, "Movie", show.getMovie().getTitle());
        add(table, "Hall", show.getHall().getName());
        add(table, "Show date", show.getShowDate().toString());
        add(table, "Show time", show.getShowTime() + " - " + show.getEndTime());
        add(table, "Seat", s.getSeatCode() + " (" + s.getSeatType() + ")");
        add(table, "Status", t.getStatus().name());
        d.add(table);
        d.add(Chunk.NEWLINE);
        Image qr = Image.getInstance(images.generatePng(tokens.decryptToken(t)));
        qr.scaleAbsolute(220, 220);
        qr.setAlignment(Image.ALIGN_CENTER);
        d.add(qr);
        Paragraph note =
                new Paragraph(
                        t.getStatus()
                                        == com.chalchitraghar.modules.tickets.enums.TicketStatus
                                                .ISSUED
                                ? "Present this QR at admission. Each QR is single-use."
                                : "NOT VALID FOR ADMISSION - ticket status: " + t.getStatus(),
                        new Font(Font.HELVETICA, 11, Font.BOLD));
        note.setAlignment(Element.ALIGN_CENTER);
        d.add(note);
        d.add(new Paragraph("Generated: " + LocalDateTime.now(clock)));
    }

    private void add(PdfPTable t, String a, String b) {
        t.addCell(new PdfPCell(new Phrase(a, new Font(Font.HELVETICA, 10, Font.BOLD))));
        t.addCell(new PdfPCell(new Phrase(b == null ? "" : b)));
    }
}
