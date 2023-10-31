package nz.ac.canterbury.seng302.identityprovider.service;

import nz.ac.canterbury.seng302.identityprovider.model.Group;
import nz.ac.canterbury.seng302.identityprovider.model.GroupRepository;
import nz.ac.canterbury.seng302.identityprovider.model.User;
import nz.ac.canterbury.seng302.identityprovider.model.UserRepository;
import nz.ac.canterbury.seng302.shared.identityprovider.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.lang.String.format;

/**
 * User service to manage the database storage and access for the User entity
 */
@Service
public class UserService {

    private final static String imageFolder = "userProfilePics";

    private final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final static int USER_LIST_SIZE = 20;

    @Autowired
    private UserRepository users;

    @Autowired
    private GroupRepository groupRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /* Hashes the user password using the BCrypt algorithm */
    public String encrypt(String password) {
        return encoder.encode(password);
    }

    /* Checks the inputted user password to verify it against the stored hash */
    public boolean matchPassword(String suppliedPassword, String encodedPassword) {
        return encoder.matches(suppliedPassword, encodedPassword);
    }

    /**
     * Register a new user in the database
     * @param newUserRequest - GRPC UserRegisterRequest containing user details
     * @return User object for further processing if required
     */
    public User register(UserRegisterRequest newUserRequest) {
        // Convert to user object
        User newUser = new User(newUserRequest.getEmail(), newUserRequest.getPassword(), UserRole.STUDENT); // Default role is Student
        newUser.setUsername(newUserRequest.getUsername());
        newUser.setBio(newUserRequest.getBio());
        newUser.setFirstName(newUserRequest.getFirstName());
        newUser.setLastName(newUserRequest.getLastName());
        newUser.setMiddleName(newUserRequest.getMiddleName());
        newUser.setNickname(newUserRequest.getNickname());
        newUser.setPronouns(newUserRequest.getPersonalPronouns());
        newUser.setImage(new byte[0]);

        // Save to database
        newUser = users.save(newUser);
        /* Adds every new student to MWAG as new accounts are always students */
        List<Group> specialGroup = groupRepository.findByLongName("Members without a group");
        List<Group> teachingGroup = groupRepository.findByLongName("Teaching Staff");
        
        int userRole = Integer.parseInt(newUser.getRoles());
            if (userRole == 1 || userRole == 2) {
            teachingGroup.get(0).addGroupMember(newUser);
            groupRepository.save(teachingGroup.get(0));
        } else {
            specialGroup.get(0).addGroupMember(newUser);
            groupRepository.save(specialGroup.get(0));
        }
        
        return newUser;
    }

    /**
     * Update an existing user in the database
     * @param updatedUserRequest - GRPC EditUserRequest containing edited user details
     * @return User object for further processing if required, null if edit fails
     */
    public User updateUser(EditUserRequest updatedUserRequest) {
        User updatedUser = getUserById(updatedUserRequest.getUserId());

        if (updatedUser == null) return null;

        updatedUser.setEmail(updatedUserRequest.getEmail());
        updatedUser.setBio(updatedUserRequest.getBio());
        updatedUser.setFirstName(updatedUserRequest.getFirstName());
        updatedUser.setLastName(updatedUserRequest.getLastName());
        updatedUser.setMiddleName(updatedUserRequest.getMiddleName());
        updatedUser.setNickname(updatedUserRequest.getNickname());
        updatedUser.setPronouns(updatedUserRequest.getPersonalPronouns());

        // Save to database
        return users.save(updatedUser);
    }

    /**
     * Save or update a user in the database.
     * @param user - User to update, if the given user does not have an ID it will be generated.
     * @return Updated user
     */
    public User save(User user) {
        return users.save(user);
    }

    /**
     * Update an existing user's Password in the database
     * @param updatedUserRequest - GRPC ChangePasswordRequest containing change password details
     * @return User object for further processing if required, null if edit fails
     */
    public User updateUserPassword (ChangePasswordRequest updatedUserRequest) {
        User updatedUser = getUserById(updatedUserRequest.getUserId());

        if (updatedUser == null) return null;
        updatedUser.setPassword(encrypt(updatedUserRequest.getNewPassword()));

        // Save to database
        users.save(updatedUser);

        return updatedUser;
    }

    /**
     * Get a user entity by its ID
     * @param id - ID of the requested user
     * @return User object, null if not found
     */
    public User getUserById(int id) {
        return users.findById(id);
    }

    /**
     * Get a user entity by its username
     * @param username - Username of the requested user
     * @return User object, null if not found
     */
    public User getUserByUsername(String username) {
        return users.findByUsername(username);
    }

    /**
     * Get a user entity by its email
     * @param email - Email of the requested user
     * @return User object, null if not found
     */
    public User getUserByEmail(String email) {
        return users.findByEmail(email);
    }

    /**
     * Get total number of users in the database
     */
    public long getCount() {
        return users.count();
    }

    /**
     * Get all users in the database
     * @return List of users
     */
    public List<User> getAll() {return users.findAll();}

    /**
     * Get all users in a paginated manner
     * @return List of paginated users
     */
    public List<User> getAllPaginated(int pageNo, int pageSize, String sortBy, boolean ascending) {
        Sort sort;
        Sort.Direction direction;

        if (ascending) direction = Sort.Direction.ASC; else direction = Sort.Direction.DESC;

        if (Objects.equals(sortBy, "name")) {
            Sort.Order order1 = new Sort.Order(direction, "firstName").ignoreCase();
            Sort.Order order2 = new Sort.Order(direction, "middleName").ignoreCase();
            Sort.Order order3 = new Sort.Order(direction, "lastName").ignoreCase();

            sort = Sort.by(order1).and(Sort.by(order2)).and(Sort.by(order3));
        } else {
            Sort.Order order = new Sort.Order(direction, sortBy).ignoreCase();
            sort = Sort.by(order);
        }

        Pageable paging = PageRequest.of(pageNo, pageSize, sort);
        /*Pageable paging = PageRequest.of(pageNo, pageSize, sort);*/

        Page<User> pagedResult = users.findAll(paging);

        if(pagedResult.hasContent()) {
            return pagedResult.getContent();
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * Finds paginated users that match First Name, Last Name, or Username with the search query.
     * @param query Search query to match in the database with user
     * @param pageNo current pagination page to get users for
     * @param pageSize how many users are on each page
     * @param sortBy how to sort the data
     * @param ascending if the data is ascending or not
     * @return List of paginated users that meet the search query
     */
    public List<User> getAllFilteredPaginated (String query, int pageNo, int pageSize, String sortBy, boolean ascending) {
        Sort sort;
        Sort.Direction direction;

        if (ascending) direction = Sort.Direction.ASC; else direction = Sort.Direction.DESC;

        if (Objects.equals(sortBy, "name")) {
            Sort.Order order1 = new Sort.Order(direction, "firstName").ignoreCase();
            Sort.Order order2 = new Sort.Order(direction, "middleName").ignoreCase();
            Sort.Order order3 = new Sort.Order(direction, "lastName").ignoreCase();

            sort = Sort.by(order1).and(Sort.by(order2)).and(Sort.by(order3));
        } else {
            Sort.Order order = new Sort.Order(direction, sortBy).ignoreCase();
            sort = Sort.by(order);
        }

        Pageable paging = PageRequest.of(pageNo, pageSize, sort);

        Page<User> pagedResult = users.search(query, paging);
        logger.info(format("Got %s users that match the string %s", pagedResult.getSize(), query));

        if(pagedResult.hasContent()) {
            return pagedResult.getContent();
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * Gets the number of pages that match a search query for users
     * @param query Search query
     * @return total number of pages that match the search query
     */
    public int getNumUsersFilteredPaginated (String query) {
        Pageable paging = PageRequest.of(1, USER_LIST_SIZE, Sort.by(new Sort.Order(Sort.Direction.ASC, "firstName").ignoreCase()));
        Page<User> pagedResult = users.search(query, paging);
        return pagedResult.getTotalPages();
    }

    /**
     * Remove user role
     * @param request - role to be removed
     * @return boolean success or not success
     */
    public boolean removeUserRole(ModifyRoleOfUserRequest request) {
        User user = users.findById(request.getUserId());
        // int userRole = request.getRole().getNumber();
        UserRole userRole = request.getRole();
        // Teaching staff group
        Group group = groupRepository.findByShortName("TS").get(0);
        // M.W.A.G. group
        Group mwag = groupRepository.findByShortName("MWAG").get(0);

        if (userRole == UserRole.TEACHER) {
            user.removeRole(request.getRole());
            if (group != null && mwag != null) {
                group.removeGroupMember(request.getUserId());
                mwag.addGroupMember(user);
                groupRepository.saveAndFlush(group);
                groupRepository.saveAndFlush(mwag);
            }
        } else {
            user.removeRole(request.getRole());
        }
        // Save to database
        users.save(user);

        return !user.getRoles().contains(String.valueOf(request.getRole().getNumber()));
    }

    /**
     * adding user role
     * @param request - role to be added
     * @return boolean success or not success
     */
    public boolean addUserRole(ModifyRoleOfUserRequest request) {
        User user = users.findById(request.getUserId());
        Group teachingGroup = groupRepository.findByShortName("TS").get(0);
        Group specialGroup = groupRepository.findByShortName("MWAG").get(0);
        if (request.getRole() == UserRole.TEACHER) {
            if (teachingGroup != null) {
                teachingGroup.addGroupMember(user);
                specialGroup.removeGroupMember(user.getUserId());
                groupRepository.save(teachingGroup);
            }
        }
        user.addRole(request.getRole());

        // Save to database
        users.save(user);

        return user.getRoles().contains(String.valueOf(request.getRole().getNumber()));
    }

    /**
     * Saving image to the database
     * @param userId - saving image to particular user
     * @param imageType - the type of image to be saved
     */
    public void saveImage (int userId, String imageType, ByteArrayOutputStream image) throws IOException {
        logger.info("Saving image for user " + userId);
        User user = users.findById(userId);
        user.setImage(image.toByteArray());
        users.save(user);
        logger.info("Saved image for user " + userId);
    }

    /**
     * Getting image of user
     * @param userId - account id of the user
     * @param imageType - the type of the user to be returned
     * @return string of image's name
     */
    public String getImage (int userId, String imageType) {
        logger.info("Getting user profile for user " + (userId));
        User user = users.findById(userId);
        if (user.getImage() == null || user.getImage().length == 0) {
            logger.info("User " + userId + " didn't have an image stored, getting default image");
            return "default";
        }
        ByteArrayInputStream inputStream = new ByteArrayInputStream(user.getImage());
        BufferedImage image;
        File imageFile = new File (userId + "." + imageType);
        try {
            image = ImageIO.read(inputStream);
            ImageIO.write(image, "jpg", imageFile);
        } catch (Exception e) {
            logger.error("Error writing file: \n" + e.getMessage());
        }

        logger.info("Got user profile image for user " + (userId));
        return imageFile.getAbsolutePath();
    }

    /**
     * Deleting user's image
     * @param userId - account id of the user
     * @param imageType - the type of the user to be returned
     * @return boolean success or not success
     */
    public boolean deleteImage (int userId, String imageType) {
        logger.info("Deleting user profile picture for user " + userId);
        User user = users.findById(userId);
        if (user == null) {
            logger.info ("Couldn't find user " + userId);
            return false;
        }
        user.setImage(new byte[0]);
        users.save(user);
        logger.info("Deleted user image for user " + userId);
        return true;
    }

    /**
     * Getting list of userResponse
     * @param usersList - list of users
     * @return list of userResponse
     */
    public List<UserResponse> getUserResponses (List<User> usersList) {
        List<UserResponse> userResponsesList = new ArrayList<>();
        for (User user: usersList) {
            List<Integer> roles = new ArrayList<>();
            for (int i = 0; i < user.getRoles().length(); i++) {
                roles.add(Integer.parseInt(user.getRoles().substring(i, i + 1)));
            }
            UserResponse.Builder userReply = UserResponse.newBuilder()
                    .setUsername(user.getUsername())
                    .setFirstName(user.getFirstName())
                    .setMiddleName(user.getMiddleName())
                    .setLastName(user.getLastName())
                    .setNickname(user.getNickname())
                    .setBio(user.getBio())
                    .setEmail(user.getEmail())
                    .setPersonalPronouns(user.getPronouns())
                    .addAllRolesValue(roles)
                    .setId(user.getUserId());
            userResponsesList.add(userReply.build());
        }
        return userResponsesList;
    }

    public void flushAndUpdate(User user) {
        users.saveAndFlush(user);
    }

    @PostConstruct
    public void initDefaultDatabase () {
        try {
            User adminUser = new User("admin200@lensfolio.nz", encrypt("OEZZsr64wvYF7kFeV3dC"), UserRole.COURSE_ADMINISTRATOR, "admin200", "Admin", "", "Admin", "", "", "");
            adminUser.addRole(UserRole.TEACHER);
            adminUser.addRole(UserRole.STUDENT);
            save(adminUser);
            User teacherUser = new User("teacher200@lensfolio.nz", encrypt("j8vgxsVUddfHk4g0skSc"), UserRole.TEACHER, "teacher200", "Teacher", "", "Teacher", "", "", "");
            teacherUser.addRole(UserRole.STUDENT);
            save(teacherUser);
            save(new User("student200@lensfolio.nz", encrypt("wkhMNHn6HROm8Lx19G3T"), UserRole.STUDENT, "student200", "Student", "", "Student", "", "", ""));
            save(new User("ksd61@lensfolio.nz", encrypt("ksd61Password"), UserRole.STUDENT, "ksd61", "Kady", "", "Kirk", "", "", ""));
            save(new User("atn18@lensfolio.nz", encrypt("atn18Password"), UserRole.STUDENT, "atn18", "Aya", "", "Thompson", "", "", ""));
            save(new User("jmr39@lensfolio.nz", encrypt("jmr39Password"), UserRole.STUDENT, "jmr39", "Jaiden", "", "Milner", "", "", ""));
            save(new User("jds52@lensfolio.nz", encrypt("jds52Password"), UserRole.STUDENT, "jds52", "Juliet", "", "Downes", "", "", ""));
            save(new User("aml141@lensfolio.nz", encrypt("aml141Password"), UserRole.STUDENT, "aml141", "Alasdair", "", "Merrill", "", "", ""));
            save(new User("swn173@lensfolio.nz", encrypt("swn173Password"), UserRole.STUDENT, "swn173", "Suhayb", "", "Wilkinson", "", "", ""));
            save(new User("ile141@lensfolio.nz", encrypt("ile141Password"), UserRole.STUDENT, "ile141", "Indigo", "", "Little", "", "", ""));
            save(new User("cwy111@lensfolio.nz", encrypt("cwy111Password"), UserRole.STUDENT, "cwy111", "Caspar", "", "Walmsley", "", "", ""));
            save(new User("mvn97@lensfolio.nz", encrypt("mvn97Password"), UserRole.STUDENT, "mvn97", "Martina", "", "Vaughan", "", "", ""));
            save(new User("mig149@lensfolio.nz", encrypt("mig149Password"), UserRole.STUDENT, "mig149", "Michele", "", "Irving", "", "", ""));
            save(new User("slr189@lensfolio.nz", encrypt("slr189Password"), UserRole.STUDENT, "slr189", "Saxon", "", "Lester", "", "", ""));
            save(new User("ras185@lensfolio.nz", encrypt("ras185Password"), UserRole.STUDENT, "ras185", "Romany", "", "Arias", "", "", ""));
            save(new User("cno108@lensfolio.nz", encrypt("cno108Password"), UserRole.STUDENT, "cno108", "Carlie", "", "Navarro", "", "", ""));
            save(new User("sle62@lensfolio.nz", encrypt("sle62Password"), UserRole.STUDENT, "sle62", "Sheldon", "", "Love", "", "", ""));
            save(new User("thg138@lensfolio.nz", encrypt("thg138Password"), UserRole.STUDENT, "thg138", "Tobey", "", "Holding", "", "", ""));
            save(new User("ebr23@lensfolio.nz", encrypt("ebr23Password"), UserRole.STUDENT, "ebr23", "Edie", "", "Blair", "", "", ""));
            save(new User("lsr48@lensfolio.nz", encrypt("lsr48Password"), UserRole.STUDENT, "lsr48", "Lilian", "", "Sumner", "", "", ""));
            save(new User("kss83@lensfolio.nz", encrypt("kss83Password"), UserRole.STUDENT, "kss83", "Kiera", "", "Shields", "", "", ""));
            save(new User("ijn105@lensfolio.nz", encrypt("ijn105Password"), UserRole.STUDENT, "ijn105", "Ishan", "", "Jefferson", "", "", ""));
            save(new User("tbh171@lensfolio.nz", encrypt("tbh171Password"), UserRole.STUDENT, "tbh171", "Tyler-Jay", "", "Beach", "", "", ""));
            save(new User("jld128@lensfolio.nz", encrypt("jld128Password"), UserRole.STUDENT, "jld128", "Jorja", "", "Lord", "", "", ""));
            save(new User("mao37@lensfolio.nz", encrypt("mao37Password"), UserRole.STUDENT, "mao37", "Mairead", "", "Arroyo", "", "", ""));
            save(new User("jft140@lensfolio.nz", encrypt("jft140Password"), UserRole.STUDENT, "jft140", "Jacqueline", "", "Frost", "", "", ""));
            save(new User("mhn101@lensfolio.nz", encrypt("mhn101Password"), UserRole.STUDENT, "mhn101", "Mehreen", "", "Henderson", "", "", ""));
            save(new User("abe186@lensfolio.nz", encrypt("abe186Password"), UserRole.STUDENT, "abe186", "Abbas", "", "Blackmore", "", "", ""));
            save(new User("aff54@lensfolio.nz", encrypt("aff54Password"), UserRole.STUDENT, "aff54", "Amaya", "", "Figueroa", "", "", ""));
            save(new User("aet163@lensfolio.nz", encrypt("aet163Password"), UserRole.STUDENT, "aet163", "Anum", "", "Elliott", "", "", ""));
            save(new User("dpr158@lensfolio.nz", encrypt("dpr158Password"), UserRole.STUDENT, "dpr158", "Dilara", "", "Prosser", "", "", ""));
            save(new User("ycl46@lensfolio.nz", encrypt("ycl46Password"), UserRole.STUDENT, "ycl46", "Yanis", "", "Carroll", "", "", ""));
            save(new User("acr107@lensfolio.nz", encrypt("acr107Password"), UserRole.STUDENT, "acr107", "Ahmet", "", "Conner", "", "", ""));
            save(new User("cld151@lensfolio.nz", encrypt("cld151Password"), UserRole.STUDENT, "cld151", "Chloe", "", "Lloyd", "", "", ""));
            save(new User("lpe89@lensfolio.nz", encrypt("lpe89Password"), UserRole.STUDENT, "lpe89", "Lubna", "", "Pierce", "", "", ""));
            save(new User("amr33@lensfolio.nz", encrypt("amr33Password"), UserRole.STUDENT, "amr33", "Amari", "", "Mercer", "", "", ""));
            save(new User("adl85@lensfolio.nz", encrypt("adl85Password"), UserRole.STUDENT, "adl85", "Alishba", "", "Daniel", "", "", ""));
            save(new User("asr155@lensfolio.nz", encrypt("asr155Password"), UserRole.STUDENT, "asr155", "Alaina", "", "Sawyer", "", "", ""));
            save(new User("acn48@lensfolio.nz", encrypt("acn48Password"), UserRole.STUDENT, "acn48", "Avani", "", "Chapman", "", "", ""));
            save(new User("lrs110@lensfolio.nz", encrypt("lrs110Password"), UserRole.STUDENT, "lrs110", "Lillie-Mai", "", "Rawlings", "", "", ""));
            save(new User("sfs102@lensfolio.nz", encrypt("sfs102Password"), UserRole.STUDENT, "sfs102", "Sandra", "", "Flores", "", "", ""));
            save(new User("mhd176@lensfolio.nz", encrypt("mhd176Password"), UserRole.STUDENT, "mhd176", "Mollie", "", "Holland", "", "", ""));
            save(new User("lwr91@lensfolio.nz", encrypt("lwr91Password"), UserRole.STUDENT, "lwr91", "Leonardo", "", "Walker", "", "", ""));
            save(new User("iss199@lensfolio.nz", encrypt("iss199Password"), UserRole.STUDENT, "iss199", "Inaayah", "", "Spears", "", "", ""));
            save(new User("lid37@lensfolio.nz", encrypt("lid37Password"), UserRole.STUDENT, "lid37", "Leia", "", "Ireland", "", "", ""));
            save(new User("egr40@lensfolio.nz", encrypt("egr40Password"), UserRole.STUDENT, "egr40", "Elena", "", "Gallagher", "", "", ""));
            save(new User("tpr145@lensfolio.nz", encrypt("tpr145Password"), UserRole.STUDENT, "tpr145", "Tai", "", "Potter", "", "", ""));
            save(new User("ddp30@lensfolio.nz", encrypt("ddp30Password"), UserRole.STUDENT, "ddp30", "Deanne", "", "Dunlop", "", "", ""));
            save(new User("kdy42@lensfolio.nz", encrypt("kdy42Password"), UserRole.STUDENT, "kdy42", "Killian", "", "Daugherty", "", "", ""));
            save(new User("mcm105@lensfolio.nz", encrypt("mcm105Password"), UserRole.STUDENT, "mcm105", "Mattie", "", "Cunningham", "", "", ""));
            save(new User("sbt165@lensfolio.nz", encrypt("sbt165Password"), UserRole.STUDENT, "sbt165", "Shona", "", "Bassett", "", "", ""));
            save(new User("kmy177@lensfolio.nz", encrypt("kmy177Password"), UserRole.STUDENT, "kmy177", "Khadeeja", "", "Mckay", "", "", ""));
            save(new User("gad159@lensfolio.nz", encrypt("gad159Password"), UserRole.STUDENT, "gad159", "Grady", "", "Arnold", "", "", ""));
            save(new User("tln26@lensfolio.nz", encrypt("tln26Password"), UserRole.STUDENT, "tln26", "Toby", "", "Lennon", "", "", ""));
            save(new User("von147@lensfolio.nz", encrypt("von147Password"), UserRole.STUDENT, "von147", "Vikki", "", "Owen", "", "", ""));
            save(new User("rma40@lensfolio.nz", encrypt("rma40Password"), UserRole.STUDENT, "rma40", "Regan", "", "Mora", "", "", ""));
            save(new User("che154@lensfolio.nz", encrypt("che154Password"), UserRole.STUDENT, "che154", "Cade", "", "Hume", "", "", ""));
            save(new User("ses85@lensfolio.nz", encrypt("ses85Password"), UserRole.STUDENT, "ses85", "Sneha", "", "Edwards", "", "", ""));
            save(new User("zbn191@lensfolio.nz", encrypt("zbn191Password"), UserRole.STUDENT, "zbn191", "Zuzanna", "", "Bolton", "", "", ""));
            save(new User("cma158@lensfolio.nz", encrypt("cma158Password"), UserRole.STUDENT, "cma158", "Caden", "", "Mustafa", "", "", ""));
            save(new User("nje155@lensfolio.nz", encrypt("nje155Password"), UserRole.STUDENT, "nje155", "Nancie", "", "Joyce", "", "", ""));
            save(new User("wde145@lensfolio.nz", encrypt("wde145Password"), UserRole.STUDENT, "wde145", "Whitney", "", "Dunne", "", "", ""));
            save(new User("twn81@lensfolio.nz", encrypt("twn81Password"), UserRole.STUDENT, "twn81", "Tevin", "", "Walton", "", "", ""));
            save(new User("hmo169@lensfolio.nz", encrypt("hmo169Password"), UserRole.STUDENT, "hmo169", "Huma", "", "Murillo", "", "", ""));
            save(new User("gmr145@lensfolio.nz", encrypt("gmr145Password"), UserRole.STUDENT, "gmr145", "Gareth", "", "Mair", "", "", ""));
            save(new User("ewl76@lensfolio.nz", encrypt("ewl76Password"), UserRole.STUDENT, "ewl76", "Ellesse", "", "Wall", "", "", ""));
            save(new User("abs175@lensfolio.nz", encrypt("abs175Password"), UserRole.STUDENT, "abs175", "Adelina", "", "Barajas", "", "", ""));
            save(new User("ale172@lensfolio.nz", encrypt("ale172Password"), UserRole.STUDENT, "ale172", "Alaya", "", "Levine", "", "", ""));
            save(new User("hdr65@lensfolio.nz", encrypt("hdr65Password"), UserRole.STUDENT, "hdr65", "Huzaifah", "", "Draper", "", "", ""));
            save(new User("mme163@lensfolio.nz", encrypt("mme163Password"), UserRole.STUDENT, "mme163", "Maude", "", "Mackenzie", "", "", ""));
            save(new User("zfh108@lensfolio.nz", encrypt("zfh108Password"), UserRole.STUDENT, "zfh108", "Ziggy", "", "Finch", "", "", ""));
            save(new User("cdn165@lensfolio.nz", encrypt("cdn165Password"), UserRole.STUDENT, "cdn165", "Callie", "", "Dean", "", "", ""));
            save(new User("lhn176@lensfolio.nz", encrypt("lhn176Password"), UserRole.STUDENT, "lhn176", "Larissa", "", "Henson", "", "", ""));
            save(new User("lmy153@lensfolio.nz", encrypt("lmy153Password"), UserRole.STUDENT, "lmy153", "Lilia", "", "Massey", "", "", ""));
            save(new User("egn167@lensfolio.nz", encrypt("egn167Password"), UserRole.STUDENT, "egn167", "Eva-Rose", "", "Gunn", "", "", ""));
            save(new User("brs172@lensfolio.nz", encrypt("brs172Password"), UserRole.STUDENT, "brs172", "Bryce", "", "Reynolds", "", "", ""));
            save(new User("tcy134@lensfolio.nz", encrypt("tcy134Password"), UserRole.STUDENT, "tcy134", "Tori", "", "Conley", "", "", ""));
            save(new User("kbt30@lensfolio.nz", encrypt("kbt30Password"), UserRole.STUDENT, "kbt30", "Kerri", "", "Bryant", "", "", ""));
            save(new User("kmn66@lensfolio.nz", encrypt("kmn66Password"), UserRole.STUDENT, "kmn66", "Kerrie", "", "Madden", "", "", ""));
            save(new User("jva24@lensfolio.nz", encrypt("jva24Password"), UserRole.STUDENT, "jva24", "Jem", "", "Ventura", "", "", ""));
            save(new User("jjh109@lensfolio.nz", encrypt("jjh109Password"), UserRole.STUDENT, "jjh109", "Joy", "", "Joseph", "", "", ""));
            save(new User("efr115@lensfolio.nz", encrypt("efr115Password"), UserRole.STUDENT, "efr115", "Elspeth", "", "Fisher", "", "", ""));
            save(new User("dws176@lensfolio.nz", encrypt("dws176Password"), UserRole.STUDENT, "dws176", "Darryl", "", "Weiss", "", "", ""));
            save(new User("lke70@lensfolio.nz", encrypt("lke70Password"), UserRole.STUDENT, "lke70", "Laiba", "", "Krause", "", "", ""));
            save(new User("pkn182@lensfolio.nz", encrypt("pkn182Password"), UserRole.STUDENT, "pkn182", "Peter", "", "Kaufman", "", "", ""));
            save(new User("lbr125@lensfolio.nz", encrypt("lbr125Password"), UserRole.STUDENT, "lbr125", "Leonidas", "", "Baxter", "", "", ""));
            save(new User("dmy155@lensfolio.nz", encrypt("dmy155Password"), UserRole.STUDENT, "dmy155", "Dawid", "", "Mccaffrey", "", "", ""));
            save(new User("kle25@lensfolio.nz", encrypt("kle25Password"), UserRole.STUDENT, "kle25", "Kayne", "", "Le", "", "", ""));
            save(new User("ala93@lensfolio.nz", encrypt("ala93Password"), UserRole.STUDENT, "ala93", "Aamna", "", "Luna", "", "", ""));
            save(new User("mga77@lensfolio.nz", encrypt("mga77Password"), UserRole.STUDENT, "mga77", "Mohammad", "", "Garza", "", "", ""));
            save(new User("ehs73@lensfolio.nz", encrypt("ehs73Password"), UserRole.STUDENT, "ehs73", "Emme", "", "Hays", "", "", ""));
            save(new User("ekh187@lensfolio.nz", encrypt("ekh187Password"), UserRole.STUDENT, "ekh187", "Elouise", "", "Koch", "", "", ""));
            save(new User("dna82@lensfolio.nz", encrypt("dna82Password"), UserRole.STUDENT, "dna82", "Dotty", "", "Nava", "", "", ""));
            save(new User("aml40@lensfolio.nz", encrypt("aml40Password"), UserRole.STUDENT, "aml40", "Aalia", "", "Mcdaniel", "", "", ""));
            save(new User("afs84@lensfolio.nz", encrypt("afs84Password"), UserRole.STUDENT, "afs84", "Amelia-Lily", "", "Fuentes", "", "", ""));
            save(new User("sdy69@lensfolio.nz", encrypt("sdy69Password"), UserRole.STUDENT, "sdy69", "Stewart", "", "Dougherty", "", "", ""));
            save(new User("epn193@lensfolio.nz", encrypt("epn193Password"), UserRole.STUDENT, "epn193", "Ella-Mai", "", "Pennington", "", "", ""));
            save(new User("ame97@lensfolio.nz", encrypt("ame97Password"), UserRole.STUDENT, "ame97", "Alia", "", "Malone", "", "", ""));
            save(new User("sst64@lensfolio.nz", encrypt("sst64Password"), UserRole.STUDENT, "sst64", "Sidra", "", "Scott", "", "", ""));
            save(new User("jwd53@lensfolio.nz", encrypt("jwd53Password"), UserRole.STUDENT, "jwd53", "Jun", "", "Whitfield", "", "", ""));
            save(new User("aas165@lensfolio.nz", encrypt("aas165Password"), UserRole.STUDENT, "aas165", "Aeryn", "", "Amos", "", "", ""));
            save(new User("awr139@lensfolio.nz", encrypt("awr139Password"), UserRole.STUDENT, "awr139", "Astrid", "", "Whittaker", "", "", ""));
            save(new User("acs95@lensfolio.nz", encrypt("acs95Password"), UserRole.STUDENT, "acs95", "Aston", "", "Collins", "", "", ""));

        } catch (Exception e) {
            logger.error (e.getMessage());
        }
    }
}
