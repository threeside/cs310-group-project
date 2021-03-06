package tas_fa20;

import java.util.*;
import java.sql.*;
import java.time.LocalTime;

public class TASDatabase {
    
        Connection conn = null;
        PreparedStatement pstSelect = null, pstUpdate = null;
        ResultSet resultSet = null;
        ResultSetMetaData metaData = null;

        String query;

        boolean hasresults;
        int resultCount, columnCount = 0;
        
    public TASDatabase() {
        
        try {

            String server = ("jdbc:mysql://localhost/tas");
            String username = "tasuser";
            String password = "CS488";

            Class.forName("com.mysql.cj.jdbc.Driver").newInstance();

            conn = DriverManager.getConnection(server, username, password);
            
        } catch (Exception e) {
            System.err.println(e.toString());
        }
        
    }
    
    /**
     * 
     * @param punchID
     * @return 
     */
    public Punch getPunch(int punchID) {
        
        try {
            
            query = "SELECT id, terminalid, badgeid, punchtypeid,"
                    + " UNIX_TIMESTAMP(originaltimestamp)*1000 AS originaltimestamp_unix_mili"
                    + " FROM punch WHERE id = ?";
            pstSelect = conn.prepareStatement(query);
            pstSelect.setInt(1, punchID);
            
            resultSet = pstSelect.executeQuery();
            
            if (resultSet.next()) {
        
                HashMap byteResults = new HashMap<String, Byte>();
                
                byteResults.put("terminalID", (byte)resultSet.getShort("terminalid"));
                byteResults.put("punchTypeID", (byte)resultSet.getShort("punchtypeiD"));
                
                return new Punch(
                        resultSet.getInt("id"),
                        resultSet.getString("badgeid"),
                        byteResults,
                        resultSet.getLong("originaltimestamp_unix_mili")
                );
                
            }
            
            else throw new Exception(
                    "Query unsuccessful: punch entry with ID `" + punchID
                    + "` either does not exist or the database has failed."
            );
            
        } catch (Exception e) {
            System.err.println(e.toString());
        }
        
        return null;
        
    }
    
    /**
     * Method for retrieving badge info and populating a Badge object from a
     * given badge ID
     * @param badgeID ID of the given badge
     * @return Badge object representing the badge info linked to the given
     * badge ID
     */
    public Badge getBadge(String badgeID) {
        
        try {
            
            query = "SELECT id, description FROM badge WHERE id = ?";
            pstSelect = conn.prepareStatement(query);
            pstSelect.setString(1, badgeID);
            
            resultSet = pstSelect.executeQuery();
            
            if (resultSet.next())
            {
                
                return new Badge(
                        resultSet.getString("id"),
                        resultSet.getString("description")
                );
                
            }
            
            else throw new Exception(
                    "Query unsuccessful: badge entry with ID `" + badgeID
                    + "` either does not exist or the database has failed."
            );
            
        } catch (Exception e) {
            System.err.println(e.toString());
        }
        
        return null;
        
    }
    
    /**
     * Overloaded method to allow Integer-based input to passed on to
     * getShift(byte)
     * @param shiftID ID of the given shift type; assumed to be 8-bit
     * @return Shift object representing the shift type linked to the given
     * shift ID
     */
    public Shift getShift(int shiftID) {
        return getShift((byte)shiftID);
    }
    
    /**
     * Method for retrieving shift type and populating a Shift object based upon
     * a given shift type ID
     * @param shiftID ID of the shift type to retrieve
     * @return Shift object representing the shift type linked to the given
     * shift ID
     */
    private Shift getShift(byte shiftID) {
        
        try {
            
            query = "SELECT * FROM shift WHERE id = ?";
            pstSelect = conn.prepareStatement(query);
            pstSelect.setByte(1, shiftID);
            
            resultSet = pstSelect.executeQuery();
            
            if (resultSet.next()) return populateShift(resultSet);
            
            else throw new Exception(
                    "Query unsuccessful: shift entry with ID `" + shiftID
                    + "` either does not exist or the database has failed."
            );
            
        } catch (Exception e) {
            System.err.println(e.toString());
        }
        
        return null;
        
    }
    
    /**
     * Method for retrieving shift type and populating a Shift object based upon
     * a given Badge; the Badge object's ID is checked against employees who
     * possess a matching ID, from which a shift type is determined
     * @param badge Badge object to derive a employee's Badge ID from
     * @return Shift object representing the shift type for the employee
     * possessing the given Badge
     */
    public Shift getShift(Badge badge) {
        
        try {
            
            query = "SELECT shift.* FROM shift LEFT JOIN employee ON "
                    + "employee.shiftid=shift.id WHERE employee.badgeid = ?";
            pstSelect = conn.prepareStatement(query);
            pstSelect.setString(1, badge.getID());
            
            resultSet = pstSelect.executeQuery();
            
            if (resultSet.next()) return populateShift(resultSet);
            
            else throw new Exception(
                    "Query unsuccessful: either no employee with badge ID `" + badge.getID()
                    + "` exists, or or the database failed."
            );
            
        } catch (Exception e) {
            System.err.println(e.toString());
        }
        
        return null;
        
    }
    
    /**
     * Generic method designed to populate a Shift object from a "SELECT * FROM
     * shift"-derived ResultSet
     * @param result Query result containing shift information
     * @return New Shift object containing the ResultSet's data, if the
     * ResultSet contains proper information; null otherwise
     */
    private Shift populateShift(ResultSet result) {
        
        HashMap byteResults = new HashMap<String, Byte>();
        HashMap localTimeResults = new HashMap<String, LocalTime>();
        
        try {
            byteResults.put("id", (byte)result.getShort("id"));
            byteResults.put("interval", (byte)result.getShort("interval"));
            byteResults.put("gracePeriod", (byte)result.getShort("graceperiod"));
            byteResults.put("dock", (byte)result.getShort("dock"));

            localTimeResults.put("start", result.getTime("start").toLocalTime());
            localTimeResults.put("stop", result.getTime("stop").toLocalTime());
            localTimeResults.put("lunchStart", result.getTime("lunchstart").toLocalTime());
            localTimeResults.put("lunchStop", result.getTime("lunchstop").toLocalTime());

            return new Shift(
                byteResults,
                localTimeResults,
                result.getString("description"),
                result.getShort("lunchdeduct")
            );
            
        } catch (Exception e) {
            System.err.println(e.toString());
        }
        
        return null;
        
    }
    
    public void close() throws SQLException {
        try {
            
            conn.close();
            
            if (resultSet != null) {
                    try {
                        resultSet.close();
                        resultSet = null;
                    } catch (Exception e) {
                    }
                }

                if (pstSelect != null) {
                    try {
                        pstSelect.close();
                        pstSelect = null;
                    } catch (Exception e) {
                    }
                }

                if (pstUpdate != null) {
                    try {
                        pstUpdate.close();
                        pstUpdate = null;
                    } catch (Exception e) {
                    }
                }

            } catch (Exception e) {
            System.err.println(e.toString());
            
        }
    }
}


    
