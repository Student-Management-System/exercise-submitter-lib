package net.ssehub.teaching.exercise_submitter.lib.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import net.ssehub.studentmgmt.backend_api.api.AuthenticationApi;
import net.ssehub.studentmgmt.backend_api.api.CourseApi;
import net.ssehub.studentmgmt.backend_api.api.CourseParticipantsApi;
import net.ssehub.studentmgmt.backend_api.api.GroupApi;
import net.ssehub.studentmgmt.backend_api.model.CourseConfigDto;
import net.ssehub.studentmgmt.backend_api.model.CourseCreateDto;
import net.ssehub.studentmgmt.backend_api.model.CourseDto;
import net.ssehub.studentmgmt.backend_api.model.GroupDto;
import net.ssehub.studentmgmt.backend_api.model.GroupSettingsDto;
import net.ssehub.studentmgmt.backend_api.model.PasswordDto;
import net.ssehub.studentmgmt.sparkyservice_api.ApiClient;
import net.ssehub.studentmgmt.sparkyservice_api.ApiException;
import net.ssehub.studentmgmt.sparkyservice_api.api.AuthControllerApi;
import net.ssehub.studentmgmt.sparkyservice_api.api.RoutingControllerApi;
import net.ssehub.studentmgmt.sparkyservice_api.api.UserControllerApi;
import net.ssehub.studentmgmt.sparkyservice_api.model.AuthenticationInfoDto;
import net.ssehub.studentmgmt.sparkyservice_api.model.ChangePasswordDto;
import net.ssehub.studentmgmt.sparkyservice_api.model.CredentialsDto;
import net.ssehub.studentmgmt.sparkyservice_api.model.UserDto;
import net.ssehub.studentmgmt.sparkyservice_api.model.UsernameDto;

/**
 * A utility class for integration tests that runs a fresh instance of the Student Management System in a Docker
 * container. Multiple instances can safely be used in parallel.
 * 
 * @author Adam
 */
public class StuMgmtDocker implements AutoCloseable {

    /**
     * Cache whether the docker images have been built. We want to build the images, as we have our own build arguments
     * (in args.properties). But if the same JVM runs this class multiple times, we can assume that nobody has
     * overwritten the images in between; thus we don't re-build images in subsequent calls.
     */
    private static boolean built = false;
    
    /**
     * An object used to guard the build-start path. This ensures that multiple parallel calls of the constructor
     * don't interfere with each other.
     */
    private static final Object BUILD_LOCK = new Object();
    
    private File dockerDirectory;
    
    private String dockerId;
    
    private int authPort;
    
    private int mgmtPort;
    
    private int webPort;
    
    private Map<String, String> userPasswords;
    
    private Map<String, String> userMgmtIds;
    
    private Map<String, String> teachersOfCourse;
    
    /**
     * Starts a new instance of the Student Management System in a docker container. Uses ports 8080 and 8000.
     * 
     * @param dockerDirectory The directory where the docker-compose.yml file for the student management system lies.
     * 
     * @throws IllegalArgumentException If the given directory is not a directory or does not contain a
     *      docker-compose.yml file.
     * @throws DockerException If executing docker fails.
     */
    public StuMgmtDocker(File dockerDirectory) throws DockerException {
        if (!dockerDirectory.isDirectory()) {
            throw new IllegalArgumentException(dockerDirectory + " is not a directory");
        }
        if (!new File(dockerDirectory, "docker-compose.yml").isFile()) {
            throw new IllegalArgumentException(dockerDirectory + " does not contain a docker-compose.yml file");
        }
        this.dockerDirectory = dockerDirectory;
        
        this.dockerId = String.format("stu-mgmt-testing-%04d", (int) (Math.random() * 1024));
        this.authPort = generateRandomPort();
        this.mgmtPort = generateRandomPort();
        this.webPort = generateRandomPort();

        synchronized (BUILD_LOCK) {
            if (!built) {
                buildImages();
                built = true;
            }
            startDocker();
        }
        
        this.userPasswords = new HashMap<>();
        this.userPasswords.put("admin_user", "admin_pw");
        
        this.userMgmtIds = new HashMap<>();
        this.teachersOfCourse = new HashMap<>();
        
        System.out.println("Waiting for services to be up...");
        waitUntilAuthReachable();
    }
    
    /**
     * Returns the URL for the Sparky-Service system.
     * 
     * @return The URL of the auth system
     */
    public String getAuthUrl() {
        return "http://localhost:" + authPort;
    }
    
    /**
     * Returns the URL of the Student Management System.
     * 
     * @return The stu-mgmt URL.
     */
    public String getStuMgmtUrl() {
        return "http://localhost:" + mgmtPort;
    }
    
    /**
     * Returns the ULR of the web client.
     * 
     * @return The web client URL.
     */
    public String getWebUrl() {
        return "http://localhost:" + webPort + "/";
    }
    
    public void createUser(String name, String password) throws DockerException {
        if (password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        
        userPasswords.put(name, password);
        
        ApiClient client = getAuthenticatedAuthClient("admin_user");
        
        UserControllerApi api = new UserControllerApi(client);
        
        UserDto user;
        try {
            UsernameDto username = new UsernameDto();
            username.setUsername(name);
            
            user = api.createLocalUser(username);
        } catch (ApiException e) {
            System.err.println(e.getResponseBody());
            throw new DockerException(e);
        }
        
        try {
            ChangePasswordDto pwDto = new ChangePasswordDto();
            pwDto.setNewPassword(password);
            user.setPasswordDto(pwDto);
            
            api.editUser(user);
        } catch (ApiException e) {
            System.err.println(e.getResponseBody());
            throw new DockerException(e);
        }
        
        // make user "known" to stu-mgmt by calling its auth route
        net.ssehub.studentmgmt.backend_api.ApiClient backendClient = getAuthenticatedBackendClient(name);
        AuthenticationApi backendApi = new AuthenticationApi(backendClient);
        try {
            net.ssehub.studentmgmt.backend_api.model.UserDto dto = backendApi.whoAmI();
            userMgmtIds.put(name, dto.getId());
            
        } catch (net.ssehub.studentmgmt.backend_api.ApiException e) {
            System.err.println(e.getResponseBody());
            throw new DockerException(e);
        }
        
        System.out.println("Created user " + name + " with password: " + password);
    }
    
    private String getToken(String user) throws DockerException {
        ApiClient client = new ApiClient();
        client.setBasePath(getAuthUrl());
        
        CredentialsDto credentials = new CredentialsDto();
        credentials.setUsername(user);
        credentials.setPassword(userPasswords.get(user));
        
        AuthControllerApi api = new AuthControllerApi(client);
        AuthenticationInfoDto auth;
        try {
            auth = api.authenticate(credentials);
        } catch (ApiException e) {
            System.err.println(e.getResponseBody());
            throw new DockerException(e);
        }
        
        return auth.getToken().getToken();
    }
    
    private ApiClient getAuthenticatedAuthClient(String username) throws DockerException {
        ApiClient client = new ApiClient();
        client.setBasePath(getAuthUrl());
        client.setAccessToken(getToken(username));
        return client;
    }
    
    private net.ssehub.studentmgmt.backend_api.ApiClient getAuthenticatedBackendClient(String username) throws DockerException {
        net.ssehub.studentmgmt.backend_api.ApiClient client = new net.ssehub.studentmgmt.backend_api.ApiClient();
        client.setBasePath(getStuMgmtUrl());
        client.setAccessToken(getToken(username));
        return client;
    }
    
    public String createCourse(String shortName, String semester, String title, String... lecturers) throws DockerException {
        net.ssehub.studentmgmt.backend_api.ApiClient client = getAuthenticatedBackendClient("admin_user");
        
        CourseApi courseApi = new CourseApi(client);
        
        GroupSettingsDto groupSettings = new GroupSettingsDto();
        groupSettings.setAllowGroups(true);
        groupSettings.setSelfmanaged(true);
        groupSettings.setAutoJoinGroupOnCourseJoined(false);
        groupSettings.setMergeGroupsOnAssignmentStarted(false);
        groupSettings.setSizeMin(BigDecimal.ZERO);
        groupSettings.setSizeMax(BigDecimal.TEN);
        
        CourseConfigDto courseConfig = new CourseConfigDto();
        courseConfig.setGroupSettings(groupSettings);
        
        CourseCreateDto courseCreate = new CourseCreateDto();
        courseCreate.setShortname(shortName);
        courseCreate.setSemester(semester);
        courseCreate.setTitle(title);
        courseCreate.setLecturers(Arrays.asList(lecturers));
        courseCreate.setIsClosed(false);
        courseCreate.setConfig(courseConfig);
        
        String courseId;
        
        try {
            CourseDto dto = courseApi.createCourse(courseCreate);
            
            courseId = dto.getId();
            
        } catch (net.ssehub.studentmgmt.backend_api.ApiException e) {
            System.err.println(e.getResponseBody());
            throw new DockerException(e);
        }
        
        this.teachersOfCourse.put(courseId, lecturers[0]);
        
        System.out.println("Created course " + courseId + " (" + title + ")");
        return courseId;
    }
    
    public void enrollStudentInCourse(String courseId, String student) throws DockerException {
        net.ssehub.studentmgmt.backend_api.ApiClient client = getAuthenticatedBackendClient(student);
        CourseParticipantsApi api = new CourseParticipantsApi(client);
        
        try {
            PasswordDto pw = new PasswordDto();
            pw.setPassword("");
            
            api.addUser(pw, courseId, userMgmtIds.get(student));
        } catch (net.ssehub.studentmgmt.backend_api.ApiException e) {
            System.err.println(e.getResponseBody());
            throw new DockerException(e);
        }
        
        System.out.println("Enrolled " + student + " in course " + courseId);
    }
    
    public String createGroupInCourse(String courseId, String groupName, String... members) throws DockerException {
        net.ssehub.studentmgmt.backend_api.ApiClient client = getAuthenticatedBackendClient(teachersOfCourse.get(courseId));
        GroupApi api = new GroupApi(client);
        
        GroupDto group = new GroupDto();
        group.setName(groupName);
        
        try {
            group = api.createGroup(group, courseId);
        } catch (net.ssehub.studentmgmt.backend_api.ApiException e) {
            System.err.println(e.getResponseBody());
            throw new DockerException(e);
        }
        
        PasswordDto pw = new PasswordDto();
        pw.setPassword("");
        
        for (String member : members) {
            try {
                api.addUserToGroup(pw, courseId, group.getId(), userMgmtIds.get(member));
            } catch (net.ssehub.studentmgmt.backend_api.ApiException e) {
                System.err.println(e.getResponseBody());
                throw new DockerException(e);
            }
        }
        
        System.out.println("Created group " + groupName + " with members: " + Arrays.toString(members));
        
        return group.getId();
    }

    /**
     * Stops and removes the docker containers.
     */
    @Override
    public void close() throws DockerException {
        stopDocker();
    }
    
    private void buildImages() throws DockerException {
        runProcess("docker", "compose", "--project-name", dockerId, "build");
    }
    
    private void startDocker() throws DockerException {
        runProcess("docker", "compose", "--project-name", dockerId, "up", "--detach");
    }
    
    private void stopDocker() throws DockerException {
        runProcess("docker", "compose", "--project-name", dockerId, "down");
    }
    
    private void waitUntilAuthReachable() {
        ApiClient client = new ApiClient();
        client.setBasePath(getAuthUrl());
        RoutingControllerApi api = new RoutingControllerApi(client);
        
        long tStart = System.currentTimeMillis();
        boolean success;
        do {
            try {
                api.isAlive();
                success = true;
            } catch (ApiException e) {
                success = false;
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e1) {}
            }
        } while (!success && System.currentTimeMillis() - tStart < 20000 /* 20 seconds */);
    }
    
    private void runProcess(String... command) throws DockerException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(dockerDirectory);
        pb.inheritIO();
        
        Properties envArgs = new Properties();
        try (FileInputStream in = new FileInputStream(new File("src/test/resources/StuMgmtDocker/args.properties"))) {
            envArgs.load(in);
        } catch (IOException e) {
            throw new DockerException("Can't load properties file with environment arguments", e);
        }
        
        Map<String, String> environment = pb.environment();
        for (Entry<Object, Object> entry : envArgs.entrySet()) {
            environment.put(entry.getKey().toString(), entry.getValue().toString());
        }
        
        environment.put("FRONTEND_SPARKY_HOST", getAuthUrl());
        environment.put("SPARKY_PORT", Integer.toString(authPort));
        environment.put("BACKEND_PORT", Integer.toString(mgmtPort));
        environment.put("FRONTEND_PORT", Integer.toString(webPort));
        
        Process p;
        try {
            p = pb.start();
        } catch (IOException e) {
            throw new DockerException("Failed to execute docker compose", e);
        }
        
        boolean interrupted;
        do {
            try {
                p.waitFor();
                interrupted = false;
            } catch (InterruptedException e) {
                interrupted = true;
            }
        } while (interrupted);
    }
    
    private int generateRandomPort() {
        return  (int) (Math.random() * (65535 - 49152)) + 49152;
    }
    
    /**
     * Runs an instance until enter is pressed in the console.
     * 
     * @param args Command line arguments. First element is used as the docker directory path.
     * 
     * @throws IOException If reading System.in fails.
     */
    public static void main(String[] args) throws IOException {
        try (StuMgmtDocker docker = new StuMgmtDocker(new File(args[0]))) {
            
            docker.createUser("adam", "123456");
            docker.createUser("student1", "123456");
            docker.createUser("student2", "123456");
            docker.createUser("student3", "123456");
            docker.createUser("student4", "123456");
            
            String courseId = docker.createCourse("java", "wise2021", "Programmierpraktikum: Java", "adam");
            docker.enrollStudentInCourse(courseId, "student1");
            docker.enrollStudentInCourse(courseId, "student2");
            docker.enrollStudentInCourse(courseId, "student3");
            docker.enrollStudentInCourse(courseId, "student4");
            
            docker.createGroupInCourse(courseId, "JP001", "student1", "student3");
            docker.createGroupInCourse(courseId, "JP002", "student2", "student4");
            
            System.out.println();
            System.out.println();
            System.out.println("Docker running:");
            System.out.println("Auth: " + docker.getAuthUrl());
            System.out.println("Mgmt: " + docker.getStuMgmtUrl());
            System.out.println("Web:  " + docker.getWebUrl());
            
            System.out.println();
            System.out.println("Press enter to stop");
            
            System.in.read();
            
        } catch (DockerException e) {
            e.printStackTrace();
        }
    }
    
}
