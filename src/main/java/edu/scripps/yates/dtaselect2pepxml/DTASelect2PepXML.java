package edu.scripps.yates.dtaselect2pepxml;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
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
import com.google.common.io.Files;

import edu.scripps.yates.dtaselectparser.DTASelectParser;
import edu.scripps.yates.utilities.files.FileUtils;
import umich.ms.fileio.filetypes.pepxml.jaxb.nested.MsmsPipelineAnalysis;

public class DTASelect2PepXML {
	private final static Logger log = Logger.getLogger(DTASelect2PepXML.class);

	public static void main(String[] args) throws JAXBException {
		try {
			Enzyme enzyme = null;
			File fastaFile = null;
			File dtaSelectFile = null;
			File rawFileFolder = null;
			// first argument is the input dtaselect file
			if (args.length == 0) {
				showError("Missing parameters", true);
			}
			if (args.length % 2 != 0) {
				showError("Error in parameters", true);
			}
			// parse arguments
			// default values:

			String enzymeName = "Trypsin/P";
			String rawFileExtension = "mzXML";
			for (int i = 0; i < args.length; i = i + 2) {
				String argumentType = args[i].trim().replace("-", "");
				if (argumentType.equals("i")) {
					String pathname = args[i + 1].trim();
					dtaSelectFile = new File(pathname);
					if (!dtaSelectFile.exists()) {
						dtaSelectFile = new File(System.getProperty("user.dir") + File.separator + pathname);
						if (!dtaSelectFile.exists()) {
							showError("DTASelect input file '" + pathname + "' not found.", false);
						}
					}
				} else if (argumentType.equals("r")) {
					// raw data
					rawFileFolder = new File(args[i + 1].trim());
					if (!rawFileFolder.exists()) {
						rawFileFolder = new File(System.getProperty("user.dir") + File.separator + args[i + 1].trim());
						if (!rawFileFolder.exists()) {
							showWarn("Raw data folder '" + args[i + 1].trim() + "' not found.", false);
						} else {
							rawFileFolder = new File(args[i + 1].trim());
						}
					}
				} else if (argumentType.equals("x")) {
					// raw file extension
					rawFileExtension = args[i + 1].trim();

				} else if (argumentType.equals("e")) {
					// enzyme name
					enzymeName = args[i + 1].trim();

				} else if (argumentType.equals("f")) {
					// fasta file
					fastaFile = new File(args[i + 1].trim());
					if (!fastaFile.exists()) {
						fastaFile = new File(System.getProperty("user.dir") + File.separator + args[i + 1].trim());
						if (!fastaFile.exists()) {
							showWarn("FASTA file '" + args[i + 1].trim() + "' not found.", false);
						} else {
							fastaFile = new File(args[i + 1].trim());
						}
					}
				}
			}
			try {
				enzyme = EnzymeLoader.loadEnzyme(enzymeName, null);
				if (enzymeName == null) {
					showError(enzymeName + " is not recognized as a valid enzyme", false);
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
					converter.convert(dtaSelectFile, enzyme, rawFileFolder, rawFileExtension, fastaFile);
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
						converter.convert(file, enzyme, rawFileFolder, rawFileExtension, fastaFile);
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

	private void convert(File dtaSelectFile, Enzyme enzyme, File rawFileFolder, String rawFileExtension, File fastaFile)
			throws JAXBException, IOException {
		Path path = Paths.get(dtaSelectFile.getParentFile().getAbsolutePath() + File.separator
				+ FilenameUtils.getBaseName(dtaSelectFile.getAbsolutePath()) + ".pep.xml");
		java.io.File outputFile = path.toFile();
		DTASelectParser parser = new DTASelectParser(dtaSelectFile);

		MsmsPipelineAnalysis msmsPipelineAnalysis = new MsMsPipelineAnalysisAdapter(parser, rawFileFolder,
				rawFileExtension, fastaFile, enzyme).adapt();

		System.out.println("Output file created in memory.");
		System.out.println("Writting file to disc.");
		JAXBContext jaxbContext = JAXBContext.newInstance(MsmsPipelineAnalysis.class);
		Marshaller marshaller = jaxbContext.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

		marshaller.marshal(msmsPipelineAnalysis, outputFile);
		System.out.println("File created at " + outputFile.getAbsolutePath() + " ("
				+ FileUtils.getDescriptiveSizeFromBytes(outputFile.length()) + ")");

		fixHeader(path);

	}

	private void fixHeader(Path path) throws IOException {
		System.out.println("Fixing file.");
		File tempFile = File.createTempFile("pepxml", "pep.xml");
		BufferedReader br = new BufferedReader(new FileReader(path.toFile()));
		BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile));
		String line;
		while ((line = br.readLine()) != null) {
			if (line.startsWith("<?xml version")) {
				bw.write(line + "\n");
				bw.write("<?xml-stylesheet type=\"text/xsl\" href=\"/local/TPP/schema/pepXML_std.xsl\"?>\n");
			} else if (line.startsWith("<msms_pipeline_an")) {
				bw.write(
						"<msms_pipeline_analysis date=\"2017-05-24T15:53:21\" xmlns=\"http://regis-web.systemsbiology.net/pepXML\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://regis-web.systemsbiology.net/pepXML /local/TPP/schema/pepXML_v120.xsd\" summary_xml=\""
								+ FilenameUtils.getName(path.toString()) + "\">\n");
			} else {
				bw.write(line + "\n");
			}
		}
		br.close();
		bw.close();
		Files.move(tempFile, path.toFile());
		System.out.println("File fixed.");
	}

	private static void showError(String string, boolean showUsage) {
		System.err.println("ERROR: " + string);
		if (showUsage) {
			showUsage();
		}
		System.exit(-1);
	}

	private static void showWarn(String string, boolean showUsage) {
		System.err.println("WARNING: " + string);
		if (showUsage) {
			showUsage();
		}
	}

	private static void showUsage() {
		System.out.println("Usage:");
		System.out.println(
				"java -jar dtaselect2pepxml.jar -i dtaselect_file [-r raw_data_folder] [-x raw_rata_extension] [-f fasta_file] [-e enzyme_name]");
		System.out.println("\nWhere enzyme name can be one is these ones:");

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
