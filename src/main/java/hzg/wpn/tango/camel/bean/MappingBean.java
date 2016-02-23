package hzg.wpn.tango.camel.bean;

import fr.esrf.TangoApi.PipeBlob;
import fr.esrf.TangoApi.PipeDataElement;
import hzg.wpn.xenv.ResourceManager;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangeProperty;
import org.apache.camel.Property;
import org.apache.camel.spi.UriParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/23/16
 */
public class MappingBean {
    private final Logger logger = LoggerFactory.getLogger(MappingBean.class);

    public void applyMapping(Exchange exchange) throws Exception {
        //TODO optimize
        String fromRouteId = exchange.getFromRouteId();

        logger.debug("Loading mapping for {}", fromRouteId);
        Map<String,String> mapping = (Map<String,String>)(Map)ResourceManager.loadProperties("etc/CamelIntegration", fromRouteId + ".mapping");

        PipeBlob root = exchange.getIn().getBody(PipeBlob.class);

        for (Iterator<PipeDataElement> it = root.iterator(); it.hasNext(); ) {
            PipeBlob blob = it.next().extractPipeBlob();
            String attributeName = blob.get(0).extractStringArray()[0];//first blob is an attribute name. See StatusServer and DataFormat
            if (mapping.containsKey(attributeName)) {
                String nxPath = mapping.get(attributeName);
                logger.debug("Replacing {} -> {}", attributeName, nxPath);
                blob.get(0).extractStringArray()[0] = nxPath;
            } else {
                logger.warn("Missing mapping for {}", attributeName);
                it.remove();
            }
        }

    }

}
