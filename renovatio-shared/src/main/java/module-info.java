open module org.shark.renovatio.shared {
    requires transitive com.fasterxml.jackson.annotation;
    requires transitive spring.context;
    requires transitive org.antlr.antlr4.runtime;
    requires transitive org.shark.renovatio.profile;
    requires transitive org.shark.renovatio.semantic.ir;
    requires static lombok;

    exports org.shark.renovatio.shared.domain;
    exports org.shark.renovatio.shared.emission;
    exports org.shark.renovatio.shared.nql;
    exports org.shark.renovatio.shared.spi;
    exports org.shark.renovatio.shared.util;
}
