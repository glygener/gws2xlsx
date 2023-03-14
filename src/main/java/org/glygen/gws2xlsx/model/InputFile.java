package org.glygen.gws2xlsx.model;

import java.util.List;

public class InputFile {
    
    String filename;
    List<GlycanObject> glycans;

    /**
     * @return the glycans
     */
    public List<GlycanObject> getGlycans() {
        return glycans;
    }

    /**
     * @param glycans the glycans to set
     */
    public void setGlycans(List<GlycanObject> glycans) {
        this.glycans = glycans;
    }

    /**
     * @return the filename
     */
    public String getFilename() {
        return filename;
    }

    /**
     * @param filename the filename to set
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }

}
