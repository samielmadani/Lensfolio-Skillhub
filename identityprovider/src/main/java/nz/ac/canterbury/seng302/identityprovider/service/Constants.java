package nz.ac.canterbury.seng302.identityprovider.service;

public class Constants {
    
    public static final String emailRegex = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";

    public static final String usernameRegex = "[a-zA-Z0-9_]+";

    public static final String nameRegex = "[a-zA-Z]+";

}