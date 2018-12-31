::Get the current batch file's short path
for %%x in (%0) do set BatchPath=%%~dpsx
for %%x in (%BatchPath%) do set BatchPath=%%~dpsx
java -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -Xmx3g -jar %BatchPath%\igv.jar %*
