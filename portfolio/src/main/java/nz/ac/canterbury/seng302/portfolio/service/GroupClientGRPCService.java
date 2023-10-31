package nz.ac.canterbury.seng302.portfolio.service;

import net.devh.boot.grpc.client.inject.GrpcClient;
import nz.ac.canterbury.seng302.shared.identityprovider.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class GroupClientGRPCService {
    @GrpcClient(value = "identity-provider-grpc-server")
    private GroupsServiceGrpc.GroupsServiceBlockingStub groupStub;

    private final Logger logger = LoggerFactory.getLogger(GroupClientGRPCService.class);

    public CreateGroupResponse createGroup(String shortName, String longName) {
        CreateGroupResponse response = groupStub.createGroup(CreateGroupRequest.newBuilder()
                        .setShortName(shortName)
                        .setLongName(longName).build());

        return response;
    }

    public GroupDetailsResponse getGroup(int id) {
        GroupDetailsResponse response = groupStub.getGroupDetails(GetGroupDetailsRequest.newBuilder().setGroupId(id).build());
        return response;
    }

    public PaginatedGroupsResponse getGroups() {
        // Can be updated to add pagination when required.
        PaginatedGroupsResponse response = groupStub.getPaginatedGroups(GetPaginatedGroupsRequest.newBuilder()
                .build());

        return response;
    }

    public RemoveGroupMembersResponse removeGroupMembers(List<Integer> selectedMembers, int groupId) {
        RemoveGroupMembersResponse response = groupStub.removeGroupMembers(RemoveGroupMembersRequest.newBuilder()
                        .setGroupId(groupId)
                        .addAllUserIds(selectedMembers)
                .build());

        return response;
    }

    public DeleteGroupResponse deleteGroup(int groupId) {
        DeleteGroupResponse response = groupStub.deleteGroup(DeleteGroupRequest.newBuilder()
                .setGroupId(groupId)
                .build());

        return response;
    }

    public AddGroupMembersResponse addGroupMembers(List<Integer> selectedMembers, int groupId) {
        AddGroupMembersResponse response = groupStub.addGroupMembers(AddGroupMembersRequest.newBuilder()
                .setGroupId(groupId)
                .addAllUserIds(selectedMembers)
                .build());

        return response;
    }

    public int getGroupUsersTotalCount(int groupId) {
        return getGroup(groupId).getMembersCount();
    }

    public int getGroupUsersPageCount(int groupId, int limit) {
        int totalUsers = getGroupUsersTotalCount(groupId);
        return (int) Math.ceil((double)totalUsers / (double)limit);
    }

    public List<Integer> getGroupUserIdsPaginated(int groupId, int limit, int page) {
        List<UserResponse> users = getGroup(groupId).getMembersList();

        // Return empty list if none
        if (users.isEmpty()) {
            return new ArrayList<>();
        }

        // Convert to List<Integer>
        List<Integer> userIds = new ArrayList<>();
        for (UserResponse user : users) {
            userIds.add(user.getId());
        }

        // Sort list so that results are consistent
        Collections.sort(userIds);

        // Get a range
        int start = Math.max( (page - 1) * limit, 0);
        int end = Math.min( start + limit, userIds.size());

        // If we are wanting the last page
        if (page == -1) {
            end = userIds.size();
            start = Math.max(end - limit, 0);
        }

        List<Integer> result = userIds.subList(start, end);

        return result;
    }

    public ModifyGroupDetailsResponse modifyGroupDetails(int groupId, String longName, String shortName) {
        return groupStub.modifyGroupDetails(ModifyGroupDetailsRequest.newBuilder()
                .setGroupId(groupId).setLongName(longName).setShortName(shortName).build());
    }
}
