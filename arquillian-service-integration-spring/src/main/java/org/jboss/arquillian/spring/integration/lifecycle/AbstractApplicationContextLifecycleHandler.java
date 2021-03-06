/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.arquillian.spring.integration.lifecycle;

import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.ServiceLoader;
import org.jboss.arquillian.spring.integration.context.ApplicationContextDestroyer;
import org.jboss.arquillian.spring.integration.context.ApplicationContextProducer;
import org.jboss.arquillian.spring.integration.context.TestScopeApplicationContext;
import org.jboss.arquillian.spring.integration.event.ApplicationContextCreatedEvent;
import org.jboss.arquillian.spring.integration.event.ApplicationContextDestroyedEvent;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.arquillian.test.spi.event.suite.AfterClass;
import org.jboss.arquillian.test.spi.event.suite.BeforeClass;

import java.util.Collection;
import java.util.List;

/**
 * <p>The application context destroyer, which is responsible for </p>
 *
 * @param <TProd> the type of {@link ApplicationContextProducer} that can provide specific type
 * @param <T>     the type of application context instance
 *
 * @author <a href="mailto:jmnarloch@gmail.com">Jakub Narloch</a>
 */
public abstract class AbstractApplicationContextLifecycleHandler<TProd extends ApplicationContextProducer<T>,
        T extends TestScopeApplicationContext> {

    /**
     * <p>Represents the instance of {@link ServiceLoader}.</p>
     */
    @Inject
    private Instance<ServiceLoader> serviceLoaderInstance;

    /**
     * <p>Represents the application context created event.</p>
     */
    @Inject
    private Event<ApplicationContextCreatedEvent> applicationContextCreatedEvent;

    /**
     * <p>Represents the application context created event.</p>
     */
    @Inject
    private Event<ApplicationContextDestroyedEvent> applicationContextDestroyedEvent;

    /**
     * <p>The before class event handler.</p>
     *
     * <p>This method delegates to the registered {@link ApplicationContextProducer} instances in order to create the
     * application context.</p>
     */
    public void beforeClass(@Observes BeforeClass event) {

        // creates the application context instance
        T applicationContext = createApplicationContext(event.getTestClass());

        if (applicationContext != null) {

            // triggers the application context created event
            applicationContextCreatedEvent.fire(new ApplicationContextCreatedEvent(applicationContext));

            setApplicationContext(applicationContext);
        }
    }

    /**
     * <p>The after class event handler.</p>
     *
     * <p>This method delegates to the registered {@link ApplicationContextDestroyer} instances in order to destroy the
     * application context.</p>
     */
    public void afterClass(@Observes AfterClass event) {

        T applicationContext = getApplicationContext();

        if (applicationContext != null) {
            destroyApplicationContext(applicationContext);

            // triggers the application context destroyed event
            applicationContextDestroyedEvent.fire(new ApplicationContextDestroyedEvent(applicationContext));
        }
    }

    /**
     * Retrieves the application context.
     *
     * @return the application context
     */
    protected abstract T getApplicationContext();

    /**
     * Sets the application context instance.
     *
     * @param applicationContext the application context instance
     */
    protected abstract void setApplicationContext(T applicationContext);

    /**
     * Retrieves the {@link ApplicationContextProducer} instance.
     *
     * @return the {@link ApplicationContextProducer} instance
     */
    protected abstract Class<TProd> getProducerClass();

    /**
     * Creates the application context.
     *
     * @param testClass the test class
     *
     * @return the application context
     */
    private T createApplicationContext(TestClass testClass) {

        ServiceLoader serviceLoader = serviceLoaderInstance.get();

        // retrieves the list of all registered application context producers
        Collection<TProd> applicationContextProducers = serviceLoader.all(getProducerClass());

        for (TProd applicationContextProducer : applicationContextProducers) {

            if (applicationContextProducer.supports(testClass)) {

                return applicationContextProducer.createApplicationContext(testClass);
            }
        }

        // the given test is not supported
        return null;
    }

    /**
     * Destroys the application context.
     *
     * @param applicationContext the application context to be destroyed
     */
    private void destroyApplicationContext(TestScopeApplicationContext applicationContext) {

        // single service is expected
        getService(ApplicationContextDestroyer.class).destroyApplicationContext(applicationContext);
    }

    /**
     * Retrieves the service by it's type. <p/> The implementation uses {@link ServiceLoader} in order to retrieve first
     * instance of specified kind. Any other registered instance in given scope is being discarded.
     *
     * @param clazz the class of the service
     * @param <T>   the service type
     *
     * @return the service instance
     */
    private <T> T getService(Class<T> clazz) {

        Collection<T> collection = serviceLoaderInstance.get().all(clazz);
        return collection.iterator().next();
    }
}
