package edu.scripps.yates.dtaselect2pepxml;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import edu.scripps.yates.dtaselectparser.util.DTASelectPSM;
import edu.scripps.yates.utilities.fasta.FastaParser;
import edu.scripps.yates.utilities.pattern.Adapter;
import edu.scripps.yates.utilities.proteomicsmodel.PSM;
import edu.scripps.yates.utilities.proteomicsmodel.PTM;
import edu.scripps.yates.utilities.proteomicsmodel.PTMSite;
import edu.scripps.yates.utilities.proteomicsmodel.Protein;
import umich.ms.fileio.filetypes.pepxml.jaxb.nested.MsmsPipelineAnalysis;
import umich.ms.fileio.filetypes.pepxml.jaxb.nested.MsmsPipelineAnalysis.MsmsRunSummary.SpectrumQuery;
import umich.ms.fileio.filetypes.pepxml.jaxb.nested.MsmsPipelineAnalysis.MsmsRunSummary.SpectrumQuery.SearchResult;
import umich.ms.fileio.filetypes.pepxml.jaxb.nested.MsmsPipelineAnalysis.MsmsRunSummary.SpectrumQuery.SearchResult.SearchHit;
import umich.ms.fileio.filetypes.pepxml.jaxb.nested.MsmsPipelineAnalysis.MsmsRunSummary.SpectrumQuery.SearchResult.SearchHit.AlternativeProtein;
import umich.ms.fileio.filetypes.pepxml.jaxb.nested.MsmsPipelineAnalysis.MsmsRunSummary.SpectrumQuery.SearchResult.SearchHit.ModificationInfo;
import umich.ms.fileio.filetypes.pepxml.jaxb.nested.MsmsPipelineAnalysis.MsmsRunSummary.SpectrumQuery.SearchResult.SearchHit.ModificationInfo.ModAminoacidMass;
import umich.ms.fileio.filetypes.pepxml.jaxb.nested.NameValueType;

public class SpectrumQueryAdapter implements Adapter<SpectrumQuery> {
	private final PSM psm;
	private static long index = 0;
	private final static DecimalFormat format4Decimals = new DecimalFormat("0.0000");

	public SpectrumQueryAdapter(PSM dtaSelectPSM) {
		psm = dtaSelectPSM;
	}

	@Override
	public SpectrumQuery adapt() {
		final MsmsPipelineAnalysis.MsmsRunSummary.SpectrumQuery spectrumQuery = new MsmsPipelineAnalysis.MsmsRunSummary.SpectrumQuery();

		if (psm.getRtInMinutes() != null) {
			spectrumQuery.setRetentionTimeSec(psm.getRtInMinutes().floatValue() * 60);
		}
		// spectrumQuery.setActivationMethod(ActivationMethodType.HCD);
		spectrumQuery.setAssumedCharge(psm.getChargeState());

		try {
			spectrumQuery.setStartScan(Integer.valueOf(psm.getScanNumber()));
		} catch (final NumberFormatException e) {

		}
		try {
			spectrumQuery
					.setEndScan(Integer.valueOf(FastaParser.getSecondScanFromPSMIdentifier(psm.getMSRun().getRunId())));
		} catch (final NumberFormatException e) {

		}
		spectrumQuery.setIndex(index++);
		if (psm.getTotalIntensity() != null) {
			spectrumQuery.setPrecursorIntensity(psm.getTotalIntensity().floatValue());
		}
		if (psm.getExperimentalMH() != null) {
			spectrumQuery.setPrecursorNeutralMass(psm.getExperimentalMH().floatValue());
		}
		spectrumQuery.setSpectrum(psm.getMSRun().getRunId());

		final SearchResult searchResult = new SearchResult();
		spectrumQuery.getSearchResult().add(searchResult);

		final SearchHit searchHit = new SearchHit();
		searchResult.getSearchHit().add(searchHit);

		searchHit.setHitRank(1l);
		searchHit.setPeptide(psm.getSequence());
		searchHit.setPeptidePrevAa(String.valueOf(psm.getBeforeSeq()));
		searchHit.setPeptideNextAa(String.valueOf(psm.getAfterSeq()));
		final List<Protein> proteinList = new ArrayList<Protein>();
		proteinList.addAll(psm.getProteins());
		// sort protein list by putting first the canonical forms
		proteinList.sort(new Comparator<Protein>() {

			@Override
			public int compare(Protein o1, Protein o2) {
				final String uniProtACC1 = o1.getAccession();
				final String uniProtACC2 = o2.getAccession();
				if (uniProtACC1 != null && uniProtACC2 != null) {
					if (FastaParser.getIsoformVersion(uniProtACC1) == null
							&& FastaParser.getIsoformVersion(uniProtACC2) != null) {
						return -1;
					} else if (FastaParser.getIsoformVersion(uniProtACC2) == null
							&& FastaParser.getIsoformVersion(uniProtACC1) != null) {
						return 1;
					}
				}
				return o1.getAccession().compareTo(o2.getAccession());
			}
		});
		if (!proteinList.isEmpty()) {
			final Protein protein = proteinList.get(0);
			searchHit.setProtein(protein.getAccession());
			searchHit.setProteinDescr(protein.getDescription());
			searchHit.setProteinMw(protein.getMw().doubleValue());
		}
		Float delta = null;
		searchHit.setNumTotProteins(proteinList.size());
		if (psm.getCalcMH() != null) {
			searchHit.setCalcNeutralPepMass(psm.getCalcMH());
			if (psm.getExperimentalMH() != null) {
				delta = psm.getExperimentalMH() - psm.getCalcMH();
				searchHit.setMassdiff(String.valueOf(delta));
			}
		}
		searchHit.setNumTolTerm(getNumTolTerm(psm));
		searchHit.setNumMissedCleavages(getNumMissedCleavages(psm));
		// if more than one protein, create alternative protein elements
		for (int i = 1; i < proteinList.size(); i++) {
			final Protein protein = proteinList.get(i);
			final AlternativeProtein alternativeProtein = new AlternativeProtein();
			alternativeProtein.setProtein(protein.getAccession());
			alternativeProtein.setProteinDescr(protein.getDescription());
			alternativeProtein.setProteinMw(protein.getMw().doubleValue());
			searchHit.getAlternativeProtein().add(alternativeProtein);
		}

		// modifications
		if (psm.getPTMs() != null && !psm.getPTMs().isEmpty()) {
			final ModificationInfo modificationInfo = new ModificationInfo();
			searchHit.setModificationInfo(modificationInfo);
			modificationInfo.setModifiedPeptide(psm.getFullSequence());
			for (final PTM ptm : psm.getPTMs()) {
				for (final PTMSite ptmSite : ptm.getPTMSites()) {

					final ModAminoacidMass modAminoacidMass = new ModAminoacidMass();
					modificationInfo.getModAminoacidMass().add(modAminoacidMass);
					if (ptm.getMassShift() != null) {
						modAminoacidMass.setMass(ptm.getMassShift()
								+ edu.scripps.yates.utilities.masses.AssignMass.getMass(ptmSite.getAA().charAt(0)));
					}
					modAminoacidMass.setPosition(ptmSite.getPosition());
				}
			}
		}
		// scores
		// xcorr
		if (psm.getXCorr() != null) {
			final NameValueType xcorr = new NameValueType();
			searchHit.getSearchScore().add(xcorr);
			xcorr.setName("xcorr");
			xcorr.setValueStr(format4Decimals.format(psm.getXCorr()));
		}
		// deltacn
		if (psm.getDeltaCn() != null) {
			final NameValueType deltaCn = new NameValueType();
			searchHit.getSearchScore().add(deltaCn);
			deltaCn.setName("deltacn");
			deltaCn.setValueStr(format4Decimals.format(psm.getDeltaCn()));
		}
		// Conf%
		if (psm instanceof DTASelectPSM && ((DTASelectPSM) psm).getConf() != null) {
			final NameValueType conf = new NameValueType();
			searchHit.getSearchScore().add(conf);
			conf.setName("Conf%");
			conf.setValueStr(format4Decimals.format(((DTASelectPSM) psm).getConf()));
		}
		// SpR
		if (psm.getSpr() != null) {
			final NameValueType spr = new NameValueType();
			searchHit.getSearchScore().add(spr);
			spr.setName("sprank");
			spr.setValueStr(format4Decimals.format(psm.getSpr()));
		}
		// Prob Score
		if (psm instanceof DTASelectPSM && ((DTASelectPSM) psm).getProb_score() != null) {
			final NameValueType probScore = new NameValueType();
			searchHit.getSearchScore().add(probScore);
			probScore.setName("Prob Score");
			probScore.setValueStr(format4Decimals.format(((DTASelectPSM) psm).getProb_score()));
		}
		// Prob
		if (psm instanceof DTASelectPSM && ((DTASelectPSM) psm).getProb() != null) {
			final NameValueType prob = new NameValueType();
			searchHit.getSearchScore().add(prob);
			prob.setName("Prob");
			prob.setValueStr(format4Decimals.format(((DTASelectPSM) psm).getProb()));
		}
		// IonProportion
		if (psm.getIonProportion() != null) {
			final NameValueType prob = new NameValueType();
			searchHit.getSearchScore().add(prob);
			prob.setName("IonProportion");
			prob.setValueStr(format4Decimals.format(psm.getIonProportion()));
		}
		// Redundancy
		if (psm instanceof DTASelectPSM && ((DTASelectPSM) psm).getRedundancy() != null) {
			final NameValueType prob = new NameValueType();
			searchHit.getSearchScore().add(prob);
			prob.setName("Redundancy");
			prob.setValueStr(format4Decimals.format(((DTASelectPSM) psm).getRedundancy()));
		}
		// pI
		if (psm.getPi() != null) {
			final NameValueType prob = new NameValueType();
			searchHit.getSearchScore().add(prob);
			prob.setName("pI");
			prob.setValueStr(format4Decimals.format(psm.getPi()));
		}
		// PPM
		if (psm.getMassErrorPPM() != null) {
			final NameValueType prob = new NameValueType();
			searchHit.getSearchScore().add(prob);
			prob.setName("PPM");
			prob.setValueStr(format4Decimals.format(psm.getMassErrorPPM()));
		}
		// precursor_mz_diff
		if (delta != null) {
			final NameValueType precursor_mz_diff = new NameValueType();
			searchHit.getSearchScore().add(precursor_mz_diff);
			precursor_mz_diff.setName("precursor_mz_diff");
			precursor_mz_diff.setValueStr(format4Decimals.format(delta));
		}

		return spectrumQuery;
	}

	private Integer getNumMissedCleavages(PSM psm2) {
		int ret = 0;
		for (int i = 0; i < psm2.getSequence().length() - 2; i++) {
			if (isTryptic(String.valueOf(psm2.getSequence().charAt(i)))) {
				ret++;
			}
		}
		return ret;
	}

	/**
	 * Returns 0, 1 or 2, if it is not tryptic, if it is semi tryptic or if it
	 * is fully tryptic, respectively
	 * 
	 * @param psm2
	 * @return
	 */
	private Integer getNumTolTerm(PSM psm2) {
		int ret = 0;
		if (isTryptic(psm2.getBeforeSeq())) {
			ret++;
		}
		if (isTryptic(String.valueOf(psm2.getSequence().charAt(psm2.getSequence().length() - 1)))) {
			ret++;
		}
		return ret;
	}

	private boolean isTryptic(String c) {
		return c.charAt(0) == 'K' || c.charAt(0) == 'R';
	}
}
