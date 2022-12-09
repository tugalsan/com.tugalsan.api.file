module com.tugalsan.api.file {
    requires java.xml.bind;
    requires com.tugalsan.api.list;
    requires com.tugalsan.api.unsafe;
    requires com.tugalsan.api.compiler;
    requires com.tugalsan.api.executable;
    requires com.tugalsan.api.pack;
    requires com.tugalsan.api.thread;
    requires com.tugalsan.api.log;
    requires com.tugalsan.api.os;
    requires com.tugalsan.api.stream;
    requires com.tugalsan.api.time;
    exports com.tugalsan.api.file.client;
    exports com.tugalsan.api.file.server;
}
