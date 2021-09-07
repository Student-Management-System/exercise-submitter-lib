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

import org.junit.jupiter.api.Test;

import com.google.gson.JsonSyntaxException;

import net.ssehub.studentmgmt.backend_api.model.UserDto;
import net.ssehub.teaching.exercise_submitter.lib.data.Assignment;
import net.ssehub.teaching.exercise_submitter.lib.data.Assignment.State;
import net.ssehub.teaching.exercise_submitter.lib.data.Course;

public class ApiConnectionTest {
    
    @Test
    public void loginAuthUrlInvalidHost() {
        ApiConnection api = new ApiConnection("http://doesnt.exist.local:8000", "http://doesnt.matter.local");
        NetworkException e = assertThrows(NetworkException.class, () -> api.login("student1", "Bunny123"));
        assertTrue(e.getCause() instanceof IOException);
    }
    
    @Test
    public void loginAuthUrlNoServiceListening() {
        ApiConnection api = new ApiConnection("http://localhost:55555", "http://doesnt.matter.local");
        NetworkException e = assertThrows(NetworkException.class, () -> api.login("student1", "Bunny123"));
        assertTrue(e.getCause() instanceof IOException);
    }
    
    @Test
    public void loginAuthResponseWrongContentType() {
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
    public void loginAuthResponseInvalidJson() {
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

    @Test
    public void getCourseInvalidHost() {
        ApiConnection api = new ApiConnection("http://doesnt.exist.local:8000", "http://doesnt.exist.local:3000");
        NetworkException e = assertThrows(NetworkException.class, () -> api.getCourse("java-ise2021"));
        assertTrue(e.getCause() instanceof IOException);
    }
    
    @Test
    public void getCourseNoServiceListening() {
        ApiConnection api = new ApiConnection("http://localhost:55555", "http://localhost:55555");
        NetworkException e = assertThrows(NetworkException.class, () -> api.getCourse("java-wise2021"));
        assertTrue(e.getCause() instanceof IOException);
    }
    
    @Test
    public void getCourseWrongContentType() {
        DummyHttpServer dummyServer = new DummyHttpServer("text/plain; charset=utf-8", "Hello World!");
        dummyServer.start();
        
        ApiConnection api = new ApiConnection(
                "http://doesnt.matter.local", "http://localhost:" + dummyServer.getPort());
        
        ApiException e = assertThrows(ApiException.class, () -> api.getCourse("java-wise2021"));
        assertAll(
            () -> assertSame(ApiException.class, e.getClass()),
            () -> assertEquals("Unknown exception: Hello World!", e.getMessage()),
            () -> assertNotNull(e.getCause())
        );
    }
    
    @Test
    public void getCourseInvalidJson() {
        DummyHttpServer dummyServer = new DummyHttpServer("application/json; charset=utf-8", "{invalid");
        dummyServer.start();
        
        ApiConnection api = new ApiConnection(
                "http://doesnt.matter.local", "http://localhost:" + dummyServer.getPort());
        
        ApiException e = assertThrows(ApiException.class, () -> api.getCourse("java-wise2021"));
        assertAll(
            () -> assertSame(ApiException.class, e.getClass()),
            () -> assertEquals("Invalid JSON response", e.getMessage()),
            () -> assertTrue(e.getCause() instanceof JsonSyntaxException)
        );
    }
    
    @Test
    public void getAssignmentsInvalidHost() {
        ApiConnection api = new ApiConnection("http://doesnt.exist.local:8000", "http://doesnt.exist.local:3000");
        NetworkException e = assertThrows(NetworkException.class,
            () -> api.getAssignments(new Course("Java", "java-wise20210")));
        assertTrue(e.getCause() instanceof IOException);
    }
    
    @Test
    public void getAssignmentsNoServiceListening() {
        ApiConnection api = new ApiConnection("http://localhost:55555", "http://localhost:55555");
        NetworkException e = assertThrows(NetworkException.class,
            () -> api.getAssignments(new Course("Java", "java-wise20210")));
        assertTrue(e.getCause() instanceof IOException);
    }
    
    @Test
    public void getAssignmentsWrongContentType() {
        DummyHttpServer dummyServer = new DummyHttpServer("text/plain; charset=utf-8", "Hello World!");
        dummyServer.start();
        
        ApiConnection api = new ApiConnection(
                "http://doesnt.matter.local", "http://localhost:" + dummyServer.getPort());
        
        ApiException e = assertThrows(ApiException.class, () -> api.getAssignments(new Course("", "java-wise2021")));
        assertAll(
            () -> assertSame(ApiException.class, e.getClass()),
            () -> assertEquals("Unknown exception: Hello World!", e.getMessage()),
            () -> assertNotNull(e.getCause())
        );
    }
    
    @Test
    public void getAssignmentsInvalidJson() {
        DummyHttpServer dummyServer = new DummyHttpServer("application/json; charset=utf-8", "{invalid");
        dummyServer.start();
        
        ApiConnection api = new ApiConnection(
                "http://doesnt.matter.local", "http://localhost:" + dummyServer.getPort());
        
        ApiException e = assertThrows(ApiException.class, () -> api.getAssignments(new Course("", "java-wise2021")));
        assertAll(
            () -> assertSame(ApiException.class, e.getClass()),
            () -> assertEquals("Invalid JSON response", e.getMessage()),
            () -> assertTrue(e.getCause() instanceof JsonSyntaxException)
        );
    }
    
    @Test
    public void getGroupNameInvalidHost() {
        ApiConnection api = new ApiConnection("http://doesnt.exist.local:8000", "http://doesnt.exist.local:3000");
        fakeLogin(api);
        
        NetworkException e = assertThrows(NetworkException.class, () -> api.getGroupName(
                new Course("", "java-wise2021"), new Assignment("123", "", State.SUBMISSION, true)));
        assertTrue(e.getCause() instanceof IOException);
    }
    
    @Test
    public void getGroupNameNoServiceListening() {
        ApiConnection api = new ApiConnection("http://localhost:55555", "http://localhost:55555");
        fakeLogin(api);
        
        NetworkException e = assertThrows(NetworkException.class, () -> api.getGroupName(
                new Course("", "java-wise2021"), new Assignment("123", "", State.SUBMISSION, true)));
        assertTrue(e.getCause() instanceof IOException);
    }
    
    @Test
    public void getGroupNameWrongContentType() {
        DummyHttpServer dummyServer = new DummyHttpServer("text/plain; charset=utf-8", "Hello World!");
        dummyServer.start();
        
        ApiConnection api = new ApiConnection(
                "http://doesnt.matter.local", "http://localhost:" + dummyServer.getPort());
        fakeLogin(api);
        
        ApiException e = assertThrows(ApiException.class, () -> api.getGroupName(
                new Course("", "java-wise2021"), new Assignment("123", "", State.SUBMISSION, true)));
        assertAll(
            () -> assertSame(ApiException.class, e.getClass()),
            () -> assertEquals("Unknown exception: Hello World!", e.getMessage()),
            () -> assertNotNull(e.getCause())
        );
    }
    
    @Test
    public void getGroupNameInvalidJson() {
        DummyHttpServer dummyServer = new DummyHttpServer("application/json; charset=utf-8", "{invalid");
        dummyServer.start();
        
        ApiConnection api = new ApiConnection(
                "http://doesnt.matter.local", "http://localhost:" + dummyServer.getPort());
        fakeLogin(api);
        
        ApiException e = assertThrows(ApiException.class, () -> api.getGroupName(
                new Course("", "java-wise2021"), new Assignment("123", "", State.SUBMISSION, true)));
        assertAll(
            () -> assertSame(ApiException.class, e.getClass()),
            () -> assertEquals("Invalid JSON response", e.getMessage()),
            () -> assertTrue(e.getCause() instanceof JsonSyntaxException)
        );
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
