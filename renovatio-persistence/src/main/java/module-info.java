module org.shark.renovatio.persistence {
    exports org.shark.renovatio.persistence.classifier;
    exports org.shark.renovatio.persistence.strategy;

    requires org.shark.renovatio.semantic.ir;
    requires org.shark.renovatio.profile;
    requires org.shark.renovatio.shared;
}
