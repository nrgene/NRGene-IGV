# IGV
NRGene's fork of IGV, based on a previous Version of the Broad's [IGV](https://github.com/igvteam/igv)

DOWNLOADING
-----------
You can download the standalone IGV jar from [here](https://s3.amazonaws.com/nrgene-igv-download/igv-nrgene-5.4.0.jar)

Then to run, just call: ```java -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -Xmx3g -jar igv-nrgene-5.4.0.jar```


BUILDING
--------
Prerequisites: Java JDK6 or greater and [Ant](http://ant.apache.org/) 1.7.0 or greater

1. Clone or download this repository.

2. Copy bin/bcel-6.0.jar from this project to /usr/share/ant/lib/ (in Linux) - otherwise, you might get a ClassNotFound exception from ant build tool when trying to build the jar.

3. Run the build tool: ant. It will create ```igv-nrgene-5.4.0.jar``` in the root directory of the project.


RUNNING
-------
After building the jar executable, IGV can be launched by executing one of the following
command line scripts:

igv.sh        (for Linux and OSX)

igv.bat       (for Windows)

The shell scripts are configured to start IGV with maximum 3gb of
memory.  This is a reasonable default for most machines.  If you are
working with very large datasets you can increase the amount of memory
available to IGV by editing the first line of the startup script.
Specifically change the value of the "-Xmx" parameter.  For example,
to start IGV with 6 gigabytes of memory change the value to -Xmx6g
