package net.ssehub.teaching.exercise_submitter.lib.submission;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

class Preparator implements Closeable{
    
    private File result;

    public Preparator(File directory) throws IOException {
        
        if(!directory.isDirectory()) {
            throw new IOException(directory.getName() + " is not a directory");
            
        }
        
        result = File.createTempFile("exercise_submission", null);
        result.delete();
        result.mkdir();
        result.deleteOnExit();
        
       
      
    }
    
    public File getResult() {
        return result;
    }

    @Override
    public void close() throws IOException {
        result.delete();
        
    }
    
}
