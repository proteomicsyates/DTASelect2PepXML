# DTASelect2PepXML
## Converter from DTASelect to pepXML 
This converter has been developed in order to obtain pepXML files from DTASelect output files, for the need of creating spectrum libraries using [SpectraST](http://tools.proteomecenter.org/wiki/index.php?title=Software:SpectraST) software from results of ProLuCID + DTASelect workflow.

## How to obtain it
Go to [here](http://sealion.scripps.edu:8080/hudson/job/DTASelect2PepXML/lastSuccessfulBuild/artifact/target/)

## How to use it
1. Download the zip and decompress it in a folder
2. Type ``java -jar DTASelect2PepXML.jar file [raw_file_extension] [enzyme_name]`` where  
   - ```file``` is either a single pepXML or a folder. In case of being a folder, all files with ```txt``` extension will be considered as input files for the conversion. (**MANDATORY**)
   - ```raw_file_extension```  this optional parameter is present in order to specify the raw file type that eventually is going to be used in the creation of the library with  [SpectraST](http://tools.proteomecenter.org/wiki/index.php?title=Software:SpectraST).  (OPTIONAL) 
   - ```enzyme_name```  this optional parameter is present in order to specify the enzyme in the pepXML file. The following values are allowed: 'Lys-N' ,'Lys-C/P' ,'Arg-C' ,'PepsinA' ,'Trypsin_Mod' ,'dualArgC_Cathep' ,'NoCleavage' ,'TrypChymo' ,'Chymotrypsin' ,'Trypsin' ,'Arg-C/P' ,'Trypsin/P' ,'dualArgC_Cathep/P' ,'Asp-N' ,'V8-DE' ,'Lys-C' ,'V8-E' ,'None'.  (OPTIONAL) 



