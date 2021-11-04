package net.ssehub.teaching.exercise_submitter.lib.student_management_system;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingConsumer;

import com.google.gson.JsonSyntaxException;

import net.ssehub.studentmgmt.backend_api.model.UserDto;
import net.ssehub.teaching.exercise_submitter.lib.data.Assignment;
import net.ssehub.teaching.exercise_submitter.lib.data.Assignment.State;
import net.ssehub.teaching.exercise_submitter.lib.data.Course;

public class ApiConnectionTest {
    
    @Nested
    public class Login {
        
        @Test
        public void authUrlInvalidHostThrows() {
            ApiConnection api = new ApiConnection("http://doesnt.exist.local:8000", "http://doesnt.matter.local");
            NetworkException e = assertThrows(NetworkException.class, () -> api.login("student1", "Bunny123"));
            assertTrue(e.getCause() instanceof IOException);
        }
        
        @Test
        public void authUrlNoServiceListeningThrows() {
            ApiConnection api = new ApiConnection("http://localhost:55555", "http://doesnt.matter.local");
            NetworkException e = assertThrows(NetworkException.class, () -> api.login("student1", "Bunny123"));
            assertTrue(e.getCause() instanceof IOException);
        }
        
        @Test
        public void authResponseWrongContentTypeThrows() {
            DummyHttpServer dummyServer = new DummyHttpServer("text/plain; charset=utf-8", "Hello World!");
            dummyServer.start();
            
            ApiConnection api = new ApiConnection(
                    "http://localhost:" + dummyServer.getPort(), "http://doesnt.matter.local");
            
            ApiException e = assertThrows(ApiException.class, () -> api.login("student1", "123456"));
            assertAll(
                () -> assertSame(ApiException.class, e.getClass()),
                () -> assertEquals("Unknown exception: Hello World!", e.getMessage()),
                () -> assertNotNull(e.getCause())
            );
        }
        
        @Test
        public void authResponseInvalidJsonThrows() {
            DummyHttpServer dummyServer = new DummyHttpServer("application/json; charset=utf-8", "{invalid");
            dummyServer.start();
            
            ApiConnection api = new ApiConnection(
                    "http://localhost:" + dummyServer.getPort(), "http://doesnt.matter.local");
            
            ApiException e = assertThrows(ApiException.class, () -> api.login("student1", "123456"));
            assertAll(
                () -> assertSame(ApiException.class, e.getClass()),
                () -> assertEquals("Invalid JSON response", e.getMessage()),
                () -> assertTrue(e.getCause() instanceof JsonSyntaxException)
            );
        }
        
    }

    public class StandardExceptionHandlingTests {
        
        protected ThrowingConsumer<ApiConnection> sut;
        
        public StandardExceptionHandlingTests(ThrowingConsumer<ApiConnection> sut) {
            this.sut = sut;
        }
        
        @Test
        public void invalidHostThrows() {
            ApiConnection api = new ApiConnection("http://doesnt.exist.local:8000", "http://doesnt.exist.local:3000");
            fakeLogin(api);
            NetworkException e = assertThrows(NetworkException.class, () -> sut.accept(api));
            assertTrue(e.getCause() instanceof IOException);
        }
        
        @Test
        public void noServiceListeningThrows() {
            ApiConnection api = new ApiConnection("http://localhost:55555", "http://localhost:55555");
            fakeLogin(api);
            NetworkException e = assertThrows(NetworkException.class, () -> sut.accept(api));
            assertTrue(e.getCause() instanceof IOException);
        }
        
        @Test
        public void wrongContentTypeThrows() {
            DummyHttpServer dummyServer = new DummyHttpServer("text/plain; charset=utf-8", "Hello World!");
            dummyServer.start();
            
            ApiConnection api = new ApiConnection(
                    "http://doesnt.matter.local", "http://localhost:" + dummyServer.getPort());
            fakeLogin(api);
            
            ApiException e = assertThrows(ApiException.class, () -> sut.accept(api));
            assertAll(
                () -> assertSame(ApiException.class, e.getClass()),
                () -> assertEquals("Unknown exception: Hello World!", e.getMessage()),
                () -> assertNotNull(e.getCause())
            );
        }
        
        @Test
        public void invalidJsonThrows() {
            DummyHttpServer dummyServer = new DummyHttpServer("application/json; charset=utf-8", "{invalid");
            dummyServer.start();
            
            ApiConnection api = new ApiConnection(
                    "http://doesnt.matter.local", "http://localhost:" + dummyServer.getPort());
            fakeLogin(api);
            
            ApiException e = assertThrows(ApiException.class, () -> sut.accept(api));
            assertAll(
                () -> assertSame(ApiException.class, e.getClass()),
                () -> assertEquals("Invalid JSON response", e.getMessage()),
                () -> assertTrue(e.getCause() instanceof JsonSyntaxException)
            );
        }
        
        
    }
    
    @Nested
    public class GetCourse extends StandardExceptionHandlingTests {
        
        public GetCourse() {
            super(api -> api.getCourse("java-wise2021"));
        }
    }
    
    @Nested
    public class GetAssignments extends StandardExceptionHandlingTests {
        
        public GetAssignments() {
            super(api -> api.getAssignments(new Course("Java", "java-wise2021")));
        }
    }
    
    @Nested
    public class GetGroupName extends StandardExceptionHandlingTests {
        
        public GetGroupName() {
            super(api -> api.getGroupName(
                    new Course("Java", "java-wise2021"), new Assignment("123", "", State.SUBMISSION, true)));
        }
        
        @Test
        public void notLoggedInThrows() {
            ApiConnection api = new ApiConnection("http://doesnt.matter.local", "http://doesnt.matter.local");
            
            AuthenticationException e = assertThrows(AuthenticationException.class, () -> sut.accept(api));
            assertEquals("Not logged in", e.getMessage());
        }
        
    }
    
    @Nested
    public class HasTutorRights extends StandardExceptionHandlingTests {
        
        public HasTutorRights() {
            super(api -> api.hasTutorRights(new Course("Java", "java-wise2021")));
        }
        
        @Test
        public void notLoggedInThrows() {
            ApiConnection api = new ApiConnection("http://doesnt.matter.local", "http://doesnt.matter.local");
            
            AuthenticationException e = assertThrows(AuthenticationException.class, () -> sut.accept(api));
            assertEquals("Not logged in", e.getMessage());
        }
        
    }
    
    @Nested
    public class GetAllGroups extends StandardExceptionHandlingTests {
        
        public GetAllGroups() {
            super(api -> api.getAllGroups(
                    new Course("Java", "java-wise2021"), new Assignment("123", "", State.IN_REVIEW, true)));
        }
        
    }
    
    @Nested
    public class GetAssessments extends StandardExceptionHandlingTests {
        
        public GetAssessments() {
            super(api -> api.getAssessment(
                    new Course("Java", "java-wise2021"), new Assignment("123", "", State.IN_REVIEW, true), "group01"));
        }
        
    }
    
    private void fakeLogin(ApiConnection api) {
        UserDto user = new UserDto();
        user.setId("123");
        assertDoesNotThrow(() -> {
            Field field = api.getClass().getDeclaredField("loggedInUser");
            field.setAccessible(true);
            field.set(api, user);
        });
    }
    
    static class DummyHttpServer implements Runnable {

        private ServerSocket serverSocket;
        
        private String response;
        
        public DummyHttpServer(String contentType, String response) {
            this.response = "HTTP/1.1 200 OK\r\n"
                    + "Content-Length: " + response.length() + "\r\n"
                    + "Content-Type: " + contentType + "\r\n"
                    + "\r\n"
                    + response;
            this.serverSocket = assertDoesNotThrow(() -> new ServerSocket(0, 1, InetAddress.getLoopbackAddress()));
        }
        
        public int getPort() {
            return this.serverSocket.getLocalPort();
        }
        
        public void start() {
            Thread th = new Thread(this);
            th.setDaemon(true);
            th.start();
        }
        
        @Override
        public void run() {
            try {
                Socket socket = serverSocket.accept();

                Thread read = new Thread(() -> {
                    try {
                        InputStream in = socket.getInputStream();
                        while (in.read() != -1) {
                            // just read
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                read.setDaemon(true);
                read.start();
                
                OutputStream out = socket.getOutputStream();
                out.write(response.getBytes(StandardCharsets.UTF_8));
                
                try {
                    read.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                
                socket.close();
                
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
    }
    
}
