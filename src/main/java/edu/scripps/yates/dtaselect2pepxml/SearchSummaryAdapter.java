package edu.scripps.yates.dtaselect2pepxml;

import java.io.File;
import java.io.IOException;
import java.util.List;

import edu.scripps.yates.dtaselectparser.DTASelectParser;
import edu.scripps.yates.utilities.pattern.Adapter;
import umich.ms.fileio.filetypes.pepxml.jaxb.nested.EngineType;
import umich.ms.fileio.filetypes.pepxml.jaxb.nested.MassType;
import umich.ms.fileio.filetypes.pepxml.jaxb.nested.MsmsPipelineAnalysis.MsmsRunSummary.SearchSummary;
import umich.ms.fileio.filetypes.pepxml.jaxb.nested.MsmsPipelineAnalysis.MsmsRunSummary.SearchSummary.EnzymaticSearchConstraint;
import umich.ms.fileio.filetypes.pepxml.jaxb.nested.MsmsPipelineAnalysis.MsmsRunSummary.SearchSummary.SearchDatabase;
import umich.ms.fileio.filetypes.pepxml.jaxb.nested.NameValueType;

public class SearchSummaryAdapter implements Adapter<SearchSummary> {
	private final DTASelectParser parser;
	private String baseName;
	private final com.compomics.util.protein.Enzyme enzyme;

	public SearchSummaryAdapter(DTASelectParser parser, com.compomics.util.protein.Enzyme enzyme2, String rawFileName) {
		this.parser = parser;
		String inputFile = parser.getInputFilePathes().iterator().next();
		this.baseName = new File(inputFile).getParentFile().getAbsolutePath() + File.separator + rawFileName;
		this.enzyme = enzyme2;
	}

	@Override
	public SearchSummary adapt() {
		SearchSummary ret = new SearchSummary();
		ret.setBaseName(baseName);
		ret.setSearchEngine(EngineType.SEQUEST);
		ret.setPrecursorMassType(MassType.MONOISOTOPIC);
		ret.setFragmentMassType(MassType.MONOISOTOPIC);
		ret.setOutData(".txt");
		ret.setOutDataType("txt");
		ret.setSearchId(1l);
		try {
			ret.setSearchEngineVersion(parser.getSearchEngineVersion());
		} catch (IOException e) {
		}
		EnzymaticSearchConstraint enzymaticSearchConstraint = new EnzymaticSearchConstraint();
		ret.setEnzymaticSearchConstraint(enzymaticSearchConstraint);
		enzymaticSearchConstraint.setEnzyme(enzyme.getTitle());
		enzymaticSearchConstraint.setMaxNumInternalCleavages(enzyme.getMiscleavages());
		SearchDatabase searchDatabase = new SearchDatabase();
		ret.setSearchDatabase(searchDatabase);
		try {
			searchDatabase.setLocalPath(parser.getFastaPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
		searchDatabase.setType("AA");
		List<String> commandLineParameterStrings;
		try {
			commandLineParameterStrings = parser.getCommandLineParameterStrings();
			for (String commandLineParameterString : commandLineParameterStrings) {
				NameValueType parameter = new NameValueType();
				if (commandLineParameterString.contains("\t")) {
					String[] split = commandLineParameterString.split("\t");
					parameter.setName(split[1]);
					parameter.setValue(split[0]);
				} else {
					parameter.setName("Command line");
					parameter.setValue(commandLineParameterString);
				}
				ret.getParameter().add(parameter);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return ret;
	}

}
