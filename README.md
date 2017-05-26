# DTASelect2PepXML
## Converter from DTASelect to pepXML 
This converter has been developed in order to obtain pepXML files from DTASelect output files, for the need of creating spectrum libraries using [SpectraST](http://tools.proteomecenter.org/wiki/index.php?title=Software:SpectraST) software from results of ProLuCID + DTASelect workflow.

## How to obtain it
Go to [here](http://sealion.scripps.edu:8080/hudson/job/DTASelect2PepXML/lastSuccessfulBuild/artifact/target/dtaselect2pepxml.jar)

## How to use it
1. Download the zip and decompress it in a folder
2. Type ``java -jar dtaselect2pepxml.jar file [raw_file_extension] [enzyme_name]`` where  
   - ```file``` is either a single pepXML or a folder. In case of being a folder, all files with ```txt``` extension will be considered as input files for the conversion. (**MANDATORY**)
   - ```raw_file_extension```  this optional parameter is present in order to specify the raw file type that eventually is going to be used in the creation of the library with  [SpectraST](http://tools.proteomecenter.org/wiki/index.php?title=Software:SpectraST).  (OPTIONAL) 
   - ```enzyme_name```  this optional parameter is present in order to specify the enzyme in the pepXML file. The following values are allowed: 'Lys-N' ,'Lys-C/P' ,'Arg-C' ,'PepsinA' ,'Trypsin_Mod' ,'dualArgC_Cathep' ,'NoCleavage' ,'TrypChymo' ,'Chymotrypsin' ,'Trypsin' ,'Arg-C/P' ,'Trypsin/P' ,'dualArgC_Cathep/P' ,'Asp-N' ,'V8-DE' ,'Lys-C' ,'V8-E' ,'None'.  (OPTIONAL) 

**Example**:
```
java -jar dtaselect2pepxml.jar c:\users\salva\desktop\data\DTASelect-filter.txt 
```

## How to create the spectral library with [SpectraST](http://tools.proteomecenter.org/wiki/index.php?title=Software:SpectraST)

1. Download and install SpectraST, included in [TPP](http://tools.proteomecenter.org/wiki/index.php?title=Software:TPP)
2. Go to the folder bin inside of the TPP installation folder.
3. Follow the next steps: 
  -  Create a consensus splib from all pep.xml converted from the dtaselect files:
```
spectrast -cNconsensus -cAC -cP0 file.pep.xml
```

This will create a consensus.splib file.  
_Note that instead of ```file.pep.xml``` you could use wildcards in order to create a library from multiple pep.xml files (i.e. ```file*.pep.xml```).  
The ```cP0``` option has to be added because the pepXML file comming from dtaselect doesn't have p-values, so we need to include all matches with pvalue>=0 (= option ```cP0```)._

 - Apply a quality control filter to the consensus splib library:
```
spectrast -cNconsensusQ -cAQ consensus.splib
```

 - For using the library in TPP, generate DECOYs by: 
```
spectrast -cNconsensusQDecoy -cAD -cc -cy1 consensusQ.splib
```

_Option ```cy1``` is the proportion of decoys over forward entries. ```cy2``` will mean that it will generate twice decoy entries over forward._


