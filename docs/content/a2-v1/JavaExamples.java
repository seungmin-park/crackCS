/** Java 21 source-launch example for A2-FND-02. No application state or network. */
class JavaExamples {
    public static void main(String[] arguments) {
        int count = 2147483647;
        long widenedAfter = count + 1;
        long widenedBefore = (long) count + 1;
        long longLiteral = count + 1L;
        requireEqual("int addition then widening", widenedAfter, -2147483648L);
        requireEqual("widening before addition", widenedBefore, 2147483648L);
        requireEqual("long literal before addition", longLiteral, 2147483648L);
        System.out.println("PASS: Java 21 integer examples: 3 assertions");
    }

    private static void requireEqual(String description, long actual, long expected) {
        if (actual != expected) {
            throw new AssertionError(description + ": " + actual + " != " + expected);
        }
    }
}
