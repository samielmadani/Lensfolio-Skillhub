package nz.ac.canterbury.seng302.portfolio.controller;

import nz.ac.canterbury.seng302.portfolio.dto.user.UserDTO;
import nz.ac.canterbury.seng302.portfolio.service.GroupClientGRPCService;
import nz.ac.canterbury.seng302.portfolio.service.GroupService;
import nz.ac.canterbury.seng302.portfolio.service.UserService;
import nz.ac.canterbury.seng302.shared.identityprovider.GroupDetailsResponse;
import nz.ac.canterbury.seng302.shared.identityprovider.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@MockBeans({@MockBean(UserService.class), @MockBean(GroupService.class), @MockBean(GroupClientGRPCService.class)})
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers=UserSearchController.class)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes= {UserSearchController.class, UserService.class, GroupService.class, GroupClientGRPCService.class})
class UserSearchControllerTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserService userService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private GroupClientGRPCService groupClientGRPCService;

    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this); //Required for mocking to work
    }

    @Test
    void searchUsersInGroup_validQuery_queryMatches () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.get("/api/users/search/1?query=&page=1");
        ArrayList<UserDTO> users = new ArrayList<>();
        users.add(new UserDTO("Steve Jobs", "appleman", 1, List.of(UserRole.TEACHER)));
        users.add(new UserDTO("Cookie Monster", "cookielover", 2, List.of(UserRole.STUDENT)));
        when(userService.getFilteredPaginatedUsers("", 1)).thenReturn(users);
        when(groupService.userIdInGroup(1, 1)).thenReturn(true);
        when(groupService.userIdInGroup(2, 1)).thenReturn(false);
        mvc.perform(request)
                .andExpect(status().isOk());
    }

    @Test
    void searchUsersInGroup_validQuery_queryNoMatches () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.get("/api/users/search/1?query=hellooo&page=1");
        ArrayList<UserDTO> users = new ArrayList<>();
        when(userService.getFilteredPaginatedUsers("hellooo", 1)).thenReturn(users);
        when(groupClientGRPCService.getGroup(1)).thenReturn(GroupDetailsResponse.newBuilder().setShortName("TS").build());
        mvc.perform(request)
                .andExpect(status().isOk());
    }

    @Test
    void searchUsersInGroup_invalidQuery () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.get("/api/users/search/1?query=invalid\nquery&page=1");
        mvc.perform(request)
                .andExpect(status().isOk());
    }

    @Test
    void getPaginationButton_validQuery () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.get("/api/users/search/1/buttons?query=");
        when(userService.getTotalPages("")).thenReturn(1);
        mvc.perform(request)
                .andExpect(status().isOk());
    }

    @Test
    void getPaginationButton_invalidQuery () throws Exception {
        RequestBuilder request = MockMvcRequestBuilders.get("/api/users/search/1/buttons?query=invalid\nrequest");
        mvc.perform(request)
                .andExpect(status().isOk());
    }
}
