module org.shark.renovatio.emitter.node {
    requires org.shark.renovatio.shared;
    requires org.shark.renovatio.architecture;
    requires org.shark.renovatio.profile;
    requires org.shark.renovatio.persistence;
    requires spring.context;
    requires spring.boot.autoconfigure;

    exports org.shark.renovatio.emitter.node to spring.beans, spring.context;
    exports org.shark.renovatio.emitter.node.prisma to spring.beans, spring.context;
}
