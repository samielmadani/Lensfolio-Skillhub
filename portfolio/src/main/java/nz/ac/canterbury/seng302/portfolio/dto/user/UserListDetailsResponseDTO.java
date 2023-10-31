package nz.ac.canterbury.seng302.portfolio.dto.user;

public class UserListDetailsResponseDTO {
    private int totalPages;
    private int currentPage;

    public UserListDetailsResponseDTO () {}

    public UserListDetailsResponseDTO(int totalPages, int currentPage) {
        this.totalPages = totalPages;
        this.currentPage = currentPage;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }
}
