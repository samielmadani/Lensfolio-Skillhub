package nz.ac.canterbury.seng302.portfolio.service;

import com.google.protobuf.ByteString;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.client.inject.GrpcClient;
import nz.ac.canterbury.seng302.shared.identityprovider.*;
import nz.ac.canterbury.seng302.shared.util.BasicStringFilteringOptions;
import nz.ac.canterbury.seng302.shared.util.FileUploadStatusResponse;
import nz.ac.canterbury.seng302.shared.util.PaginationRequestOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

@Service
public class UserClientGRPCService {

    private static final Logger logger = LoggerFactory.getLogger(UserClientGRPCService.class);

    @GrpcClient(value = "identity-provider-grpc-server")
    private UserAccountServiceGrpc.UserAccountServiceBlockingStub userStub;
    @GrpcClient("identity-provider-grpc-server")
    private UserAccountServiceGrpc.UserAccountServiceStub userStubStream;

    public UserRegisterResponse receiveUserRegistration(final String username,
     final String password,
     final String firstName,
     final String middleName,
     final String lastName,
     final String nickname,
     final String bio,
     final String personalPronouns,
     final String email) throws StatusRuntimeException {

        UserRegisterResponse response = userStub.register(UserRegisterRequest.newBuilder()
        .setUsername(username)
        .setPassword(password)
        .setFirstName(firstName)
        .setMiddleName(middleName)
        .setLastName(lastName)
        .setNickname(nickname)
        .setBio(bio)
        .setPersonalPronouns(personalPronouns)
        .setEmail(email)
        .build());
        
        return response;
    }

    public EditUserResponse receiveEditUserRequest(final int userId,
                                                   final String firstName,
                                                   final String middleName,
                                                   final String lastName,
                                                   final String nickName,
                                                   final String bio,
                                                   final String personalPronouns,
                                                   final String email) {
        EditUserResponse response = userStub.editUser(EditUserRequest.newBuilder()
                .setUserId(userId)
                .setFirstName(firstName)
                .setMiddleName(middleName)
                .setLastName(lastName)
                .setNickname(nickName)
                .setBio(bio)
                .setPersonalPronouns(personalPronouns)
                .setEmail(email).build());

        return response;
    }

    public ChangePasswordResponse receiveChangePasswordRequest(final int userId,
        final String currentPassword,
        final String newPassword) {
            ChangePasswordResponse response = userStub.changeUserPassword(ChangePasswordRequest.newBuilder()
            .setUserId(userId)
            .setCurrentPassword(currentPassword)
            .setNewPassword(newPassword)
            .build());
        return response;
    }   

    public UserResponse receiveGetUserAccountById(final int Id) {
        logger.info("Getting user account " + Id);
        UserResponse response = userStub.getUserAccountById(GetUserByIdRequest.newBuilder()
                .setId(Id).build());

        return response;
    }
    public PaginatedUsersResponse receiveGetPaginatedUsers(int offset, int limit, String orderBy, boolean ascending) {
        GetPaginatedUsersRequest request = GetPaginatedUsersRequest.newBuilder()
                .setPaginationRequestOptions(PaginationRequestOptions.newBuilder()
                        .setOffset(offset)
                        .setLimit(limit)
                        .setOrderBy(orderBy)
                        .setIsAscendingOrder(ascending)
                        .build())
                .build();

        PaginatedUsersResponse response = userStub.getPaginatedUsers(request);
        return response;
    }

    public PaginatedUsersResponse receiveGetFilteredPaginatedUsers (String query, int offset, int limit, String orderBy, boolean ascending) {
        GetPaginatedUsersFilteredRequest request = GetPaginatedUsersFilteredRequest.newBuilder().setFilteringOptions(BasicStringFilteringOptions.newBuilder().setFilterText(query).build())
                .setPaginationRequestOptions(PaginationRequestOptions.newBuilder().setOffset(offset).setLimit(limit).setOrderBy(orderBy).setIsAscendingOrder(ascending).build()).build();
        PaginatedUsersResponse response = userStub.getPaginatedUsersFilteredByName(request);
        logger.info(format("Got %s users that matched query %s for page %s", response.getUsersCount(), query, offset));
        return response;
    }

    public UserRoleChangeResponse receiveRemoveRoleFromUser(final int id, final UserRole role) {
        UserRoleChangeResponse response = userStub.removeRoleFromUser(ModifyRoleOfUserRequest.newBuilder()
                .setUserId(id)
                .setRole(role)
                .build());

        return response;
    }

    public UserRoleChangeResponse receiveAddRoleToUser(final int id, final UserRole role) {
        UserRoleChangeResponse response = userStub.addRoleToUser(ModifyRoleOfUserRequest.newBuilder()
                .setUserId(id)
                .setRole(role)
                .build());

        return response;
    }

    public void uploadImage (int userId, InputStream imgInfo) throws InterruptedException {
        final CountDownLatch finishLatch = new CountDownLatch(1);
        StreamObserver<UploadUserProfilePhotoRequest> requestObserver = userStubStream.withDeadlineAfter(5, TimeUnit.SECONDS)
                .uploadUserProfilePhoto(new StreamObserver<>() {
                    @Override
                    public void onNext(FileUploadStatusResponse value) {
                        logger.info("Received message: \n" + value);
                    }

                    @Override
                    public void onError(Throwable t) {
                        logger.error("Image upload failed! \r\n" + t);
                        finishLatch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        logger.info("Profile picture uploaded!");
                        finishLatch.countDown();
                    }
                });

        ProfilePhotoUploadMetadata imageMetadata = ProfilePhotoUploadMetadata.newBuilder().setUserId(userId).setFileType("jpg").build();
        UploadUserProfilePhotoRequest request = UploadUserProfilePhotoRequest.newBuilder().setMetaData(imageMetadata).build();

        try {
            requestObserver.onNext(request);
            logger.info("Sent image metadata: \r\n" + imageMetadata);
            byte[] imageBuffer = new byte[1024];
            while (true) {
                int n = imgInfo.read(imageBuffer);
                if (n <= 0) {
                    break;
                }

                if (finishLatch.getCount() == 0) {
                    return;
                }

                request = UploadUserProfilePhotoRequest.newBuilder().setFileContent(ByteString.copyFrom(imageBuffer, 0, n)).build();
                requestObserver.onNext(request);
                logger.info("Sent " + n + " bytes of image data");
            }
        } catch (Exception e) {
            logger.error("Unexpected error: " + e.getMessage());
            requestObserver.onError(e);
            return;
        }

        requestObserver.onCompleted();

        if (!finishLatch.await(30, TimeUnit.SECONDS)) {
            logger.warn("Profile picture couldn't be uploaded in 30 seconds!");
        }
    }

    public DeleteUserProfilePhotoResponse deleteImage (int userId) {
        return userStub.deleteUserProfilePhoto(DeleteUserProfilePhotoRequest.newBuilder()
                                                                            .setUserId(userId)
                                                                            .build());
    }

}
