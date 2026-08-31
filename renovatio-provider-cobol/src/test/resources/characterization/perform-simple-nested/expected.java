final class PerformSimpleNestedFixture { static String run() { return outer(); } static String outer() { return inner(); } static String inner() { return "done"; } }
