# IGV
NRGene's fork of IGV, based on a previous Version of the Broad's [IGV](https://github.com/igvteam/igv)

BUILDING
--------

Prerequisites:

Java J2SE 6.0 or greater
Ant 1.7.0 or greater (http://ant.apache.org/)

Important: before building the jar from sources one needs to ```cp ./bin/bcel-6.0.jar /usr/share/ant/lib/``` otherwise, you might get a ClassNotFound exception when trying to build the jar

1.  Download and unzip the source distribution file.

2.  Run the provided ant script by running "ant" from the root directory
    of the distribution.

The above build tool will create ```igv.jar``` in the root directory of the distribution.


RUNNING
-------

After building igv.jar IGV can be launched by executing one of the following
command line scripts:

igv.sh        (for Linux and OSX)
igv.bat       (for Windows)

The shell script is configured to start IGV with maximum 3000MB of
memory.  This is a reasonable default for most machines.  If you are
working with very large datasets you can increase the amount of memory
available to IGV by editing the first line of the startup script.
Specifically change the value of the "-Xmx" parameter.  For example,
to start IGV with 1 gigabyte of memory  change the value to -Xmx10000m

DOWNLOAD
---------
It is also possible to download the standalone jar from this [link](https://s3.amazonaws.com/nrgene-igv-download/igv-nrgene-5.4.0.jar)

Then just call: ```java -jar igv-nrgene-5.4.0.jar```



