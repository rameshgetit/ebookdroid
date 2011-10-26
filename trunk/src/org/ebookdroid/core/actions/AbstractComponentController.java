package org.ebookdroid.core.actions;

import org.ebookdroid.core.log.LogContext;

import android.app.Activity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This class defines base features for action controller.
 *
 * @param <ManagedComponent>
 *            manager GUI component class
 */
public abstract class AbstractComponentController<ManagedComponent> implements IActionController<ManagedComponent> {

    protected static final LogContext LCTX = LogContext.ROOT.lctx("Actions");

    private final Map<Integer, ActionEx> m_actions = new LinkedHashMap<Integer, ActionEx>();

    private final ReentrantReadWriteLock m_actionsLock = new ReentrantReadWriteLock();

    private final ManagedComponent m_managedComponent;

    private final IActionController<?> m_parent;

    private final ActionDispatcher m_dispatcher;

    /**
     * Constructor
     *
     * @param managedComponent
     *            managed component
     */
    protected AbstractComponentController(final Activity base, final ManagedComponent managedComponent) {
        this(base, null, managedComponent);
    }

    /**
     * Constructor.
     *
     * @param parent
     *            the parent controller
     * @param managedComponent
     *            managed component
     */
    protected AbstractComponentController(final Activity base, final IActionController<?> parent,
            final ManagedComponent managedComponent) {
        m_parent = parent;
        m_managedComponent = managedComponent;
        m_dispatcher = new ActionDispatcher(base, this);
    }

    /**
     * @return the parent controller
     * @see IActionController#getParent()
     */
    public IActionController<?> getParent() {
        return m_parent;
    }

    /**
     * @return the managed component
     * @see IActionController#getManagedComponent()
     */
    public ManagedComponent getManagedComponent() {
        return m_managedComponent;
    }

    /**
     * @return the action dispatcher
     * @see IActionController#getDispatcher()
     */
    public ActionDispatcher getDispatcher() {
        return m_dispatcher;
    }

    /**
     * Searches for an action by the given id
     *
     * @param id
     *            action id
     * @return an instance of {@link ActionEx} object or <code>null</code>
     * @see IActionController#getAction(java.lang.String)
     */
    public ActionEx getAction(final int id) {
        m_actionsLock.readLock().lock();
        try {
            ActionEx actionEx = m_actions.get(id);
            if (actionEx == null && m_parent != null) {
                actionEx = m_parent.getAction(id);
            }
            return actionEx;
        } finally {
            m_actionsLock.readLock().unlock();
        }
    }

    /**
     * Creates and register a global action
     *
     * @param id
     *            action id
     * @return an instance of {@link ActionEx} object
     * @see IActionController#getOrCreateAction(String, String, IActionParameter[])
     */
    public ActionEx getOrCreateAction(final int id) {
        ActionEx result = null;
        m_actionsLock.writeLock().lock();
        try {
            result = getAction(id);
            if (result == null) {
                result = createAction(id);
                try {
                    ActionInitControllerMethod.getInstance().invoke(this, result);
                } catch (Throwable e) {
                    LCTX.e("Action " + id + "initialization failed: ", e);
                }

                m_actions.put(result.getId(), result);
            }
        } finally {
            m_actionsLock.writeLock().unlock();
        }

        return result;
    }

    /**
     * Creates an action
     *
     * @param id
     *            action id
     * @param parameters
     *            action parameters
     * @return an instance of {@link ActionEx} object
     * @see IActionController#createAction(String, String, IActionParameter[])
     */
    public ActionEx createAction(final int id, final IActionParameter... parameters) {
        ActionEx result = new ActionEx(this, id);

        result.putValue(MANAGED_COMPONENT_PROPERTY, getManagedComponent());
        result.putValue(COMPONENT_CONTROLLER_PROPERTY, this);

        for (IActionParameter actionParameter : parameters) {
            result.addParameter(actionParameter);
        }

        try {
            ActionInitControllerMethod.getInstance().invoke(this, result);
        } catch (Throwable e) {
            LCTX.e("Action " + id + "initialization failed: ", e);
        }

        m_actionsLock.writeLock().lock();
        try {
            m_actions.put(result.getId(), result);
        } finally {
            m_actionsLock.writeLock().unlock();
        }

        return result;
    }
}
