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

package org.h2gis.drivers.dbf;

import org.h2gis.api.AbstractFunction;
import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.api.ScalarFunction;
import org.h2gis.utilities.URIUtility;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author Nicolas Fortin
 */
public class DBFWrite  extends AbstractFunction implements ScalarFunction {

    public DBFWrite() {
        addProperty(PROP_REMARKS, "Transfer the content of a table into a DBF\n" +
                "CALL DBFWRITE('FILENAME', 'TABLE'[,'ENCODING'])");
    }

    @Override
    public String getJavaStaticMethod() {
        return "exportTable";  //To change body of implemented methods use File | Settings | File Templates.
    }

    public static void exportTable(Connection connection, String fileName, String tableReference) throws IOException, SQLException {
        DBFDriverFunction driverFunction = new DBFDriverFunction();
        driverFunction.exportTable(connection, tableReference, URIUtility.fileFromString(fileName), new EmptyProgressVisitor());
    }

    public static void exportTable(Connection connection, String fileName, String tableReference,String encoding) throws IOException, SQLException {
        DBFDriverFunction driverFunction = new DBFDriverFunction();
        driverFunction.exportTable(connection, tableReference, new File(fileName), new EmptyProgressVisitor(), encoding);
    }
}
