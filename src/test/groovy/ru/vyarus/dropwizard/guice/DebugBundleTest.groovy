package ru.vyarus.dropwizard.guice

import com.google.inject.Injector
import com.google.inject.ProvisionException
import io.dropwizard.Application
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import org.glassfish.hk2.api.MultiException
import org.glassfish.hk2.api.ServiceLocator
import org.glassfish.hk2.utilities.binding.AbstractBinder
import ru.vyarus.dropwizard.guice.module.installer.feature.jersey.HK2Managed
import ru.vyarus.dropwizard.guice.module.installer.feature.jersey.ResourceInstaller
import ru.vyarus.dropwizard.guice.module.installer.feature.jersey.provider.JerseyProviderInstaller
import ru.vyarus.dropwizard.guice.module.jersey.debug.HK2DebugBundle
import ru.vyarus.dropwizard.guice.module.jersey.debug.service.ContextDebugService
import ru.vyarus.dropwizard.guice.module.jersey.debug.service.WrongContextException
import ru.vyarus.dropwizard.guice.support.TestConfiguration
import ru.vyarus.dropwizard.guice.test.spock.UseDropwizardApp

import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider

/**
 * @author Vyacheslav Rusakov
 * @since 17.01.2016
 */
@UseDropwizardApp(DebugApp)
class DebugBundleTest extends AbstractTest {

    @Inject
    ContextDebugService debugService
    @Inject
    Injector injector
    @Inject
    javax.inject.Provider<ServiceLocator> locator;

    def "Check correct scopes"() {

        setup: "need to request hk resource to force instantiation"
        new URL("http://localhost:8080/hk/foo").getText()

        expect:
        debugService.guiceManaged as Set == [GuiceResource, GuiceMapper] as Set
        debugService.hkManaged as Set == [HkResource, HkMapper] as Set

    }

    def "Check detection"() {

        when: "forcing guice to create hk bean"
        injector.getInstance(HkResource)
        then: "detected"
        def ex = thrown(ProvisionException)
        ex.getCause() instanceof WrongContextException

        when: "force hk to create guice bean"
        locator.get().getService(GuiceResource, "test")
        then:
        ex = thrown(MultiException)
        ex.getErrors()[0] instanceof WrongContextException

    }

    static class DebugApp extends Application<TestConfiguration> {

        @Override
        void initialize(Bootstrap<TestConfiguration> bootstrap) {
            bootstrap.addBundle(GuiceBundle.builder()
                    .bundles(new HK2DebugBundle())
                    .disableBundleLookup()
                    .installers(ResourceInstaller, JerseyProviderInstaller)
                    .extensions(GuiceResource, HkResource, GuiceMapper, HkMapper)
                    .build())
        }

        @Override
        void run(TestConfiguration configuration, Environment environment) throws Exception {
            environment.jersey().register(new AbstractBinder() {
                @Override
                protected void configure() {
                    // debug bundle ignores qualifiers.. using it to instantiate wrong class
                    bind(GuiceResource).to(GuiceResource).named("test")
                }
            })
        }
    }

    @Path("/guice")
    static class GuiceResource {
        @Path("/foo")
        @GET
        String foo() {
            return ""
        }
    }

    @Path("/hk")
    @HK2Managed
    static class HkResource {
        @Path("/foo")
        @GET
        String foo() {
            return ""
        }
    }

    @Provider
    static class GuiceMapper implements ExceptionMapper<IOException> {
        @Override
        Response toResponse(IOException exception) {
            return null
        }
    }

    @Provider
    @HK2Managed
    static class HkMapper implements ExceptionMapper<IOException> {
        @Override
        Response toResponse(IOException exception) {
            return null
        }
    }
}