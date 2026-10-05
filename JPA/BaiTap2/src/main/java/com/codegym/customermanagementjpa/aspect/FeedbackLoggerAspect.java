package com.codegym.customermanagementjpa.aspect;

import com.codegym.customermanagementjpa.exception.BadWordException;
import com.codegym.customermanagementjpa.model.Feedback;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;

@Aspect
@Component
public class FeedbackLoggerAspect {

    private static final Logger logger = LoggerFactory.getLogger(FeedbackLoggerAspect.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String LOG_FILE_PATH = "logs/bad_words.log";

    /**
     * Intercept khi có ngoại lệ BadWordException ném ra trong quá trình lưu feedback.
     * Yêu cầu: Ghi rõ author, nội dung feedback, ngày giờ feedback vào log.
     */
    @AfterThrowing(
            pointcut = "execution(* com.codegym.customermanagementjpa.service.IFeedbackService.save(..)) || " +
                       "execution(* com.codegym.customermanagementjpa.service.FeedbackService.save(..))",
            throwing = "ex"
    )
    public void logBadWordException(JoinPoint joinPoint, Exception ex) {
        if (ex instanceof BadWordException badWordEx) {
            Feedback feedback = badWordEx.getFeedback();

            // Nếu feedback trong exception null thì lấy từ đối số phương thức
            if (feedback == null && joinPoint.getArgs().length > 0 && joinPoint.getArgs()[0] instanceof Feedback) {
                feedback = (Feedback) joinPoint.getArgs()[0];
            }

            String author = (feedback != null && feedback.getAuthor() != null) ? feedback.getAuthor() : "Không xác định";
            String content = (feedback != null && feedback.getFeedback() != null) ? feedback.getFeedback() : "Trống";
            String formattedTime = badWordEx.getTimestamp().format(FORMATTER);
            String badWord = badWordEx.getBadWord();

            String logFormatted = String.format(
                    "\n" +
                    "====================== [LOG CẢNH BÁO TỪ XẤU (AOP)] ======================\n" +
                    "  [THỜI GIAN] : %s\n" +
                    "  [TÁC GIẢ]   : %s\n" +
                    "  [NỘI DUNG]  : %s\n" +
                    "  [TỪ XẤU]    : %s\n" +
                    "  [CHI TIẾT]  : %s\n" +
                    "=========================================================================",
                    formattedTime, author, content, badWord, badWordEx.getMessage()
            );

            // 1. Ghi log qua SLF4J
            logger.error(logFormatted);

            // 2. In ra console System.err để quan sát trực tiếp trên terminal
            System.err.println(logFormatted);

            // 3. Ghi vào file log riêng (logs/bad_words.log) phục vụ kiểm tra
            writeLogToFile(formattedTime, author, content, badWord, badWordEx.getMessage());
        }
    }

    private static final String[] LOG_FILE_PATHS = new String[]{
            "d:/Module4/JPA/BaiTap2/logs/bad_words.log",
            "logs/bad_words.log"
    };

    private synchronized void writeLogToFile(String time, String author, String content, String badWord, String message) {
        for (String path : LOG_FILE_PATHS) {
            try {
                File logFile = new File(path);
                File parentDir = logFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }

                try (FileWriter fw = new FileWriter(logFile, true);
                     PrintWriter pw = new PrintWriter(fw)) {
                    pw.printf("[%s] [BAD_WORD_DETECTED] Author: \"%s\" | BadWord: \"%s\" | Content: \"%s\" | Msg: \"%s\"%n",
                            time, author, badWord, content.replace("\n", " "), message);
                }
            } catch (Exception e) {
                logger.warn("Không thể ghi log ra file {}: {}", path, e.getMessage());
            }
        }
    }
}
