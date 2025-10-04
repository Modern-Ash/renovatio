open module org.shark.renovatio.core {
    requires transitive org.shark.renovatio.shared;
    requires spring.context;
    requires spring.web;
    requires org.mapstruct;
    requires jakarta.annotation;
    requires org.apache.pdfbox;

    exports org.shark.renovatio.core.dto;
    exports org.shark.renovatio.core.entity;
    exports org.shark.renovatio.core.infrastructure;
    exports org.shark.renovatio.core.mapper;
    exports org.shark.renovatio.core.service;
}
