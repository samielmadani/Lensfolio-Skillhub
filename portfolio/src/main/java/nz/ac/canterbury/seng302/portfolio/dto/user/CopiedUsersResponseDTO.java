package nz.ac.canterbury.seng302.portfolio.dto.user;

import java.util.List;

public class CopiedUsersResponseDTO {

    public CopiedUsersResponseDTO(List<Integer> copied, int noGroupId) {
        this.copied = copied;
        this.noGroupId = noGroupId;
    }

    @Override
    public String toString() {
        return "CopiedUsersResponseDTO{" +
                "copied=" + copied +
                ", noGroupId=" + noGroupId +
                '}';
    }

    public CopiedUsersResponseDTO() {}

    private List<Integer> copied;

    private int noGroupId;

    public List<Integer> getCopied() {
        return copied;
    }

    public void setCopied(List<Integer> copied) {
        this.copied = copied;
    }

    public int getNoGroupId() {
        return noGroupId;
    }

    public void setNoGroupId(int noGroupId) {
        this.noGroupId = noGroupId;
    }
}
