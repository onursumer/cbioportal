package org.cbioportal.web.util;

import org.cbioportal.model.ClinicalData;
import org.cbioportal.model.DataBin;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(MockitoJUnitRunner.class)
public class DataBinnerTest
{
    private DataBinner dataBinner;
    
    @Spy
    private DataBinHelper dataBinHelper;
    
    @InjectMocks
    private DiscreteDataBinner discreteDataBinner;
    
    @InjectMocks
    private LinearDataBinner linearDataBinner;
    
    @InjectMocks
    private ScientificSmallDataBinner scientificSmallDataBinner;
    
    @InjectMocks
    private LogScaleDataBinner logScaleDataBinner;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        
        dataBinner = new DataBinner(
            dataBinHelper, discreteDataBinner, linearDataBinner, scientificSmallDataBinner, logScaleDataBinner);
    }
    
    @Test
    public void testLinearDataBinner()
    {
        String studyId = "blca_tcga";
        String attributeId = "AGE";
        String[] values = {
            "34","37","41","42","43","44","45","45","46","47","47","47","47","48","48","48","48","48","48","49","49",
            "49","50","50","50","51","52","52","52","52","52","52","53","53","53","53","54","54","54","54","54","54",
            "54","55","55","55","55","55","55","56","56","56","56","56","56","56","56","56","57","57","57","57","57",
            "57","57","57","57","57","58","58","58","58","58","58","59","59","59","59","59","59","59","59","59","59",
            "59","59","59","59","60","60","60","60","60","60","60","60","60","60","60","60","60","60","60","60","60",
            "60","60","60","61","61","61","61","61","61","61","61","61","61","61","61","62","62","62","62","62","62",
            "62","62","63","63","63","63","63","63","63","63","63","64","64","64","64","64","64","64","64","64","64",
            "64","64","64","65","65","65","65","65","65","65","65","65","65","65","66","66","66","66","66","66","66",
            "66","66","66","66","66","66","66","66","66","66","67","67","67","67","67","67","67","67","67","67","67",
            "67","67","68","68","68","68","68","68","68","68","68","68","68","68","69","69","69","69","69","69","69",
            "69","69","69","69","69","69","70","70","70","70","70","70","70","70","70","70","70","70","70","70","70",
            "70","71","71","71","71","71","71","71","71","72","72","72","72","72","72","72","72","72","72","73","73",
            "73","73","73","73","73","73","73","73","73","73","73","73","73","73","74","74","74","74","74","74","74",
            "74","74","74","74","74","75","75","75","75","75","75","75","75","75","75","75","75","75","75","75","75",
            "75","75","75","75","75","76","76","76","76","76","76","76","76","76","76","76","76","76","76","76","77",
            "77","77","77","77","77","77","77","77","77","77","77","78","78","78","78","78","78","78","78","78","78",
            "78","78","79","79","79","79","79","79","79","79","79","79","79","79","79","80","80","80","80","80","80",
            "80","80","80","80","80","80","81","81","81","81","81","81","81","81","82","82","82","82","82","82","82",
            "82","83","83","83","83","83","83","83","84","84","84","84","84","84","84","85","85","85","85","85","86",
            "86","86","87","87","87","87","87","88","89","90","90","90"
        };

        List<ClinicalData> clinicalData = mockClinicalData(attributeId, studyId, values);
        List<String> patientIds = clinicalData.stream().map(ClinicalData::getPatientId).collect(Collectors.toList());
        
        List<DataBin> dataBins = dataBinner.calculateClinicalDataBins(attributeId, clinicalData, patientIds);

        Assert.assertEquals(6, dataBins.size());
        
        Assert.assertEquals("<=", dataBins.get(0).getSpecialValue());
        Assert.assertEquals(new Double(40.0), dataBins.get(0).getEnd());
        Assert.assertEquals(2, dataBins.get(0).getCount().intValue());

        Assert.assertEquals(new Double(40.0), dataBins.get(1).getStart());
        Assert.assertEquals(new Double(50.0), dataBins.get(1).getEnd());
        Assert.assertEquals(23, dataBins.get(1).getCount().intValue());

        Assert.assertEquals(new Double(50.0), dataBins.get(2).getStart());
        Assert.assertEquals(new Double(60.0), dataBins.get(2).getEnd());
        Assert.assertEquals(83, dataBins.get(2).getCount().intValue());

        Assert.assertEquals(new Double(60.0), dataBins.get(3).getStart());
        Assert.assertEquals(new Double(70.0), dataBins.get(3).getEnd());
        Assert.assertEquals(124, dataBins.get(3).getCount().intValue());

        Assert.assertEquals(new Double(70.0), dataBins.get(4).getStart());
        Assert.assertEquals(new Double(80.0), dataBins.get(4).getEnd());
        Assert.assertEquals(131, dataBins.get(4).getCount().intValue());

        Assert.assertEquals(new Double(80.0), dataBins.get(5).getStart());
        Assert.assertEquals(new Double(90.0), dataBins.get(5).getEnd());
        Assert.assertEquals(48, dataBins.get(5).getCount().intValue());
    }

    @Test
    public void testLinearDataBinnerWithPediatricAge()
    {
        String studyId = "skcm_broad";
        String attributeId = "AGE_AT_PROCUREMENT";
        String[] values = {
            "72","22","55","57","70","21","67","42","26","54","39","67","46","52","47","53","51","35","53","46","42",
            "31","83","29","52","47","41","35","45","40","50","37","54","71","49","49","56","74","48","48","76","35",
            "67","74","63","47","67","39","31","62","58","65","72","27","59","87","69","41","39","53","17","44","76",
            "51","46","63","41","38","34","69","63","65","59","58","60","56","65","26","58","48","65","44","45","28",
            "46","48","52","52","70","57","63","68","45","51","39","79","53","41","47","39","49","42","37","78","69",
            "33","71","80","49","65","67","70","46","49","63","28","62","27","51"
        };

        List<ClinicalData> clinicalData = mockClinicalData(attributeId, studyId, values);
        List<String> patientIds = clinicalData.stream().map(ClinicalData::getPatientId).collect(Collectors.toList());

        List<DataBin> dataBins = dataBinner.calculateClinicalDataBins(attributeId, clinicalData, patientIds);

        Assert.assertEquals(8, dataBins.size());

        Assert.assertEquals("<=", dataBins.get(0).getSpecialValue());
        Assert.assertEquals(new Double(18.0), dataBins.get(0).getEnd());
        Assert.assertEquals(1, dataBins.get(0).getCount().intValue());

        Assert.assertEquals(new Double(18.0), dataBins.get(1).getStart());
        Assert.assertEquals(new Double(30.0), dataBins.get(1).getEnd());
        Assert.assertEquals(9, dataBins.get(1).getCount().intValue());

        Assert.assertEquals(new Double(30.0), dataBins.get(2).getStart());
        Assert.assertEquals(new Double(40.0), dataBins.get(2).getEnd());
        Assert.assertEquals(16, dataBins.get(2).getCount().intValue());

        Assert.assertEquals(new Double(40.0), dataBins.get(3).getStart());
        Assert.assertEquals(new Double(50.0), dataBins.get(3).getEnd());
        Assert.assertEquals(31, dataBins.get(3).getCount().intValue());

        Assert.assertEquals(new Double(50.0), dataBins.get(4).getStart());
        Assert.assertEquals(new Double(60.0), dataBins.get(4).getEnd());
        Assert.assertEquals(25, dataBins.get(4).getCount().intValue());

        Assert.assertEquals(new Double(60.0), dataBins.get(5).getStart());
        Assert.assertEquals(new Double(70.0), dataBins.get(5).getEnd());
        Assert.assertEquals(24, dataBins.get(5).getCount().intValue());

        Assert.assertEquals(new Double(70.0), dataBins.get(6).getStart());
        Assert.assertEquals(new Double(80.0), dataBins.get(6).getEnd());
        Assert.assertEquals(11, dataBins.get(6).getCount().intValue());

        Assert.assertEquals(new Double(80.0), dataBins.get(7).getStart());
        Assert.assertEquals(">", dataBins.get(7).getSpecialValue());
        Assert.assertEquals(2, dataBins.get(7).getCount().intValue());
    }

    @Test
    public void testLinearDataBinnerWithNA()
    {
        String studyId = "blca_tcga";
        String attributeId = "LYMPH_NODE_EXAMINED_COUNT";
        String[] values = {
            "170","141","140","112","111","108","107","104","93","87","86","85","83","79","77","77","76","75","74","73",
            "73","71","71","70","69","68","67","66","65","64","64","62","61","61","60","58","58","57","54","54","53","53",
            "52","51","48","48","48","47","47","47","47","46","46","45","45","44","43","41","41","41","41","40","40","40",
            "38","38","38","37","37","36","36","36","35","34","34","34","34","33","31","31","30","30","30","30","30","29",
            "29","29","28","28","28","28","28","28","28","28","28","28","27","27","27","27","27","27","27","27","27","27",
            "26","26","26","26","25","25","25","25","25","24","24","24","24","24","24","23","23","23","23","23","22","22",
            "22","22","21","21","21","21","21","21","20","20","20","20","19","19","19","19","19","19","19","19","18","18",
            "18","18","18","18","18","18","18","17","17","17","17","17","17","17","16","16","16","16","16","16","16","16",
            "16","16","16","15","15","15","15","15","15","15","15","14","14","14","14","14","14","14","14","14","14","14",
            "14","14","14","14","14","14","14","14","13","13","13","13","13","13","13","13","12","12","12","12","12","12",
            "12","12","12","11","11","11","11","11","11","11","11","11","11","11","10","10","10","10","10","9","9","9",
            "9","9","8","8","8","8","8","8","8","8","8","8","8","8","8","8","8","7","7","7","7","7","7","7","7","6","6",
            "6","6","6","6","6","6","5","5","5","5","5","5","5","5","5","4","4","4","4","4","3","3","3","2","2","2","2",
            "2","2","2","2","2","2","2","1","0","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA",
            "NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA",
            "NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA",
            "NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA",
            "NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA",
            "NA","NA","NA","NA","NA","NA"
        };

        List<ClinicalData> clinicalData = mockClinicalData(attributeId, studyId, values);
        List<String> patientIds = clinicalData.stream().map(ClinicalData::getPatientId).collect(Collectors.toList());

        List<DataBin> dataBins = dataBinner.calculateClinicalDataBins(attributeId, clinicalData, patientIds);

        Assert.assertEquals(8, dataBins.size());

        Assert.assertEquals("<=", dataBins.get(0).getSpecialValue());
        Assert.assertEquals(new Double(10.0), dataBins.get(0).getEnd());
        Assert.assertEquals(71, dataBins.get(0).getCount().intValue());

        Assert.assertEquals(new Double(10.0), dataBins.get(1).getStart());
        Assert.assertEquals(new Double(20.0), dataBins.get(1).getEnd());
        Assert.assertEquals(94, dataBins.get(1).getCount().intValue());

        Assert.assertEquals(new Double(20.0), dataBins.get(2).getStart());
        Assert.assertEquals(new Double(30.0), dataBins.get(2).getEnd());
        Assert.assertEquals(58, dataBins.get(2).getCount().intValue());

        Assert.assertEquals(new Double(30.0), dataBins.get(3).getStart());
        Assert.assertEquals(new Double(40.0), dataBins.get(3).getEnd());
        Assert.assertEquals(19, dataBins.get(3).getCount().intValue());

        Assert.assertEquals(new Double(40.0), dataBins.get(4).getStart());
        Assert.assertEquals(new Double(50.0), dataBins.get(4).getEnd());
        Assert.assertEquals(17, dataBins.get(4).getCount().intValue());

        Assert.assertEquals(new Double(50.0), dataBins.get(5).getStart());
        Assert.assertEquals(new Double(60.0), dataBins.get(5).getEnd());
        Assert.assertEquals(10, dataBins.get(5).getCount().intValue());

        Assert.assertEquals(new Double(60.0), dataBins.get(6).getStart());
        Assert.assertEquals(">", dataBins.get(6).getSpecialValue());
        Assert.assertEquals(34, dataBins.get(6).getCount().intValue());
        
        Assert.assertEquals("NA", dataBins.get(7).getSpecialValue());
        Assert.assertEquals(109, dataBins.get(7).getCount().intValue());
    }    
    
    @Test
    public void testDiscreteDataBinner()
    {
        String studyId = "acyc_fmi_2014";
        String attributeId = "ACTIONABLE_ALTERATIONS";
        String[] values = {
            "1","0","0","2","2","2","1","0","0","1","0","0","0","5","0","0","1","0","2","0","3","0","0","0","1","0","0","1"
        };

        List<ClinicalData> clinicalData = mockClinicalData(attributeId, studyId, values);
        List<String> patientIds = clinicalData.stream().map(ClinicalData::getPatientId).collect(Collectors.toList());

        List<DataBin> dataBins = dataBinner.calculateClinicalDataBins(attributeId, clinicalData, patientIds);

        Assert.assertEquals(5, dataBins.size());
        
        Assert.assertEquals(new Double(0.0), dataBins.get(0).getStart());
        Assert.assertEquals(new Double(0.0), dataBins.get(0).getEnd());
        Assert.assertEquals(16, dataBins.get(0).getCount().intValue());

        Assert.assertEquals(new Double(1.0), dataBins.get(1).getStart());
        Assert.assertEquals(new Double(1.0), dataBins.get(1).getEnd());
        Assert.assertEquals(6, dataBins.get(1).getCount().intValue());

        Assert.assertEquals(new Double(2.0), dataBins.get(2).getStart());
        Assert.assertEquals(new Double(2.0), dataBins.get(2).getEnd());
        Assert.assertEquals(4, dataBins.get(2).getCount().intValue());

        Assert.assertEquals(new Double(3.0), dataBins.get(3).getStart());
        Assert.assertEquals(new Double(3.0), dataBins.get(3).getEnd());
        Assert.assertEquals(1, dataBins.get(3).getCount().intValue());

        Assert.assertEquals(new Double(5.0), dataBins.get(4).getStart());
        Assert.assertEquals(new Double(5.0), dataBins.get(4).getEnd());
        Assert.assertEquals(1, dataBins.get(4).getCount().intValue());
    }

    @Test
    public void scientificDataBinner()
    {
        String studyId = "blca_dfarber_mskcc_2014";
        String attributeId = "SILENT_RATE";
        String[] values = {
            "2.87E-06","1.92E-06","1.32E-06","1.78E-06","4.93E-06","3.01E-06","3.07E-06","3.67E-06","1.00E-06",
            "1.61E-06","3.05E-08","3.73E-06","2.44E-06","6.03E-07","1.03E-06","1.29E-06","1.00E-06","7.35E-06",
            "1.53E-06","1.64E-06","6.41E-07","7.46E-07","2.99E-06","5.95E-07","1.06E-05","6.87E-07","5.74E-07",
            "3.73E-06","1.22E-06","5.89E-06","5.22E-06","2.55E-06","5.95E-07","4.43E-07","2.87E-06","1.68E-07",
            "2.47E-06","9.10E-06","9.62E-07","1.23E-06","9.77E-07","1.21E-06","3.67E-06","1.06E-06","4.19E-06",
            "1.61E-06","6.60E-07","8.04E-07","5.45E-07","2.04E-06"
        };

        List<ClinicalData> clinicalData = mockClinicalData(attributeId, studyId, values);
        List<String> sampleIds = clinicalData.stream().map(ClinicalData::getSampleId).collect(Collectors.toList());

        List<DataBin> dataBins = dataBinner.calculateClinicalDataBins(attributeId, clinicalData, sampleIds);

        Assert.assertEquals(4, dataBins.size());

        Assert.assertEquals(new Double("1e-8"), dataBins.get(0).getStart());
        Assert.assertEquals(new Double("1e-7"), dataBins.get(0).getEnd());
        Assert.assertEquals(1, dataBins.get(0).getCount().intValue());

        Assert.assertEquals(new Double("1e-7"), dataBins.get(1).getStart());
        Assert.assertEquals(new Double("1e-6"), dataBins.get(1).getEnd());
        Assert.assertEquals(16, dataBins.get(1).getCount().intValue());

        Assert.assertEquals(new Double("1e-6"), dataBins.get(2).getStart());
        Assert.assertEquals(new Double("1e-5"), dataBins.get(2).getEnd());
        Assert.assertEquals(32, dataBins.get(2).getCount().intValue());

        Assert.assertEquals(">", dataBins.get(3).getSpecialValue());
        Assert.assertEquals(new Double("1e-5"), dataBins.get(3).getStart());
        Assert.assertEquals(1, dataBins.get(3).getCount().intValue());
    }
    
    // TODO logScaleDataBinner tests
    
    
    List<ClinicalData> mockClinicalData(String attributeId, String studyId, String[] values)
    {
        List<ClinicalData> clinicalDataList =  new ArrayList<>();
        
        for (int index = 0; index < values.length; index++) 
        {
            ClinicalData clinicalData = new ClinicalData();
            
            clinicalData.setAttrId(attributeId);
            clinicalData.setStudyId(studyId);
            clinicalData.setSampleId(studyId + "_sample_" + index);
            clinicalData.setPatientId(studyId + "_patient_" + index);
            clinicalData.setAttrValue(values[index]);
            
            clinicalDataList.add(clinicalData);
        }
        
        return clinicalDataList;
    }
}
