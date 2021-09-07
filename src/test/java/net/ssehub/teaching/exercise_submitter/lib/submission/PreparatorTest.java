package net.ssehub.teaching.exercise_submitter.lib.submission;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;



public class PreparatorTest {

    private static final File TESTDATA = new File("src/test/resources/PreparatorTest");

    @Test
    public void throwsIfDirDoesntExist() {
        IOException ex = assertThrows(IOException.class, () -> {
            try (Preparator preparator = new Preparator()) {
                preparator.prepareDir(new File("doesnt_exist"));
            }
        });
        assertEquals("doesnt_exist is not a directory", ex.getMessage());
    }

    @Test
    public void throwsIfDirIsFile() {
        IOException ex = assertThrows(IOException.class, () -> {
            try (Preparator preparator = new Preparator()) {
                preparator.prepareDir(new File(TESTDATA, "file.txt"));
            }
        });
        assertEquals("file.txt is not a directory", ex.getMessage());
    }

    @Test
    public void prepareEmptyFolder() {
        File source = new File(TESTDATA, "emptyDir");
        source.mkdir();
        assertTrue(source.isDirectory(), "precondition: empty test directory exists");

        assertDoesNotThrow(() -> {
            try (Preparator preparator = new Preparator()) {
                File result = preparator.getResult();
                preparator.prepareDir(source);

                assertAll(
                    () -> assertTrue(result.isDirectory()),
                    () -> assertNotEquals(result, source),
                    
                    //.classpath + .project generated
                    () -> assertEquals(2, result.listFiles().length),
                    () -> assertTrue(new File(result, ".classpath").isFile()),
                    () -> assertTrue(new File(result, ".project").isFile())
                );
            }
        });
    }

    @Test
    public void clearsCopiedEmptyDir() {
        File source = new File(TESTDATA, "emptyDir");
        source.mkdir();
        assertTrue(source.isDirectory(), "precondition: empty test directory exists");

        assertDoesNotThrow(() -> {
            File result;
            try (Preparator preparator = new Preparator()) {
                preparator.prepareDir(source);
                result = preparator.getResult();
                assertTrue(result.isDirectory());
            }

            assertFalse(result.exists());
        });

    }

    @Test
    public void copiesFiles() {
        File source = new File(TESTDATA, "notEmptyDir");

        assertDoesNotThrow(() -> {
            File result;
            try (Preparator preparator = new Preparator()) {
                preparator.prepareDir(source);
                result = preparator.getResult();
                
                assertAll(
                    () -> assertTrue(result.isDirectory()),
                    // file + .project and .classpath 
                    () -> assertTrue(new File(result, "file.txt").isFile()),
                    () -> assertTrue(new File(result, ".project").isFile()),
                    () -> assertTrue(new File(result, ".classpath").isFile()),
                    
                    () -> assertEquals(3, result.listFiles().length),
                    
                    // test that copied content is correct
                    () -> {
                        String fileContent = Files.readString(new File(result, "file.txt").toPath());
                        assertEquals("This is a file.\n", fileContent);
                    }
                ); 
                
            }
        });

    }
    
    @Test
    public void clearsCopiedNotEmptyDir() {
        File source = new File(TESTDATA, "notEmptyDir");

        assertDoesNotThrow(() -> {
            File result;
            try (Preparator preparator = new Preparator()) {
                preparator.prepareDir(source);
                result = preparator.getResult();
                assertTrue(result.isDirectory());
            }

            assertFalse(result.exists());
        });

    }

    @Test
    public void copiesFileInSubDirectory() {
        File source = new File(TESTDATA, "notEmptyDirWithSubDir");

        assertDoesNotThrow(() -> {
            File result;
            try (Preparator preparator = new Preparator()) {
                preparator.prepareDir(source);
                result = preparator.getResult();
                File subDir = new File(result, "notEmptyDir");
                File classpath = new File(result, ".classpath");
                File project = new File(result, ".project");
                
                assertAll(
                    () -> assertTrue(result.isDirectory()),
                    () -> assertTrue(subDir.isDirectory()),
                    
                    //.project and .classpath are generated
                    () -> assertTrue(classpath.exists()),
                    () -> assertTrue(project.exists()),
                    
                    // plus one sub directory
                    () -> assertEquals(3, result.listFiles().length),
                    
                    // exactly one file in sub directory
                    () -> assertTrue(new File(subDir, "file.txt").isFile()),
                    () -> assertEquals(1, subDir.listFiles().length),
                    
                    // test that copied content is correct
                    () -> {
                        String fileContent = Files.readString((new File(subDir, "file.txt").toPath()));
                        assertEquals("This is a file.\n", fileContent);
                    }
                );
            }
        });

    }
    
    @Test
    public void iso88591ToUtf8() {
        File source = new File(TESTDATA, "encodings");

        assertDoesNotThrow(() -> {
            File result;
            try (Preparator preparator = new Preparator()) {
                preparator.prepareDir(source);
                result = preparator.getResult();
                assertTrue(result.isDirectory());

                File utf8file = new File(result, "ISO-8859-1.txt");
                
                try (FileInputStream fs = new FileInputStream(utf8file)) {
                    byte[] bytes = fs.readAllBytes();
                    
                    //throws exception if charset is not utf-8
                    String content = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
                    assertEquals("ISO-8859-1\nöäüÖÄÜß\n", content);
                }
            }
        });
    }
    
    @Test
    public void cp1252ToUtf8() {
        File source = new File(TESTDATA, "encodings");
        
        assertDoesNotThrow(() -> {
            File result;
            try (Preparator preparator = new Preparator()) {
                preparator.prepareDir(source);
                result = preparator.getResult();
                assertTrue(result.isDirectory());
                
                File utf8file = new File(result, "cp1252.txt");
                
                try (FileInputStream fs = new FileInputStream(utf8file)) {
                    byte[] bytes = fs.readAllBytes();
                    
                    //throws exception if charset is not utf-8
                    String content = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
                    assertEquals("cp 1252\nöäüÖÄÜß\n", content);
                }
            }
        });
    }
    
    @Test
    public void notConvertingNonTextFile() {
        File source = new File(TESTDATA, "nonText");
        
        File originalPicture = new File(source, "picture.png");
        
        assertDoesNotThrow(() -> {
            File result;
            try (Preparator preparator = new Preparator()) {
                preparator.prepareDir(source);
                result = preparator.getResult();
                assertTrue(result.isDirectory());
                
                File copiedPicture = new File(result, "picture.png");
                
                byte[] originalBytes;
                byte[] copiedBytes;
                try (FileInputStream original = new FileInputStream(originalPicture);
                        FileInputStream copied = new FileInputStream(copiedPicture)) {
                    originalBytes = original.readAllBytes();
                    copiedBytes = copied.readAllBytes();
                }
                
                assertArrayEquals(originalBytes, copiedBytes);
            }
        });
    }
    
    @Test
    @Disabled("There seems to be no way to create an invalid ISO 8859-1 file; thus the invalid file is always "
            + "(wrongly) converted to UTF-8 instead of copying it over unchanged") // TODO
    public void invalidEncodingNotConverted() {
        File source = new File(TESTDATA, "encodings");
        
//        try (FileOutputStream out = new FileOutputStream(new File(source, "invalid.txt"))) {
//            out.write("invalid UTF-8: ".getBytes(StandardCharsets.UTF_8));
//            out.write(new byte[] {(byte) 0xe9, 0x00});
//            out.write('\n');
//            out.write("invalid CP-1252: ".getBytes(StandardCharsets.UTF_8));
//            out.write(new byte[] {(byte) 0x81});
//            out.write('\n');
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        
        assertDoesNotThrow(() -> {
            File result;
            try (Preparator preparator = new Preparator()) {
                preparator.prepareDir(source);
                result = preparator.getResult();
                assertTrue(result.isDirectory());
                
                File copied = new File(result, "invalid.txt");
                
                try (FileInputStream fs = new FileInputStream(copied)) {
                    byte[] bytes = fs.readAllBytes();
                    
                    byte[] expected = {
                        'i', 'n', 'v', 'a', 'l', 'i', 'd', ' ', 'U', 'T', 'F', '-', '8', ':', ' ',
                        (byte) 0xe9, 0x00, '\n',
                        'i', 'n', 'v', 'a', 'l', 'i', 'd', ' ', 'C', 'P', '-', '1', '2', '5', '2', ':', ' ',
                        (byte) 0x81, '\n'
                    };
                    assertArrayEquals(expected, bytes);
                }
            }
        });
    }
    
    @Test
    public void checkEncoding() throws IOException {
        File encodingsDir = new File(TESTDATA, "encodings");
        File utf8 = new File(encodingsDir, "utf-8.txt");
        File cp1252 = new File(encodingsDir, "cp1252.txt");
        File iso88591 = new File(encodingsDir, "ISO-8859-1.txt");
        
        // utf-8.long.txt has a three-byte UTF-8 character at the 1024 byte mark
        // this covers the edge case, that the input buffer has remaining bytes after a decoding pass
        File utf8Long = new File(encodingsDir, "utf-8.long.txt");
        
        // invalid starts as a text file, but then has random bytes that fit neither UTF-8 nor CP-1252
        // there seems to be no way to create an invalid ISO 8859-1 file, though...
        File invalid = new File(encodingsDir, "invalid.txt");
        
        assertAll(
            () -> assertTrue(assertDoesNotThrow(
                () -> Preparator.checkEncoding(utf8.toPath(), StandardCharsets.UTF_8))),
            () -> assertTrue(assertDoesNotThrow(
                () -> Preparator.checkEncoding(cp1252.toPath(), Charset.forName("cp1252")))),
            () -> assertTrue(assertDoesNotThrow(
                () -> Preparator.checkEncoding(iso88591.toPath(), StandardCharsets.ISO_8859_1))),
            () -> assertTrue(assertDoesNotThrow(
                () -> Preparator.checkEncoding(utf8Long.toPath(), StandardCharsets.UTF_8))),
            
            () -> assertFalse(assertDoesNotThrow(
                () -> Preparator.checkEncoding(cp1252.toPath(), StandardCharsets.UTF_8))),
            () -> assertFalse(assertDoesNotThrow(
                () -> Preparator.checkEncoding(iso88591.toPath(), StandardCharsets.UTF_8))),
            
            () -> assertFalse(assertDoesNotThrow(
                () -> Preparator.checkEncoding(invalid.toPath(), StandardCharsets.UTF_8))),
//            () -> assertFalse(assertDoesNotThrow(
//                () -> Preparator.checkEncoding(invalid.toPath(), StandardCharsets.ISO_8859_1))),
            () -> assertFalse(
                assertDoesNotThrow(() -> Preparator.checkEncoding(invalid.toPath(), Charset.forName("cp1252"))))
        );
        
    }
    
    @Test
    public void createProjectAndClasspathFilesWithCorrectContent() {
        File source = new File(TESTDATA, "emptyDir");
        source.mkdir();
        assertTrue(source.isDirectory(), "precondition: empty test directory exists");
        
        assertDoesNotThrow(() -> {
            try (Preparator preparator = new Preparator()) {
                preparator.prepareDir(source);
                File result = preparator.getResult();

                File classpath = new File(result, ".classpath");
                File project = new File(result, ".project");
                
                assertAll(
                    () -> assertTrue(classpath.isFile()),
                    () -> assertTrue(project.isFile()),
                    () -> assertEquals(2, result.listFiles().length),
                    
                    () -> {
                        String content = Files.readString(classpath.toPath());
                        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                                + "<classpath>\n"
                                + "    <classpathentry kind=\"src\" path=\"\"/>\n"
                                + "    <classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER"
                                    + "/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-11\"/>\n"
                                + "    <classpathentry kind=\"output\" path=\"\"/>\n"
                                + "</classpath>\n", content);
                    },
                    () -> {
                        String content = Files.readString(project.toPath());
                        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                                + "<projectDescription>\n"
                                // replaces $projectName with the name of the folder
                                + "    <name>emptyDir</name>\n"
                                + "    <comment></comment>\n"
                                + "    <projects>\n"
                                + "    </projects>\n"
                                + "    <buildSpec>\n"
                                + "        <buildCommand>\n"
                                + "            <name>org.eclipse.jdt.core.javabuilder</name>\n"
                                + "            <arguments>\n"
                                + "            </arguments>\n"
                                + "        </buildCommand>\n"
                                + "    </buildSpec>\n"
                                + "    <natures>\n"
                                + "        <nature>org.eclipse.jdt.core.javanature</nature>\n"
                                + "    </natures>\n"
                                + "</projectDescription>\n", content);
                    }
                );
            }
        });
    }
    
    @Test
    public void doesNotOverrideExistingClasspathFile() {
        File source = new File(TESTDATA, "existingClasspath");
        
        assertDoesNotThrow(() -> {
            try (Preparator preparator = new Preparator()) {
                preparator.prepareDir(source);
                File result = preparator.getResult();

                // classpath should be copied
                File classpath = new File(result, ".classpath");
                // project is generated
                File project = new File(result, ".project");
                
                assertAll(
                    () -> assertTrue(classpath.isFile()),
                    () -> assertTrue(project.isFile()),
                    () -> assertEquals(2, result.listFiles().length),
                    
                    () -> {
                        String content = Files.readString(classpath.toPath());
                        assertEquals("existing classpath\n", content);
                    }
                );
            }
        });
    }
    
    @Test
    public void doesNotOverrideExistingProjectFile() {
        File source = new File(TESTDATA, "existingProject");
        
        assertDoesNotThrow(() -> {
            try (Preparator preparator = new Preparator()) {
                preparator.prepareDir(source);
                File result = preparator.getResult();
                
                // classpath is generated
                File classpath = new File(result, ".classpath");
                // project should be copied
                File project = new File(result, ".project");
                
                assertAll(
                    () -> assertTrue(classpath.isFile()),
                    () -> assertTrue(project.isFile()),
                    () -> assertEquals(2, result.listFiles().length),
                    
                    () -> {
                        String content = Files.readString(project.toPath());
                        assertEquals("existing project\n", content);
                    }
                );
            }
        });
    }
    
    @Test
    public void deletesOldFiles() {
        File source = new File(TESTDATA, "notEmptyDirWithSubDir");
        
        assertDoesNotThrow(() -> {
            try (Preparator preparator = new Preparator()) {
                preparator.prepareDir(source);
                preparator.deleteOldFiles();
                File result = preparator.getResult();
                assertTrue(result.list().length == 0);
                
            }
        });
        
        
    }
    @Test
    public void deletesOldFilesExceptSvn() {
        File source = new File(TESTDATA, "notEmptyDirWithSubDir");
        
        assertDoesNotThrow(() -> {
            try (Preparator preparator = new Preparator()) {
                preparator.prepareDir(source);
                File result = preparator.getResult();
                File svn = new File(result, ".svn");
                svn.mkdir();
                
                preparator.deleteOldFiles();
             
                assertTrue(result.list().length == 1);
                assertTrue(svn.exists());
                
            }
        });
        
        
    }
    @Test 
    public void copyInNotEmptyDir() {
        File source = new File(TESTDATA, "notEmptyDirWithSubDir");
        
        assertDoesNotThrow(() -> {
            try (Preparator preparator = new Preparator()) {
                File result = preparator.getResult();
                File svn = new File(result,".svn");
                result.mkdir();
                svn.mkdir();
           
                File subDir = new File(result, "notEmptyDir");
                File classpath = new File(result, ".classpath");
                File project = new File(result, ".project");
                
                preparator.prepareDir(source);
                
                assertAll(
                        () -> assertTrue(result.isDirectory()),
                        () -> assertTrue(subDir.isDirectory()),
                        () -> assertTrue(svn.isDirectory()),
                        
                        //.project and .classpath are generated
                        () -> assertTrue(classpath.exists()),
                        () -> assertTrue(project.exists()),
                        
                       
                        () -> assertEquals(4, result.listFiles().length),
                        
                       
                        () -> assertTrue(new File(subDir, "file.txt").isFile()),
                        () -> assertEquals(1, subDir.listFiles().length),
                        () -> assertEquals(0, svn.listFiles().length),
                        
                        // test that copied content is correct
                        () -> {
                            String fileContent = Files.readString((new File(subDir, "file.txt").toPath()));
                            assertEquals("This is a file.\n", fileContent);
                        }
                );
               
                
            }
        });
    }

}
