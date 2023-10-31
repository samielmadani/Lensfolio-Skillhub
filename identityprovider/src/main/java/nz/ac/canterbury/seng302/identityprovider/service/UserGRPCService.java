package nz.ac.canterbury.seng302.identityprovider.service;

import com.google.protobuf.ByteString;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import nz.ac.canterbury.seng302.identityprovider.model.Group;
import nz.ac.canterbury.seng302.identityprovider.model.User;
import nz.ac.canterbury.seng302.shared.identityprovider.*;
import nz.ac.canterbury.seng302.shared.util.FileUploadStatus;
import nz.ac.canterbury.seng302.shared.util.FileUploadStatusResponse;
import nz.ac.canterbury.seng302.shared.util.PaginationResponseOptions;
import nz.ac.canterbury.seng302.shared.util.ValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@GrpcService
public class UserGRPCService extends UserAccountServiceGrpc.UserAccountServiceImplBase {
    @Autowired
    private UserService userService;
    @Autowired
    private GroupService groupService;
    @Autowired
    private ErrorValidation errorValidation;

    private static final Logger logger = LoggerFactory.getLogger(UserGRPCService.class);
    private static int userId = 0;

    //rpc Register (UserRegisterRequest) returns (UserRegisterResponse);
    @Override
    public void register(UserRegisterRequest request, StreamObserver<UserRegisterResponse> responseObserver) {
        UserRegisterResponse.Builder reply = UserRegisterResponse.newBuilder();
        
        if (errorValidation.isAccountValid(request)) {
            userId = userId + 1;
            reply.setMessage(
                    String.format(
                            "%s, %s, %s, %s, %s, %s, %s, %s",
                            request.getUsername(),
                            request.getFirstName(),
                            request.getMiddleName(),
                            request.getLastName(),
                            request.getNickname(),
                            request.getBio(),
                            request.getPersonalPronouns(),
                            request.getEmail()
                        )
                ).setNewUserId(userId)
                .setIsSuccess(true)
                .addValidationErrors(errorValidation.getUsernameError(request.getUsername()))
                .addValidationErrors(errorValidation.getPasswordError(request.getPassword()))
                .addValidationErrors(errorValidation.getFirstNameError(request.getFirstName()))
                .addValidationErrors(errorValidation.getMiddleNameError(request.getMiddleName()))
                .addValidationErrors(errorValidation.getLastNameError(request.getLastName()))
                .addValidationErrors(errorValidation.getNickNameError(request.getNickname()))
                .addValidationErrors(errorValidation.getBioError(request.getBio()))
                .addValidationErrors(errorValidation.getEmailError(request.getEmail()));

            UserRegisterRequest finalRequest = UserRegisterRequest.newBuilder()
                    .setUsername(request.getUsername())
                    .setPassword(userService.encrypt(request.getPassword()))
                    .setFirstName( request.getFirstName())
                    .setMiddleName(request.getMiddleName())
                    .setLastName(request.getLastName())
                    .setNickname(request.getNickname())
                    .setBio(request.getBio())
                    .setPersonalPronouns(request.getPersonalPronouns())
                    .setEmail(request.getEmail())
                    .build();

            userService.register(finalRequest);
        } else {

            reply.setMessage("Invalid inputs to create account.")
            .setIsSuccess(false)
            .setNewUserId(1)
            .addValidationErrors(errorValidation.getUsernameError(request.getUsername()))
            .addValidationErrors(errorValidation.getPasswordError(request.getPassword()))
            .addValidationErrors(errorValidation.getFirstNameError(request.getFirstName()))
            .addValidationErrors(errorValidation.getMiddleNameError(request.getMiddleName()))
            .addValidationErrors(errorValidation.getLastNameError(request.getLastName()))
            .addValidationErrors(errorValidation.getNickNameError(request.getNickname()))
            .addValidationErrors(errorValidation.getBioError(request.getBio()))
            .addValidationErrors(errorValidation.getEmailError(request.getEmail()));
        }
        
        
        responseObserver.onNext(reply.build());
        responseObserver.onCompleted();
    }

    @Override
    public void editUser(EditUserRequest request, StreamObserver<EditUserResponse> responseObserver) {
        EditUserResponse.Builder reply = EditUserResponse.newBuilder();
        if (errorValidation.isEditValid(request)) {
            reply.setMessage(
                            String.format(
                                    "%s, %s, %s, %s, %s, %s, %s",
                                    request.getFirstName(),
                                    request.getMiddleName(),
                                    request.getLastName(),
                                    request.getNickname(),
                                    request.getBio(),
                                    request.getPersonalPronouns(),
                                    request.getEmail()
                            )
                    ).setIsSuccess(true)
                    .addValidationErrors(errorValidation.getFirstNameError(request.getFirstName()))
                    .addValidationErrors(errorValidation.getMiddleNameError(request.getMiddleName()))
                    .addValidationErrors(errorValidation.getLastNameError(request.getLastName()))
                    .addValidationErrors(errorValidation.getNickNameError(request.getNickname()))
                    .addValidationErrors(errorValidation.getBioError(request.getBio()))
                    .addValidationErrors(errorValidation.getEmailError(request.getEmail(), request.getUserId()));

            userService.updateUser(request);
        } else {
            reply.setMessage("Invalid inputs to edit account.")
                    .setIsSuccess(false)
                    .addValidationErrors(errorValidation.getFirstNameError(request.getFirstName()))
                    .addValidationErrors(errorValidation.getMiddleNameError(request.getMiddleName()))
                    .addValidationErrors(errorValidation.getLastNameError(request.getLastName()))
                    .addValidationErrors(errorValidation.getNickNameError(request.getNickname()))
                    .addValidationErrors(errorValidation.getBioError(request.getBio()))
                    .addValidationErrors(errorValidation.getEmailError(request.getEmail(), request.getUserId()));
        }
        responseObserver.onNext(reply.build());
        responseObserver.onCompleted();
    }

    @Override
    public void changeUserPassword(ChangePasswordRequest request, StreamObserver<ChangePasswordResponse> responseObserver) {
        logger.info("editUser() has been called");
        User user = userService.getUserById(request.getUserId());

        ValidationError.Builder error = ValidationError.newBuilder();

        ChangePasswordResponse.Builder reply = ChangePasswordResponse.newBuilder();
        if (errorValidation.isPasswordValid(request.getNewPassword()) && userService.matchPassword(request.getCurrentPassword(), user.getPassword())) {
            reply.setMessage("Success")
                    .setIsSuccess(true)
                    .addValidationErrors(errorValidation.getPasswordError(request.getNewPassword()))
                    .addValidationErrors(errorValidation.getOldPasswordErrorCorrect());
            userService.updateUserPassword(request);
        } else {
            reply.setMessage("Invalid inputs to edit account.")
                    .setIsSuccess(false)
                    .addValidationErrors(errorValidation.getPasswordError(request.getNewPassword()));

            if (!userService.matchPassword(request.getCurrentPassword(), user.getPassword())) {
                reply.addValidationErrors(errorValidation.getOldPasswordErrorIncorrect());
            } else {
                reply.addValidationErrors(errorValidation.getOldPasswordErrorCorrect());
            }
        }

        responseObserver.onNext(reply.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getUserAccountById(GetUserByIdRequest request, StreamObserver<UserResponse> responseObserver) {
        logger.info("getting user account: " + request.getId());
        User user = userService.getUserById(request.getId());
        logger.info("Got user account: " + user.getUserId());
        String roles = user.getRoles();
        List<Integer> roleInts = new ArrayList<>();
        for (int i = 0; i < roles.length(); i++) {
            roleInts.add(Integer.parseInt(roles.substring(i, i+1)));
        }

        UserResponse.Builder reply = UserResponse.newBuilder()
        .setUsername(user.getUsername())
        .setFirstName(user.getFirstName())
        .setMiddleName(user.getMiddleName())
        .setLastName(user.getLastName())
        .setNickname(user.getNickname())
        .setBio(user.getBio())
        .setEmail(user.getEmail())
        .setId(request.getId())
        .setPersonalPronouns(user.getPronouns())
        .addAllRolesValue(roleInts)
        .setProfileImagePath(userService.getImage(user.getUserId(), "jpg"));

        responseObserver.onNext(reply.build());
        responseObserver.onCompleted();
    }
    @Override
    public void getPaginatedUsers(GetPaginatedUsersRequest request, StreamObserver<PaginatedUsersResponse> responseObserver) {
        List<User> users = userService.getAllPaginated(request.getPaginationRequestOptions().getOffset(), request.getPaginationRequestOptions().getLimit(), request.getPaginationRequestOptions().getOrderBy(), request.getPaginationRequestOptions().getIsAscendingOrder());
        List<UserResponse> allResponses = userService.getUserResponses(users);

        PaginatedUsersResponse.Builder reply =  PaginatedUsersResponse.newBuilder().addAllUsers(allResponses)
                .setPaginationResponseOptions(PaginationResponseOptions.newBuilder()
                        .setResultSetSize((int) userService.getCount())
                        .build());

        responseObserver.onNext(reply.build());
        responseObserver.onCompleted();
    }

    @Override
    public void removeRoleFromUser(ModifyRoleOfUserRequest request, StreamObserver<UserRoleChangeResponse> responseObserver) {
        boolean b = userService.removeUserRole(request);

        UserRoleChangeResponse.Builder reply = UserRoleChangeResponse.newBuilder().setIsSuccess(b);

        responseObserver.onNext(reply.build());
        responseObserver.onCompleted();
    }


    @Override
    public void getPaginatedUsersFilteredByName(GetPaginatedUsersFilteredRequest request, StreamObserver<PaginatedUsersResponse> responseObserver) {
        List<User> users = userService.getAllFilteredPaginated(request.getFilteringOptions().getFilterText(), request.getPaginationRequestOptions().getOffset(), request.getPaginationRequestOptions().getLimit(), request.getPaginationRequestOptions().getOrderBy(), request.getPaginationRequestOptions().getIsAscendingOrder());
        List<UserResponse> allResponses = userService.getUserResponses(users);

        PaginatedUsersResponse.Builder reply =  PaginatedUsersResponse.newBuilder().addAllUsers(allResponses)
                .setPaginationResponseOptions(PaginationResponseOptions.newBuilder()
                        .setResultSetSize(userService.getNumUsersFilteredPaginated(request.getFilteringOptions().getFilterText()))
                        .build());

        responseObserver.onNext(reply.build());
        responseObserver.onCompleted();
    }

    @Override
    public void addRoleToUser(ModifyRoleOfUserRequest request, StreamObserver<UserRoleChangeResponse> responseObserver) {
        boolean b = userService.addUserRole(request);
        List<Group> teacherGroup = groupService.getGroupByShortName("TS");

        // should check if the user already have the role or not
        if (request.getRole() == UserRole.TEACHER) {
            if (teacherGroup.size() > 0) {
                groupService.addGroupUsers(teacherGroup.get(0).getGroupId(), List.of(request.getUserId()));
            }
        }

        UserRoleChangeResponse.Builder reply = UserRoleChangeResponse.newBuilder().setIsSuccess(b);

        responseObserver.onNext(reply.build());
        responseObserver.onCompleted();
    }

    //https://www.youtube.com/watch?v=rEmFMPv3TsA&ab_channel=TECHSCHOOL
    //rpc UploadUserProfilePhoto (stream UploadUserProfilePhotoRequest) returns (stream FileUploadStatusResponse);
    @Override
    public StreamObserver<UploadUserProfilePhotoRequest> uploadUserProfilePhoto(StreamObserver<FileUploadStatusResponse> responseObserver) {
        return new StreamObserver<>() {
            private int user;
            private String imageType;
            private ByteArrayOutputStream image;

            @Override
            public void onNext(UploadUserProfilePhotoRequest value) {
                if (value.getUploadDataCase() == UploadUserProfilePhotoRequest.UploadDataCase.METADATA) {
                    ProfilePhotoUploadMetadata metadata = value.getMetaData();
                    logger.info("Received request from user " + metadata.getUserId() + " to upload profile picture of type " + metadata.getFileType());

                    user = metadata.getUserId();
                    imageType = metadata.getFileType();
                    image = new ByteArrayOutputStream();
                    return;
                }

                ByteString fileContent = value.getFileContent();
                logger.info("Received partial image data with size " + fileContent.size());

                if (image == null) {
                    logger.info("Tried to upload image data, but the image meta data wasn't sent before the stream");
                    responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Image meta data hasn't been sent").asRuntimeException());
                    return;
                }

                try {
                    fileContent.writeTo(image);
                } catch (IOException e) {
                    logger.info("Tried to write image data, but the information couldn't be found. \r\n" + e.getMessage());
                    responseObserver.onError(Status.INTERNAL.withDescription("Can't get image information: " + e.getMessage()).asRuntimeException());
                }
            }

            @Override
            public void onError(Throwable t) {
                logger.error(t.getMessage());
            }

            @Override
            public void onCompleted() {
                try {
                    userService.saveImage(user, imageType, image);
                } catch (IOException e) {
                    responseObserver.onError(Status.INTERNAL.withDescription("Can't save image file to server: " + e.getMessage()).asRuntimeException());
                }

                FileUploadStatusResponse response = FileUploadStatusResponse.newBuilder()
                        .setStatus(FileUploadStatus.SUCCESS)
                        .setMessage("File uploaded!")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public void deleteUserProfilePhoto(DeleteUserProfilePhotoRequest request, StreamObserver<DeleteUserProfilePhotoResponse> responseObserver) {
        logger.info ("Deleting user profile picture for user " + request.getUserId());
        DeleteUserProfilePhotoResponse.Builder reply;
        if (userService.deleteImage(request.getUserId(), "jpg")) {
            logger.info("Successfully deleted user image for user " + request.getUserId());
            reply = DeleteUserProfilePhotoResponse.newBuilder().setIsSuccess(true).setMessage("Success!");
        } else {
            logger.info ("Couldn't delete user image for user " + request.getUserId());
            reply = DeleteUserProfilePhotoResponse.newBuilder().setIsSuccess(false).setMessage("Image couldn't be found!");
        }
        responseObserver.onNext(reply.build());
        responseObserver.onCompleted();
    }
}
