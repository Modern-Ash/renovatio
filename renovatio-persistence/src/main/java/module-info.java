module org.shark.renovatio.persistence {
    exports org.shark.renovatio.persistence.classifier;
    exports org.shark.renovatio.persistence.strategy;
    exports org.shark.renovatio.persistence.registry;
    exports org.shark.renovatio.persistence.config to spring.beans, spring.context;

    requires org.shark.renovatio.semantic.ir;
    requires org.shark.renovatio.profile;
    requires org.shark.renovatio.shared;
    requires spring.context;
    requires spring.boot.autoconfigure;
}
