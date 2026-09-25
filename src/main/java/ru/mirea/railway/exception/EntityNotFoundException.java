package ru.mirea.railway.exception;

/**
 * Выбрасывается, когда запись по указанному идентификатору
 * не найдена в базе данных.
 */
public class EntityNotFoundException extends RuntimeException {

    private final String entityName;
    private final Object entityId;

    public EntityNotFoundException(String entityName, Object entityId) {
        super(String.format("%s с ID=%s не найден(а)", entityName, entityId));
        this.entityName = entityName;
        this.entityId = entityId;
    }

    public EntityNotFoundException(String message) {
        super(message);
        this.entityName = null;
        this.entityId = null;
    }

    public String getEntityName() {
        return entityName;
    }

    public Object getEntityId() {
        return entityId;
    }
}