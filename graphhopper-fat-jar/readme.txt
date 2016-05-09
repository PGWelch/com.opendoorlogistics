This project build a fat jar (i.e. uber jar) with all graphhopper dependencies to be referenced as a library in the ODL Studio Eclipse projects.
The majority of the relevant source code for this project actually sits within the project graphhopper-odl-integration. 
graphhopper-odl-integration does not build a fat jar though (i.e. it includes no dependencies), so it can be used by other Maven projects.
