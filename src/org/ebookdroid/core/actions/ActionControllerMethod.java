package org.ebookdroid.core.actions;

import org.ebookdroid.core.log.LogContext;
import org.ebookdroid.core.utils.AndroidVersion;
import org.ebookdroid.utils.LengthUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActionControllerMethod {

    private static final LogContext LCTX = LogContext.ROOT.lctx("Actions");

    private static HashMap<Class<?>, Map<Integer, Method>> s_methods = new HashMap<Class<?>, Map<Integer, Method>>();

    private final IActionController<?> m_controller;

    private final ActionEx m_action;

    private Object m_target;

    private Method m_method;

    private Throwable m_errorInfo;

    /**
     * Constructor
     * 
     * @param controller
     *            action controller
     * @param actionId
     *            action id
     */
    ActionControllerMethod(final IActionController<?> controller, final ActionEx action) {
        m_controller = controller;
        m_action = action;
    }

    /**
     * Invokes controller method for the given controller and action
     * 
     * @param action
     *            action
     * @return execution result
     * @throws Throwable
     *             thrown by reflection API or executed method
     */
    public Object invoke(final ActionEx action) throws Throwable {
        final Method m = getMethod();
        if (m != null) {
            return m.invoke(m_target, action);
        } else {
            throw m_errorInfo;
        }
    }

    /**
     * @return <code>true</code> if method is exist
     */
    public boolean isValid() {
        return null != getMethod();
    }

    /**
     * Returns reflection error info.
     * 
     * @return {@link Throwable}
     */
    public Throwable getErrorInfo() {
        getMethod();
        return m_errorInfo;
    }

    /**
     * @return {@link Method}
     */
    Method getMethod() {
        if (m_method == null && m_errorInfo == null) {
            List<String> classes = new ArrayList<String>();

            for (IActionController<?> c = m_controller; m_method == null && c != null; c = c.getParent()) {
                classes.add(c.getManagedComponent().getClass().getSimpleName());
                classes.add(c.getClass().getSimpleName());

                m_method = getMethod(c.getManagedComponent(), m_action.id);
                m_target = m_method != null ? c.getManagedComponent() : null;
                if (m_method == null) {
                    m_method = getMethod(c, m_action.id);
                    m_target = m_method != null ? c : null;
                }
            }

            if (m_method == null) {
                String text = "No appropriate method found for action " + m_action.name + " in the following classes: "
                        + classes;
                m_errorInfo = new NoSuchMethodException(text);
            } else {
                LCTX.d("Action method found for " + m_action.name + ": " + m_method);
            }
        }
        return m_method;
    }

    /**
     * Gets the method.
     * 
     * @param target
     *            a possible action target
     * @param actionId
     *            the action id
     * 
     * @return the method
     */
    private static synchronized Method getMethod(final Object target, final int actionId) {
        Class<? extends Object> clazz = target.getClass();

        Map<Integer, Method> methods = s_methods.get(clazz);
        if (methods == null) {
            methods = getActionMethods(clazz);
            s_methods.put(clazz, methods);
        }
        return methods.get(actionId);
    }

    /**
     * Gets the method.
     * 
     * @param clazz
     *            an action target class
     * 
     * @return the map of action methods method
     */
    private static Map<Integer, Method> getActionMethods(final Class<?> clazz) {
        final Map<Integer, Method> result = new HashMap<Integer, Method>();

        if (AndroidVersion.is1x) {
            if (clazz.isAnnotationPresent(ActionTarget.class)) {
                ActionTarget a = clazz.getAnnotation(ActionTarget.class);
                for(ActionMethodDef def : a.actions()) {
                    try {
                        Method m = clazz.getMethod(def.method(), ActionEx.class);
                        result.put(def.id(), m);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        } else {
            final Method[] methods = clazz.getMethods();
            for (final Method method : methods) {
                final int modifiers = method.getModifiers();
                if (Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers)) {
                    final Class<?>[] args = method.getParameterTypes();
                    if (LengthUtils.length(args) == 1 && ActionEx.class.equals(args[0])) {
                        if (method.isAnnotationPresent(ActionMethod.class)) {
                            final ActionMethod annotation = method.getAnnotation(ActionMethod.class);
                            if (annotation != null) {
                                for (int id : annotation.ids()) {
                                    result.put(id, method);
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    @Override
    public String toString() {
        Method m = getMethod();
        return (m != null ? "" + m : "no method: " + m_errorInfo.getMessage());
    }
}
