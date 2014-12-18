# Introduction #
ODL Studio is free open source software for (a) customer mapping and analysis, (b) sales territory design, management and mapping and (c) vehicle fleet scheduling. 
ODL Studio works off an Excel spreadsheet and supports data editing, geocoding, interactive mapping and pdf report generation.

**To download ODL Studio, please go to http://www.opendoorlogistics.com**

# Programmer's guide #

*The following guide is intended for programmers wishing to modify or add to the ODL Studio project. If you simply want to use ODL Studio, download it from http://www.opendoorlogistics.com and work through the tutorials on the website.*

To build and run ODL Studio through Eclipse you need to import all of the following Eclipse projects. You do not need to import the Maven project 'odl-geotools-fat-jar' as this is pre-built and its jar is available in the libs directory of com.opendoorlogistics.core.

After importing Eclipse should automatically link the dependencies between various project (available under project properties, Java Build Path, Projects). If this fails then setup the dependencies as listed for each project.

Each project holds any 3rd party jars it is dependent on within its lib folder, so you don't need to download any other jars to build them.

#### com.opendoorlogistics.api
This is an Eclipse project defining the API which ODL plugin components use.
The project defines interfaces only - it does not contain any implementation code.

*Dependencies on other com.opendoorlogistics projects* : none

#### com.opendoorlogistics.codefromweb	
This is an Eclipse project containing miscellaneous pieces of code from various webpages (all weak copy-left) including a fork of the excellent jxmapviewer2 project (see https://github.com/msteiger/jxmapviewer2). They are kept separate from the main projects to simplify the copyright.

*Dependencies on other com.opendoorlogistics projects* : none

#### com.opendoorlogistics.components	
This is an Eclipse project containing various built-in ODL Studio components (e.g. p-median optimiser, reporter, geocoders). 

*Dependencies on other com.opendoorlogistics projects* : com.opendoorlogistics.api, com.opendoorlogistics.codefromweb, com.opendoorlogistics.core

#### com.opendoorlogistics.core	
This is an Eclipse project containing the ODL Studio engine.
The engine is the framework which loads components, runs scripts, draws the map etc.

*Dependencies on other com.opendoorlogistics projects* : com.opendoorlogistics.api, com.opendoorlogistics.codefromweb

#### com.opendoorlogistics.jsprit.debugger
This project references both the jsprit project and the studio project.
You should run this project to debug jsprit within ODL Studio.

*Dependencies on other com.opendoorlogistics projects* : com.opendoorlogistics.api, com.opendoorlogistics.codefromweb, com.opendoorlogistics.components, com.opendoorlogistics.core, com.opendoorlogistics.jsprit, com.opendoorlogistics.studio

#### com.opendoorlogistics.jsprit
This is the jsprit integration project.
It should be exported as a fat jar built by Eclipse.
The fat jar is then placed in the plugins subdirectory of the ODL Studio installation directory.
It is loaded by the java simple plugin framework (https://code.google.com/p/jspf/).
To debug jsprit with ODL Studio use the jsprit.debugger project.

*Dependencies on other com.opendoorlogistics projects* : com.opendoorlogistics.api

#### com.opendoorlogistics.studio
This project contains the ODL Studio UI, based on java swing.

*Dependencies on other com.opendoorlogistics projects* : com.opendoorlogistics.api, com.opendoorlogistics.codefromweb, com.opendoorlogistics.components, com.opendoorlogistics.core



