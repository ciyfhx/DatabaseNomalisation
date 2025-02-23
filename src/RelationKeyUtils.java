import java.util.*;
import java.util.stream.Collectors;

public class RelationKeyUtils {
    public static void main(String[] args) {
        String relationStr = "R(A,B,C,D)";
        String fdsStr = "AB->C,AD->B,B->D";

        // Parse the relation
        Relation relation = Relation.parseRelation(relationStr);
        System.out.println("Parsed Relation: " + relation);

        // Parse the functional dependencies
        List<FunctionalDependency> fdList = FunctionalDependency.parseFDs(fdsStr);
        System.out.println("Parsed FDs:");
        for (FunctionalDependency fd : fdList) {
            System.out.println("  " + fd);
        }
//
//        String fdsStr_ = "A->B,B->C,C->D,D->E";
//        Set<String> result = getClosureFromAttributes(Set.of("E"), FunctionalDependency.parseFDs(fdsStr_));
//        System.out.println(result);

        System.out.println("Candidate Keys: " + getCandidateKeys(relation, fdList));

    }


    public static List<Set<String>> getCandidateKeys(Relation relation, List<FunctionalDependency> fds){
        List<Set<String>> superKeys = getSuperKeys(relation, fds);
        return superKeys.stream().filter(superKey -> isCandidateKey(relation, fds, superKey))
                .toList();
    }

    /**
     * Check if a set of attributes is a Candidate Key.
     * For a set of attributes: 'X' to be a Candidate Key, all proper subset of 'X' must *NOT* be a Super Key.
     * @param relation
     * @param fds
     * @return
     */
    public static boolean isCandidateKey(Relation relation, List<FunctionalDependency> fds, Set<String> attributes){
        List<Set<String>> subsets = getCombinations(attributes)
                .stream()
                .filter(combination -> combination.size() == attributes.size() -1)  //We only need to check subset that is one size smaller
                .toList();
        return subsets.stream().noneMatch(subset -> isSuperKey(relation, fds, subset));
    }

    public static List<Set<String>> getSuperKeys(Relation relation, List<FunctionalDependency> fds) {
        Set<String> attributes = relation.getAttributes();
        List<Set<String>> superkeys = new ArrayList<>();

        //Get all possible combinations
        List<Set<String>> combinations = getCombinations(attributes);

        for(Set<String> combination : combinations) {
            if(isSuperKey(relation, fds, combination)) superkeys.add(combination);
        }

        superkeys.sort(Comparator.comparingInt(Set::size));

        return superkeys;
    }

    /**
     * Check if a set of attributes is a Super Key.
     * For a set of attributes: 'X' to be a Super Key, the closure of 'X' must contain all attributes in the Relation.
     * @param attributes
     * @param fds
     * @return
     */
    public static boolean isSuperKey(Relation relation, List<FunctionalDependency> fds, Set<String> attributes) {
        Set<String> closure = getClosureFromAttributes(attributes, fds).getClosure();
        return closure.containsAll(relation.getAttributes());
    }

    public static ClosureResult getClosureFromAttributes(Set<String> attributes, List<FunctionalDependency> fds) {
        Set<String> closure = new HashSet<String>(attributes);
        int depth = 0;

        boolean changed = true;
        while (changed) {
            changed = false;
            depth++;

            // Try each FD: if FD.left ⊆ closure, then add FD.right to closure
            for (FunctionalDependency fd : fds) {
                // Example of fd.left being a Set<String>
                // We check if closure contains all of them
                if (closure.containsAll(fd.getLeft())) {
                    // If the left side is contained in closure, we can add the right side
                    // If we successfully add *new* attributes, mark changed = true
                    if (closure.addAll(fd.getRight())) {
                        changed = true;
                    }
                }
            }
        }
        return new ClosureResult(closure, depth);
    }

    /**
     * Returns all subsets (the power set) of the given set (without empty set).
     *
     * @param input a Set of elements
     * @param <T>   type of elements
     * @return a List of Sets, where each Set is a subset of 'input'
     */
    public static <T> List<Set<T>> getCombinations(Set<T> input) {
        // Convert to a list so we can refer to elements by index
        List<T> elements = new ArrayList<>(input);
        List<Set<T>> allSubsets = new ArrayList<>();

        // There are 2^n subsets for n elements
        int n = elements.size();
        int totalSubsets = 1 << n;  // 2^n

        // For each number from 0 to 2^n - 1,
        // use the binary form to decide which elements go into the subset
        for (int mask = 1; mask < totalSubsets; mask++) {
            Set<T> subset = new HashSet<>();
            for (int i = 0; i < n; i++) {
                // Check if the i-th bit of 'mask' is set
                if ((mask & (1 << i)) != 0) {
                    subset.add(elements.get(i));
                }
            }
            allSubsets.add(subset);
        }
        allSubsets.sort(Comparator.comparingInt(Set::size));
        return allSubsets;
    }

}