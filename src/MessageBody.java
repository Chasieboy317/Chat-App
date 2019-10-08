import java.io.Serializable;

public class MessageBody implements Serializable {

    String message;

    public byte[] getFilePacket() {
        return filePacket;
    }

    public void setFilePacket(byte[] filePacket) {
        this.filePacket = filePacket;
    }

    public int getStartByte() {
        return startByte;
    }

    public void setStartByte(int startByte) {
        this.startByte = startByte;
    }

    public int getSizeOfPacket() {
        return sizeOfPacket;
    }

    public void setSizeOfPacket(int sizeOfPacket) {
        this.sizeOfPacket = sizeOfPacket;
    }

    byte[] filePacket;
    int startByte;
    int sizeOfPacket;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public MessageBody(String message) {
        this.message = message;
    }

    public MessageBody(byte[] filePacket, int startByte, int sizeOfPacket){ this.filePacket = filePacket; this.startByte = startByte; this.sizeOfPacket=sizeOfPacket;};

}
