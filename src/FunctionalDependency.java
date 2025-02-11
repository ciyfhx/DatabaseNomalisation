import java.util.*;

public class FunctionalDependency {
    private Set<String> left;
    private Set<String> right;

    public FunctionalDependency(Set<String> left, Set<String> right) {
        this.left = left;
        this.right = right;
    }

    public Set<String> getLeft() {
        return left;
    }

    public Set<String> getRight() {
        return right;
    }

    @Override
    public String toString() {
        return left + " -> " + right;
    }

    // Parse "A->B,A->C,A->D" into a list of FunctionalDependency objects
    public static List<FunctionalDependency> parseFDs(String fdsString) {
        List<FunctionalDependency> fdList = new ArrayList<>();

        String[] fdArray = fdsString.split("\\s*,\\s*");
        for (String fd : fdArray) {
            String[] sides = fd.split("->");
            String[] leftSide = sides[0].trim().split("");
            String[] rightSide = sides[1].trim().split("");

            Set<String> leftSet = new HashSet<>(Arrays.stream(leftSide).toList());
            Set<String> rightSet = new HashSet<>(Arrays.stream(rightSide).toList());

            fdList.add(new FunctionalDependency(leftSet, rightSet));
        }

        return fdList;
    }

    public boolean isTrivialFunctionalDependency(FunctionalDependency fd) {
        return fd.getLeft().containsAll(fd.getRight());
    }

}
