<%@ page contentType="text/html;charset=UTF-8" language="java" %> <%@ taglib
prefix="form" uri="http://www.springframework.org/tags/form" %>
<html>
  <head>
    <title>Login</title>
  </head>
  <body>
    <h2>Login Form</h2>
    <form:form action="login" method="post" modelAttribute="login">
      <table>
        <tr>
          <td>Account:</td>
          <td><form:input path="account" /></td>
        </tr>
        <tr>
          <td>Password:</td>
          <td><form:password path="password" /></td>
        </tr>
        <tr>
          <td></td>
          <td><input type="submit" value="Login" /></td>
        </tr>
      </table>
    </form:form>
  </body>
</html>
