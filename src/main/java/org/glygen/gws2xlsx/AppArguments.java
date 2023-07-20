package org.glygen.gws2xlsx;

public class AppArguments {
    
    String gwsFile;
    String inputFolder;
    String outputFolder;
    String jobFile;
    Boolean cartoonGeneration;
    Boolean glytoucanGeneration;
    Boolean debug;
    Boolean rerun;
    /**
     * @return the gwsFile
     */
    public String getGwsFile() {
        return gwsFile;
    }
    /**
     * @param gwsFile the gwsFile to set
     */
    public void setGwsFile(String gwsFile) {
        this.gwsFile = gwsFile;
    }
    /**
     * @return the inputFolder
     */
    public String getInputFolder() {
        return inputFolder;
    }
    /**
     * @param inputFolder the inputFolder to set
     */
    public void setInputFolder(String inputFolder) {
        this.inputFolder = inputFolder;
    }
    /**
     * @return the outputFolder
     */
    public String getOutputFolder() {
        return outputFolder;
    }
    /**
     * @param outputFolder the outputFolder to set
     */
    public void setOutputFolder(String outputFolder) {
        this.outputFolder = outputFolder;
    }
    /**
     * @return the jobFile
     */
    public String getJobFile() {
        return jobFile;
    }
    /**
     * @param jobFile the jobFile to set
     */
    public void setJobFile(String jobFile) {
        this.jobFile = jobFile;
    }
    /**
     * @return the cartoonGeneration
     */
    public Boolean getCartoonGeneration() {
        return cartoonGeneration;
    }
    /**
     * @param cartoonGeneration the cartoonGeneration to set
     */
    public void setCartoonGeneration(Boolean cartoonGeneration) {
        this.cartoonGeneration = cartoonGeneration;
    }
    /**
     * @return the glytoucanGeneration
     */
    public Boolean getGlytoucanGeneration() {
        return glytoucanGeneration;
    }
    /**
     * @param glytoucanGeneration the glytoucanGeneration to set
     */
    public void setGlytoucanGeneration(Boolean glytoucanGeneration) {
        this.glytoucanGeneration = glytoucanGeneration;
    }
    /**
     * @return the debug
     */
    public Boolean getDebug() {
        return debug;
    }
    /**
     * @param debug the debug to set
     */
    public void setDebug(Boolean debug) {
        this.debug = debug;
    }
   
    /**
     * 
     * @param rerun
     */
    public void setRerun(boolean rerun) {
       this.rerun = rerun;
        
    }
    
    /**
     * 
     * @return the rerun flag
     */
    public Boolean getRerun() {
        return rerun;
    }   
}
