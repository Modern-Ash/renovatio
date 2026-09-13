module org.modernash.renovatio.emitter.node {
    requires org.modernash.renovatio.shared;
    requires org.modernash.renovatio.architecture;
    requires org.modernash.renovatio.profile;
    requires org.modernash.renovatio.persistence;
    requires spring.context;
    requires spring.boot.autoconfigure;

    exports org.modernash.renovatio.emitter.node to spring.beans, spring.context;
    exports org.modernash.renovatio.emitter.node.prisma to spring.beans, spring.context;
}
