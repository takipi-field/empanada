# empanada


javac *.java -cp aws-lambda-java-core-1.0.0.jar;zip -r lambdajam.zip *.sh *.class

Enviornment variables required are:

Lambda Handler should be set to TakipiWrapper::handleRequest

TAKIPI_HANDLER this is the class of the actual handler
TAKIPI_HOST this is where the collector is running
TAKIPI_PORT this is the collector port
TAKIPI_DISABLE when set to TRUE, it will not enable TAKIPI
TAKIPI_PRINT when set to TRUE, more info is printed to stdout.

