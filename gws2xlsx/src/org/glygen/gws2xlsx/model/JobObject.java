package org.glygen.gws2xlsx.model;

import java.util.ArrayList;
import java.util.List;

public class JobObject {
    
    List<InputFile> files;

    /**
     * @return the files
     */
    public List<InputFile> getFiles() {
        return files;
    }

    /**
     * @param files the files to set
     */
    public void setFiles(List<InputFile> files) {
        this.files = files;
    }
    
    public void addFile(InputFile file) {
        if (files == null) {
            files = new ArrayList<InputFile>();
        }
        files.add(file);
    }
    
}
