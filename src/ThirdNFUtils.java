import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;

public class ThirdNFUtils {

    public static void main(String[] args) {
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
        System.out.println("3NF: " + isIn3NF(relation, fdList));
        System.out.println("Minimal Basis: " + getMinimalBasis(fdList));
        System.out.println("Decomposed 3NF: " + decomposeRelationInto3NF(relation, fdList));
    }

    public static List<Relation> decomposeRelationInto3NF(Relation relation, List<FunctionalDependency> fds) {
        //Step 1: Derive a minimal basis of FDs
        List<FunctionalDependency> minimalBasis = getMinimalBasis(fds);

        //Step 2: In the minimal basis, combine the FDs whose left hand sides are the same
        List<FunctionalDependency> combineLhsFDs = minimalBasis.stream()
                .collect(
                        groupingBy(FunctionalDependency::getLeft,
                        flatMapping(fd -> fd.getRight().stream(), toSet()))
                ).entrySet() //Convert back to List<FunctionalDependency> from Map<Set<String>, Set<String>>
                .stream().map(entry -> new FunctionalDependency(entry.getKey(), entry.getValue()))
                .toList();

        //Step 3: Create a table for each FD remained
        List<Relation> relations = new ArrayList<>();

        int i = 1;
        for (FunctionalDependency fd : combineLhsFDs) {
            relations.add(new Relation("R_"+i++, Stream.of(fd.getLeft(), fd.getRight())
                    .flatMap(Set::stream)
                    .collect(toSet())));
        }

        //Step 4: If none of the tables contains a key of the original table R, create a table that contains a key of R
        List<Set<String>> candidateKeys = RelationKeyUtils.getCandidateKeys(relation, minimalBasis);
        boolean hasKey = relations.stream().anyMatch(
                rel -> candidateKeys.stream().anyMatch(key -> rel.getAttributes().containsAll(key))
        );

        // If none of the tables includes at least one candidate key, we add a new table containing one
        if (!hasKey && !candidateKeys.isEmpty()) {
            // e.g. pick the first candidate key, or pick any if multiple
            Set<String> firstKey = candidateKeys.getFirst();
            relations.add(new Relation("R_" + i++, firstKey));
        }

        //Step 5: Remove redundant tables
        // A table is redundant if it is a duplicate or its attribute set is a subset of another table's attribute set

        //Remove duplicate tables
        relations = relations.stream()
                .filter(distinctByKey(Relation::getAttributes))
                .toList();

        //Remove table that is a subset of another table
        List<Relation> finalRelations = new ArrayList<>(relations);
        finalRelations.removeIf(r ->
                finalRelations.stream().anyMatch(o ->
                        o != r && o.getAttributes().containsAll(r.getAttributes())
                )
        );

        return relations;

    }

    public static List<FunctionalDependency> getMinimalBasis(List<FunctionalDependency> fds){
        //Step 1: Transform the FDs, so that each right hand side contains only one attribute
        List<FunctionalDependency> step1Transformation = new ArrayList<>();

        for (FunctionalDependency fd : fds) {
            fd.getRight().forEach(a ->
                    step1Transformation.add(new FunctionalDependency(fd.getLeft(), Set.of(a)))
            );
        }

        //Step 2: Remove redundant FDs
        List<FunctionalDependency> step2Transformation = new ArrayList<>(step1Transformation);
        for (FunctionalDependency fd : step1Transformation) {
            step2Transformation.remove(fd);

            //Compute new closure
            Set<String> closure = RelationKeyUtils.getClosureFromAttributes(fd.getLeft(), step2Transformation).getClosure();

            //Check redundant
            if (!closure.containsAll(fd.getRight())) {
                //FD is not redundant because we cannot get the same attributes from the new sets

                //Add back the FD
                step2Transformation.add(fd);
            }
        }

        //Step 3: Remove redundant attributes on the left hand side of each FD
        List<FunctionalDependency> step3Transformation = new ArrayList<>(step2Transformation);
        for (FunctionalDependency fd : step2Transformation.stream()
                .filter(fd -> fd.getLeft().size() > 1).toList()) {
            Set<String> leftAttributes = new HashSet<>(fd.getLeft());
            for(String attribute : fd.getLeft()) {
                leftAttributes.remove(attribute);

                Set<String> closure = RelationKeyUtils.getClosureFromAttributes(leftAttributes, step3Transformation).getClosure();
                if(!closure.containsAll(fd.getRight())) {
                    //Attribute is not redundant because we cannot get the same attributes
                    //Add back the attribute
                    leftAttributes.add(attribute);
                }
            }
            //Update the original FD
            step3Transformation.remove(fd);
            step3Transformation.add(new FunctionalDependency(leftAttributes, fd.getRight()));
        }

        return step3Transformation;

    }

    public static boolean isIn3NF(Relation relation, List<FunctionalDependency> fds) {
        List<Set<String>> superKeys = RelationKeyUtils.getSuperKeys(relation, fds);
        List<Set<String>> candidateKeys = RelationKeyUtils.getCandidateKeys(relation, fds);
        List<FunctionalDependency> nonTrivialFds = fds.stream().filter(fd -> !fd.isTrivialFunctionalDependency(fd)).toList();

        for (FunctionalDependency fd : nonTrivialFds) {
            boolean isKey = superKeys.stream()
                    .anyMatch(key -> key.equals(fd.getLeft()));
            boolean attributesIsContainedInAKey = candidateKeys.stream().anyMatch(o ->
                    o.containsAll(fd.getRight())
            );
            if (!isKey && !attributesIsContainedInAKey) {
                //FD X->Y
                // Found a non-trivial FD whose left side isn't a key => Not in BCNF
                // Found a non-trivial FD whose attribute in Y is contained in a key => Not in 3NF
                return false;
            }
        }

        return true;
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }


}
