Geocoding data files for postcodes are built by using the com.opendoorlogistics.studio.jar (from the ODL Studio install directory) as a command line interface.
This uses the tab-separated format from the http://www.geonames.org/ bulk postcode download file (see http://download.geonames.org/export/zip/).

If you run the command:

    java -Xmx2G -jar com.opendoorlogistics.studio.jar -buildgdf "C:\data\allCountries.txt" "C:\data\gdffiles"

using your local java runtime install, this will create files for each country listed in the allCountries.txt file (found here http://download.geonames.org/export/zip/allCountries.zip) and save them to the output directory C:\data\gdffiles. You should empty all files from the output directory before running - if an output file (for example ie.gdf or ie.gdf.p) already exists in your output directory the builder will mistakenly try to open it and you will get an exception "Exception of type IllegalArgumentException : Name already used: version". The builder may also build files with the extension .t; you can delete these afterwards.

The allCountries.zip from Geonames does not have Great Britain unit-postcode level data and hence you should not use the gb.gdf and gb.gdf.p files which are built using allCountries.zip. If you want the latest CodePoint Open postcodes for the UK (see http://www.ordnancesurvey.co.uk/business-and-government/products/code-point-open.html), you have to convert them to Geonames format first as they come in a CSV format with a separate file for each postcode area. The following command takes all the .csv files from the directory C:\data\Codepoint open\ and writes them out together to a single file cp2Geonames.txt, which you can then use as input into the -buildgdf command above. 

    java -Xmx2G -jar com.opendoorlogistics.studio.jar -cp2Geonames "C:\data\Codepoint open\" "c:\data\cp2Geonames.txt"

Building the gdf file for the UK is slow due to the amount of data and can take 20 to 40 minutes.
