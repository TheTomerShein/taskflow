package il.openu.taskflow.service;

import il.openu.taskflow.entity.ActivityLog;
import il.openu.taskflow.entity.Board;
import il.openu.taskflow.entity.Project;
import il.openu.taskflow.entity.User;
import il.openu.taskflow.repository.ActivityLogRepository;
import il.openu.taskflow.repository.BoardRepository;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

/**
 * Business logic for boards - create board with activity logging.
 */
@Stateless
public class BoardService {

    @Inject
    private BoardRepository boardRepository;

    @Inject
    private ActivityLogRepository activityLogRepository;

    /**
     * Creates a new board inside a project and logs the action.
     *
     * @param name    the board name
     * @param project the parent project
     * @param user    the user who created the board
     * @return the saved Board entity
     */
    public Board createBoard(String name, Project project, User user) {
        Board board = new Board();
        board.setName(name);
        board.setProject(project);

        Board savedBoard = boardRepository.save(board);

        // Log board creation
        ActivityLog log = new ActivityLog(project, savedBoard, user,
                ActivityLog.ActionType.BOARD_CREATED, "Board created: " + name);
        activityLogRepository.save(log);

        return savedBoard;
    }
}
