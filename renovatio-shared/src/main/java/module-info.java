open module org.modernash.renovatio.shared {
    requires transitive com.fasterxml.jackson.annotation;
    requires transitive spring.context;
    requires transitive org.antlr.antlr4.runtime;
    requires transitive org.modernash.renovatio.profile;
    requires transitive org.modernash.renovatio.semantic.ir;
    requires static lombok;

    exports org.modernash.renovatio.shared.domain;
    exports org.modernash.renovatio.shared.emission;
    exports org.modernash.renovatio.shared.nql;
    exports org.modernash.renovatio.shared.security;
    exports org.modernash.renovatio.shared.spi;
    exports org.modernash.renovatio.shared.util;
}
