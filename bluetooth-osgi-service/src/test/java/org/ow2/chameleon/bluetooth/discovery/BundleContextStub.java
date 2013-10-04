/*
 * Copyright 2013 OW2 Chameleon
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ow2.chameleon.bluetooth.discovery;

import org.easymock.EasyMock;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

public class BundleContextStub implements BundleContext {

    private HashMap<Object, Dictionary> m_services = new HashMap<Object, Dictionary>();

    public Map<Object, Dictionary> getServices() {
        return m_services;
    }

    @Override
    public String getProperty(String key) {
        return null;
    }



    @Override
    public Bundle installBundle(String location) throws BundleException {
        return null;
    }

    @Override
    public Bundle installBundle(String location, InputStream input)
            throws BundleException {
        return null;
    }

    @Override
    public Bundle getBundle() {
        return null;
    }

    @Override
    public Bundle getBundle(long id) {
        return null;
    }

    @Override
    public Bundle getBundle(String location) {
        return null;
    }

    @Override
    public Bundle[] getBundles() {
        return null;
    }

    @Override
    public void addServiceListener(ServiceListener listener, String filter)
            throws InvalidSyntaxException {

    }

    @Override
    public void addServiceListener(ServiceListener listener) {

    }

    @Override
    public void removeServiceListener(ServiceListener listener) {

    }

    @Override
    public void addBundleListener(BundleListener listener) {

    }

    @Override
    public void removeBundleListener(BundleListener listener) {

    }

    @Override
    public void addFrameworkListener(FrameworkListener listener) {

    }

    @Override
    public void removeFrameworkListener(FrameworkListener listener) {

    }

    @Override
    public <S> ServiceRegistration<S> registerService(Class<S> clazz, S service, Dictionary<String, ?> properties) {
        m_services.put(service, properties);
        return EasyMock.createMock(ServiceRegistration.class);
    }

    @Override
    public ServiceRegistration registerService(String[] clazzes,
                                               Object service, Dictionary properties) {
        m_services.put(service, properties);
        return EasyMock.createMock(ServiceRegistration.class);
    }

    @Override
    public ServiceRegistration registerService(String clazz, Object service,
                                               Dictionary properties) {
        m_services.put(service, properties);
        return EasyMock.createMock(ServiceRegistration.class);
    }

    @Override
    public ServiceReference[] getServiceReferences(String clazz, String filter)
            throws InvalidSyntaxException {
        return null;
    }

    @Override
    public ServiceReference[] getAllServiceReferences(String clazz,
                                                      String filter) throws InvalidSyntaxException {
        return null;
    }

    @Override
    public ServiceReference getServiceReference(String clazz) {
        return null;
    }

    @Override
    public <S> ServiceReference<S> getServiceReference(Class<S> clazz) {
        return null;
    }

    @Override
    public <S> Collection<ServiceReference<S>> getServiceReferences(Class<S> clazz, String filter) throws InvalidSyntaxException {
        return null;
    }

    @Override
    public <S> S getService(ServiceReference<S> reference) {
        return null;
    }

    @Override
    public boolean ungetService(ServiceReference reference) {
        return false;
    }

    @Override
    public File getDataFile(String filename) {
        return null;
    }

    @Override
    public Filter createFilter(String filter) throws InvalidSyntaxException {
        return null;
    }



}
