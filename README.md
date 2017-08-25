# Introduction #
## ODL Studio is free open source software for :
- (a) customer mapping and analysis,
- (b) sales territory design, management and mapping,
- (c) vehicle fleet scheduling. 

ODL Studio works off an Excel spreadsheet and supports data editing, geocoding, interactive mapping and pdf report generation.

## To download ODL Studio, please go to [Open Door Logistics](http://www.opendoorlogistics.com)

# Programmer's guide #

**The following guide is intended for programmers wishing to modify or add to the ODL Studio project. If you simply want to use ODL Studio, download it from [Open Door Logistics](http://www.opendoorlogistics.com) and work through the tutorials on the website.**

### Building the projects 

To build and run ODL Studio through Eclipse you need to import all of the following Eclipse projects. You do not need to import the Maven project *odl-geotools-fat-jar* as this is pre-built and its jar is available in the *lib* directory of *com.opendoorlogistics.core*.

After importing Eclipse should automatically link the dependencies between various projects (available under *Project properties, Java Build Path, Projects*). If this fails then setup the dependencies as listed below for each project.

Each project holds any 3rd party jars it is dependent on within its *lib* folder, so you don't need to download any other jars to build them.

## List of Eclipse projects

#### com.opendoorlogistics.api
- This is an Eclipse project defining the API which ODL plugin components use.
- The project defines interfaces only - it does not contain any implementation code.

*Dependencies on other com.opendoorlogistics projects* : none

#### com.opendoorlogistics.codefromweb	
- This is an Eclipse project containing miscellaneous pieces of code from various webpages (all weak copy-left) including a fork of the excellent jxmapviewer2 project (see https://github.com/msteiger/jxmapviewer2). 
- They are kept separate from the main projects to simplify the copyright.

*Dependencies on other com.opendoorlogistics projects* : none

#### com.opendoorlogistics.components	
- This is an Eclipse project containing various built-in ODL Studio components (e.g. p-median optimiser, reporter, geocoders). 

*Dependencies on other com.opendoorlogistics projects* : com.opendoorlogistics.api, com.opendoorlogistics.codefromweb, com.opendoorlogistics.core

#### com.opendoorlogistics.core	
- This is an Eclipse project containing the ODL Studio engine.
- The engine is the framework which loads components, runs scripts, draws the map etc.

*Dependencies on other com.opendoorlogistics projects* : com.opendoorlogistics.api, com.opendoorlogistics.codefromweb

*Dependencies on other com.opendoorlogistics projects* : com.opendoorlogistics.api, com.opendoorlogistics.codefromweb, com.opendoorlogistics.components, com.opendoorlogistics.core, com.opendoorlogistics.jsprit, com.opendoorlogistics.studio

#### com.opendoorlogistics.jsprit
- This is the jsprit integration project which integrates the jsprit vehicle routing toolkit (https://github.com/jsprit/jsprit) into ODL Studio.
To build it 
- (a) export it from Eclipse as a fat jar (*Export, Runnable jar*),
- (b) place the exported jar in the *plugins* subdirectory of the ODL Studio installation directory.

The jar will be automatically loaded by ODL Studio, using the java simple plugin framework (https://code.google.com/p/jspf/). To debug it, place the fat jar in a plugins subdirectory within 
the Studio code directory and then debug ODL Studio as normal. You must build the jar before debugging.

When jsprit is loaded as a plugin it has its own separate class loader, to avoid 'jar hell' problems.

*Dependencies on other com.opendoorlogistics projects* : com.opendoorlogistics.api

#### com.opendoorlogistics.studio
- This project contains the ODL Studio UI, based on java swing. To debug it, start the AppFrame.java class.

*Dependencies on other com.opendoorlogistics projects* : com.opendoorlogistics.api, com.opendoorlogistics.codefromweb, com.opendoorlogistics.components, com.opendoorlogistics.core



