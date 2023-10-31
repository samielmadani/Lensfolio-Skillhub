package nz.ac.canterbury.seng302.identityprovider.service;

import java.util.regex.Pattern;

import nz.ac.canterbury.seng302.identityprovider.model.User;
import nz.ac.canterbury.seng302.shared.identityprovider.EditUserRequest;
import nz.ac.canterbury.seng302.shared.identityprovider.UserRegisterRequest;
import nz.ac.canterbury.seng302.shared.util.ValidationError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ErrorValidation {
    @Autowired
    private UserService userService;
    
    public Boolean isEditValid(EditUserRequest request) {
        return isFirstNameValid(request.getFirstName())
                && isMiddleNameValid(request.getMiddleName())
                && isLastNameValid(request.getLastName())
                && isNickNameValid(request.getNickname())
                && isEmailValid(request.getEmail())
                && !isEmailInUse(request.getEmail(), request.getUserId())
                && isBioValid(request.getBio());
    }


    // Checks if user has input correct values to register account (WIP)
    public Boolean isAccountValid(UserRegisterRequest request) {
        return isUsernameValid(request.getUsername())
                && isPasswordValid(request.getPassword())
                && isFirstNameValid(request.getFirstName())
                && isMiddleNameValid(request.getMiddleName())
                && isLastNameValid(request.getLastName())
                && isEmailValid(request.getEmail())
                && isNickNameValid(request.getNickname())
                && !isUsernameInUse(request.getUsername())
                && !isEmailInUse(request.getEmail())
                && isBioValid(request.getBio());
    }

    public Boolean isUsernameValid(String username) {
        if (username.length() > 50) {
            return false;
        }
  
        return Pattern.matches(Constants.usernameRegex, username);
    }

    public Boolean isUsernameInUse(String username) {
        return userService.getUserByUsername(username) != null;
    }

    public Boolean isPasswordValid(String password) {
        boolean hasDigit = false;
        boolean hasUpperCase = false;
        boolean hasLowerCase = false;
        char character;
        if (password.length() > 100 || password.length() < 8) {
            return false;
        }

        for (int i=0; i < password.length(); i++) {
            character = password.charAt(i);

            if (Character.isUpperCase(character)) {
                hasUpperCase = true;
            } else if (Character.isLowerCase(character)) {
                hasLowerCase = true;
            } else if (Character.isDigit(character)) {
                hasDigit = true;
            }
            if (hasDigit && hasLowerCase && hasUpperCase) {
                return true;
            }
        }
        return false;
    }

    public Boolean isFirstNameValid(String firstName) {
        if (firstName.length() > 50) {
            return false;
        }
        return Pattern.matches(Constants.nameRegex, firstName);
    }

    public Boolean isMiddleNameValid(String middleName) {
        if (middleName.length() > 50) {
            return false;
        }
        return middleName.equals("") || Pattern.matches(Constants.nameRegex, middleName);
    }

    public Boolean isLastNameValid(String lastName) {
        if (lastName.length() > 50) {
            return false;
        }
        return Pattern.matches(Constants.nameRegex, lastName);
    }

    public Boolean isNickNameValid(String nickName) {
        if (nickName.length() > 50) {
            return false;
        }
        return nickName.equals("") || Pattern.matches(Constants.nameRegex, nickName);
    }

    public Boolean isBioValid(String bio) {
        return bio.length() <= 255;
    }

    public Boolean isEmailValid(String email) {
        if (email.length() > 50) {
            return false;
        }
        return Pattern.matches(Constants.emailRegex, email);
    }

    public Boolean isEmailInUse(String email) {
        return userService.getUserByEmail(email) != null;
    }

    public Boolean isEmailInUse(String email, int userId) {
        User user = userService.getUserByEmail(email);

        if (user == null) return false;

        return user.getUserId() != userId;
    }

    public ValidationError getUsernameError(String username) {
        ValidationError.Builder error = ValidationError.newBuilder();
        if (username.length() > 50) {
            return error.setFieldName("username").setErrorText("Username is too long").build();
        }
        if (username.length() == 0 ) {
            return error.setFieldName("username").setErrorText("Must provide username").build();
        }

        if (!isUsernameValid(username)) {
            return error.setFieldName("username").setErrorText("Username can only contain letters and numbers").build();
        } else if (isUsernameInUse(username)) {
            return error.setFieldName("username").setErrorText("This Username has been taken").build();
        }

        return error.setFieldName("username").setErrorText("").build();
    }

    public ValidationError getPasswordError(String password) {
        ValidationError.Builder error = ValidationError.newBuilder();
        
        if (password.length() < 8) {
            return error.setFieldName("password").setErrorText("Password must be at least 8 characters").build();
        }
        else if (password.length() > 100) {
            return error.setFieldName("username").setErrorText("Password must be 100 characters or less").build();
        }

        if (isPasswordValid(password)) {
            return error.setFieldName("password").setErrorText("").build();
        } 

        return error.setFieldName("password").setErrorText("Password must contain atleast 1 uppercase letter, 1 lowercase letter and 1 digit").build();
    }

    public ValidationError getOldPasswordErrorCorrect() {
        ValidationError.Builder error = ValidationError.newBuilder();
        return error.setFieldName("oldPassword").setErrorText("").build();
    }

    public ValidationError getOldPasswordErrorIncorrect() {
        ValidationError.Builder error = ValidationError.newBuilder();
        return error.setFieldName("oldPassword").setErrorText("Current Password Invalid").build();
    }

    public ValidationError getFirstNameError(String firstName) {
        ValidationError.Builder error = ValidationError.newBuilder();
        
        if (isFirstNameValid(firstName)) {
            return error.setFieldName("firstName").setErrorText("").build();
        }

        if (firstName.length() == 0 ) {
            return error.setFieldName("firstName").setErrorText("Must provide first name").build();
        }

        if (firstName.length() > 50 ) {
            return error.setFieldName("firstName").setErrorText("First Name is too long").build();

        }

        return error.setFieldName("firstName").setErrorText("First Name can only contain letters").build();
    }

    public ValidationError getMiddleNameError(String middleName) {
        ValidationError.Builder error = ValidationError.newBuilder();
        
        if (isMiddleNameValid(middleName)) {
            return error.setFieldName("middleName").setErrorText("").build();
        } 

        if (middleName.length() > 50 ) {
            return error.setFieldName("middleName").setErrorText("Middle Name is too long").build();
        }

        return error.setFieldName("middleName").setErrorText("Middle Name can only contain letters").build();
    }

    public ValidationError getLastNameError(String lastName) {
        ValidationError.Builder error = ValidationError.newBuilder();
        
        if (isLastNameValid(lastName)) {
            return error.setFieldName("lastName").setErrorText("").build();
        }

        if (lastName.length() == 0 ) {
            return error.setFieldName("lastName").setErrorText("Must provide last name").build();
        }

        if (lastName.length() > 50 ) {
            return error.setFieldName("lastName").setErrorText("Last Name is too long").build();
        }

        return error.setFieldName("lastName").setErrorText("Last Name can only contain letters").build();
    }

    public ValidationError getNickNameError(String nickName) {
        ValidationError.Builder error = ValidationError.newBuilder();
        
        if (isNickNameValid(nickName)) {
            return error.setFieldName("nickName").setErrorText("").build();
        } 

        if (nickName.length() > 50 ) {
            return error.setFieldName("nickName").setErrorText("Nick Name is too long").build();
        }

        return error.setFieldName("nickName").setErrorText("Nickname can only contain letters").build();
    }

    public ValidationError getBioError(String bio) {
        ValidationError.Builder error = ValidationError.newBuilder();

        if (isBioValid(bio)) {
            return error.setFieldName("bio").setErrorText("").build();
        }

        if (bio.length() > 255 ) {
            return error.setFieldName("bio").setErrorText("Bio can be upto 255 characters").build();
        }

        return error.setFieldName("bio").setErrorText("").build();
    }
    
    public ValidationError getEmailError(String email) {
        ValidationError.Builder error = ValidationError.newBuilder();

        if (email.length() > 50 ) {
            return error.setFieldName("email").setErrorText("Email is too long").build();
        }

        if (email.length() == 0 ) {
            return error.setFieldName("email").setErrorText("Must enter an email").build();
        }

        if (!isEmailValid(email)) {
            return error.setFieldName("email").setErrorText("Email is invalid, must followed by the corect domain").build();
        } else if (isEmailInUse(email)) {
            return error.setFieldName("email").setErrorText("An account with this email already exists").build();
        }

        return error.setFieldName("email").setErrorText("").build();
    }

    public ValidationError getEmailError(String email, int userId) {
        ValidationError.Builder error = ValidationError.newBuilder();

        if (email.length() > 50 ) {
            return error.setFieldName("email").setErrorText("Email is too long").build();
        }

        if (email.length() == 0 ) {
            return error.setFieldName("email").setErrorText("Must enter an email").build();
        }

        if (!isEmailValid(email)) {
            return error.setFieldName("email").setErrorText("Email is invalid, must followed by the corect domain").build();
        } else if (isEmailInUse(email, userId)) {
            return error.setFieldName("email").setErrorText("An account with this email already exists").build();
        }

        return error.setFieldName("email").setErrorText("").build();
    }

    public ValidationError getGroupShortNameError(String name) {
        ValidationError.Builder error = ValidationError.newBuilder();

        if (name.length() > 15) {
            return error.setFieldName("short").setErrorText("Short name can only be 15 characters").build();
        }
        return error.setFieldName("short").setErrorText("").build();
    }

    public ValidationError getGroupLongNameError(String name) {
        ValidationError.Builder error = ValidationError.newBuilder();

        if (name.length() > 50) {
            return error.setFieldName("long").setErrorText("Long name can only be 50 characters").build();
        }
        return error.setFieldName("long").setErrorText("").build();
    }
}
