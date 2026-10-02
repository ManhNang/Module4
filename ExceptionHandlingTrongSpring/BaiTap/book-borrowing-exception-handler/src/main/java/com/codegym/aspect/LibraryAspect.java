package com.codegym.aspect;

import com.codegym.model.Book;
import com.codegym.model.BorrowTicket;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Aspect
@Component
public class LibraryAspect {

    private static final Logger logger = LoggerFactory.getLogger(LibraryAspect.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Đếm tổng số lượt thao tác ghé thăm thư viện
    private final AtomicInteger visitorCount = new AtomicInteger(0);

    /**
     * Yêu cầu: Ghi log số lượng người đã ghé thăm thư viện sách (tất cả các thao tác).
     * Sử dụng join point khác với afterthrowing: ở đây dùng @After trên tất cả controller action.
     */
    @After("execution(* com.codegym.controller.*.*(..))")
    public void logVisitorTraffic(JoinPoint joinPoint) {
        int currentCount = visitorCount.incrementAndGet();
        String methodName = joinPoint.getSignature().toShortString();
        String timestamp = LocalDateTime.now().format(FORMATTER);

        logger.info("\n========================= [THỐNG KÊ LƯỢT GHÉ THĂM] =========================");
        logger.info(">> Thời gian: {}", timestamp);
        logger.info(">> Phương thức vừa được gọi: {}", methodName);
        logger.info(">> Tổng số lượt ghé thăm thư viện: {}", currentCount);
        logger.info("===========================================================================\n");
    }

    /**
     * Yêu cầu: Ghi log tất cả các hành động khiến trạng thái sách của thư viện bị thay đổi.
     * 1. Hành động Mượn sách
     */
    @AfterReturning(pointcut = "execution(* com.codegym.service.IBookService.borrowBook(..))", returning = "result")
    public void logAfterBorrowBook(JoinPoint joinPoint, Object result) {
        if (result instanceof BorrowTicket ticket) {
            String timestamp = LocalDateTime.now().format(FORMATTER);
            logger.info("\n[TRẠNG THÁI SÁCH THAY ĐỔI] ===> MƯỢN SÁCH THÀNH CÔNG");
            logger.info(">> Thời gian: {}", timestamp);
            logger.info(">> Cuốn sách: \"{}\" (Mã ID: {})", ticket.getBook().getTitle(), ticket.getBook().getId());
            logger.info(">> Số lượng sách còn lại trong thư viện: {}", ticket.getBook().getQuantity());
            logger.info(">> Cấp mã số mượn sách ngẫu nhiên (5 chữ số): {}", ticket.getBorrowCode());
            logger.info("============================================================\n");
        }
    }

    /**
     * Yêu cầu: Ghi log tất cả các hành động khiến trạng thái sách của thư viện bị thay đổi.
     * 2. Hành động Trả sách
     */
    @AfterReturning(pointcut = "execution(* com.codegym.service.IBookService.returnBook(..))", returning = "result")
    public void logAfterReturnBook(JoinPoint joinPoint, Object result) {
        if (result instanceof Book book) {
            Object[] args = joinPoint.getArgs();
            String code = args.length > 0 ? String.valueOf(args[0]) : "N/A";
            String timestamp = LocalDateTime.now().format(FORMATTER);

            logger.info("\n[TRẠNG THÁI SÁCH THAY ĐỔI] ===> TRẢ SÁCH THÀNH CÔNG");
            logger.info(">> Thời gian: {}", timestamp);
            logger.info(">> Đã hoàn trả mã mượn: {}", code);
            logger.info(">> Cuốn sách đã trả: \"{}\" (Mã ID: {})", book.getTitle(), book.getId());
            logger.info(">> Số lượng sách trong kho tăng lên: {}", book.getQuantity());
            logger.info("============================================================\n");
        }
    }

    /**
     * Yêu cầu: Ghi log tất cả các hành động khiến trạng thái sách của thư viện bị thay đổi.
     * 3. Hành động Thêm mới / Cập nhật sách
     */
    @AfterReturning(pointcut = "execution(* com.codegym.service.IBookService.save(..))")
    public void logAfterSaveBook(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof Book book) {
            String timestamp = LocalDateTime.now().format(FORMATTER);
            logger.info("\n[TRẠNG THÁI SÁCH THAY ĐỔI] ===> LƯU HOẶC CẬP NHẬT ĐẦU SÁCH");
            logger.info(">> Thời gian: {}", timestamp);
            logger.info(">> Tên sách: \"{}\" | Tác giả: \"{}\" | Số lượng: {}", book.getTitle(), book.getAuthor(), book.getQuantity());
            logger.info("============================================================\n");
        }
    }

    /**
     * Yêu cầu: Ghi log tất cả các hành động khiến trạng thái sách của thư viện bị thay đổi.
     * 4. Hành động Xóa sách
     */
    @AfterReturning(pointcut = "execution(* com.codegym.service.IBookService.delete(..))")
    public void logAfterDeleteBook(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        String bookId = args.length > 0 ? String.valueOf(args[0]) : "N/A";
        String timestamp = LocalDateTime.now().format(FORMATTER);

        logger.info("\n[TRẠNG THÁI SÁCH THAY ĐỔI] ===> XÓA ĐẦU SÁCH");
        logger.info(">> Thời gian: {}", timestamp);
        logger.info(">> Đã xóa đầu sách có ID: {}", bookId);
        logger.info("============================================================\n");
    }

    /**
     * Ghi log khi xảy ra ngoại lệ trong quá trình thao tác với sách
     */
    @AfterThrowing(pointcut = "execution(* com.codegym.service.IBookService.*(..))", throwing = "ex")
    public void logAfterServiceThrowing(JoinPoint joinPoint, Throwable ex) {
        logger.warn("\n[NGOẠI LỆ ĐÃ XẢY RA] ===> Phương thức: {}", joinPoint.getSignature().toShortString());
        logger.warn(">> Thông báo lỗi: {}", ex.getMessage());
        logger.warn("============================================================\n");
    }

    public int getVisitorCount() {
        return visitorCount.get();
    }
}
