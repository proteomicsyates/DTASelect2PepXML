package edu.scripps.yates.dtaselect2pepxml;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import edu.scripps.yates.dbindex.model.AssignMass;
import edu.scripps.yates.dtaselectparser.util.DTASelectModification;
import edu.scripps.yates.dtaselectparser.util.DTASelectPSM;
import edu.scripps.yates.dtaselectparser.util.DTASelectProtein;
import edu.scripps.yates.utilities.fasta.FastaParser;
import edu.scripps.yates.utilities.pattern.Adapter;
import umich.ms.fileio.filetypes.pepxml.jaxb.nested.MsmsPipelineAnalysis;
import umich.ms.fileio.filetypes.pepxml.jaxb.nested.MsmsPipelineAnalysis.MsmsRunSummary.SpectrumQuery;
import umich.ms.fileio.filetypes.pepxml.jaxb.nested.MsmsPipelineAnalysis.MsmsRunSummary.SpectrumQuery.SearchResult;
import umich.ms.fileio.filetypes.pepxml.jaxb.nested.MsmsPipelineAnalysis.MsmsRunSummary.SpectrumQuery.SearchResult.SearchHit;
import umich.ms.fileio.filetypes.pepxml.jaxb.nested.MsmsPipelineAnalysis.MsmsRunSummary.SpectrumQuery.SearchResult.SearchHit.AlternativeProtein;
import umich.ms.fileio.filetypes.pepxml.jaxb.nested.MsmsPipelineAnalysis.MsmsRunSummary.SpectrumQuery.SearchResult.SearchHit.ModificationInfo;
import umich.ms.fileio.filetypes.pepxml.jaxb.nested.MsmsPipelineAnalysis.MsmsRunSummary.SpectrumQuery.SearchResult.SearchHit.ModificationInfo.ModAminoacidMass;
import umich.ms.fileio.filetypes.pepxml.jaxb.nested.NameValueType;

public class SpectrumQueryAdapter implements Adapter<SpectrumQuery> {
	private final DTASelectPSM psm;
	private static long index = 0;
	private final static DecimalFormat format4Decimals = new DecimalFormat("0.0000");

	public SpectrumQueryAdapter(DTASelectPSM dtaSelectPSM) {
		this.psm = dtaSelectPSM;
	}

	@Override
	public SpectrumQuery adapt() {
		MsmsPipelineAnalysis.MsmsRunSummary.SpectrumQuery spectrumQuery = new MsmsPipelineAnalysis.MsmsRunSummary.SpectrumQuery();

		if (psm.getRTInMin() != null) {
			spectrumQuery.setRetentionTimeSec(psm.getRTInMin().floatValue() * 60);
		}
		// spectrumQuery.setActivationMethod(ActivationMethodType.HCD);
		spectrumQuery.setAssumedCharge(psm.getChargeState());

		try {
			spectrumQuery.setStartScan(Integer.valueOf(FastaParser.getScanFromPSMIdentifier(psm.getRawPSMID())));
		} catch (NumberFormatException e) {

		}
		try {
			spectrumQuery.setEndScan(Integer.valueOf(FastaParser.getSecondScanFromPSMIdentifier(psm.getRawPSMID())));
		} catch (NumberFormatException e) {

		}
		spectrumQuery.setIndex(index++);
		if (psm.getTotalIntensity() != null) {
			spectrumQuery.setPrecursorIntensity(psm.getTotalIntensity().floatValue());
		}
		if (psm.getMh() != null) {
			spectrumQuery.setPrecursorNeutralMass(psm.getMh().floatValue());
		}
		spectrumQuery.setSpectrum(psm.getRawPSMID());

		SearchResult searchResult = new SearchResult();
		spectrumQuery.getSearchResult().add(searchResult);

		SearchHit searchHit = new SearchHit();
		searchResult.getSearchHit().add(searchHit);

		searchHit.setHitRank(1l);
		searchHit.setPeptide(psm.getSequence().getSequence());
		searchHit.setPeptidePrevAa(String.valueOf(psm.getSequence().getBeforeSeq()));
		searchHit.setPeptideNextAa(String.valueOf(psm.getSequence().getAfterSeq()));
		List<DTASelectProtein> proteinList = new ArrayList<DTASelectProtein>();
		proteinList.addAll(psm.getProteins());
		// sort protein list by putting first the canonical forms
		proteinList.sort(new Comparator<DTASelectProtein>() {

			@Override
			public int compare(DTASelectProtein o1, DTASelectProtein o2) {
				String uniProtACC1 = FastaParser.getUniProtACC(o1.getLocus());
				String uniProtACC2 = FastaParser.getUniProtACC(o2.getLocus());
				if (uniProtACC1 != null && uniProtACC2 != null) {
					if (FastaParser.getIsoformVersion(uniProtACC1) == null
							&& FastaParser.getIsoformVersion(uniProtACC2) != null) {
						return -1;
					} else if (FastaParser.getIsoformVersion(uniProtACC2) == null
							&& FastaParser.getIsoformVersion(uniProtACC1) != null) {
						return 1;
					}
				}
				return o1.getLocus().compareTo(o2.getLocus());
			}
		});
		if (!proteinList.isEmpty()) {
			DTASelectProtein protein = proteinList.get(0);
			searchHit.setProtein(protein.getLocus());
			searchHit.setProteinDescr(protein.getDescription());
			searchHit.setProteinMw(protein.getMw());
		}
		Double delta = null;
		searchHit.setNumTotProteins(proteinList.size());
		if (psm.getCalcMh() != null) {
			searchHit.setCalcNeutralPepMass(psm.getCalcMh().floatValue());
			if (psm.getMh() != null) {
				delta = psm.getMh() - psm.getCalcMh();
				searchHit.setMassdiff(String.valueOf(delta));
			}
		}
		searchHit.setNumTolTerm(getNumTolTerm(psm));
		searchHit.setNumMissedCleavages(getNumMissedCleavages(psm));
		// if more than one protein, create alternative protein elements
		for (int i = 1; i < proteinList.size(); i++) {
			DTASelectProtein protein = proteinList.get(i);
			AlternativeProtein alternativeProtein = new AlternativeProtein();
			alternativeProtein.setProtein(protein.getLocus());
			alternativeProtein.setProteinDescr(protein.getDescription());
			alternativeProtein.setProteinMw(protein.getMw());
			searchHit.getAlternativeProtein().add(alternativeProtein);
		}

		// modifications
		if (psm.getSequence().getModifications() != null && !psm.getSequence().getModifications().isEmpty()) {
			ModificationInfo modificationInfo = new ModificationInfo();
			searchHit.setModificationInfo(modificationInfo);
			modificationInfo.setModifiedPeptide(psm.getFullSequence());
			for (DTASelectModification ptm : psm.getSequence().getModifications()) {
				ModAminoacidMass modAminoacidMass = new ModAminoacidMass();
				modificationInfo.getModAminoacidMass().add(modAminoacidMass);
				if (ptm.getModificationShift() != null) {
					modAminoacidMass.setMass(ptm.getModificationShift() + AssignMass.getMass(ptm.getAa()));
				}
				modAminoacidMass.setPosition(ptm.getModPosition());
			}
		}
		// scores
		// xcorr
		if (psm.getXcorr() != null) {
			NameValueType xcorr = new NameValueType();
			searchHit.getSearchScore().add(xcorr);
			xcorr.setName("xcorr");
			xcorr.setValueStr(format4Decimals.format(psm.getXcorr()));
		}
		// deltacn
		if (psm.getDeltacn() != null) {
			NameValueType deltaCn = new NameValueType();
			searchHit.getSearchScore().add(deltaCn);
			deltaCn.setName("deltacn");
			deltaCn.setValueStr(format4Decimals.format(psm.getDeltacn()));
		}
		// Conf%
		if (psm.getConf() != null) {
			NameValueType conf = new NameValueType();
			searchHit.getSearchScore().add(conf);
			conf.setName("Conf%");
			conf.setValueStr(format4Decimals.format(psm.getConf()));
		}
		// SpR
		if (psm.getSpr() != null) {
			NameValueType spr = new NameValueType();
			searchHit.getSearchScore().add(spr);
			spr.setName("sprank");
			spr.setValueStr(format4Decimals.format(psm.getSpr()));
		}
		// Prob Score
		if (psm.getProb_score() != null) {
			NameValueType probScore = new NameValueType();
			searchHit.getSearchScore().add(probScore);
			probScore.setName("Prob Score");
			probScore.setValueStr(format4Decimals.format(psm.getProb_score()));
		}
		// Prob
		if (psm.getProb() != null) {
			NameValueType prob = new NameValueType();
			searchHit.getSearchScore().add(prob);
			prob.setName("Prob");
			prob.setValueStr(format4Decimals.format(psm.getProb()));
		}
		// IonProportion
		if (psm.getIonProportion() != null) {
			NameValueType prob = new NameValueType();
			searchHit.getSearchScore().add(prob);
			prob.setName("IonProportion");
			prob.setValueStr(format4Decimals.format(psm.getIonProportion()));
		}
		// Redundancy
		if (psm.getRedundancy() != null) {
			NameValueType prob = new NameValueType();
			searchHit.getSearchScore().add(prob);
			prob.setName("Redundancy");
			prob.setValueStr(format4Decimals.format(psm.getRedundancy()));
		}
		// pI
		if (psm.getPi() != null) {
			NameValueType prob = new NameValueType();
			searchHit.getSearchScore().add(prob);
			prob.setName("pI");
			prob.setValueStr(format4Decimals.format(psm.getPi()));
		}
		// PPM
		if (psm.getPpmError() != null) {
			NameValueType prob = new NameValueType();
			searchHit.getSearchScore().add(prob);
			prob.setName("PPM");
			prob.setValueStr(format4Decimals.format(psm.getPpmError()));
		}
		// precursor_mz_diff
		if (delta != null) {
			NameValueType precursor_mz_diff = new NameValueType();
			searchHit.getSearchScore().add(precursor_mz_diff);
			precursor_mz_diff.setName("precursor_mz_diff");
			precursor_mz_diff.setValueStr(format4Decimals.format(delta));
		}

		return spectrumQuery;
	}

	private Integer getNumMissedCleavages(DTASelectPSM psm2) {
		int ret = 0;
		for (int i = 0; i < psm2.getSequence().getSequence().length() - 2; i++) {
			if (isTryptic(psm2.getSequence().getSequence().charAt(i))) {
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
	private Integer getNumTolTerm(DTASelectPSM psm2) {
		int ret = 0;
		if (isTryptic(psm2.getSequence().getBeforeSeq())) {
			ret++;
		}
		if (isTryptic(psm2.getSequence().getSequence().charAt(psm2.getSequence().getSequence().length() - 1))) {
			ret++;
		}
		return ret;
	}

	private boolean isTryptic(char c) {
		return c == 'K' || c == 'R';
	}
}
