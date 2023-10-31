package nz.ac.canterbury.seng302.identityprovider.model;

import nz.ac.canterbury.seng302.identityprovider.model.User;
import nz.ac.canterbury.seng302.shared.identityprovider.UserRole;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UserTest {

    @Test
    public void test_minimum_user_constructor () {
        User user = new User("a@a", "Password", UserRole.STUDENT);

        Assertions.assertEquals(0, user.getUserId());
        Assertions.assertEquals("a@a", user.getEmail());
        Assertions.assertEquals("Password", user.getPassword());
        Assertions.assertEquals(UserRole.STUDENT, UserRole.forNumber(Integer.parseInt(user.getRoles())));
    }

    @Test
    public void test_maximum_user_constructor () {
        User user = new User("a@a", "Password", UserRole.TEACHER, "Admin", "Aiden", "Malcom", "Smith", "com", "A test user account", "they/them");

        Assertions.assertEquals(0, user.getUserId());
        Assertions.assertEquals("a@a", user.getEmail());
        Assertions.assertEquals("Password", user.getPassword());
        Assertions.assertEquals(UserRole.TEACHER, UserRole.forNumber(Integer.parseInt(user.getRoles())));
        Assertions.assertEquals("Admin", user.getUsername());
        Assertions.assertEquals("Aiden", user.getFirstName());
        Assertions.assertEquals("Malcom", user.getMiddleName());
        Assertions.assertEquals("Smith", user.getLastName());
        Assertions.assertEquals("com", user.getNickname());
        Assertions.assertEquals("A test user account", user.getBio());
        Assertions.assertEquals("they/them", user.getPronouns());
    }

    @Test
    public void test_is_teacher_true () {
        User user = new User("a@a", "Password", UserRole.TEACHER);
        Assertions.assertTrue(user.isTeacher());
    }

    @Test
    public void test_id_teacher_false () {
        User user = new User("a@a", "Password", UserRole.STUDENT);
        Assertions.assertFalse(user.isTeacher());
    }

    @Test
    public void test_user_to_string () {
        User user = new User("a@a", "Password", UserRole.TEACHER, "Admin", "Aiden", "Malcom", "Smith", "com", "A test user account", "they/them");
        Assertions.assertEquals("User{" +
                "userId=" + 0 +
                ", email='" + "a@a" + '\'' +
                ", password='" + "Password" + '\'' +
                ", username='" + "Admin" + '\'' +
                ", role=" + "1" +
                ", firstName='" + "Aiden" + '\'' +
                ", middleName='" + "Malcom" + '\'' +
                ", lastName='" + "Smith" + '\'' +
                ", nickname='" + "com" + '\'' +
                ", bio='" + "A test user account" + '\'' +
                ", pronouns='" + "they/them" + '\'' +
                ", groups='[]'}", user.toString());
    }
}
