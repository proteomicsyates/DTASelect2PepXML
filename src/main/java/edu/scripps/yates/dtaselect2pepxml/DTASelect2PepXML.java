package edu.scripps.yates.dtaselect2pepxml;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import com.compomics.dbtoolkit.io.EnzymeLoader;
import com.compomics.dbtoolkit.toolkit.EnzymeDigest;
import com.compomics.util.io.MascotEnzymeReader;
import com.compomics.util.protein.Enzyme;

import edu.scripps.yates.dtaselectparser.DTASelectParser;
import edu.scripps.yates.utilities.files.FileUtils;
import umich.ms.fileio.filetypes.pepxml.jaxb.nested.MsmsPipelineAnalysis;

public class DTASelect2PepXML {
	private final static Logger log = Logger.getLogger(DTASelect2PepXML.class);

	public static void main(String[] args) throws JAXBException {
		try {
			Enzyme enzyme = null;
			// first argument is the input dtaselect file
			if (args.length == 0) {
				showError("Missing parameters", true);
			}
			File dtaSelectFile = new File(args[0]);
			if (!dtaSelectFile.exists()) {
				dtaSelectFile = new File(System.getProperty("user.dir") + File.separator + args[0]);
				if (!dtaSelectFile.exists()) {
					showError("DTASelect input file '" + args[0] + "' not found.", false);
				}
			}
			String rawDataExtension = "mzXML";
			// second is the raw data extension
			if (args.length > 1) {
				rawDataExtension = args[1];
			}

			// third is the enzyme name
			try {
				enzyme = EnzymeLoader.loadEnzyme("Trypsin/P", null);
				if (args.length > 2) {
					enzyme = EnzymeLoader.loadEnzyme(args[2], null);
					if (enzyme == null) {
						showError(args[2] + " is not recognized as a valid enzyme", false);
					}
				}
				System.out.println("\n-----------------\nUsing the following enzyme:\n" + enzyme.toString()
						+ "\n-----------------");

			} catch (IOException e) {
				e.printStackTrace();
				showError(e.getMessage(), false);
			}

			if (dtaSelectFile.isFile()) {
				try {
					DTASelect2PepXML converter = new DTASelect2PepXML();
					converter.convert(dtaSelectFile, enzyme, rawDataExtension);
				} catch (Exception e) {
					e.printStackTrace();
					log.error(e);
					System.exit(-1);
				}
			} else {
				// apply it to all txt files in the folder
				File[] files = dtaSelectFile.listFiles(new FileFilter() {

					@Override
					public boolean accept(File pathname) {
						if (FilenameUtils.getExtension(pathname.getAbsolutePath()).equals("txt")) {
							return true;
						}
						return false;
					}
				});
				for (File file : files) {
					try {
						DTASelect2PepXML converter = new DTASelect2PepXML();
						converter.convert(file, enzyme, rawDataExtension);
					} catch (Exception e) {
						e.printStackTrace();
						log.error(e);
					}
				}
			}
		} finally {
			System.out.println("Program finished. Good bye!");
		}
	}

	public DTASelect2PepXML() {

	}

	private void convert(File dtaSelectFile, Enzyme enzyme, String rawDataExtension)
			throws FileNotFoundException, JAXBException {
		Path path = Paths.get(dtaSelectFile.getParentFile().getAbsolutePath() + File.separator
				+ FilenameUtils.getBaseName(dtaSelectFile.getAbsolutePath()) + ".pep.xml");
		java.io.File outputFile = path.toFile();
		DTASelectParser parser = new DTASelectParser(dtaSelectFile);

		MsmsPipelineAnalysis msmsPipelineAnalysis = new MsMsPipelineAnalysisAdapter(parser, rawDataExtension, enzyme)
				.adapt();
		System.out.println("Output file created in memory.");
		System.out.println("Writting file to disc.");
		JAXBContext jaxbContext = JAXBContext.newInstance(MsmsPipelineAnalysis.class);
		Marshaller marshaller = jaxbContext.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

		marshaller.marshal(msmsPipelineAnalysis, outputFile);
		System.out.println("File created at " + outputFile.getAbsolutePath() + " ("
				+ FileUtils.getDescriptiveSizeFromBytes(outputFile.length()) + ")");

	}

	private static void showError(String string, boolean showUsage) {
		System.err.println(string);
		if (showUsage) {
			showUsage();
		}
		System.exit(-1);
	}

	private static void showUsage() {
		System.out.println("Usage:");
		System.out.println("java -jar dtaselect2pepxml dtaselect_file raw_data_extension [enzyme_name]");
		System.out.println("Where enzyme name can be one is these ones:");

		InputStream in = EnzymeDigest.class.getClassLoader().getResourceAsStream("enzymes.txt");
		if (in != null) {
			try {
				MascotEnzymeReader mer = new MascotEnzymeReader(in);
				String[] enzymes = mer.getEnzymeNames();
				for (String enzyme : enzymes) {
					System.out.print("'" + enzyme + "' ,");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
}
