package il.openu.taskflow.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

import java.util.Optional;

/**
 * Base abstract repository providing common CRUD operations for JPA entities.
 *
 * @param <T> the entity type
 */
public abstract class AbstractRepository<T> {

    protected Class<T> entityClass;

    @PersistenceContext(unitName = "TaskFlowPU")
    protected EntityManager em;

    protected AbstractRepository(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    /**
     * Persists a new entity to the database.
     * Flushes the entity manager immediately to synchronize the persistence context.
     *
     * @param entity the entity to save
     * @return the saved entity
     */
    public T save(T entity) {
        em.persist(entity);
        em.flush();
        return entity;
    }

    /**
     * Updates an existing entity in the database.
     *
     * @param entity the entity to update
     * @return the managed entity instance
     */
    public T update(T entity) {
        return em.merge(entity);
    }

    /**
     * Finds an entity by its primary key.
     *
     * @param id the primary key
     * @return an Optional containing the entity if found, or empty otherwise
     */
    public Optional<T> findById(Long id) {
        return Optional.ofNullable(em.find(entityClass, id));
    }

    /**
     * Retrieves all instances of the entity type.
     *
     * @return a list of all entities
     */
    public List<T> findAll() {
        return em.createQuery("SELECT e FROM " + entityClass.getSimpleName() + " e", entityClass)
                .getResultList();
    }

    /**
     * Removes an entity from the database.
     * Merges the entity first to ensure it is managed in the current persistence context.
     *
     * @param entity the entity to delete
     */
    public void delete(T entity) {
        em.remove(em.merge(entity));
    }

    /**
     * Removes an entity by its primary key if it exists.
     *
     * @param id the primary key of the entity to delete
     */
    public void deleteById(Long id) {
        findById(id).ifPresent(this::delete);
    }
}