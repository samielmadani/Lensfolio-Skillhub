package nz.ac.canterbury.seng302.identityprovider.service;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import nz.ac.canterbury.seng302.identityprovider.model.Group;
import nz.ac.canterbury.seng302.identityprovider.model.User;
import nz.ac.canterbury.seng302.shared.identityprovider.*;
import nz.ac.canterbury.seng302.shared.util.ValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@GrpcService
public class GroupGRPCService extends GroupsServiceGrpc.GroupsServiceImplBase {
    @Autowired
    private GroupService groupService;
    @Autowired
    private ErrorValidation errorValidation;

    private static final Logger logger = LoggerFactory.getLogger(GroupGRPCService.class);

    @Override
    public void createGroup(CreateGroupRequest request, StreamObserver<CreateGroupResponse> responseObserver) {
        logger.info("Create Group was called");

        ValidationError shortNameError = errorValidation.getGroupShortNameError(request.getShortName());
        ValidationError longNameError = errorValidation.getGroupLongNameError(request.getLongName());

        CreateGroupResponse.Builder reply = CreateGroupResponse.newBuilder();

        if (shortNameError.getErrorText().equals("") && longNameError.getErrorText().equals("")) {
            //No Error
            Group newGroup = groupService.save(request.getShortName(), request.getLongName());

            reply.setIsSuccess(true)
                    .setNewGroupId(newGroup.getGroupId())
                    .setMessage("Group created");
        }
        else {
            //Error
            reply.setIsSuccess(false)
                    .setMessage("Group not created");
        }

        reply.addValidationErrors(shortNameError).addValidationErrors(longNameError);

        responseObserver.onNext(reply.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getGroupDetails(GetGroupDetailsRequest request, StreamObserver<GroupDetailsResponse> responseObserver) {
        logger.info("Getting a group");

        GroupDetailsResponse.Builder reply = GroupDetailsResponse.newBuilder();

        logger.info(groupService.getGroups().toString());

        Group group = groupService.getGroupById(request.getGroupId());

        logger.info(String.valueOf(request.getGroupId()));
        logger.info(group.toString());

        reply.setGroupId(group.getGroupId())
                .setLongName(group.getLongName())
                .setShortName(group.getShortName());

        for (User user : group.getGroupMembers()) {
            List<Integer> roleInts = new ArrayList<>();
            for (int i = 0; i < user.getRoles().length(); i++) {
                roleInts.add(Integer.parseInt(user.getRoles().substring(i, i+1)));
            }

            UserResponse.Builder tempResponse = UserResponse.newBuilder()
                    .setUsername(user.getUsername())
                    .setFirstName(user.getFirstName())
                    .setMiddleName(user.getMiddleName())
                    .setLastName(user.getLastName())
                    .setNickname(user.getNickname())
                    .setBio(user.getBio())
                    .setEmail(user.getEmail())
                    .setPersonalPronouns(user.getPronouns())
                    .addAllRolesValue(roleInts)
                    .setId(user.getUserId());

            reply.addMembers(tempResponse);
        }

        responseObserver.onNext(reply.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getPaginatedGroups(GetPaginatedGroupsRequest request, StreamObserver<PaginatedGroupsResponse> responseObserver) {
        logger.info("Getting all groups");

        PaginatedGroupsResponse.Builder reply = PaginatedGroupsResponse.newBuilder();

        List<Group> groups = groupService.getGroups();

        for (Group group : groups) {
            GroupDetailsResponse.Builder tempResponse = GroupDetailsResponse.newBuilder();
            tempResponse.setGroupId(group.getGroupId())
                    .setShortName(group.getShortName())
                    .setLongName(group.getLongName());

            for (User user : group.getGroupMembers()) {
                List<Integer> roleInts = new ArrayList<>();
                for (int i = 0; i < user.getRoles().length(); i++) {
                    roleInts.add(Integer.parseInt(user.getRoles().substring(i, i+1)));
                }

                UserResponse.Builder userTempResponse = UserResponse.newBuilder()
                        .setUsername(user.getUsername())
                        .setFirstName(user.getFirstName())
                        .setMiddleName(user.getMiddleName())
                        .setLastName(user.getLastName())
                        .setNickname(user.getNickname())
                        .setBio(user.getBio())
                        .setEmail(user.getEmail())
                        .setPersonalPronouns(user.getPronouns())
                        .addAllRolesValue(roleInts)
                        .setId(user.getUserId());

                tempResponse.addMembers(userTempResponse);
            }
            reply.addGroups(tempResponse);
        }

        responseObserver.onNext(reply.build());
        responseObserver.onCompleted();
    }

    @Override
    public void removeGroupMembers(RemoveGroupMembersRequest request, StreamObserver<RemoveGroupMembersResponse> responseObserver) {
        logger.info("Removing group members");

        RemoveGroupMembersResponse.Builder reply = RemoveGroupMembersResponse.newBuilder();

        reply.setIsSuccess(groupService.removeGroupUsers(request.getGroupId(), request.getUserIdsList()));

        responseObserver.onNext(reply.build());
        responseObserver.onCompleted();
    }

    @Override
    public void deleteGroup(DeleteGroupRequest request, StreamObserver<DeleteGroupResponse> responseObserver) {
        logger.info("Deleting a group");

        DeleteGroupResponse.Builder reply = DeleteGroupResponse.newBuilder();

        reply.setIsSuccess(groupService.deleteGroup(request.getGroupId()));

        responseObserver.onNext(reply.build());
        responseObserver.onCompleted();

    }

    @Override
    public void addGroupMembers(AddGroupMembersRequest request, StreamObserver<AddGroupMembersResponse> responseObserver) {
        logger.info("Adding group members");

        AddGroupMembersResponse.Builder reply = AddGroupMembersResponse.newBuilder();

        reply.setIsSuccess(groupService.addGroupUsers(request.getGroupId(), request.getUserIdsList()));

        responseObserver.onNext(reply.build());
        responseObserver.onCompleted();
    }

    @Override
    public void modifyGroupDetails(ModifyGroupDetailsRequest request, StreamObserver<ModifyGroupDetailsResponse> responseObserver) {
        logger.info("Modifying group details for group " + request.getGroupId());
        ModifyGroupDetailsResponse.Builder reply = ModifyGroupDetailsResponse.newBuilder();
        if (groupService.updateGroupInfo(request.getGroupId(), request.getLongName(), request.getShortName())) {
            logger.info("Successfully changed details for group " + request.getGroupId());
            reply.setIsSuccess(true);
        } else {
            logger.info("Couldn't change details for group " + request.getGroupId());
            reply.setIsSuccess(false);
        }
        responseObserver.onNext(reply.build());
        responseObserver.onCompleted();
    }
}
