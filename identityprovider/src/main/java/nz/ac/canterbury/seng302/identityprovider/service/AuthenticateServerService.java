package nz.ac.canterbury.seng302.identityprovider.service;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import nz.ac.canterbury.seng302.identityprovider.authentication.AuthenticationServerInterceptor;
import nz.ac.canterbury.seng302.identityprovider.authentication.JwtTokenUtil;
import nz.ac.canterbury.seng302.identityprovider.model.User;
import nz.ac.canterbury.seng302.shared.identityprovider.AuthState;
import nz.ac.canterbury.seng302.shared.identityprovider.AuthenticateRequest;
import nz.ac.canterbury.seng302.shared.identityprovider.AuthenticateResponse;
import nz.ac.canterbury.seng302.shared.identityprovider.AuthenticationServiceGrpc.AuthenticationServiceImplBase;
import org.springframework.beans.factory.annotation.Autowired;

@GrpcService
public class AuthenticateServerService extends AuthenticationServiceImplBase{
    @Autowired
    private UserService userService;

    private final UserGRPCService userGRPCService = new UserGRPCService();

    private final JwtTokenUtil jwtTokenService = JwtTokenUtil.getInstance();

    /**
     * Attempts to authenticate a user with a given username and password. 
     */
    @Override
    public void authenticate(AuthenticateRequest request, StreamObserver<AuthenticateResponse> responseObserver) {
        AuthenticateResponse.Builder reply = AuthenticateResponse.newBuilder();
        String username = request.getUsername();
        String password = request.getPassword();

        User currentUser = userService.getUserByUsername(username);
        if (currentUser != null && userService.matchPassword(password, currentUser.getPassword())) {
            String token = jwtTokenService.generateTokenForUser(currentUser.getUsername(), currentUser.getUserId(),
                    currentUser.getFirstName() + " " + currentUser.getLastName(), currentUser.getRoles());
            reply
                    .setEmail(currentUser.getEmail())
                    .setFirstName(currentUser.getFirstName())
                    .setLastName(currentUser.getLastName())
                    .setMessage("Logged in successfully!")
                    .setSuccess(true)
                    .setToken(token)
                    .setUserId(currentUser.getUserId())
                    .setUsername(currentUser.getUsername());
        } else {
            reply
                    .setMessage("Log in attempt failed: username or password incorrect")
                    .setSuccess(false)
                    .setToken("");
        }

        responseObserver.onNext(reply.build());
        responseObserver.onCompleted();
    }

    /**
     * The AuthenticationInterceptor already handles validating the authState for us, so here we just need to
     * retrieve that from the current context and return it in the gRPC body
     */
    @Override
    public void checkAuthState(Empty request, StreamObserver<AuthState> responseObserver) {
        responseObserver.onNext(AuthenticationServerInterceptor.AUTH_STATE.get());
        responseObserver.onCompleted();
    }
}
