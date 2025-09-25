module com.tugalsan.api.file {
    requires java.xml.bind;
    requires com.tugalsan.api.list;    
    requires com.tugalsan.api.union;
    requires com.tugalsan.api.function;
    requires com.tugalsan.api.tuple;
    requires com.tugalsan.api.thread;
    requires com.tugalsan.api.log;
    requires com.tugalsan.api.charset;
    requires com.tugalsan.api.os;
    requires com.tugalsan.api.stream;
    requires com.tugalsan.api.string;
    requires com.tugalsan.api.time;
    exports com.tugalsan.api.file.client;
    exports com.tugalsan.api.file.server;
}
