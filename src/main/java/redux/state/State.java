package redux.state;

import redux.store.RequestForChange;

import java.util.Objects;

public abstract class State<A> {
    private A action;
    private String id;

    public State(RequestForChange<A> requestForChange) {
        this.action = requestForChange.getAction();
        this.id = requestForChange.getId();
    }

    public abstract boolean fromAction(A action);

    public A getAction() {
        return action;
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof State)) return false;
        State<?> state = (State<?>) o;
        return Objects.equals(getId(), state.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public String toString() {
        return "State{" +
                "action=" + action +
                ", id='" + id + '\'' +
                '}';
    }
}
