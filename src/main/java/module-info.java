module app {
    requires javafx.base;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.controls;
    requires static lombok;
    requires javafx.swing;
    requires org.apache.pdfbox;
    requires com.fasterxml.jackson.databind;
    requires java.sql;
    requires com.fasterxml.jackson.datatype.jsr310;
//    requires com.fasterxml.jackson.annotation;
//    requires java.desktop;
//    requires java.sql;
//    requires javafx.base;
//    requires javafx.controls;
//    requires javafx.graphics;
//    requires static lombok;
//    requires org.apache.pdfbox;
//    requires javafx.swing;
//    requires javafx.fxml;
//    requires com.fasterxml.jackson.databind;
//    requires com.fasterxml.jackson.core;
//    requires com.fasterxml.jackson.datatype.jsr310;
//
    opens app.controller to javafx.fxml;
//    exports app.model;
    exports app;
}