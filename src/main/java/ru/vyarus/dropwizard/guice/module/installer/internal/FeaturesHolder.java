package ru.vyarus.dropwizard.guice.module.installer.internal;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import ru.vyarus.dropwizard.guice.module.installer.FeatureInstaller;
import ru.vyarus.dropwizard.guice.module.installer.order.OrderComparator;
import ru.vyarus.dropwizard.guice.module.installer.order.Ordered;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Bean used to hold found extensions (after scan with installers) to register them dropwizard after
 * injector creation.
 *
 * @author Vyacheslav Rusakov
 * @since 01.09.2014
 */
public class FeaturesHolder {
    private final List<FeatureInstaller> installers;
    private final List<Class<? extends FeatureInstaller>> installerTypes;
    private final Map<Class<? extends FeatureInstaller>, List<Class<?>>> features = Maps.newHashMap();

    public FeaturesHolder(final List<FeatureInstaller> installers) {
        this.installers = installers;
        this.installerTypes = Lists.transform(installers,
                new Function<FeatureInstaller, Class<? extends FeatureInstaller>>() {
                    @Override
                    public Class<? extends FeatureInstaller> apply(final FeatureInstaller input) {
                        return input.getClass();
                    }
                });
    }

    /**
     * @param installer installer type
     * @param feature   feature type to store
     */
    public void register(final Class<? extends FeatureInstaller> installer, final Class feature) {
        Preconditions.checkArgument(installerTypes.contains(installer), "Installer %s not registered",
                installer.getSimpleName());
        if (!features.containsKey(installer)) {
            features.put(installer, Lists.<Class<?>>newArrayList());
        }
        features.get(installer).add(feature);
    }

    /**
     * @return list of all registered installer instances
     */
    public List<FeatureInstaller> getInstallers() {
        return installers;
    }

    /**
     * @param installer installer type
     * @return list of all found extensions for installer or null if nothing found.
     */
    public List<Class<?>> getFeatures(final Class<? extends FeatureInstaller> installer) {
        return features.get(installer);
    }

    /**
     * Order extension according to {@link ru.vyarus.dropwizard.guice.module.installer.order.Order} annotation.
     * Installer must implement {@link ru.vyarus.dropwizard.guice.module.installer.order.Ordered} otherwise
     * no order appear.
     */
    public void order() {
        final OrderComparator comparator = new OrderComparator();
        for (Class<? extends FeatureInstaller> installer : installerTypes) {
            if (Ordered.class.isAssignableFrom(installer)) {
                final List<Class<?>> extensions = features.get(installer);
                if (extensions == null || extensions.size() <= 1) {
                    continue;
                }
                Collections.sort(extensions, comparator);
            }
        }
    }
}
