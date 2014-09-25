/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.fusepool.deduplication.transformer;

import eu.fusepool.deduplication.transformer.SilkConfigFileParser;
import eu.fusepool.deduplication.transformer.FileUtil;
import eu.fusepool.deduplication.utm2wgs84.RdfCoordinatesConverter;
import eu.fusepool.p3.transformer.HttpRequestEntity;
import eu.fusepool.p3.transformer.RdfGeneratingTransformer;
import de.fuberlin.wiwiss.silk.Silk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.utils.smushing.SameAsSmusher;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DuplicatesTransformer extends RdfGeneratingTransformer {

    final String BASE_URI = "http://example.org/";
    private String configFileName;
    
    public DuplicatesTransformer(String configFilename){
    	this.configFileName = configFilename;
    }

    private static final Logger log = LoggerFactory.getLogger(DuplicatesTransformer.class);

    @Override
    public Set<MimeType> getSupportedInputFormats() {
        try {
            MimeType mimeType = new MimeType("text/plain;charset=UTF-8");
            return Collections.singleton(mimeType);
        } catch (MimeTypeParseException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    protected TripleCollection generateRdf(HttpRequestEntity entity) throws IOException {
        final InputStream inputRdfData = entity.getData();
        TripleCollection duplicates = findDuplicates(inputRdfData);
        return duplicates;
    }

    protected TripleCollection findDuplicates(InputStream inputRdf) throws IOException {
    	File configFile = FileUtil.inputStreamToFile(getClass().getResourceAsStream(configFileName), "silk-config-", ".xml");
        File inputRdfFile = File.createTempFile("input-", ".ttl");
        File wgs84File = File.createTempFile("wgs84-", ".ttl");
        File outFile = File.createTempFile("output-", ".nt");
        
        //save the data coming from the stream into a temp file 
        FileOutputStream outputRdfStream = new FileOutputStream(inputRdfFile);
        IOUtils.copy(inputRdf, outputRdfStream);
        outputRdfStream.close();
        
        // convert utm coordinates in wgs84 and save the data in a temp file
        FileOutputStream convStream = new FileOutputStream(wgs84File);
        RdfCoordinatesConverter converter = new RdfCoordinatesConverter();
        converter.utm2wgs84(inputRdfFile, convStream);
        log.info("Finished conversion in WGS84");
        
        //update the silk config file with the paths of the target source and output files
        String configAbsolutePath = configFile.getAbsolutePath();
        SilkConfigFileParser parser = new SilkConfigFileParser(configAbsolutePath);
        parser.updateSourceFile(wgs84File.getAbsolutePath());
        parser.updateOutputFile(outFile.getAbsolutePath());
		parser.saveChanges();

        // interlink entities
        Silk.executeFile(configFile, null, 1, true);
        log.info("Finished Silk Task");
        
        // returns the result to the client
        return parseResult(outFile.getAbsolutePath());
    }

    /**
     * Smushes the input RDF graph using the of equivalent links. Returns the
     * same graph replacing all the equivalent URIs with a preferred one adding
     * all the statements to it.
     *
     * @param inputRdfData
     * @param duplicates
     * @return
     */
    protected TripleCollection smushData(InputStream inputRdfData, TripleCollection duplicates) {
        MGraph inputGraph = new SimpleMGraph();
        Parser parser = Parser.getInstance();
        parser.parse(inputGraph, inputRdfData, SupportedFormat.TURTLE, null);
        SameAsSmusher smusher = new SameAsSmusher() {
            @Override
            protected UriRef getPreferedIri(Set<UriRef> uriRefs) {
                UriRef preferedIri = null;
                Set<UriRef> canonUris = new HashSet<UriRef>();
                for (UriRef uriRef : uriRefs) {
                    if (uriRef.getUnicodeString().startsWith(BASE_URI)) {
                        canonUris.add(uriRef);
                    }
                }
                if (canonUris.size() > 0) {
                    preferedIri = canonUris.iterator().next();
                }
                if (canonUris.size() == 0) {
                    preferedIri = uriRefs.iterator().next();
                }
                return preferedIri;
            }
        };

        //smusher.smush(inputGraph, duplicates, true); //remove the use of a LockableMGraph
        return inputGraph;
    }

    /**
     * Reads the silk output (n-triples) and returns the owl:sameas statements
     * as a result
     *
     * @param fileName
     * @return
     * @throws IOException
     */
    public TripleCollection parseResult(String fileName) throws IOException {
    	Parser parser = Parser.getInstance();
        return parser.parse(new FileInputStream(fileName), SupportedFormat.N_TRIPLE);
    }

    @Override
    public boolean isLongRunning() {
        // TODO Auto-generated method stub
        return false;
    }

}
