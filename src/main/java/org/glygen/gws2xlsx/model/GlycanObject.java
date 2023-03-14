package org.glygen.gws2xlsx.model;

public class GlycanObject {
    Integer rowNumber;
    RegistrationStatus status = RegistrationStatus.NONE;
    String gwsSequence;
    String glycoCT;
    String wurcs;
    String error;
    byte[] cartoon;
    String glytoucanID;
    String glytoucanHash;
    
    /**
     * @return the status
     */
    public RegistrationStatus getStatus() {
        return status;
    }
    /**
     * @param status the status to set
     */
    public void setStatus(RegistrationStatus status) {
        this.status = status;
    }
    /**
     * @return the gwsSequence
     */
    public String getGwsSequence() {
        return gwsSequence;
    }
    /**
     * @param gwsSequence the gwsSequence to set
     */
    public void setGwsSequence(String gwsSequence) {
        this.gwsSequence = gwsSequence;
    }
    /**
     * @return the glycoCT
     */
    public String getGlycoCT() {
        return glycoCT;
    }
    /**
     * @param glycoCT the glycoCT to set
     */
    public void setGlycoCT(String glycoCT) {
        this.glycoCT = glycoCT;
    }
    /**
     * @return the wurcs
     */
    public String getWurcs() {
        return wurcs;
    }
    /**
     * @param wurcs the wurcs to set
     */
    public void setWurcs(String wurcs) {
        this.wurcs = wurcs;
    }
    /**
     * @return the error
     */
    public String getError() {
        return error;
    }
    /**
     * @param error the error to set
     */
    public void setError(String error) {
        this.error = error;
    }
    /**
     * @return the rowNumber
     */
    public Integer getRowNumber() {
        return rowNumber;
    }
    /**
     * @param rowNumber the rowNumber to set
     */
    public void setRowNumber(Integer rowNumber) {
        this.rowNumber = rowNumber;
    }
    /**
     * @return the cartoon
     */
    public byte[] getCartoon() {
        return cartoon;
    }
    /**
     * @param cartoon the cartoon to set
     */
    public void setCartoon(byte[] cartoon) {
        this.cartoon = cartoon;
    }
    /**
     * @return the glytoucanID
     */
    public String getGlytoucanID() {
        return glytoucanID;
    }
    /**
     * @param glytoucanID the glytoucanID to set
     */
    public void setGlytoucanID(String glytoucanID) {
        this.glytoucanID = glytoucanID;
    }
    /**
     * @return the glytoucanHash
     */
    public String getGlytoucanHash() {
        return glytoucanHash;
    }
    /**
     * @param glytoucanHash the glytoucanHash to set
     */
    public void setGlytoucanHash(String glytoucanHash) {
        this.glytoucanHash = glytoucanHash;
    }

}
