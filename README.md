# IGV
NRGene's fork of IGV, based on a previous Version of the Broad's [IGV](https://github.com/igvteam/igv)

BUILDING
--------

Prerequisites:

Java J2SE 6.0 or greater
Ant 1.7.0 or greater (http://ant.apache.org/)

Important: to have the jar signed, download and put bcel-6.0.jar in: ```/usr/share/ant/lib/``` or somewhere where in ant's classpath

1.  Download and unzip the source distribution file.

2.  Run the provided ant script by running "ant" from the root directory
    of the distribution.

The above script will build "igv.jar" in the root directory of the distribution.


RUNNING
-------

After building igv.jar IGV can be launched by executing one of the following
command line scripts:

igv.sh        (for LINUX and MAC OsX)

The shell script is configured to start IGV with maximum 3000MB of
memory.  This is a reasonable default for most machines.  If you are
working with very large datasets you can increase the amount of memory
available to IGV by editing the first line of the startup script.
Specifically change the value of the "-Xmx" parameter.  For example,
to start IGV with 1 gigabyte of memory  change the value to -Xmx10000m

