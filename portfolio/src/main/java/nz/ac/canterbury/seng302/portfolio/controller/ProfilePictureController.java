package nz.ac.canterbury.seng302.portfolio.controller;

import nz.ac.canterbury.seng302.portfolio.service.UserClientGRPCService;
import nz.ac.canterbury.seng302.portfolio.service.UserService;
import nz.ac.canterbury.seng302.shared.identityprovider.AuthState;
import nz.ac.canterbury.seng302.shared.identityprovider.DeleteUserProfilePhotoResponse;
import nz.ac.canterbury.seng302.shared.identityprovider.UserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

@Controller
public class ProfilePictureController {

    @Autowired
    private UserService users;

    @Autowired
    private UserClientGRPCService userClientGRPCService;

    private final Logger logger = LoggerFactory.getLogger(ProfilePictureController.class);

    @PostMapping("api/uploadProfilePicture")
    public ResponseEntity<String> handleProfilePictureUpload (@AuthenticationPrincipal AuthState principal, @RequestParam("image") MultipartFile file) throws IOException, InterruptedException {
        if (principal == null) {
            logger.info("Tried to upload a profile picture for a user, but their Authentication Principal wasn't defined");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        int userId = users.getIdFromAuthState(principal);
        if (userId < 0) {
            logger.info("Tried to upload a profile picture for a user, but the user doesn't exist in the database!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        logger.info("Uploading new profile picture for user {}", userId);
        userClientGRPCService.uploadImage(userId, file.getInputStream());

        return ResponseEntity.ok("Uploaded file for user " + userId);
    }


    @ResponseBody
    @GetMapping(value="api/user/profilePicture", produces=MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getUserProfilePicture (@AuthenticationPrincipal AuthState principal, @RequestParam(name="userId", required=false) Integer userId) throws IOException {
        logger.info("Getting profile picture for user");
        if (principal == null) {
            logger.info("Tried to get profile picture for a user but there was no Authentication Principal!");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        if (userId == null) {
            userId = users.getIdFromAuthState(principal);
        }

        if (userId == -100) {
            logger.info("Tried to get userId from Authentication Principal, but it couldn't be found!");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        logger.info("Getting user profile picture for user {}", userId);
        UserResponse res = userClientGRPCService.receiveGetUserAccountById(userId);
            if (res.getProfileImagePath().equals("default")) {
                logger.info("User {} doesn't have a profile picture, getting default", userId);
                //https://www.libsea.com/article/how-to-convert-image-url-to-byte-array-in-java
                URL url = new URL("https://i.ibb.co/XkWw2rY/User.png");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                InputStream is = connection.getInputStream();
                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[10240];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    outStream.write(buffer, 0, len);
                }
                is.close();
                return ResponseEntity.ok(outStream.toByteArray());
            }
        logger.info("Got user profile picture image path: {}", res.getProfileImagePath());
        return ResponseEntity.ok(Files.readAllBytes(Paths.get(res.getProfileImagePath())));
    }

    @DeleteMapping("api/user/profilePicture")
    public ResponseEntity<String> deleteProfilePicture (@AuthenticationPrincipal AuthState principal) {
        logger.info("Deleting profile picture for user");
        if (principal == null) {
            logger.info("Tried to delete profile picture for a user but there was no Authentication Principal!");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        int userId = users.getIdFromAuthState(principal);

        if (userId == -100) {
            logger.info("Tried to get userId from Authentication Principal, but it couldn't be found!");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        DeleteUserProfilePhotoResponse response = userClientGRPCService.deleteImage(userId);
        if (response.getIsSuccess()) {
            logger.info("Successfully deleted user photo for user {}", userId);
            return ResponseEntity.ok(response.getMessage());
        } else {
            logger.info ("Unable to delete user photo for user {}", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

}
