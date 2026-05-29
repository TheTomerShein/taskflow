package il.openu.taskflow.api;

import il.openu.taskflow.bean.AuthBean;
import il.openu.taskflow.entity.Board;
import il.openu.taskflow.entity.Project;
import il.openu.taskflow.entity.User;
import il.openu.taskflow.repository.ProjectRepository;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Set;

@Path("/projects")
public class ProjectResource {

    @Inject
    private ProjectRepository projectRepository;

    @Inject
    private AuthBean authBean;

    @GET
    @Path("/{id}/boards")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response getProjectBoards(@PathParam("id") Long id) {
        User currentUser = authBean.getCurrentUser();
        if (currentUser == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\": \"User not logged in\"}")
                    .build();
        }

        Project project = projectRepository.findById(id).orElse(null);
        if (project == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"Project not found\"}")
                    .build();
        }

        // Authorization check: User must be owner or member
        boolean isOwner = project.getOwner().getId().equals(currentUser.getId());
        boolean isMember = project.getMembers().stream()
                .anyMatch(member -> member.getId().equals(currentUser.getId()));

        if (!isOwner && !isMember) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("{\"error\": \"User is not authorized for this project\"}")
                    .build();
        }

        // Force initialization of lazy collections before returning to prevent LazyInit exceptions during JSON-B serialization
        Set<Board> boards = project.getBoards();
        for (Board board : boards) {
            board.getTasks().size(); // Initialize tasks
        }

        return Response.ok(boards).build();
    }
}
