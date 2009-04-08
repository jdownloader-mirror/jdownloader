package jd.event;

import java.util.EventListener;
import java.util.Vector;

public abstract class JDBroadcaster<T extends EventListener,TT extends JDEvent> {

    transient private Vector<T> callList = null;

    transient private Vector<T> removeList = null;

    public JDBroadcaster() {
        callList = new Vector<T>();
        removeList = new Vector<T>();
    }

    public void addListener(T listener) {
        synchronized (callList) {
            synchronized (removeList) {
                if (removeList.contains(listener)) removeList.remove(listener);
            }
            if (!callList.contains(listener)) callList.add(listener);
        }
    }

    public boolean hasListener() {
        return callList.size() > 0;
    }

    public void fireEvent(TT event) {
        synchronized (callList) {
            for (int i = callList.size() - 1; i >= 0; i--) {
                this.fireEvent(callList.get(i), event);
            }
            synchronized (removeList) {
                callList.removeAll(removeList);
                removeList.clear();
            }
        }
    }

    protected abstract void fireEvent(T listener, TT event);

    public void removeListener(T listener) {
        synchronized (removeList) {
            if (!removeList.contains(listener)) removeList.add(listener);
        }
    }
}
