open module org.modernash.renovatio.core {
    requires transitive org.modernash.renovatio.application;
    requires transitive org.modernash.renovatio.shared;
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

    exports org.modernash.renovatio.core.dto;
    exports org.modernash.renovatio.core.entity;
    exports org.modernash.renovatio.core.infrastructure;
    exports org.modernash.renovatio.core.mapper;
    exports org.modernash.renovatio.core.service;
}
