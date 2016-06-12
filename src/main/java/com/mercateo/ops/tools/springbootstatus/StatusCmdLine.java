package com.mercateo.ops.tools.springbootstatus;
import com.zwitserloot.cmdreader.Description;
import com.zwitserloot.cmdreader.Mandatory;
import com.zwitserloot.cmdreader.Sequential;

public class StatusCmdLine {

    @Mandatory
    @Sequential(1)
    @Description("Process ID of the Boot application you want to grab the Status/Version from")
    String pid;

    @com.zwitserloot.cmdreader.Shorthand("v")
    @Description("Create verbose output for debugging")
    boolean verbose = false;

    @com.zwitserloot.cmdreader.Shorthand("t")
    @Description("Path to tools.jar")
    String toolsJar = null;

    @com.zwitserloot.cmdreader.Shorthand("h")
    @Description("This help")
    boolean help = false;

}
