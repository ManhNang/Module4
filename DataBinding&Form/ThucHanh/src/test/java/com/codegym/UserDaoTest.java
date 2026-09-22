package com.codegym;

import com.codegym.controller.UserController;
import com.codegym.model.Login;
import com.codegym.model.User;
import com.codegym.model.UserDao;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

public class UserDaoTest {

    @Test
    public void testCheckLoginSuccess() {
        Login login = new Login("john", "123456");
        User user = UserDao.checkLogin(login);
        Assertions.assertNotNull(user);
        Assertions.assertEquals("John", user.getName());
        Assertions.assertEquals("john@codegym.vn", user.getEmail());
        Assertions.assertEquals(21, user.getAge());
    }

    @Test
    public void testCheckLoginFailure() {
        Login login = new Login("unknown", "wrongpass");
        User user = UserDao.checkLogin(login);
        Assertions.assertNull(user);
    }

    @Test
    public void testUserControllerHome() {
        UserController controller = new UserController();
        ModelAndView mav = controller.home();
        Assertions.assertEquals("home", mav.getViewName());
        Assertions.assertNotNull(mav.getModel().get("login"));
    }

    @Test
    public void testUserControllerLoginSuccess() {
        UserController controller = new UserController();
        ModelAndView mav = controller.login(new Login("john", "123456"));
        Assertions.assertEquals("user", mav.getViewName());
        Assertions.assertNotNull(mav.getModel().get("user"));
    }

    @Test
    public void testUserControllerLoginFailure() {
        UserController controller = new UserController();
        ModelAndView mav = controller.login(new Login("fake", "fake"));
        Assertions.assertEquals("error", mav.getViewName());
    }
}
