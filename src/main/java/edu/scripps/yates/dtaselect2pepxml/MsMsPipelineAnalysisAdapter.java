package edu.scripps.yates.dtaselect2pepxml;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.io.FilenameUtils;

import com.compomics.util.protein.Enzyme;

import edu.scripps.yates.dtaselectparser.DTASelectParser;
import edu.scripps.yates.dtaselectparser.util.DTASelectPSM;
import edu.scripps.yates.utilities.pattern.Adapter;
import umich.ms.fileio.filetypes.pepxml.jaxb.nested.EngineType;
import umich.ms.fileio.filetypes.pepxml.jaxb.nested.MsmsPipelineAnalysis;
import umich.ms.fileio.filetypes.pepxml.jaxb.nested.MsmsPipelineAnalysis.MsmsRunSummary.SampleEnzyme;
import umich.ms.fileio.filetypes.pepxml.jaxb.nested.MsmsPipelineAnalysis.MsmsRunSummary.SampleEnzyme.Specificity;

public class MsMsPipelineAnalysisAdapter implements Adapter<MsmsPipelineAnalysis> {
	private static final String UNKNOWN = "UNKNOWN";
	private final DTASelectParser parser;
	private final String fileName;
	private final String baseName;
	private final String rawDataType;
	private final com.compomics.util.protein.Enzyme enzyme;

	public MsMsPipelineAnalysisAdapter(DTASelectParser parser, String rawDataType,
			com.compomics.util.protein.Enzyme enzyme2) {
		this.parser = parser;
		String inputFile = parser.getInputFilePathes().iterator().next();
		this.fileName = FilenameUtils.getName(inputFile);
		this.baseName = inputFile + File.separator + FilenameUtils.getBaseName(fileName);
		this.rawDataType = rawDataType;
		this.enzyme = enzyme2;

	}

	@Override
	public MsmsPipelineAnalysis adapt() {
		try {
			System.out.println("Reading input file: " + fileName);
			MsmsPipelineAnalysis msmsPipelineAnalysis = new MsmsPipelineAnalysis();
			msmsPipelineAnalysis.setSummaryXml(fileName);
			Date now = new Date();
			GregorianCalendar c = new GregorianCalendar();
			c.setTime(now);
			XMLGregorianCalendar date2 = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
			msmsPipelineAnalysis.setDate(date2);

			List<MsmsPipelineAnalysis.MsmsRunSummary> msmsRunSummaryList = msmsPipelineAnalysis.getMsmsRunSummary();

			Map<String, Set<DTASelectPSM>> psmsByRawFile = new HashMap<String, Set<DTASelectPSM>>();
			Collection<DTASelectPSM> values = parser.getDTASelectPSMsByPSMID().values();
			for (DTASelectPSM psm : values) {
				if (psmsByRawFile.containsKey(psm.getRawFileName())) {
					psmsByRawFile.get(psm.getRawFileName()).add(psm);
				} else {
					Set<DTASelectPSM> psms = new HashSet<DTASelectPSM>();
					psms.add(psm);
					psmsByRawFile.put(psm.getRawFileName(), psms);
				}
			}

			for (String rawFileName : psmsByRawFile.keySet()) {

				MsmsPipelineAnalysis.MsmsRunSummary msmsRunSummary = new MsmsPipelineAnalysis.MsmsRunSummary();
				msmsRunSummaryList.add(msmsRunSummary);
				msmsRunSummary.setBaseName(
						new File(parser.getInputFilePathes().iterator().next()).getParentFile().getAbsolutePath()
								+ File.separator + rawFileName);
				msmsRunSummary.setMsManufacturer(UNKNOWN);
				msmsRunSummary.setMsDetector(UNKNOWN);
				msmsRunSummary.setMsModel(UNKNOWN);
				msmsRunSummary.setMsIonization(UNKNOWN);
				msmsRunSummary.setMsMassAnalyzer(UNKNOWN);
				msmsRunSummary.setRawDataType(rawDataType);
				msmsRunSummary.setRawData(rawDataType);
				// enzyme
				if (enzyme != null) {
					SampleEnzyme sampleEnzyme = new SampleEnzyme();
					msmsRunSummary.setSampleEnzyme(sampleEnzyme);
					if (enzyme.getTitle() != null) {
						sampleEnzyme.setName(enzyme.getTitle());
					}

					// specificicity
					Specificity specificity = new Specificity();
					sampleEnzyme.getSpecificity().add(specificity);
					String sense = "C";
					if (enzyme.getPosition() == Enzyme.CTERM) {
						sense = "C";
					} else if (enzyme.getPosition() == Enzyme.NTERM) {
						sense = "N";
					}
					if (enzyme.getCleavage().length > 0) {
						specificity.setCut(getString(enzyme.getCleavage()));
					}
					specificity.setSense(sense);
					if (enzyme.getRestrict() != null && enzyme.getRestrict().length > 0) {
						specificity.setNoCut(getString(enzyme.getRestrict()));
					}

				}
				msmsRunSummary.setSearchEngine(EngineType.SEQUEST);
				// search summary
				msmsRunSummary.getSearchSummary().add(new SearchSummaryAdapter(parser, enzyme, rawFileName).adapt());
				List<MsmsPipelineAnalysis.MsmsRunSummary.SpectrumQuery> spectrumQueryList = msmsRunSummary
						.getSpectrumQuery();

				for (DTASelectPSM dtaSelectPSM : psmsByRawFile.get(rawFileName)) {
					spectrumQueryList.add(new SpectrumQueryAdapter(dtaSelectPSM).adapt());
				}
			}
			return msmsPipelineAnalysis;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (DatatypeConfigurationException e) {
			e.printStackTrace();
		}
		return null;
	}

	private String getString(char[] aminoAcidBefore) {
		StringBuilder sb = new StringBuilder();
		for (char character : aminoAcidBefore) {
			sb.append(character);
		}
		return sb.toString();
	}

}
