package il.openu.taskflow.service;

import il.openu.taskflow.entity.Board;
import il.openu.taskflow.entity.Project;
import il.openu.taskflow.entity.User;
import il.openu.taskflow.repository.BoardRepository;
import jakarta.ejb.Stateless;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

/**
 * Business logic for boards — create board with async activity logging via JTA-synchronized CDI events.
 */
@Stateless
public class BoardService {

    @Inject
    private BoardRepository boardRepository;

    @Inject
    private Event<ActivityEvent> eventPublisher;

    /**
     * Creates a new board inside a project and logs the action asynchronously via JMS after transaction commit.
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

        // Fire CDI event — will be processed after transaction commits successfully
        eventPublisher.fire(new ActivityEvent(
                "BOARD_CREATED",
                project.getId(),
                savedBoard.getId(),
                null,
                user.getId(),
                "Board created: " + name
        ));

        return savedBoard;
    }
}
