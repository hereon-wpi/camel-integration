package hzg.wpn.tango.camel;

import hzg.wpn.tango.camel.bean.MappingBean;
import hzg.wpn.xenv.ResourceManager;
import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.model.RoutesDefinition;
import org.tango.DeviceState;
import org.tango.server.ServerManager;
import org.tango.server.annotation.*;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/12/16
 */
@Device
public class CamelIntegration {
    @DeviceProperty(name = "TIK_TAK_SERVER")
    private String tikTakUri;

    public void setTikTakUri(String tikTakUri) {
        this.tikTakUri = tikTakUri;
    }

    private CamelContext camelContext;

    @Init
    @StateMachine(endState = DeviceState.ON)
    public void init() throws Exception {

        SimpleRegistry registry = new SimpleRegistry();
        registry.put("mapping", new MappingBean());


        camelContext = new DefaultCamelContext(registry);

        RoutesDefinition routeDefinition = camelContext.loadRoutesDefinition(
                ResourceManager.loadResource("etc/CamelIntegration","routes.xml"));

        //TODO set default error handler

        camelContext.addRouteDefinitions(routeDefinition.getRoutes());
    }

    @Command
    @StateMachine(endState = DeviceState.RUNNING)
    public void start() throws Exception {
        camelContext.start();
    }

    @Command
    @StateMachine(endState = DeviceState.ON)
    public void stop() throws Exception {
        camelContext.stop();
    }

    @Delete
    public void delete() throws Exception {
        stop();
    }

    public static void main(String[] args) {
        //TODO define in the Tango DB


        ServerManager.getInstance().start(args, CamelIntegration.class);
    }
}
