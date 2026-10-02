package com.codegym.controller;

import com.codegym.aspect.LibraryAspect;
import com.codegym.model.Book;
import com.codegym.model.BorrowTicket;
import com.codegym.service.IBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping({"/", "/books"})
public class BookController {

    private final IBookService bookService;
    private final LibraryAspect libraryAspect;

    @Autowired
    public BookController(IBookService bookService, LibraryAspect libraryAspect) {
        this.bookService = bookService;
        this.libraryAspect = libraryAspect;
    }

    @ModelAttribute("visitorCount")
    public int populateVisitorCount() {
        return libraryAspect.getVisitorCount();
    }

    // Danh sách các đầu sách
    @GetMapping
    public String listBooks(Model model) {
        List<Book> books = bookService.findAll();
        model.addAttribute("books", books);
        return "index";
    }

    // Màn hình chi tiết cuốn sách (có nút Mượn sách)
    @GetMapping("/{id}/view")
    public String viewBook(@PathVariable("id") Long id, Model model) {
        Book book = bookService.findById(id);
        model.addAttribute("book", book);
        return "view";
    }

    // Xử lý mượn sách
    @PostMapping("/{id}/borrow")
    public String borrowBook(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        BorrowTicket ticket = bookService.borrowBook(id);
        redirectAttributes.addFlashAttribute("successMessage", 
                "Mượn sách thành công! Bạn hãy ghi nhớ mã số mượn sách gồm 5 chữ số dưới đây để trả sách sau này.");
        redirectAttributes.addFlashAttribute("borrowTicket", ticket);
        return "redirect:/books/" + id + "/view";
    }

    // Màn hình trả sách
    @GetMapping("/return")
    public String returnBookForm() {
        return "return";
    }

    // Xử lý trả sách
    @PostMapping("/return")
    public String processReturnBook(@RequestParam("borrowCode") String borrowCode, RedirectAttributes redirectAttributes) {
        Book book = bookService.returnBook(borrowCode);
        redirectAttributes.addFlashAttribute("successMessage", 
                "Trả sách thành công! Cuốn sách \"" + book.getTitle() + "\" đã được hoàn trả về thư viện. Số lượng sách hiện tại: " + book.getQuantity() + ".");
        return "redirect:/books";
    }

    // Màn hình thêm mới sách
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("book", new Book());
        return "create";
    }

    // Xử lý thêm mới sách
    @PostMapping("/create")
    public String saveBook(@ModelAttribute("book") Book book, RedirectAttributes redirectAttributes) {
        bookService.save(book);
        redirectAttributes.addFlashAttribute("successMessage", "Thêm mới đầu sách \"" + book.getTitle() + "\" thành công!");
        return "redirect:/books";
    }

    // Màn hình cập nhật sách
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        Book book = bookService.findById(id);
        model.addAttribute("book", book);
        return "update";
    }

    // Xử lý cập nhật sách
    @PostMapping("/{id}/edit")
    public String updateBook(@ModelAttribute("book") Book book, RedirectAttributes redirectAttributes) {
        bookService.save(book);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật đầu sách \"" + book.getTitle() + "\" thành công!");
        return "redirect:/books";
    }

    // Màn hình xác nhận xóa sách
    @GetMapping("/{id}/delete")
    public String showDeleteForm(@PathVariable("id") Long id, Model model) {
        Book book = bookService.findById(id);
        model.addAttribute("book", book);
        return "delete";
    }

    // Xử lý xóa sách
    @PostMapping("/{id}/delete")
    public String deleteBook(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        Book book = bookService.findById(id);
        bookService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa đầu sách \"" + book.getTitle() + "\" khỏi thư viện!");
        return "redirect:/books";
    }
}
