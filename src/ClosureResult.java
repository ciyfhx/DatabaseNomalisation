import java.util.Set;

public class ClosureResult {
    private final Set<String> closure;
    private final Integer depth;

    public ClosureResult(Set<String> closure, Integer depth) {
        this.closure = closure;
        this.depth = depth;
    }

    public Set<String> getClosure() {
        return closure;
    }

    public Integer getDepth() {
        return depth;
    }

    @Override
    public String toString() {
        return "(" + closure + ", " + depth + ")";
    }
}
