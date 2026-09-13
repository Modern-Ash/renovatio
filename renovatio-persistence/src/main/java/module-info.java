module org.modernash.renovatio.persistence {
    exports org.modernash.renovatio.persistence.classifier;
    exports org.modernash.renovatio.persistence.strategy;
    exports org.modernash.renovatio.persistence.registry;
    exports org.modernash.renovatio.persistence.config to spring.beans, spring.context;

    requires org.modernash.renovatio.semantic.ir;
    requires org.modernash.renovatio.profile;
    requires org.modernash.renovatio.shared;
    requires spring.context;
    requires spring.boot.autoconfigure;
}
