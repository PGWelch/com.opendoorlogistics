# Introduction #
ODL Studio is free open source software for (a) customer mapping and analysis, (b) sales territory design, management and mapping and (c) vehicle fleet scheduling. 
ODL Studio works off an Excel spreadsheet and supports data editing, geocoding, interactive mapping and pdf report generation.

**To download ODL Studio, please go to http://www.opendoorlogistics.com**

# Programmer's guide #

*The following guide is intended for programmers wishing to modify or add to the ODL Studio project. If you simply want to use ODL Studio, download it from http://www.opendoorlogistics.com and work through the tutorials on the website.*

To build and run ODL Studio through eclipse you need to import all of the eclipse projects (beginning com.opendoorlogistics....).
You do not need to import the odl-geotools-fat-jar project.

#### com.opendoorlogistics.api
This is an Eclipse project defining the API which ODL plugin components use.
The project defines interfaces only - it does not contain any implementation code.
External dependencies are stored in the lib folder.

#### com.opendoorlogistics.codefromweb	
This is an Eclipse project containing miscellaneous snippets of code from various webpages (all weak copy-left).
They are kept separate from the main projects to make the copyright notices etc simpler.

#### com.opendoorlogistics.components	
This is an Eclipse project containing various built-in ODL Studio components (e.g. p-median optimiser). 
The components project references the core, codefromweb and api projects.
External dependencies are stored in the lib folder.

#### com.opendoorlogistics.core	
This is an Eclipse project containing the ODL Studio engine.
The engine is the framework which loads components, runs scripts etc.
The core project references the api and codefromweb projects.
External dependencies are stored in the lib folder.

#### com.opendoorlogistics.jsprit.debugger
This project references both the jsprit project and the studio project.
You should run this project to debug jsprit within ODL Studio.

#### com.opendoorlogistics.jsprit
This is the jsprit integration project.
The jsprit project references the api project only.
It should be exported as a fat jar built by eclipse.
The fat jar is then placed in the plugins subdirectory of the ODL Studio installation directory.
It is loaded by the java simple plugin framework (https://code.google.com/p/jspf/).
To debug jsprit with ODL Studio use the jsprit.debugger project.
External dependencies are stored in the lib folder.

#### com.opendoorlogistics.studio
This project contains the ODL Studio UI.
It references the api, codefromweb, components and core projects.
External dependencies are stored in the lib folder.

#### odl-geotools-fat-jar
This a Maven project which builds a fat jar of the required geotools library.
The fat jar is copied into the core project's lib directory.
