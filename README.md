# DTASelect2PepXML
## Converter from DTASelect to pepXML 
This converter has been developed in order to obtain pepXML files from DTASelect output files, for the need of creating spectrum libraries using [SpectraST](http://tools.proteomecenter.org/wiki/index.php?title=Software:SpectraST) software from results of ProLuCID + DTASelect workflow.

## How to obtain it
Go to [here](http://sealion.scripps.edu:8080/hudson/job/DTASelect2PepXML/lastSuccessfulBuild/artifact/target/dtaselect2pepxml.jar) and download the jar file. It will contain all required dependencies, so you don't have to worry about it.

## How to use it
1. Download the jar file and save it to your computer.
2. In the command line, move to the folder where you downloaded the program and type 
```
java -jar dtaselect2pepxml.jar pepxml_file
```
where:  
   - ```pepxml_file``` is either a single pepXML file or a folder. In case of being a folder, all files with ```txt``` extension will be considered as input files for the conversion. (**MANDATORY**)

**Examples**:
```
java -jar dtaselect2pepxml.jar c:\users\salva\desktop\data\DTASelect-filter.txt 
```
This will generate a pepXML file as DTASelect-filter.pep.xml.
By default, the pepXML file will have a reference to a Trypsin/P enzyme and to a mzXML file. In order to change these two references, see optionall parameters below.
```
java -jar dtaselect2pepxml.jar c:\users\salva\desktop\data
```
This will generate a pepXML for each file in the folder with an extension .txt.
  
---
Optionally there are these two optional parameters:
 - ```raw_file_extension```  this optional parameter is present in order to specify the raw file type that eventually is going to be used in the creation of the library with  [SpectraST](http://tools.proteomecenter.org/wiki/index.php?title=Software:SpectraST).  (**OPTIONAL**) 
   - ```enzyme_name```  this optional parameter is present in order to specify the enzyme in the pepXML file. The following values are allowed: 'Lys-N' ,'Lys-C/P' ,'Arg-C' ,'PepsinA' ,'Trypsin_Mod' ,'dualArgC_Cathep' ,'NoCleavage' ,'TrypChymo' ,'Chymotrypsin' ,'Trypsin' ,'Arg-C/P' ,'Trypsin/P' ,'dualArgC_Cathep/P' ,'Asp-N' ,'V8-DE' ,'Lys-C' ,'V8-E' ,'None'.  (**OPTIONAL**) 
**Example**:
```
java -jar dtaselect2pepxml.jar c:\users\salva\desktop\data\DTASelect-filter.txt mzML Chymotrypsin
```
This will generate a pepXML file as DTASelect-filter.pep.xml. with a Chymotrypsin annotated inside and with a reference to a mzML file.

---  

## How to create the spectral library with [SpectraST](http://tools.proteomecenter.org/wiki/index.php?title=Software:SpectraST)

1. Download and install SpectraST, included in [TPP](http://tools.proteomecenter.org/wiki/index.php?title=Software:TPP)
2. Go to the folder bin inside of the TPP installation folder.
3. Follow the next steps: 
  -  Create a consensus splib from all pep.xml converted from the dtaselect files:
```
spectrast -cNraw -cP0 file.pep.xml
```
**IMPORTANT** Referenced RAW files need to be present in the same folder. As an example:  
  
Having a PSM in the DTASelect file like:
```
 041117_DDA_Rep1.53788.53788.5	6.7315	0.5368	100.0	4584.0947	4584.0986	-0.9	1.068073E7	1	10.015502	4.98	36.7	1	-.MRECISIHVGQAGVQIGNACWELYCLEHGIQPDGQMPSDK.T
```
And asuming that the ```raw_file_extension``` value was "mzXML", a file like ```041117_DDA_Rep1.mzXML``` is going to be needed to be present in the same folder in order to allow SpectraST to read the spectrum that matched to that peptide.  
  
This will create a spectra library file (raw.splib). Additionally _raw.pepidx_, _raw.spidx_ and _raw.sptxt_ files will be also created. _raw.sptxt_ file is compatible with [Skyline](https://skyline.ms/project/home/software/Skyline/begin.view) and can be edited and viewed there.   
  
_Note that instead of ```file.pep.xml``` you could use wildcards in order to create a library from multiple pep.xml files (i.e. ```file*.pep.xml```).  
Option ```-cP0``` has to be added because the pepXML file comming from dtaselect doesn't have p-values, so we need to include all matches with pvalue>=0 (= option ```-cP0```)._

 - **Create a consensus library:**
 ```
 spectrast -cNcons -cAC raw.splib
 ```
 _Option ```-cAC``` indicates to create a consensus library, meaning that a single consensus spectrum will be created for each peptide sequence from all spectra matching to that peptide.
 This will create another set of files for a library named as cons.splib._
 
 - **Apply a quality control filter to the consensus splib library:**
```
spectrast -cNconsQ -cAQ cons.splib
```
_Option ```-cAQ``` indicates to perform the quality control filters which will discard some of the spectra in the library.
This will create another set of files for a library named as consQ.splib_

 - **Appending DECOY spectra to the library by:** 
```
spectrast -cNconsQdecoy -cAD -cc -cy1 consQ.splib
```
This will create another set of files for a library named as consQDecoy.splib
_Option ```-cAD``` generates decoy spectra to the library
Option ```-cc``` concatenates the generated decoy spectra to the library
Option ```-cy1``` is the proportion of decoys over forward entries. ```cy2``` will mean that it will generate twice decoy entries over forward._


