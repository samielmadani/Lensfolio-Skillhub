package nz.ac.canterbury.seng302.portfolio.dto.user;

import java.util.List;

public class RemovedUserResponseDTO {

    public RemovedUserResponseDTO(List<Integer> altered) {
        this.altered = altered;
    }

    public RemovedUserResponseDTO() {}

    private List<Integer> altered;

    public List<Integer> getAltered() {
        return altered;
    }

    public void setAltered(List<Integer> altered) {
        this.altered = altered;
    }
}
