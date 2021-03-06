/**
 * H2GIS is a library that brings spatial support to the H2 Database Engine
 * <http://www.h2database.com>. H2GIS is developed by CNRS
 * <http://www.cnrs.fr/>.
 *
 * This code is part of the H2GIS project. H2GIS is free software; 
 * you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation;
 * version 3.0 of the License.
 *
 * H2GIS is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details <http://www.gnu.org/licenses/>.
 *
 *
 * For more information, please consult: <http://www.h2gis.org/>
 * or contact directly: info_at_h2gis.org
 */

package org.h2gis.sfs.osgi;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.h2gis.api.Function;
import org.h2gis.sfs.CreateSpatialExtension;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keep connection open and track arrival and departure of h2spatial OSGi functions
 * @author Nicolas Fortin
 */
public class FunctionTracker extends ServiceTracker<Function, Function> {
    private DataSource dataSource;
    private static final Logger LOGGER = LoggerFactory.getLogger(FunctionTracker.class);

    /**
     * Constructor
     * @param dataSource Active datasource
     * @param bundleContext BundleContext
     * @throws SQLException
     */
    public FunctionTracker(DataSource dataSource, BundleContext bundleContext) throws SQLException {
        super(bundleContext,Function.class,null);
        this.dataSource = dataSource;
    }

    @Override
    public void open() {
        super.open();
    }

    @Override
    public Function addingService(ServiceReference<Function> reference) {
        // Do not register system functions (h2spatial functions) because it should already be done through initialisation.
        if(reference.getBundle().getBundleId() != context.getBundle().getBundleId()) {
            Function function = super.addingService(reference);
            try {
                Connection connection = dataSource.getConnection();
                try {
                    CreateSpatialExtension.registerFunction(connection.createStatement(), function, ""); //bundle.getSymbolicName() + ":" + bundle.getVersion().toString() + ":"
                } finally {
                    connection.close();
                }
            } catch (SQLException ex) {
                LOGGER.error(ex.getLocalizedMessage(), ex);
            }
            return function;
        } else {
            return super.addingService(reference);
        }
    }

    @Override
    public void removedService(ServiceReference<Function> reference, Function service) {
        // Do not unregister system functions (h2spatial functions)
        if(reference.getBundle().getBundleId() != context.getBundle().getBundleId()) {
            try {
                Connection connection = dataSource.getConnection();
                try {
                    CreateSpatialExtension.unRegisterFunction(connection.createStatement(), service);
                } finally {
                    connection.close();
                }
            } catch (SQLException ex) {
                LOGGER.error(ex.getLocalizedMessage(), ex);
            }
            super.removedService(reference, service);
        }
    }
}
