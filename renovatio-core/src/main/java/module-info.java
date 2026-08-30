open module org.shark.renovatio.core {
    requires transitive org.shark.renovatio.shared;
    requires spring.context;
    requires spring.core;
    requires spring.web;
    requires spring.beans;
    requires org.mapstruct;
    requires jakarta.annotation;
    requires org.apache.pdfbox;
    requires org.slf4j;
    requires io.swagger.v3.oas.annotations;
    requires static lombok;

    exports org.shark.renovatio.core.dto;
    exports org.shark.renovatio.core.entity;
    exports org.shark.renovatio.core.infrastructure;
    exports org.shark.renovatio.core.mapper;
    exports org.shark.renovatio.core.service;
}
