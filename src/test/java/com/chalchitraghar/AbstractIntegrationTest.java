package com.chalchitraghar;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Clock;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.chalchitraghar.modules.bookings.repository.BookingRepository;
import com.chalchitraghar.modules.bookings.repository.BookingSeatRepository;
import com.chalchitraghar.modules.auth.repository.PasswordResetOtpRepository;
import com.chalchitraghar.modules.halls.entity.Hall;
import com.chalchitraghar.modules.halls.enums.Status;
import com.chalchitraghar.modules.halls.repository.HallRepository;
import com.chalchitraghar.modules.halls.repository.SeatTemplateRepository;
import com.chalchitraghar.modules.movies.entity.Movie;
import com.chalchitraghar.modules.movies.enums.MovieStatus;
import com.chalchitraghar.modules.movies.repository.MovieRepository;
import com.chalchitraghar.modules.payments.repository.PaymentRepository;
import com.chalchitraghar.modules.payments.entity.Payment;
import com.chalchitraghar.modules.payments.enums.PaymentProvider;
import com.chalchitraghar.modules.payments.enums.PaymentMethod;
import com.chalchitraghar.modules.payments.enums.PaymentStatus;
import com.chalchitraghar.modules.seats.entity.Seat;
import com.chalchitraghar.modules.seats.enums.SeatStatus;
import com.chalchitraghar.modules.seats.repository.SeatRepository;
import com.chalchitraghar.modules.shows.entity.Show;
import com.chalchitraghar.modules.shows.repository.ShowRepository;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.modules.users.enums.Role;
import com.chalchitraghar.modules.users.repository.UserRepository;
import com.chalchitraghar.modules.tickets.repository.TicketRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class AbstractIntegrationTest {

    @Autowired
    protected Clock clock;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected BCryptPasswordEncoder passwordEncoder;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordResetOtpRepository passwordResetOtpRepository;

    @Autowired
    protected MovieRepository movieRepository;

    @Autowired
    protected HallRepository hallRepository;

    @Autowired
    protected SeatTemplateRepository seatTemplateRepository;

    @Autowired
    protected ShowRepository showRepository;

    @Autowired
    protected SeatRepository seatRepository;

    @Autowired
    protected BookingRepository bookingRepository;

    @Autowired
    protected BookingSeatRepository bookingSeatRepository;

    @Autowired
    protected PaymentRepository paymentRepository;

    @Autowired
    protected TicketRepository ticketRepository;

    @BeforeEach
    void cleanDatabase() {
        ticketRepository.deleteAll();
        paymentRepository.deleteAll();
        bookingSeatRepository.deleteAll();
        bookingRepository.deleteAll();
        seatRepository.deleteAll();
        showRepository.deleteAll();
        seatTemplateRepository.deleteAll();
        hallRepository.deleteAll();
        movieRepository.deleteAll();
        passwordResetOtpRepository.deleteAll();
        userRepository.deleteAll();
    }

    protected String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }

    protected User saveUser(String email, Role role) {
        User user = User.builder()
                .name(role.name() + " User")
                .email(email)
                .password(passwordEncoder.encode("password123"))
                .role(role)
                .build();
        return userRepository.save(user);
    }

    protected String loginToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(json(Map.of("email", email, "password", "password123"))))
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.path("data").path("token").asText();
    }

    protected String tokenFor(String email, Role role) throws Exception {
        saveUser(email, role);
        return loginToken(email);
    }

    protected Movie saveMovie(String title, MovieStatus status) {
        return movieRepository.save(Movie.builder()
                .title(title)
                .genre("Drama")
                .durationMinutes(120)
                .language("Nepali")
                .description("Test movie")
                .posterUrl("https://example.com/poster.jpg")
                .releaseDate(LocalDate.now(clock).minusDays(5))
                .status(status)
                .build());
    }

    protected Hall saveHall(String name, Status status) {
        Hall hall = hallRepository.save(Hall.builder()
                .name(name)
                .capacity(188)
                .layoutRef("standard")
                .status(status)
                .build());
        if (status == Status.INACTIVE) {
            hall.setStatus(Status.INACTIVE);
            return hallRepository.save(hall);
        }
        return hall;
    }

    protected JsonNode postSeatLayout(Long hallId, String adminToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/halls/{hallId}/seat-layout", hallId)
                        .header("Authorization", bearer(adminToken)))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    protected Show saveShowWithSeats(Movie movie, Hall hall, String adminToken) throws Exception {
        postSeatLayout(hall.getId(), adminToken);
        MvcResult result = mockMvc.perform(post("/api/admin/shows")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(json(showRequest(movie.getId(), hall.getId(), 10, "10:00", "12:00"))))
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        Long showId = body.path("data").path("id").asLong();
        return showRepository.findById(showId).orElseThrow();
    }

    protected Map<String, Object> movieRequest(String title, MovieStatus status) {
        return Map.of(
                "title", title,
                "genre", "Drama",
                "durationMinutes", 120,
                "language", "Nepali",
                "description", "Test movie",
                "posterUrl", "https://example.com/poster.jpg",
                "releaseDate", LocalDate.now(clock).toString(),
                "status", status.name()
        );
    }

    protected Map<String, Object> hallRequest(String name, Status status) {
        return hallRequest(name, 188, "standard", status);
    }

    protected Map<String, Object> hallRequest(String name, Integer capacity, String layoutRef, Status status) {
        return Map.of(
                "name", name,
                "capacity", capacity,
                "layoutRef", layoutRef,
                "status", status.name()
        );
    }

    protected Map<String, Object> showRequest(Long movieId, Long hallId, int daysFromNow, String showTime, String endTime) {
        return Map.of(
                "movieId", movieId,
                "hallId", hallId,
                "showDate", LocalDate.now(clock).plusDays(daysFromNow).toString(),
                "showTime", LocalTime.parse(showTime).toString(),
                "endTime", LocalTime.parse(endTime).toString()
        );
    }

    protected List<Seat> seatsForShow(Long showId) {
        return seatRepository.findByShowIdOrderByPositionIndexAsc(showId);
    }

    protected void expireLock(Seat seat) {
        seat.setSeatStatus(SeatStatus.LOCKED);
        seat.setLockedAt(java.time.LocalDateTime.now(clock).minusMinutes(20));
        seat.setLockExpiresAt(java.time.LocalDateTime.now(clock).minusMinutes(10));
        seatRepository.save(seat);
    }

    protected Payment saveSuccessfulPayment(com.chalchitraghar.modules.bookings.entity.Booking booking) {
        String reference = "PAY-TEST-" + booking.getId() + "-" + System.nanoTime();
        return paymentRepository.save(Payment.builder().booking(booking).paymentReference(reference)
                .provider(PaymentProvider.ESEWA).method(PaymentMethod.ONLINE).status(PaymentStatus.SUCCESS)
                .amount(booking.getTotalAmount()).currency(booking.getCurrency()).providerTransactionId(reference)
                .initiatedAt(java.time.LocalDateTime.now(clock)).completedAt(java.time.LocalDateTime.now(clock)).build());
    }
}
