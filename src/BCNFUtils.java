import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BCNFUtils {

    public static void main(String[] args) {
//        String relationStr = "R(A,B,C,D,E,F)";
//        String fdsStr = "B->D,C";
        String relationStr = "R(A,B,C,D,E)";
        String fdsStr = "A->B,A->C,BC->A,D->E";

        // Parse the relation
        Relation relation = Relation.parseRelation(relationStr);
        System.out.println("Parsed Relation: " + relation);

        // Parse the functional dependencies
        List<FunctionalDependency> fdList = FunctionalDependency.parseFDs(fdsStr);
        System.out.println("Parsed FDs:");
        for (FunctionalDependency fd : fdList) {
            System.out.println("  " + fd);
        }

        System.out.println("Super Keys: " + RelationKeyUtils.getSuperKeys(relation, fdList));
        System.out.println("Candidate Keys: " + RelationKeyUtils.getCandidateKeys(relation, fdList));
        System.out.println("BCNF: " + isInBCNF(relation, fdList));

        List<Relation> decomposeBCNFRelations = decomposeRelationIntoBCNF(relation, fdList);
        System.out.println("Decomposed BCNF: " + decomposeBCNFRelations);

    }

    public static List<Relation> decomposeRelationIntoBCNF(Relation relation, List<FunctionalDependency> fds) {
        //Infer all possible FDs not included 'fds' given
        List<FunctionalDependency> allFDs = inferAllFunctionalDependencies(relation, fds);
        return recDecomposeRelationIntoBCNF(relation, fds, allFDs);
    }
    public static List<Relation> recDecomposeRelationIntoBCNF(Relation relation, List<FunctionalDependency> fds, List<FunctionalDependency> allFDs) {
        // 1) If relation is already in BCNF, then immediately return
        if (isInBCNF(relation, fds)) {
            return List.of(relation);
        }

        // 2) Find violating FDs
        List<FunctionalDependency> violatingFDs = getViolatingBCNFFunctionalDependencies(relation, fds);

        // If we don't find any violating FD, we're done
        // (But typically, isInBCNF() = false => we must have at least one violating FD)
        if (violatingFDs.isEmpty()) {
            return List.of(relation);
        }

        // We'll pick the FIRST violating FD.
        FunctionalDependency fd = violatingFDs.getFirst();

        // Let X = fd.left, Y = fd.right
        // 3) Decompose:
        Set<String> originalAttrs = new HashSet<>(relation.getAttributes());
        Set<String> X = fd.getLeft();
        Set<String> r1Attrs = RelationKeyUtils.getClosureFromAttributes(X, fds).getClosure();
        // R2 = (R \ r1Attrs) ∪ X
        Set<String> r2Attrs = new HashSet<>(originalAttrs);
        r2Attrs.removeAll(r1Attrs);
        r2Attrs.addAll(X);


        // Create new relation objects
        Relation r1 = new Relation(
                relation.getName() + "_1",
                r1Attrs  // or keep them sorted if you like
        );
        Relation r2 = new Relation(
                relation.getName() + "_2",
                r2Attrs
        );


        // 4) Project FDs onto R1 and R2
        List<FunctionalDependency> fdsForR1 = projectFDs(r1, allFDs);
        List<FunctionalDependency> fdsForR2 = projectFDs(r2, allFDs);

        // 5) Recursively decompose each sub-relation
        List<Relation> result = new ArrayList<>();
        result.addAll(recDecomposeRelationIntoBCNF(r1, fdsForR1, allFDs));
        result.addAll(recDecomposeRelationIntoBCNF(r2, fdsForR2, allFDs));

        return result;
    }

    public static List<FunctionalDependency> inferAllFunctionalDependencies(Relation relation, List<FunctionalDependency> fds){
        List<Set<String>> combinations = RelationKeyUtils.getCombinations(relation.getAttributes());
        List<FunctionalDependency> allFds = new ArrayList<>();
        for(Set<String> combination : combinations) {
            Set<String> closure = RelationKeyUtils.getClosureFromAttributes(combination, fds).getClosure();
            FunctionalDependency fd = new FunctionalDependency(combination, closure);
            allFds.add(fd);
        }
        return allFds;
    }

    /**
     * Returns all FDs from 'fds' that involve ONLY attributes in 'relation'.
     * i.e., both left side and right side must be a subset of relation's attributes.
     */
    public static List<FunctionalDependency> projectFDs(Relation relation, List<FunctionalDependency> fds) {
        Set<String> relAttrs = new HashSet<>(relation.getAttributes());
        List<FunctionalDependency> result = new ArrayList<>();

        for (FunctionalDependency fd : fds) {
            // Filter out FDs that reference attributes not in 'relAttrs'
            if (relAttrs.containsAll(fd.getLeft())) {
                FunctionalDependency projectFD = new FunctionalDependency(new HashSet<>(fd.getLeft()), new HashSet<>(fd.getRight()));
                projectFD.getRight().retainAll(relAttrs);
                result.add(projectFD);
            }
        }
        return result;
    }

    public static List<FunctionalDependency> getViolatingBCNFFunctionalDependencies(Relation relation, List<FunctionalDependency> fds) {
        List<Set<String>> superKeys = RelationKeyUtils.getSuperKeys(relation, fds);
        List<FunctionalDependency> nonTrivialFds = fds.stream().filter(fd -> !fd.isTrivialFunctionalDependency(fd)).toList();
        List<FunctionalDependency> violatingFDs = new ArrayList<>();

        for (FunctionalDependency fd : nonTrivialFds) {
            boolean isKey = superKeys.stream()
                    .anyMatch(key -> key.equals(fd.getLeft()));
            if (!isKey) {
                // Found a non-trivial FD whose left side isn't a key => Not in BCNF
                violatingFDs.add(fd);
            }
        }

        return violatingFDs;
    }

    public static boolean isInBCNF(Relation relation, List<FunctionalDependency> fds) {
        List<Set<String>> superKeys = RelationKeyUtils.getSuperKeys(relation, fds);
        List<FunctionalDependency> nonTrivialFds = fds.stream().filter(fd -> !fd.isTrivialFunctionalDependency(fd)).toList();

        for (FunctionalDependency fd : nonTrivialFds) {
            boolean isKey = superKeys.stream()
                    .anyMatch(key -> key.equals(fd.getLeft()));
            if (!isKey) {
                // Found a non-trivial FD whose left side isn't a key => Not in BCNF
                return false;
            }
        }

        return true;
    }


}
