package ch.so.agi.sodata;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import ch.interlis.ili2c.Ili2c;
import ch.interlis.ili2c.Ili2cFailure;
import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.iom_j.Iom_jObject;
import ch.interlis.iom_j.xtf.XtfWriter;
import ch.interlis.iox.IoxException;
import ch.interlis.iox.IoxWriter;
import ch.interlis.iox_j.EndBasketEvent;
import ch.interlis.iox_j.EndTransferEvent;
import ch.interlis.iox_j.ObjectEvent;
import ch.interlis.iox_j.StartBasketEvent;
import ch.interlis.iox_j.StartTransferEvent;
import ch.so.agi.meta2file.model.FileFormat;
import ch.so.agi.meta2file.model.Item;
import ch.so.agi.meta2file.model.ServiceType;
import ch.so.agi.meta2file.model.ThemePublication;

@Service
public class ConfigService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Value("${app.configFile}")
    private String CONFIG_FILE;   

    @Value("${app.ilidataDir}")
    private String ilidataDir;
    
    private static final String DEFAULT_DOWNLOAD_HOST_URL = "https://files.geo.so.ch";

    private static final String ILI_TOPIC = "IliRepository09.RepositoryIndex";
    private static final String BID = "DatasetIdx16.DataIndex";
    private static final String TAG = "DatasetIdx16.DataIndex.DatasetMetadata";

    private Map<String,String> downloadHostUrlMap = new HashMap<>();
    
    public Map<String, String> getDownloadHostUrlMap() {
        return downloadHostUrlMap;
    }

    public void parse() throws Ili2cFailure, IOException, IoxException, XMLStreamException {
        IoxWriter ioxWriter = createIlidataWriter();
        ioxWriter.write(new StartTransferEvent("SOGIS-20230218", "", null));
        ioxWriter.write(new StartBasketEvent(ILI_TOPIC,BID));
        
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.registerModule(new JavaTimeModule());
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // TODO: wieder entfernen, wenn stabil? Oder tolerant sein?

        XMLInputFactory xif = XMLInputFactory.newInstance();
        XMLStreamReader xr = xif.createXMLStreamReader(new FileInputStream( new File(CONFIG_FILE)));

        int tid=0;
        while (xr.hasNext()) {
            xr.next();
            if (xr.getEventType() == XMLStreamConstants.START_ELEMENT) {
                if ("themePublication".equals(xr.getLocalName())) {
                    var themePublication = xmlMapper.readValue(xr, ThemePublication.class);
                    var identifier = themePublication.getIdentifier();
                    var items = themePublication.getItems();
                    
                    log.debug("Identifier: {}", themePublication.getIdentifier());
                                        
                    if (themePublication.getModel() != null && themePublication.getModel().trim().length() > 0) {
                        tid++;
                        
                        if (themePublication.getDownloadHostUrl() != null) {
                            downloadHostUrlMap.put(identifier, themePublication.getDownloadHostUrl().toString());

                        } else {
                            downloadHostUrlMap.put(identifier, DEFAULT_DOWNLOAD_HOST_URL);
                        }
                        
                        Iom_jObject iomObj = new Iom_jObject("DatasetIdx16.DataIndex.DatasetMetadata", String.valueOf(tid));
                        iomObj.setattrvalue("id", themePublication.getIdentifier());
                        iomObj.setattrvalue("version", "current");
                        iomObj.setattrvalue("owner", themePublication.getOwner().getOfficeAtWeb().toString());
                        
                        Iom_jObject model = new Iom_jObject("DatasetIdx16.ModelLink", null);
                        model.setattrvalue("name", themePublication.getModel());
                        model.setattrvalue("locationHint", "https://geo.so.ch/models");
                        iomObj.addattrobj("model", model);

                        iomObj.setattrvalue("epsgCode", "2056");
                        iomObj.setattrvalue("publishingDate", themePublication.getLastPublishingDate().format(DateTimeFormatter.ISO_DATE));

                        Iom_jObject boundary = new Iom_jObject("DatasetIdx16.BoundingBox", null);
                        
                        double left = themePublication.getBbox().getLeft();
                        double bottom = themePublication.getBbox().getBottom();
                        double right = themePublication.getBbox().getRight();
                        double top = themePublication.getBbox().getTop();
                        
                        String westlimit = String.valueOf(ApproxSwissProj.CHtoWGSlng(left, bottom));
                        String southlimit = String.valueOf(ApproxSwissProj.CHtoWGSlat(left, bottom));
                        String eastlimit = String.valueOf(ApproxSwissProj.CHtoWGSlng(right, top));
                        String northlimit = String.valueOf(ApproxSwissProj.CHtoWGSlat(right, top));

                        boundary.setattrvalue("westlimit", westlimit);
                        boundary.setattrvalue("southlimit", southlimit);
                        boundary.setattrvalue("eastlimit", eastlimit);
                        boundary.setattrvalue("northlimit", northlimit);
                        iomObj.addattrobj("boundary", boundary);

                        Iom_jObject title = new Iom_jObject("DatasetIdx16.MultilingualText", null);
                        Iom_jObject titleLocalisedText = new Iom_jObject("DatasetIdx16.LocalisedText", null);
                        titleLocalisedText.setattrvalue("Language", "de");
                        titleLocalisedText.setattrvalue("Text", themePublication.getTitle());
                        title.addattrobj("LocalisedText", titleLocalisedText);
                        iomObj.addattrobj("title", title);
                        
                        Iom_jObject shortDescription = new Iom_jObject("DatasetIdx16.MultilingualMText", null);
                        Iom_jObject localisedMTextshortDescription = new Iom_jObject("DatasetIdx16.LocalisedMText", null);
                        localisedMTextshortDescription.setattrvalue("Language", "de");
                        localisedMTextshortDescription.setattrvalue("Text", "<![CDATA["+themePublication.getShortDescription()+"]]>");
                        shortDescription.addattrobj("LocalisedText", localisedMTextshortDescription);
                        iomObj.addattrobj("shortDescription", shortDescription);

                        if (themePublication.getKeywordsList() != null) iomObj.setattrvalue("keywords", String.join(",", themePublication.getKeywordsList()));
                        iomObj.setattrvalue("technicalContact", themePublication.getServicer().getOfficeAtWeb().toString());
                        if (themePublication.getFurtherInformation() != null) iomObj.setattrvalue("furtherInformation", themePublication.getFurtherInformation().toString());

                        if (themePublication.getServices() != null) {
                            for (ch.so.agi.meta2file.model.Service service : themePublication.getServices()) {
                                if (service.getType().equals(ServiceType.WMS)) {
                                    Iom_jObject knownWMS = new Iom_jObject("DatasetIdx16.WebService_", null);
                                    knownWMS.setattrvalue("value", service.getEndpoint().toString());
                                    iomObj.addattrobj("knownWMS", knownWMS);
                                } else if (service.getType().equals(ServiceType.WFS)) {
                                    Iom_jObject knownWFS = new Iom_jObject("DatasetIdx16.WebService_", null);
                                    knownWFS.setattrvalue("value", service.getEndpoint().toString());
                                    iomObj.addattrobj("knownWFS", knownWFS);
                                } else if (service.getType().equals(ServiceType.DATA)) {
                                    Iom_jObject furtherWS = new Iom_jObject("DatasetIdx16.WebService_", null);
                                    furtherWS.setattrvalue("value", service.getEndpoint().toString());
                                    iomObj.addattrobj("furtherWS", furtherWS);
                                } else if (service.getType().equals(ServiceType.WGC)) {
                                    Iom_jObject knownPortal = new Iom_jObject("DatasetIdx16.WebSite_", null);
                                    knownPortal.setattrvalue("value", service.getEndpoint().toString() + "?l=" + service.getLayers().get(0).getIdentifier());
                                    iomObj.addattrobj("knownPortal", knownPortal);
                                }
                            }    
                        }
                        
                        // Testeshalber nur XTF/ITF
                        for (FileFormat fileFormat : themePublication.getFileFormats()) {
                            if (fileFormat.getName().contains("INTERLIS")) {
                                Iom_jObject files = new Iom_jObject("DatasetIdx16.DataFile", null); 
                              
                                String fileExt;
                                String mimeType;
                                if(fileFormat.getName().equalsIgnoreCase("INTERLIS 1")) {
                                    mimeType = "application/interlis+txt;version=1.0";
                                    fileExt = "itf";
                                } else {
                                    mimeType = "application/interlis+xml;version=2.3";
                                    fileExt = "xtf";
                                }
                                files.setattrvalue("fileFormat", mimeType);
                                
                                if (themePublication.getItems().size() > 1) {
                                    for (Item item : themePublication.getItems()) {
                                        Iom_jObject file = new Iom_jObject("DatasetIdx16.File", null);
                                        file.setattrvalue("path", "files/"+item.getIdentifier() + "." + themePublication.getIdentifier()+"."+fileExt);
                                        files.addattrobj("file", file);
                                    }
                                } else  {
                                    Iom_jObject file = new Iom_jObject("DatasetIdx16.File", null);
                                    file.setattrvalue("path", "files/"+themePublication.getIdentifier()+"."+fileExt);
                                    files.addattrobj("file", file);

                                }
                                iomObj.addattrobj("files", files);
                            }
                        }

                        log.debug("ilidata object id: {}", iomObj.getobjectoid());
                        ioxWriter.write(new ObjectEvent(iomObj));
                        
                    } else {
                        log.debug("Skipping: {}. Reason: raster data.", themePublication.getIdentifier());
                    }

                }
            }
        }

        ioxWriter.write(new EndBasketEvent());
        ioxWriter.write(new EndTransferEvent());
        ioxWriter.flush();
        ioxWriter.close();        
    }
    
    private IoxWriter createIlidataWriter() throws IOException, Ili2cFailure, IoxException {
        String ILIDATA16 = "DatasetIdx16.ili";
        
        String tmpdir = System.getProperty("java.io.tmpdir");
        File iliFile = Paths.get(tmpdir, ILIDATA16).toFile();
        InputStream resource = new ClassPathResource("ili/"+ILIDATA16).getInputStream();
        Files.copy(resource, iliFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        
        ArrayList<String> filev = new ArrayList<>(List.of(iliFile.getAbsolutePath()));
        TransferDescription td = Ili2c.compileIliFiles(filev, null);


        File dataFile = Paths.get(ilidataDir, "ilidata.xml").toFile();
        IoxWriter ioxWriter = new XtfWriter(dataFile, td);
        
        iliFile.delete();

        return ioxWriter;
    }    

}
