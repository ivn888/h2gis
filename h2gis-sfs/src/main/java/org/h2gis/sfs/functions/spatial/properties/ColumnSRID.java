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

package org.h2gis.sfs.functions.spatial.properties;

import org.h2.util.StringUtils;
import org.h2gis.api.AbstractFunction;
import org.h2gis.api.ScalarFunction;
import org.h2gis.utilities.SFSUtilities;
import org.h2gis.utilities.TableLocation;

import java.sql.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Get the column SRID from constraints and data.
 * @author Nicolas Fortin
 */
public class ColumnSRID extends AbstractFunction implements ScalarFunction {
    private static final String SRID_FUNC = ST_SRID.class.getSimpleName();
    private static final Pattern SRID_CONSTRAINT_PATTERN = Pattern.compile("ST_SRID\\s*\\(\\s*((([\"`][^\"`]+[\"`])|(\\w+)))\\s*\\)\\s*=\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    public ColumnSRID() {
        addProperty(PROP_REMARKS, "Get the column SRID from constraints and data.");
        addProperty(PROP_NAME, "_ColumnSRID");
    }

    @Override
    public String getJavaStaticMethod() {
        return "getSRID";
    }

    /**
     *
     * @param constraint Constraint expression ex:"ST_SRID(the_geom) = 27572"
     * @return The SRID or 0 if no constraint are found or constraint on other column
     */
    public static int getSRIDFromConstraint(String constraint, String columnName) {
        int srid = 0;
        Matcher matcher = SRID_CONSTRAINT_PATTERN.matcher(constraint);
        while (matcher.find()) {
            String extractedColumnName = matcher.group(1).replace("\"","").replace("`","");
            int sridConstr = Integer.valueOf(matcher.group(5));
            if (extractedColumnName.equalsIgnoreCase(columnName)) {
                if(srid != 0 && srid != sridConstr) {
                    // Two srid constraint on the same column
                    return 0;
                }
                srid = sridConstr;
            }
        }
        return srid;
    }

    /**
     * Read table constraints from database metadata.
     * @param connection Active connection
     * @param catalogName Catalog name or empty string
     * @param schemaName Schema name or empty string
     * @param tableName table name
     * @return Found table constraints
     * @throws SQLException
     */
    public static String fetchConstraint(Connection connection, String catalogName, String schemaName, String tableName) throws SQLException {
        // Merge column constraint and table constraint
        PreparedStatement pst = SFSUtilities.prepareInformationSchemaStatement(connection, catalogName, schemaName,
                tableName, "INFORMATION_SCHEMA.CONSTRAINTS", "", "TABLE_CATALOG", "TABLE_SCHEMA","TABLE_NAME");
        ResultSet rsConstraint = pst.executeQuery();
        try {
            StringBuilder constraint = new StringBuilder();
            while (rsConstraint.next()) {
                String tableConstr = rsConstraint.getString("CHECK_EXPRESSION");
                if(tableConstr != null) {
                    constraint.append(tableConstr);
                }
            }
            return constraint.toString();
        } finally {
            rsConstraint.close();
            pst.close();
        }
    }


    /**
     * @param connection Active connection
     * @param tableName Target table name
     * @param columnName Spatial field name
     * @param constraint Column constraint
     * @return The column SRID from constraints and data.
     */
    public static int getSRID(Connection connection, String catalogName, String schemaName, String tableName, String columnName,String constraint) {
        try {
            Statement st = connection.createStatement();
            // Merge column constraint and table constraint
            constraint+=fetchConstraint(connection, catalogName, schemaName,tableName);
            if(constraint.toUpperCase().contains(SRID_FUNC)) {
                // Check constraint
                // Extract column and SRID constraint value
                // constraint = ".. ST_SRID(the_geom) = 27572 .."
                int srid = getSRIDFromConstraint(constraint, columnName);
                if(srid > 0) {
                    return srid;
                }
            }
            // Fetch the first geometry to find a stored SRID
            ResultSet rs = st.executeQuery(String.format("select ST_SRID(%s) from %s LIMIT 1;",
                    StringUtils.quoteJavaString(columnName.toUpperCase()),new TableLocation(catalogName, schemaName, tableName)));
            if(rs.next()) {
                int srid = rs.getInt(1);
                if(srid > 0) {
                    return srid;
                }
            }
            rs.close();
            // Unable to find a valid SRID
            return 0;
        } catch (SQLException ex) {
            return 0;
        }
    }
}
