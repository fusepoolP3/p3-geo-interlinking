/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.fusepool.deduplication.extractor;

import eu.fusepool.extractor.Entity;
import eu.fusepool.extractor.RdfGeneratingExtractor;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import org.apache.clerezza.rdf.core.BNode;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.rdf.ontologies.RDFS;
import org.apache.clerezza.rdf.ontologies.SIOC;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.commons.io.IOUtils;


public class LongRunningExtractor extends RdfGeneratingExtractor {

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
    protected TripleCollection generateRdf(Entity entity) throws IOException {
        try {
            Thread.sleep(5*1000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        final String text = IOUtils.toString(entity.getData(), "UTF-8");
        final TripleCollection result = new SimpleMGraph();
        final GraphNode node = new GraphNode(new BNode(), result);
        node.addProperty(RDF.type, new UriRef("http://example.org/ontology#TextDescription"));
        node.addPropertyValue(RDFS.comment, "This took a long while");
        node.addPropertyValue(SIOC.content, text);
        node.addPropertyValue(new UriRef("http://example.org/ontology#textLength"), text.length());
        return result;
    }

    @Override
    public boolean isLongRunning() {
        return true;
    }


    
}
