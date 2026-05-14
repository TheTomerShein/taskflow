package il.openu.taskflow.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

import java.util.Optional;

public abstract class AbstractRepository<T> {

    protected Class<T> entityClass;

    @PersistenceContext(unitName = "TaskFlowPU")
    protected EntityManager em;

    protected AbstractRepository(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    public T save(T entity) {
        em.persist(entity);
        return entity;
    }

    public T update(T entity) {
        return em.merge(entity);
    }

    public Optional<T> findById(Long id) {
        return Optional.ofNullable(em.find(entityClass, id));
    }

    public List<T> findAll() {
        return em.createQuery("SELECT e FROM " + entityClass.getSimpleName() + " e", entityClass)
                .getResultList();
    }

    public void delete(T entity) {
        em.remove(em.merge(entity));
    }

    public void deleteById(Long id) {
        findById(id).ifPresent(this::delete);
    }
}