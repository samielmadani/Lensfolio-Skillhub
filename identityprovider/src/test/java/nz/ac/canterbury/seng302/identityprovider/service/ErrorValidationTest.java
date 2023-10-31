package nz.ac.canterbury.seng302.identityprovider.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nz.ac.canterbury.seng302.identityprovider.model.User;
import nz.ac.canterbury.seng302.shared.identityprovider.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import nz.ac.canterbury.seng302.shared.util.ValidationError;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

public class ErrorValidationTest {

    @Mock
    private ErrorValidation errorValidation;

    private UserService userService;

    @BeforeEach
    public void setup () {
        errorValidation = new ErrorValidation();
    }

    @Test
    public void testIsUserNameValidWithValidUsername() {
        assertTrue(errorValidation.isUsernameValid("James"));
    }
    
    @Test
    public void testIsUserNameInvalidWithInvalidUsername() {
        assertFalse(errorValidation.isUsernameValid(" 19"));
    }
    
    @Test
    public void testIsPasswordValidWithValidPassword() {
        assertTrue(errorValidation.isPasswordValid("Asder123"));
    }
    
    @Test
    public void testIsPasswordInvalidWithInvalidPassword() {
        assertFalse(errorValidation.isPasswordValid("abc"));
    }
        
    @Test
    public void testIsFirstNameValidWithValidFirstName() {
        assertTrue(errorValidation.isFirstNameValid("James"));
    }
    
    @Test
    public void testIsFirstNameInvalidWithFirstNameInvalid() {
        assertFalse(errorValidation.isFirstNameValid("2"));
    }

    @Test
    public void testIsLastNameValidWithValidLastName() {
        assertTrue(errorValidation.isLastNameValid("Hazlehurst"));
    }
    
    @Test
    public void testIsLastNameInvalidWithLastNameInvalid() {
        assertFalse(errorValidation.isLastNameValid("2"));
    }

    @Test
    public void testIsmiddleNameValidWithValidMiddleName() {
        assertTrue(errorValidation.isMiddleNameValid("Barry"));
    }
    
    @Test
    public void testIsMiddleNameInvalidWithMiddleNameInvalid() {
        assertFalse(errorValidation.isMiddleNameValid("2"));
    }

    @Test
    public void testIsNicknameValidWithValidNicknameName() {
        assertTrue(errorValidation.isNickNameValid("Nick"));
    }
    
    @Test
    public void testIsNicknameInvalidWithNicknameNameInvalid() {
        assertFalse(errorValidation.isNickNameValid("2"));
    }

    @Test
    public void testIsEmailValidWithValidEmail() {
        assertTrue(errorValidation.isEmailValid("bob@gmail.com"));
    }

    @Test
    public void testIsEmailInvalidWithInvalidEmail() {
        assertFalse(errorValidation.isEmailValid("bob@.gmail"));
    }

    @Test
    public void testIsBioValidWithBioInvalid() {
        assertFalse(errorValidation.isBioValid("2".repeat(256)));
    }

    @Test
    public void testIsBioValidWithBioValid() {
        assertTrue(errorValidation.isBioValid("2".repeat(255)));
    }

    @Test
    public void testIsBioValidWithBioEmpty() { assertTrue(errorValidation.isBioValid("")); }

    @Test
    public void testGetUsernameErrorWithValidUserName(){
        ValidationError.Builder error = ValidationError.newBuilder();
        error.setFieldName("username").setErrorText("");

        // Mocks the userService and returns a value to emulate the database being searched for the username in question
        userService = Mockito.mock(UserService.class);
        Mockito.when(userService.getUserByUsername("JamesJamesJames12345")).thenReturn(null);
        ReflectionTestUtils.setField(errorValidation, "userService", userService);

        assertEquals(error.getErrorText(), errorValidation.getUsernameError("JamesJamesJames12345").getErrorText());
    }

    @Test
    public void testGetUsernameErrorWithInvalidLengthUserName(){
        ValidationError.Builder error = ValidationError.newBuilder();
        error.setFieldName("username").setErrorText("Username is too long");

        // Mocks the userService and returns a value to emulate the database being searched for the username in question
        userService = Mockito.mock(UserService.class);
        Mockito.when(userService.getUserByUsername("A12345678901234567890123456789012345678901234567890")).thenReturn(null);
        ReflectionTestUtils.setField(errorValidation, "userService", userService);

        assertEquals(error.getErrorText(), errorValidation.getUsernameError("A12345678901234567890123456789012345678901234567890").getErrorText());
    }

    @Test
    public void testGetUsernameErrorWithInvalidCharacterUserName() {
        ValidationError.Builder error = ValidationError.newBuilder();
        error.setFieldName("username").setErrorText("Username can only contain letters and numbers");

        // Mocks the userService and returns a value to emulate the database being searched for the username in question
        userService = Mockito.mock(UserService.class);
        Mockito.when(userService.getUserByUsername("james123######")).thenReturn(null);
        ReflectionTestUtils.setField(errorValidation, "userService", userService);

        assertEquals(error.getErrorText(), errorValidation.getUsernameError("james123######").getErrorText());
    }

    @Test
    public void testGetUsernameErrorWithUsernameInUse() {
        ValidationError.Builder error = ValidationError.newBuilder();
        error.setFieldName("username").setErrorText("This Username has been taken");

        // Mocks the userService and returns a value to emulate the database being searched for the username in question
        // In this case there is the user in the DB and it therefore returns the user.
        User user = new User("a@a", "Password", UserRole.TEACHER, "Admin", "Aiden", "Malcom", "Smith", "com", "A test user account", "they/them");

        userService = Mockito.mock(UserService.class);
        Mockito.when(userService.getUserByUsername("Admin")).thenReturn(user);
        ReflectionTestUtils.setField(errorValidation, "userService", userService);

        assertEquals(error.getErrorText(), errorValidation.getUsernameError("Admin").getErrorText());
    }
    
    @Test
    public void testGetPasswordErrorWithValidPassword(){
        ValidationError.Builder error = ValidationError.newBuilder();
        error.setFieldName("password").setErrorText("");
        assertEquals(error.getErrorText(), errorValidation.getPasswordError("ADSwer3123!!").getErrorText());
    }

    @Test
    public void testGetPasswordErrorWithShortPassword() {
        ValidationError.Builder error = ValidationError.newBuilder();
        error.setFieldName("password").setErrorText("Password must be at least 8 characters");
        assertEquals(error.getErrorText(), errorValidation.getPasswordError("ab!AA12").getErrorText());
    }
    
    @Test
    public void testGetPasswordErrorWithLongPassword() {
        ValidationError.Builder error = ValidationError.newBuilder();
        error.setFieldName("password").setErrorText("Password must be 100 characters or less");
        assertEquals(error.getErrorText(), errorValidation.getPasswordError("*AbcDef12!*AbcDef12!*AbcDef12!*AbcDef12!*AbcDef12!*AbcDef12!*AbcDef12!*AbcDef12!*AbcDef12!*AbcDef12!q").getErrorText());
    }

    @Test
    public void testGetPasswordErrorWithInvalidCharacters() {
        ValidationError.Builder error = ValidationError.newBuilder();
        error.setFieldName("password").setErrorText("Password must contain atleast 1 uppercase letter, 1 lowercase letter and 1 digit");
        assertEquals(error.getErrorText(), errorValidation.getPasswordError("AAAAAAA2").getErrorText());
        assertEquals(error.getErrorText(), errorValidation.getPasswordError("ttttttt8").getErrorText());
        assertEquals(error.getErrorText(), errorValidation.getPasswordError("tttttttt").getErrorText());
        assertEquals(error.getErrorText(), errorValidation.getPasswordError("22222222").getErrorText());
        assertEquals(error.getErrorText(), errorValidation.getPasswordError("TTTTTTTT").getErrorText());
        assertEquals(error.getErrorText(), errorValidation.getPasswordError("AAAAAAAt").getErrorText());

    }

    @Test
    public void testGetFirstNameErrorWithValidFirstName() {
        ValidationError.Builder error = ValidationError.newBuilder();
        error.setFieldName("firstName").setErrorText("");
        assertEquals(error.getErrorText(), errorValidation.getFirstNameError("Bob").getErrorText());
    }

    @Test
    public void testGetFirstNameErrorWithInvalidFirstName() {
        ValidationError.Builder error = ValidationError.newBuilder();
        error.setFieldName("firstName").setErrorText("First Name can only contain letters");
        assertEquals(error.getErrorText(), errorValidation.getFirstNameError("Bob3").getErrorText());
    }

    @Test
    public void testGetMiddleNameErrorWithValidMiddleName() {
        ValidationError.Builder error = ValidationError.newBuilder();
        error.setFieldName("middleName").setErrorText("");
        assertEquals(error.getErrorText(), errorValidation.getMiddleNameError("Bob").getErrorText());
    }

    @Test
    public void testGetMiddleNameErrorWithInvalidMiddleName() {
        ValidationError.Builder error = ValidationError.newBuilder();
        error.setFieldName("middleName").setErrorText("Middle Name can only contain letters");
        assertEquals(error.getErrorText(), errorValidation.getMiddleNameError("Bob3").getErrorText());
    }

    @Test
    public void testGetLastNameErrorWithValidLastName() {
        ValidationError.Builder error = ValidationError.newBuilder();
        error.setFieldName("lastName").setErrorText("");
        assertEquals(error.getErrorText(), errorValidation.getLastNameError("Bob").getErrorText());
    }

    @Test
    public void testGetLastNameErrorWithInvalidLastName() {
        ValidationError.Builder error = ValidationError.newBuilder();
        error.setFieldName("lastName").setErrorText("Last Name can only contain letters");
        assertEquals(error.getErrorText(), errorValidation.getLastNameError("Bob3").getErrorText());
    }
    
    @Test
    public void testGetNickNameErrorWithValidNickName() {
        ValidationError.Builder error = ValidationError.newBuilder();
        error.setFieldName("nickName").setErrorText("");
        assertEquals(error.getErrorText(), errorValidation.getNickNameError("Bob").getErrorText());
    }

    @Test
    public void testGetNickNameErrorWithInvalidNickName() {
        ValidationError.Builder error = ValidationError.newBuilder();
        error.setFieldName("nickName").setErrorText("Nickname can only contain letters");
        assertEquals(error.getErrorText(), errorValidation.getNickNameError("Bob3").getErrorText());
    }

    @Test
    public void testGetEmailErrorWithInvalidEmail() {
        ValidationError.Builder error = ValidationError.newBuilder();
        error.setFieldName("email").setErrorText("Email is invalid, must followed by the corect domain");
        assertEquals(error.getErrorText(), errorValidation.getEmailError("bob@.gmail").getErrorText());
    }

    @Test
    public void testGetBioErrorWithValidBioLength() {
        ValidationError.Builder error = ValidationError.newBuilder();
        error.setFieldName("bio").setErrorText("");
        assertEquals(error.getErrorText(), errorValidation.getBioError("a".repeat(255)).getErrorText());
    }

    @Test
    public void testGetBioErrorWithEmptyBioLength() {
        ValidationError.Builder error = ValidationError.newBuilder();
        error.setFieldName("bio").setErrorText("");
        assertEquals(error.getErrorText(), errorValidation.getBioError(" ").getErrorText());
    }

    @Test
    public void testGetBioErrorWithInvalidBioLength() {
        ValidationError.Builder error = ValidationError.newBuilder();
        error.setFieldName("bio").setErrorText("Bio can be upto 255 characters");
        assertEquals(error.getErrorText(), errorValidation.getBioError("a".repeat(256)).getErrorText());
        }

    @Test
    public void testGetEmailErrorWithValidEmail(){
        ValidationError.Builder error = ValidationError.newBuilder();
        error.setFieldName("email").setErrorText("");

        // Mocks the userService and returns a value to emulate the database being searched for the username in question
        userService = Mockito.mock(UserService.class);
        Mockito.when(userService.getUserByEmail("username@domain.com")).thenReturn(null);
        ReflectionTestUtils.setField(errorValidation, "userService", userService);

        assertEquals(error.getErrorText(), errorValidation.getEmailError("username@domain.com").getErrorText());
    }

    @Test
    public void testGetEmailErrorWithEmailInUse() {
        ValidationError.Builder error = ValidationError.newBuilder();
        error.setFieldName("email").setErrorText("An account with this email already exists");

        // Mocks the userService and returns a value to emulate the database being searched for the username in question
        // In this case there is the user in the DB and it therefore returns the user.
        User user = new User("a@admin.com", "Password", UserRole.TEACHER, "Admin", "Aiden", "Malcom", "Smith", "com", "A test user account", "they/them");

        userService = Mockito.mock(UserService.class);
        Mockito.when(userService.getUserByEmail("a@admin.com")).thenReturn(user);
        ReflectionTestUtils.setField(errorValidation, "userService", userService);

        assertEquals(error.getErrorText(), errorValidation.getEmailError("a@admin.com").getErrorText());
    }


    @Test
    public void testGetEmailErrorWithEmptyEmail() {
        ValidationError.Builder error = ValidationError.newBuilder();
        error.setFieldName("email").setErrorText("Must enter an email");
        assertEquals(error.getErrorText(), errorValidation.getEmailError("").getErrorText());
    }

    @Test
    public void testGetEmailErrorWithEmailTooLong() {
        ValidationError.Builder error = ValidationError.newBuilder();
        error.setFieldName("email").setErrorText("Email is too long");
        assertEquals(error.getErrorText(), errorValidation.getEmailError("abcdefghijklmnopqrstuvwxyz@ABCDEFGHIJKLMNOPQRSTU.com").getErrorText());
    }

    @Test
    public void testGetGroupShortNameError() {
        ValidationError.Builder error = ValidationError.newBuilder();
        error.setFieldName("short").setErrorText("Short name can only be 15 characters");
        assertEquals(error.getErrorText(), errorValidation.getGroupShortNameError("a".repeat(20)).getErrorText());
    }

    @Test
    public void testGetGroupShortNameNoError() {
        ValidationError.Builder error = ValidationError.newBuilder();
        error.setFieldName("short").setErrorText("");
        assertEquals(error.getErrorText(), errorValidation.getGroupShortNameError("a".repeat(5)).getErrorText());
    }

    @Test
    public void testGetGroupLongNameError() {
        ValidationError.Builder error = ValidationError.newBuilder();
        error.setFieldName("long").setErrorText("Long name can only be 50 characters");
        assertEquals(error.getErrorText(), errorValidation.getGroupLongNameError("a".repeat(60)).getErrorText());
    }

    @Test
    public void testGetGroupLongNameNoError() {
        ValidationError.Builder error = ValidationError.newBuilder();
        error.setFieldName("long").setErrorText("");
        assertEquals(error.getErrorText(), errorValidation.getGroupLongNameError("a".repeat(5)).getErrorText());
    }

}
