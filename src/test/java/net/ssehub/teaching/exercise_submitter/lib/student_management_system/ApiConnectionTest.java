package net.ssehub.teaching.exercise_submitter.lib.student_management_system;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import net.ssehub.teaching.exercise_submitter.lib.data.Course;

public class ApiConnectionTest {
    
    @Test
    public void loginInvalidAuthResponse() {
        DummyHttpServer dummyServer = new DummyHttpServer("text/plain; charset=utf-8", "Hello World!");
        dummyServer.start();
        
        ApiConnection api = new ApiConnection("http://localhost:" + dummyServer.getPort(), "http://doesnt.matter.local");
        
        ApiException e = assertThrows(ApiException.class, () -> api.login("student1", "123456"));
        assertSame(ApiException.class, e.getClass());
    }

    @Test
    public void getCourseWrongUrl() {
        ApiConnection api = new ApiConnection("http://doesnt.exist.local:8000", "http://doesnt.exist.local:3000");
        assertThrows(NetworkException.class, () -> api.getCourse("java", "wise2021"));
    }
    
    @Test
    public void getCourseInvalidResponse() {
        DummyHttpServer dummyServer = new DummyHttpServer("text/plain; charset=utf-8", "Hello World!");
        dummyServer.start();
        
        ApiConnection api = new ApiConnection("http://doesnt.matter.local", "http://localhost:" + dummyServer.getPort());
        
        ApiException e = assertThrows(ApiException.class, () -> api.getCourse("java", "wise2021"));
        assertSame(ApiException.class, e.getClass());
    }
    
    @Test
    public void getAssignmentsWrongUrl() {
        ApiConnection api = new ApiConnection("http://doesnt.exist.local:8000", "http://doesnt.exist.local:3000");
        assertThrows(NetworkException.class, () -> api.getAssignments(new Course("Java", "java-wise20210")));
    }
    
    static class DummyHttpServer implements Runnable {

        private ServerSocket serverSocket;
        
        private String response;
        
        public DummyHttpServer(String responseType, String response) {
            this.response = "HTTP/1.1 200 OK\r\n"
                    + "Content-Length: " + response.length() + "\r\n"
                    + "Content-Type: " + responseType + "\r\n"
                    + "\r\n"
                    + response;
            this.serverSocket = assertDoesNotThrow(() -> new ServerSocket(0));
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
