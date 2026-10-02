package com.codegym.service;

import com.codegym.exception.BookNotFoundException;
import com.codegym.exception.BookOutOfStockException;
import com.codegym.exception.InvalidBorrowCodeException;
import com.codegym.model.Book;
import com.codegym.model.BorrowTicket;
import com.codegym.repository.BookRepository;
import com.codegym.repository.BorrowTicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@Transactional
public class BookServiceImpl implements IBookService {

    private final BookRepository bookRepository;
    private final BorrowTicketRepository borrowTicketRepository;

    @Autowired
    public BookServiceImpl(BookRepository bookRepository, BorrowTicketRepository borrowTicketRepository) {
        this.bookRepository = bookRepository;
        this.borrowTicketRepository = borrowTicketRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Book> findAll() {
        return bookRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Book findById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException("Không tìm thấy cuốn sách với mã số: " + id));
    }

    @Override
    public void save(Book book) {
        bookRepository.save(book);
    }

    @Override
    public void delete(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new BookNotFoundException("Không tìm thấy cuốn sách với mã số: " + id);
        }
        bookRepository.deleteById(id);
    }

    @Override
    public BorrowTicket borrowBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException("Không tìm thấy cuốn sách với mã số: " + bookId));

        if (book.getQuantity() <= 0) {
            throw new BookOutOfStockException("Sách \"" + book.getTitle() + "\" hiện tại đã hết số lượng (còn lại: 0), không thể mượn thêm!");
        }

        // Giảm số lượng sách đi 1
        book.setQuantity(book.getQuantity() - 1);
        bookRepository.save(book);

        // Sinh mã mượn sách ngẫu nhiên gồm 5 chữ số (10000 - 99999)
        Random random = new Random();
        String code;
        do {
            int randomNum = 10000 + random.nextInt(90000);
            code = String.valueOf(randomNum);
        } while (borrowTicketRepository.existsByBorrowCode(code));

        BorrowTicket ticket = new BorrowTicket(code, book, LocalDateTime.now());
        return borrowTicketRepository.save(ticket);
    }

    @Override
    public Book returnBook(String borrowCode) {
        if (borrowCode == null || borrowCode.trim().isEmpty()) {
            throw new InvalidBorrowCodeException("Vui lòng nhập mã mượn sách gồm 5 chữ số!");
        }

        String trimmedCode = borrowCode.trim();
        BorrowTicket ticket = borrowTicketRepository.findByBorrowCode(trimmedCode)
                .orElseThrow(() -> new InvalidBorrowCodeException("Mã mượn sách \"" + trimmedCode + "\" không hợp lệ hoặc không tồn tại trong hệ thống!"));

        if (ticket.isReturned()) {
            throw new InvalidBorrowCodeException("Mã mượn sách \"" + trimmedCode + "\" đã được hoàn trả sách trước đó!");
        }

        // Đánh dấu đã trả sách
        ticket.setReturned(true);
        ticket.setReturnDate(LocalDateTime.now());
        borrowTicketRepository.save(ticket);

        // Tăng số lượng sách tương ứng lên 1
        Book book = ticket.getBook();
        book.setQuantity(book.getQuantity() + 1);
        return bookRepository.save(book);
    }
}
