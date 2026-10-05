package com.codegym.customermanagementjpa;

import com.codegym.customermanagementjpa.aspect.FeedbackLoggerAspect;
import com.codegym.customermanagementjpa.exception.BadWordException;
import com.codegym.customermanagementjpa.exception.GlobalExceptionHandler;
import com.codegym.customermanagementjpa.model.Feedback;
import com.codegym.customermanagementjpa.repository.IFeedbackRepository;
import com.codegym.customermanagementjpa.service.BadWordFilter;
import com.codegym.customermanagementjpa.service.FeedbackService;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BadWordAndAopTest {

    @Mock
    private IFeedbackRepository feedbackRepository;

    private BadWordFilter badWordFilter;
    private FeedbackService feedbackService;
    private FeedbackLoggerAspect feedbackLoggerAspect;
    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        badWordFilter = new BadWordFilter();
        feedbackService = new FeedbackService(feedbackRepository, badWordFilter);
        feedbackLoggerAspect = new FeedbackLoggerAspect();
        globalExceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Test 1: BadWordFilter phát hiện chính xác từ xấu trong danh sách")
    void testBadWordDetection() {
        assertTrue(badWordFilter.containsBadWord("Bức ảnh này xấu quá"));
        assertEquals("xấu", badWordFilter.findFirstBadWord("Bức ảnh này xấu quá"));

        assertTrue(badWordFilter.containsBadWord("Nội dung dở tệ"));
        assertTrue(badWordFilter.containsBadWord("spam quảng cáo"));
        assertTrue(badWordFilter.containsBadWord("Đồ ngu"));

        assertFalse(badWordFilter.containsBadWord("Bức ảnh thiên văn này tuyệt đẹp!"));
        assertNull(badWordFilter.findFirstBadWord("Bức ảnh thiên văn này tuyệt đẹp!"));
    }

    @Test
    @DisplayName("Test 2: FeedbackService lưu thành công khi không có từ xấu")
    void testSaveFeedbackSuccess() {
        Feedback validFb = new Feedback(5, "Học viên CodeGym", "Bức ảnh hôm nay rất đẹp và kỳ ảo.");
        when(feedbackRepository.save(any(Feedback.class))).thenReturn(validFb);

        Feedback saved = feedbackService.save(validFb);
        assertNotNull(saved);
        assertEquals("Học viên CodeGym", saved.getAuthor());
        verify(feedbackRepository, times(1)).save(validFb);
    }

    @Test
    @DisplayName("Test 3: FeedbackService ném BadWordException khi feedback chứa từ xấu")
    void testSaveFeedbackThrowsExceptionOnBadWord() {
        Feedback badFb = new Feedback(1, "Người ẩn danh", "Ảnh quá xấu và dở!");

        BadWordException exception = assertThrows(BadWordException.class, () -> {
            feedbackService.save(badFb);
        });

        assertNotNull(exception.getBadWord());
        assertEquals(badFb, exception.getFeedback());
        assertTrue(exception.getMessage().contains("không phù hợp"));
        // Đảm bảo không lưu vào database
        verify(feedbackRepository, never()).save(any(Feedback.class));
    }

    @Test
    @DisplayName("Test 4: FeedbackLoggerAspect (Spring AOP) ghi log chi tiết khi xảy ra lỗi từ xấu")
    void testAspectLogging() {
        Feedback badFb = new Feedback(1, "NguyenVanA", "Toàn tin lừa đảo spam");
        badFb.setDate(LocalDate.now());
        BadWordException badWordEx = new BadWordException("Phát hiện từ cấm: lừa đảo", badFb, "lừa đảo");

        JoinPoint joinPoint = mock(JoinPoint.class);
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{badFb});

        // Kích hoạt Advice @AfterThrowing của Aspect
        assertDoesNotThrow(() -> {
            feedbackLoggerAspect.logBadWordException(joinPoint, badWordEx);
        });

        // Kiểm tra file log đã được ghi nhận
        File logFile = new File("logs/bad_words.log");
        assertTrue(logFile.exists(), "File log bad_words.log phải được tạo ra");
    }

    @Test
    @DisplayName("Test 5: GlobalExceptionHandler bắt BadWordException và trả về error page")
    void testExceptionHandlerReturnsErrorView() {
        Feedback badFb = new Feedback(1, "User123", "Chửi bậy vcl");
        BadWordException ex = new BadWordException("Từ ngữ xấu: bậy", badFb, "bậy");
        Model model = new ConcurrentModel();

        String viewName = globalExceptionHandler.handleBadWordException(ex, model);

        assertEquals("error", viewName);
        assertEquals("bậy", model.getAttribute("badWord"));
        assertEquals("User123", model.getAttribute("author"));
        assertEquals("Chửi bậy vcl", model.getAttribute("content"));
        assertNotNull(model.getAttribute("timestamp"));
    }
}
