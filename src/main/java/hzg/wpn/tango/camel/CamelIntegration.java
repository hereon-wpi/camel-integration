package hzg.wpn.tango.camel;

import fr.esrf.Tango.DevFailed;
import hzg.wpn.xenv.ResourceManager;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Route;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RoutesDefinition;
import org.slf4j.MDC;
import org.tango.DeviceState;
import org.tango.server.ServerManager;
import org.tango.server.ServerManagerUtils;
import org.tango.server.annotation.*;
import org.tango.server.device.DeviceManager;
import org.tango.utils.DevFailedUtils;

import java.io.IOException;
import java.util.List;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 2/12/16
 */
@Device
public class CamelIntegration {
    @DeviceManagement
    private DeviceManager deviceManager;
    @State
    private volatile DeviceState state;
    @Status
    private volatile String status;

    public void setDeviceManager(DeviceManager deviceManager) {
        this.deviceManager = deviceManager;
    }

    private CamelContext camelContext;

    @Init
    @StateMachine(endState = DeviceState.ON)
    public void init() throws Exception {

        SimpleRegistry registry = new SimpleRegistry();


        camelContext = new DefaultCamelContext(registry);


        RoutesDefinition routeDefinition = camelContext.loadRoutesDefinition(
                ResourceManager.loadResource("etc/CamelIntegration","routes.xml"));

        routeDefinition.getRoutes().forEach(routeDefinition1 -> routeDefinition1.onException(DevFailed.class)
                .process(exchange -> {
                    MDC.setContextMap(deviceManager.getDevice().getMdcContextMap());
                    DevFailed exception = (DevFailed) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
                    setState(DeviceState.ALARM);
                    setStatus(DevFailedUtils.toString(exception));
                }));


        List<RouteDefinition> routes = routeDefinition.getRoutes();
        camelContext.addRouteDefinitions(routes);
    }

    @Attribute
    public String[] getRouteDefinitions(){
        return
                camelContext.getRouteDefinitions().stream().map(RouteDefinition::getId).toArray(String[]::new);
    }

    @Attribute
    public String[] getRoutes(){
        return
                camelContext.getRoutes().stream().map(Route::getId).toArray(String[]::new);
    }

    @Command
    public void start() throws Exception {
        try {
            camelContext.start();
            setState(DeviceState.RUNNING);
            setStatus("STARTED");
        } catch (Exception e) {
            setState(DeviceState.FAULT);
            setStatus(e.getMessage());
            throw e;
        }
    }

    @Command
    public void stop() throws Exception {
        try {
            camelContext.stop();
            setState(DeviceState.ON);
            setStatus("STOPPED");
        } catch (Exception e) {
            setState(DeviceState.FAULT);
            setStatus(e.getMessage());
            throw e;
        }
    }

    @Delete
    public void delete() throws Exception {
        stop();
    }

    public static void main(String[] args) throws IOException {
        ServerManager.getInstance().start(args, CamelIntegration.class);
        ServerManagerUtils.writePidFile(null);
    }

    public DeviceState getState() {
        return state;
    }

    public void setState(DeviceState state) {
        this.state = state;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
