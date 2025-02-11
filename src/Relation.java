import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class Relation {
    private String name;
    private Set<String> attributes;

    public Relation(String name, Set<String> attributes) {
        this.name = name;
        this.attributes = attributes;
    }

    public String getName() {
        return name;
    }

    public Set<String> getAttributes() {
        return attributes;
    }

    @Override
    public String toString() {
        return "Relation{" +
                "name='" + name + '\'' +
                ", attributes=" + attributes +
                '}';
    }

    // Parse "R(A,B,C,D)" into a Relation object
    public static Relation parseRelation(String relationString) {
        // Example input: R(A,B,C,D)
        // 1) Extract the relation name "R"
        // 2) Extract the attributes "A,B,C,D"
        // 3) Split on commas

        // Find the index of '(' and ')'
        int openParen = relationString.indexOf('(');
        int closeParen = relationString.indexOf(')');

        // Relation name is everything before '('
        String name = relationString.substring(0, openParen).trim();

        // Inside parentheses: the comma-separated attributes
        String attributesPart = relationString.substring(openParen + 1, closeParen).trim();
        String[] attributesArray = attributesPart.split("\\s*,\\s*");
        Set<String> attributes = Set.of(attributesArray);

        return new Relation(name, attributes);
    }

}
