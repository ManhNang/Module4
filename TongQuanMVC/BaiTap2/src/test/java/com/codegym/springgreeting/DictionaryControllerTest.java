package com.codegym.springgreeting;

import com.codegym.springgreeting.controller.DictionaryController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.*;

class DictionaryControllerTest {

    private DictionaryController controller;
    private Model model;

    @BeforeEach
    void setUp() {
        controller = new DictionaryController();
        model = new ConcurrentModel();
    }

    @Test
    void testIndexReturnsIndexView() {
        String view = controller.index(model);
        assertEquals("index", view);
        assertNotNull(model.getAttribute("sampleWords"));
    }

    @Test
    void testLookupExistingWord() {
        String view = controller.lookup("hello", model);
        assertEquals("index", view);
        assertEquals(true, model.getAttribute("found"));
        assertEquals("Xin chào", model.getAttribute("meaning"));
    }

    @Test
    void testLookupNonExistingWord() {
        String view = controller.lookup("unknownxyz", model);
        assertEquals("index", view);
        assertEquals(false, model.getAttribute("found"));
        assertNotNull(model.getAttribute("message"));
    }

    @Test
    void testLookupBlankWord() {
        String view = controller.lookup("   ", model);
        assertEquals("index", view);
        assertEquals(false, model.getAttribute("found"));
        assertEquals("Vui lòng nhập từ tiếng Anh cần tra cứu.", model.getAttribute("message"));
    }
}
